package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.generated.jooq.enums.UserIdentifierType
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ApiKeyRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ProjectRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.UserIdentifierRecord
import com.ampnet.blockchainapiservice.model.result.ApiKey
import com.ampnet.blockchainapiservice.testcontainers.SharedTestContainers
import com.ampnet.blockchainapiservice.util.BaseUrl
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import java.time.Duration
import java.util.UUID

@JooqTest
@Import(JooqApiKeyRepository::class)
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JooqApiKeyRepositoryIntegTest : TestBase() {

    companion object {
        private val PROJECT_ID = UUID.randomUUID()
        private val OWNER_ID = UUID.randomUUID()
        private const val API_KEY = "api-key"
    }

    @Suppress("unused")
    private val postgresContainer = SharedTestContainers.postgresContainer

    @Autowired
    private lateinit var repository: JooqApiKeyRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        postgresContainer.cleanAllDatabaseTables(dslContext)

        dslContext.executeInsert(
            UserIdentifierRecord(
                id = OWNER_ID,
                userIdentifier = "user-identifier",
                identifierType = UserIdentifierType.ETH_WALLET_ADDRESS
            )
        )

        dslContext.executeInsert(
            ProjectRecord(
                id = PROJECT_ID,
                ownerId = OWNER_ID,
                issuerContractAddress = ContractAddress("0"),
                baseRedirectUrl = BaseUrl("base-redirect-url"),
                chainId = ChainId(1337L),
                customRpcUrl = "custom-rpc-url",
                createdAt = TestData.TIMESTAMP
            )
        )
    }

    @Test
    fun mustCorrectlyFetchApiKeyById() {
        val id = UUID.randomUUID()

        suppose("some API key is stored in database") {
            dslContext.executeInsert(
                ApiKeyRecord(
                    id = id,
                    projectId = PROJECT_ID,
                    apiKey = API_KEY,
                    createdAt = TestData.TIMESTAMP
                )
            )
        }

        verify("API key is correctly fetched by ID") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(
                    ApiKey(
                        id = id,
                        projectId = PROJECT_ID,
                        apiKey = API_KEY,
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchApiKeyByValue() {
        val id = UUID.randomUUID()

        suppose("some API key is stored in database") {
            dslContext.executeInsert(
                ApiKeyRecord(
                    id = id,
                    projectId = PROJECT_ID,
                    apiKey = API_KEY,
                    createdAt = TestData.TIMESTAMP
                )
            )
        }

        verify("API key is correctly fetched by ID") {
            val result = repository.getByValue(API_KEY)

            assertThat(result).withMessage()
                .isEqualTo(
                    ApiKey(
                        id = id,
                        projectId = PROJECT_ID,
                        apiKey = API_KEY,
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }
    }

    @Test
    fun mustReturnNullWhenFetchingNonExistentApiKeyById() {
        verify("null is returned when fetching non-existent API key") {
            val result = repository.getById(UUID.randomUUID())

            assertThat(result).withMessage()
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyFetchAllApiKeysForSomeProject() {
        suppose("some API keys are stored in database") {
            dslContext.batchInsert(
                ApiKeyRecord(
                    id = UUID.randomUUID(),
                    projectId = PROJECT_ID,
                    apiKey = "api-key-1",
                    createdAt = TestData.TIMESTAMP
                ),
                ApiKeyRecord(
                    id = UUID.randomUUID(),
                    projectId = PROJECT_ID,
                    apiKey = "api-key-2",
                    createdAt = TestData.TIMESTAMP + Duration.ofSeconds(10L)
                )
            ).execute()
        }

        verify("API keys are correctly fetched by project ID") {
            val result = repository.getAllByProjectId(PROJECT_ID)

            assertThat(result.map { it.apiKey }).withMessage()
                .isEqualTo(
                    listOf("api-key-1", "api-key-2")
                )
        }
    }

    @Test
    fun mustCorrectlyDetermineIfApiKeyExists() {
        suppose("some API key is stored in database") {
            dslContext.executeInsert(
                ApiKeyRecord(
                    id = UUID.randomUUID(),
                    projectId = PROJECT_ID,
                    apiKey = API_KEY,
                    createdAt = TestData.TIMESTAMP
                )
            )
        }

        verify("must correctly determine that API key exists") {
            assertThat(repository.exists(API_KEY)).withMessage()
                .isTrue()
            assertThat(repository.exists("unknown-api-key")).withMessage()
                .isFalse()
        }
    }

    @Test
    fun mustCorrectlyStoreApiKey() {
        val id = UUID.randomUUID()
        val apiKey = ApiKey(
            id = id,
            projectId = PROJECT_ID,
            apiKey = API_KEY,
            createdAt = TestData.TIMESTAMP
        )

        val storedApiKey = suppose("API key is stored in database") {
            repository.store(apiKey)
        }

        verify("storing API key returns correct result") {
            assertThat(storedApiKey).withMessage()
                .isEqualTo(apiKey)
        }

        verify("API key was stored in database") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(apiKey)
        }
    }
}
