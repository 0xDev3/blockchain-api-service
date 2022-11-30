package dev3.blockchainapiservice.controller

import dev3.blockchainapiservice.JsonSchemaDocumentation
import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.blockchain.properties.Chain
import dev3.blockchainapiservice.exception.ResourceNotFoundException
import dev3.blockchainapiservice.model.response.ApiUsagePeriodResponse
import dev3.blockchainapiservice.model.result.ApiUsagePeriod
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.model.result.RequestUsage
import dev3.blockchainapiservice.model.result.UserWalletAddressIdentifier
import dev3.blockchainapiservice.repository.ApiRateLimitRepository
import dev3.blockchainapiservice.repository.ProjectRepository
import dev3.blockchainapiservice.service.UtcDateTimeProvider
import dev3.blockchainapiservice.util.BaseUrl
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.springframework.http.ResponseEntity
import java.time.Duration
import java.util.UUID

class ApiUsageControllerTest : TestBase() {

    companion object {
        private val USER_IDENTIFIER = UserWalletAddressIdentifier(
            id = UUID.randomUUID(),
            walletAddress = WalletAddress("a")
        )
        private val PROJECT = Project(
            id = UUID.randomUUID(),
            ownerId = USER_IDENTIFIER.id,
            issuerContractAddress = ContractAddress("155034"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = Chain.HARDHAT_TESTNET.id,
            customRpcUrl = "custom-rpc-url",
            createdAt = TestData.TIMESTAMP
        )
        private val API_USAGE_PERIOD = ApiUsagePeriod(
            projectId = PROJECT.id,
            writeRequestUsage = RequestUsage(0, 0),
            readRequestUsage = RequestUsage(1, 1),
            startDate = TestData.TIMESTAMP,
            endDate = TestData.TIMESTAMP + Duration.ofDays(30L)
        )
        private val RESPONSE = ResponseEntity.ok(
            ApiUsagePeriodResponse(
                projectId = PROJECT.id,
                writeRequestUsage = RequestUsage(0, 0),
                readRequestUsage = RequestUsage(1, 1),
                startDate = TestData.TIMESTAMP.value,
                endDate = (TestData.TIMESTAMP + Duration.ofDays(30L)).value
            )
        )
    }

    @Test
    fun mustCorrectlyGetCurrentApiUsageInfoForProject() {
        val projectRepository = mock<ProjectRepository>()

        suppose("some project is returned") {
            given(projectRepository.getById(PROJECT.id))
                .willReturn(PROJECT)
        }

        val apiRateLimitRepository = mock<ApiRateLimitRepository>()

        suppose("some api usage will be returned") {
            given(apiRateLimitRepository.getCurrentApiUsagePeriod(PROJECT.id, TestData.TIMESTAMP))
                .willReturn(API_USAGE_PERIOD)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some timestamp will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val controller = ApiUsageController(projectRepository, apiRateLimitRepository, utcDateTimeProvider)

        verify("controller returns correct response") {
            val response = controller.getCurrentApiUsageInfoForProject(USER_IDENTIFIER, PROJECT.id)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(RESPONSE)
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenUserIsNotProjectOwner() {
        val projectRepository = mock<ProjectRepository>()

        suppose("some project is returned") {
            given(projectRepository.getById(PROJECT.id))
                .willReturn(PROJECT.copy(ownerId = UUID.randomUUID()))
        }

        val controller = ApiUsageController(projectRepository, mock(), mock())

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                controller.getCurrentApiUsageInfoForProject(USER_IDENTIFIER, PROJECT.id)
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenProjectDoesNotExist() {
        val projectRepository = mock<ProjectRepository>()

        suppose("some project is returned") {
            given(projectRepository.getById(PROJECT.id))
                .willReturn(null)
        }

        val controller = ApiUsageController(projectRepository, mock(), mock())

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                controller.getCurrentApiUsageInfoForProject(USER_IDENTIFIER, PROJECT.id)
            }
        }
    }

    @Test
    fun mustCorrectlyGetCurrentApiUsageInfoForApiKey() {
        val apiRateLimitRepository = mock<ApiRateLimitRepository>()

        suppose("some api usage will be returned") {
            given(apiRateLimitRepository.getCurrentApiUsagePeriod(PROJECT.id, TestData.TIMESTAMP))
                .willReturn(API_USAGE_PERIOD)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some timestamp will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val controller = ApiUsageController(mock(), apiRateLimitRepository, utcDateTimeProvider)

        verify("controller returns correct response") {
            val response = controller.getCurrentApiUsageInfoForApiKey(PROJECT)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(RESPONSE)
        }
    }
}
