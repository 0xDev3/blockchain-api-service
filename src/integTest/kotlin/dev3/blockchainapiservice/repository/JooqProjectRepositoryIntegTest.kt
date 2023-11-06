package dev3.blockchainapiservice.repository

import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.exception.DuplicateIssuerContractAddressException
import dev3.blockchainapiservice.generated.jooq.enums.UserIdentifierType
import dev3.blockchainapiservice.generated.jooq.tables.records.ProjectRecord
import dev3.blockchainapiservice.generated.jooq.tables.records.UserIdentifierRecord
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.testcontainers.SharedTestContainers
import dev3.blockchainapiservice.util.BaseUrl
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import java.util.UUID

@JooqTest
@Import(JooqProjectRepository::class)
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JooqProjectRepositoryIntegTest : TestBase() {

    companion object {
        private val OWNER_ID = UUID.randomUUID()
        private val ISSUER_CONTRACT_ADDRESS = ContractAddress("1550e4")
        private val BASE_REDIRECT_URL = BaseUrl("base-redirect-url")
        private val CHAIN_ID = ChainId(1337L)
        private const val CUSTOM_RPC_URL = "custom-rpc-url"
    }

    @Suppress("unused")
    private val postgresContainer = SharedTestContainers.postgresContainer

    @Autowired
    private lateinit var repository: JooqProjectRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        postgresContainer.cleanAllDatabaseTables(dslContext)

        dslContext.executeInsert(
            UserIdentifierRecord(
                id = OWNER_ID,
                userIdentifier = USER_IDENTIFIER,
                identifierType = UserIdentifierType.ETH_WALLET_ADDRESS,
                stripeClientId = null
            )
        )
    }

    @Test
    fun mustCorrectlyFetchProjectById() {
        val id = UUID.randomUUID()

        suppose("some project is stored in database") {
            dslContext.executeInsert(
                ProjectRecord(
                    id = id,
                    ownerId = OWNER_ID,
                    issuerContractAddress = ISSUER_CONTRACT_ADDRESS,
                    baseRedirectUrl = BASE_REDIRECT_URL,
                    chainId = CHAIN_ID,
                    customRpcUrl = CUSTOM_RPC_URL,
                    createdAt = TestData.TIMESTAMP
                )
            )
        }

        verify("project is correctly fetched by ID") {
            val result = repository.getById(id)

            expectThat(result)
                .isEqualTo(
                    Project(
                        id = id,
                        ownerId = OWNER_ID,
                        issuerContractAddress = ISSUER_CONTRACT_ADDRESS,
                        baseRedirectUrl = BASE_REDIRECT_URL,
                        chainId = CHAIN_ID,
                        customRpcUrl = CUSTOM_RPC_URL,
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }
    }

    @Test
    fun mustReturnNullWhenFetchingNonExistentProjectById() {
        verify("null is returned when fetching non-existent project") {
            val result = repository.getById(UUID.randomUUID())

            expectThat(result)
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyFetchProjectByIssuer() {
        val id = UUID.randomUUID()

        suppose("some project is stored in database") {
            dslContext.executeInsert(
                ProjectRecord(
                    id = id,
                    ownerId = OWNER_ID,
                    issuerContractAddress = ISSUER_CONTRACT_ADDRESS,
                    baseRedirectUrl = BASE_REDIRECT_URL,
                    chainId = CHAIN_ID,
                    customRpcUrl = CUSTOM_RPC_URL,
                    createdAt = TestData.TIMESTAMP
                )
            )
        }

        verify("project is correctly fetched by issuer contract address") {
            val result = repository.getByIssuer(ISSUER_CONTRACT_ADDRESS, CHAIN_ID)

            expectThat(result)
                .isEqualTo(
                    Project(
                        id = id,
                        ownerId = OWNER_ID,
                        issuerContractAddress = ISSUER_CONTRACT_ADDRESS,
                        baseRedirectUrl = BASE_REDIRECT_URL,
                        chainId = CHAIN_ID,
                        customRpcUrl = CUSTOM_RPC_URL,
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }
    }

    @Test
    fun mustReturnNullWhenFetchingNonExistentProjectByIssuerContractAddress() {
        verify("null is returned when fetching non-existent project") {
            val result = repository.getByIssuer(ContractAddress("dead"), ChainId(0L))

            expectThat(result)
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyFetchAllProjectsForSomeUser() {
        suppose("some projects are stored in database") {
            dslContext.batchInsert(
                ProjectRecord(
                    id = UUID.randomUUID(),
                    ownerId = OWNER_ID,
                    issuerContractAddress = ContractAddress("a1"),
                    baseRedirectUrl = BASE_REDIRECT_URL,
                    chainId = CHAIN_ID,
                    customRpcUrl = CUSTOM_RPC_URL,
                    createdAt = TestData.TIMESTAMP
                ),
                ProjectRecord(
                    id = UUID.randomUUID(),
                    ownerId = OWNER_ID,
                    issuerContractAddress = ContractAddress("a2"),
                    baseRedirectUrl = BASE_REDIRECT_URL,
                    chainId = CHAIN_ID,
                    customRpcUrl = CUSTOM_RPC_URL,
                    createdAt = TestData.TIMESTAMP
                )
            ).execute()
        }

        verify("projects are correctly fetched by user ID") {
            val result = repository.getAllByOwnerId(OWNER_ID)

            expectThat(result.map { it.issuerContractAddress })
                .isEqualTo(
                    listOf(ContractAddress("a1"), ContractAddress("a2"))
                )
        }
    }

    @Test
    fun mustCorrectlyStoreProject() {
        val id = UUID.randomUUID()
        val project = Project(
            id = id,
            ownerId = OWNER_ID,
            issuerContractAddress = ISSUER_CONTRACT_ADDRESS,
            baseRedirectUrl = BASE_REDIRECT_URL,
            chainId = CHAIN_ID,
            customRpcUrl = CUSTOM_RPC_URL,
            createdAt = TestData.TIMESTAMP
        )

        val storedProject = suppose("project is stored in database") {
            repository.store(project)
        }

        verify("storing project returns correct result") {
            expectThat(storedProject)
                .isEqualTo(project)
        }

        verify("project was stored in database") {
            val result = repository.getById(id)

            expectThat(result)
                .isEqualTo(project)
        }
    }

    @Test
    fun mustThrowDuplicateIssuerContractAddressExceptionWhenStoringProjectWithDuplicateIssuerContractAddress() {
        suppose("project is stored in database") {
            repository.store(
                Project(
                    id = UUID.randomUUID(),
                    ownerId = OWNER_ID,
                    issuerContractAddress = ISSUER_CONTRACT_ADDRESS,
                    baseRedirectUrl = BASE_REDIRECT_URL,
                    chainId = CHAIN_ID,
                    customRpcUrl = CUSTOM_RPC_URL,
                    createdAt = TestData.TIMESTAMP
                )
            )
        }

        verify("storing project with duplicate issuer and chainId throws DuplicateIssuerContractAddressException") {
            expectThrows<DuplicateIssuerContractAddressException> {
                repository.store(
                    Project(
                        id = UUID.randomUUID(),
                        ownerId = OWNER_ID,
                        issuerContractAddress = ISSUER_CONTRACT_ADDRESS,
                        baseRedirectUrl = BASE_REDIRECT_URL,
                        chainId = CHAIN_ID,
                        customRpcUrl = CUSTOM_RPC_URL,
                        createdAt = TestData.TIMESTAMP
                    )
                )
            }
        }
    }
}
