package dev3.blockchainapiservice.controller

import dev3.blockchainapiservice.ControllerTestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.blockchain.properties.Chain
import dev3.blockchainapiservice.config.ApplicationProperties
import dev3.blockchainapiservice.config.CustomHeaders
import dev3.blockchainapiservice.exception.ErrorCode
import dev3.blockchainapiservice.generated.jooq.enums.UserIdentifierType
import dev3.blockchainapiservice.generated.jooq.tables.records.ApiKeyRecord
import dev3.blockchainapiservice.generated.jooq.tables.records.ProjectRecord
import dev3.blockchainapiservice.generated.jooq.tables.records.UserIdentifierRecord
import dev3.blockchainapiservice.model.response.ApiUsagePeriodResponse
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.model.result.RequestUsage
import dev3.blockchainapiservice.security.WithMockUser
import dev3.blockchainapiservice.testcontainers.HardhatTestContainer
import dev3.blockchainapiservice.util.BaseUrl
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.util.UUID

class ApiUsageControllerApiTest : ControllerTestBase() {

    companion object {
        private val PROJECT_ID = UUID.randomUUID()
        private val OWNER_ID = UUID.randomUUID()
        private val PROJECT = Project(
            id = PROJECT_ID,
            ownerId = OWNER_ID,
            issuerContractAddress = ContractAddress("0"),
            baseRedirectUrl = BaseUrl("https://example.com/"),
            chainId = Chain.HARDHAT_TESTNET.id,
            customRpcUrl = null,
            createdAt = TestData.TIMESTAMP
        )
        private const val API_KEY = "api-key"
    }

    @Autowired
    private lateinit var applicationProperties: ApplicationProperties

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        postgresContainer.cleanAllDatabaseTables(dslContext)

        dslContext.executeInsert(
            UserIdentifierRecord(
                id = OWNER_ID,
                userIdentifier = WalletAddress(HardhatTestContainer.ACCOUNT_ADDRESS_1).rawValue,
                identifierType = UserIdentifierType.ETH_WALLET_ADDRESS
            )
        )

        dslContext.executeInsert(
            ProjectRecord(
                id = PROJECT.id,
                ownerId = PROJECT.ownerId,
                issuerContractAddress = PROJECT.issuerContractAddress,
                baseRedirectUrl = PROJECT.baseRedirectUrl,
                chainId = PROJECT.chainId,
                customRpcUrl = PROJECT.customRpcUrl,
                createdAt = PROJECT.createdAt
            )
        )

        dslContext.executeInsert(
            ApiKeyRecord(
                id = UUID.randomUUID(),
                projectId = PROJECT_ID,
                apiKey = API_KEY,
                createdAt = TestData.TIMESTAMP
            )
        )
    }

    @Test
    @WithMockUser
    fun mustCorrectlyFetchApiUsageForProject() {
        val response = suppose("request to API usage for project is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/api-usage/by-project/$PROJECT_ID")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ApiUsagePeriodResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    ApiUsagePeriodResponse(
                        projectId = PROJECT_ID,
                        writeRequestUsage = RequestUsage(
                            used = 0,
                            remaining = applicationProperties.apiRate.freeTierWriteRequests
                        ),
                        readRequestUsage = RequestUsage(
                            used = 0,
                            remaining = applicationProperties.apiRate.freeTierReadRequests
                        ),
                        startDate = response.startDate,
                        endDate = response.endDate
                    )
                )
        }
    }

    @Test
    fun mustReturn401UnauthorizedWhenFetchingApiUsageForProjectWithoutJwt() {
        verify("401 is returned for missing JWT") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/api-usage/by-project/$PROJECT_ID")
            )
                .andExpect(MockMvcResultMatchers.status().isUnauthorized)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.BAD_AUTHENTICATION)
        }
    }

    @Test
    @WithMockUser
    fun mustReturn404NotFoundWhenFetchingApiUsageForNonExistentProject() {
        verify("404 is returned for non-existent project") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/api-usage/by-project/${UUID.randomUUID()}")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    fun mustCorrectlyFetchApiUsageForApiKey() {
        val response = suppose("request to API usage for API key is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/api-usage")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ApiUsagePeriodResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    ApiUsagePeriodResponse(
                        projectId = PROJECT_ID,
                        writeRequestUsage = RequestUsage(
                            used = 0,
                            remaining = applicationProperties.apiRate.freeTierWriteRequests
                        ),
                        readRequestUsage = RequestUsage(
                            used = 0,
                            remaining = applicationProperties.apiRate.freeTierReadRequests
                        ),
                        startDate = response.startDate,
                        endDate = response.endDate
                    )
                )
        }
    }

    @Test
    fun mustReturn401UnauthorizedWhenFetchingApiUsageForInvalidApiKey() {
        verify("401 is returned for invalid API key") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/api-usage")
                    .header(CustomHeaders.API_KEY_HEADER, "invalid-api-key")
            )
                .andExpect(MockMvcResultMatchers.status().isUnauthorized)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.NON_EXISTENT_API_KEY)
        }
    }
}