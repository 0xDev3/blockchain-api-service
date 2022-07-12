package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.generated.jooq.enums.UserIdentifierType
import com.ampnet.blockchainapiservice.generated.jooq.tables.ApiKeyTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.ProjectTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.UserIdentifierTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ContractDeploymentRequestRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ProjectRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.UserIdentifierRecord
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.filters.AndList
import com.ampnet.blockchainapiservice.model.filters.ContractDeploymentRequestFilters
import com.ampnet.blockchainapiservice.model.filters.OrList
import com.ampnet.blockchainapiservice.model.params.StoreContractDeploymentRequestParams
import com.ampnet.blockchainapiservice.model.result.ContractDeploymentRequest
import com.ampnet.blockchainapiservice.testcontainers.PostgresTestContainer
import com.ampnet.blockchainapiservice.util.BaseUrl
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.ContractBinaryData
import com.ampnet.blockchainapiservice.util.ContractId
import com.ampnet.blockchainapiservice.util.ContractTag
import com.ampnet.blockchainapiservice.util.ContractTrait
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.Import
import java.util.UUID

@JooqTest
@Import(JooqContractDeploymentRequestRepository::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JooqContractDeploymentRequestRepositoryIntegTest : TestBase() {

    companion object {
        private val PROJECT_ID_1 = UUID.randomUUID()
        private val PROJECT_ID_2 = UUID.randomUUID()
        private val OWNER_ID = UUID.randomUUID()
        private val CONTRACT_ID = ContractId("contract-id")
        private val CONTRACT_DATA = ContractBinaryData("00")
        private val CHAIN_ID = ChainId(1337L)
        private const val REDIRECT_URL = "redirect-url"
        private val ARBITRARY_DATA = TestData.EMPTY_JSON_OBJECT
        private const val DEPLOY_SCREEN_BEFORE_ACTION_MESSAGE = "deploy-screen-before-action-message"
        private const val DEPLOY_SCREEN_AFTER_ACTION_MESSAGE = "deploy-screen-after-action-message"
        private val CONTRACT_ADDRESS = ContractAddress("1337")
        private val DEPLOYER_ADDRESS = WalletAddress("123")
        private val TX_HASH = TransactionHash("tx-hash")
    }

    @Suppress("unused")
    private val postgresContainer = PostgresTestContainer()

    @Autowired
    private lateinit var repository: JooqContractDeploymentRequestRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        dslContext.delete(ApiKeyTable.API_KEY).execute()
        dslContext.delete(ProjectTable.PROJECT).execute()
        dslContext.delete(UserIdentifierTable.USER_IDENTIFIER).execute()
//        dslContext.delete(ContractDeploymentRequestTable.CONTRACT_DEPLOYMENT_REQUEST).execute()

        dslContext.executeInsert(
            UserIdentifierRecord(
                id = OWNER_ID,
                userIdentifier = "user-identifier",
                identifierType = UserIdentifierType.ETH_WALLET_ADDRESS
            )
        )

        dslContext.executeInsert(
            ProjectRecord(
                id = PROJECT_ID_1,
                ownerId = OWNER_ID,
                issuerContractAddress = ContractAddress("0"),
                baseRedirectUrl = BaseUrl("base-redirect-url"),
                chainId = ChainId(1337L),
                customRpcUrl = "custom-rpc-url",
                createdAt = TestData.TIMESTAMP
            )
        )

        dslContext.executeInsert(
            ProjectRecord(
                id = PROJECT_ID_2,
                ownerId = OWNER_ID,
                issuerContractAddress = ContractAddress("1"),
                baseRedirectUrl = BaseUrl("base-redirect-url"),
                chainId = ChainId(1337L),
                customRpcUrl = "custom-rpc-url",
                createdAt = TestData.TIMESTAMP
            )
        )
    }

    @Test
    fun mustCorrectlyFetchContractDeploymentRequestById() {
        val id = UUID.randomUUID()
        val record = createRecord(id)

        suppose("some contract deployment request exists in database") {
            dslContext.executeInsert(record)
        }

        verify("contract deployment request is correctly fetched by ID") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(record.toModel())
        }
    }

    @Test
    fun mustReturnNullWhenFetchingNonExistentContractDeploymentRequestById() {
        verify("null is returned when fetching non-existent contract deployment request") {
            val result = repository.getById(UUID.randomUUID())

            assertThat(result).withMessage()
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyFetchContractDeploymentRequestsByProjectIdAndFilters() {
        fun uuid() = UUID.randomUUID()
        val project1ContractsWithMatchingCid = listOf(
            createRecord(id = uuid(), projectId = PROJECT_ID_1, ContractId("cid-1")),
            createRecord(id = uuid(), projectId = PROJECT_ID_1, ContractId("cid-2")),
            createRecord(id = uuid(), projectId = PROJECT_ID_1, ContractId("cid-3"))
        )
        val project1NonDeployedContractsWithMatchingCid = listOf(
            createRecord(id = uuid(), projectId = PROJECT_ID_1, ContractId("cid-1"), contractAddress = null),
            createRecord(id = uuid(), projectId = PROJECT_ID_1, ContractId("cid-2"), contractAddress = null),
            createRecord(id = uuid(), projectId = PROJECT_ID_1, ContractId("cid-3"), contractAddress = null)
        )
        val project1ContractsWithNonMatchingCid = listOf(
            createRecord(id = uuid(), projectId = PROJECT_ID_1, ContractId("ignored-cid")),
            createRecord(id = uuid(), projectId = PROJECT_ID_1, ContractId("ignored-cid"))
        )
        val project1ContractsWithMatchingTags = listOf(
            createRecord(id = uuid(), projectId = PROJECT_ID_1, contractTags = listOf("tag-1")),
            createRecord(id = uuid(), projectId = PROJECT_ID_1, contractTags = listOf("tag-2")),
            createRecord(id = uuid(), projectId = PROJECT_ID_1, contractTags = listOf("ignored-tag", "tag-2"))
        )
        val project1ContractsWithNonMatchingTags = listOf(
            createRecord(id = uuid(), projectId = PROJECT_ID_1, contractTags = listOf("ignored-tag")),
            createRecord(id = uuid(), projectId = PROJECT_ID_1, contractTags = listOf("ignored-tag"))
        )
        val project1ContractsWithMatchingTraits = listOf(
            createRecord(id = uuid(), projectId = PROJECT_ID_1, contractImplements = listOf("trait-1")),
            createRecord(id = uuid(), projectId = PROJECT_ID_1, contractImplements = listOf("trait-2")),
            createRecord(id = uuid(), projectId = PROJECT_ID_1, contractImplements = listOf("ignored-trait", "trait-2"))
        )
        val project1ContractsWithNonMatchingTraits = listOf(
            createRecord(id = uuid(), projectId = PROJECT_ID_1, contractImplements = listOf("ignored-trait")),
            createRecord(id = uuid(), projectId = PROJECT_ID_1, contractImplements = listOf("ignored-trait"))
        )

        val project2MatchingContracts = listOf(
            createRecord(
                id = uuid(),
                projectId = PROJECT_ID_2,
                ContractId("cid-1"),
                contractTags = listOf("tag-1", "tag-2"),
                contractImplements = listOf("trait-1", "trait-2")
            ),
            createRecord(
                id = uuid(),
                projectId = PROJECT_ID_2,
                ContractId("cid-2"),
                contractTags = listOf("tag-1", "tag-2"),
                contractImplements = listOf("trait-1", "trait-2")
            ),
            createRecord(
                id = uuid(),
                projectId = PROJECT_ID_2,
                ContractId("cid-3"),
                contractTags = listOf("ignored-tag", "tag-3"),
                contractImplements = listOf("ignored-trait", "trait-3")
            )
        )
        val project2NonMatchingContracts = listOf(
            createRecord(
                id = uuid(),
                projectId = PROJECT_ID_2,
                ContractId("cid-1"),
                contractTags = listOf("tag-1", "tag-2"),
                contractImplements = listOf("trait-1", "trait-2"),
                contractAddress = null
            ),
            createRecord(
                id = uuid(),
                projectId = PROJECT_ID_2,
                ContractId("ignored-cid"),
                contractTags = listOf("tag-1", "tag-2"),
                contractImplements = listOf("trait-1", "trait-2")
            ),
            createRecord(
                id = uuid(),
                projectId = PROJECT_ID_2,
                ContractId("cid-3"),
                contractTags = listOf("tag-1"),
                contractImplements = listOf("ignored-trait", "trait-1", "trait-2")
            ),
            createRecord(
                id = uuid(),
                projectId = PROJECT_ID_2,
                ContractId("cid-3"),
                contractTags = listOf("ignored-tag", "tag-1", "tag-2"),
                contractImplements = listOf("trait-1")
            )
        )

        suppose("some contract deployment requests exist in database") {
            dslContext.batchInsert(
                project1ContractsWithMatchingCid + project1NonDeployedContractsWithMatchingCid +
                    project1ContractsWithNonMatchingCid + project1ContractsWithMatchingTags +
                    project1ContractsWithNonMatchingTags + project1ContractsWithMatchingTraits +
                    project1ContractsWithNonMatchingTraits + project2MatchingContracts + project2NonMatchingContracts
            ).execute()
        }

        verify("must correctly fetch project 1 contracts with matching contract ID") {
            assertThat(
                repository.getAllByProjectId(
                    projectId = PROJECT_ID_1,
                    filters = ContractDeploymentRequestFilters(
                        contractIds = OrList(ContractId("cid-1"), ContractId("cid-2"), ContractId("cid-3")),
                        contractTags = OrList(),
                        contractImplements = OrList(),
                        deployedOnly = false
                    )
                )
            ).withMessage()
                .containsExactlyInAnyOrderElementsOf(
                    (project1ContractsWithMatchingCid + project1NonDeployedContractsWithMatchingCid)
                        .map { it.toModel() }
                )
        }

        verify("must correctly fetch project 1 deployed contracts with matching contract ID") {
            assertThat(
                repository.getAllByProjectId(
                    projectId = PROJECT_ID_1,
                    filters = ContractDeploymentRequestFilters(
                        contractIds = OrList(ContractId("cid-1"), ContractId("cid-2"), ContractId("cid-3")),
                        contractTags = OrList(),
                        contractImplements = OrList(),
                        deployedOnly = true
                    )
                )
            ).withMessage()
                .containsExactlyInAnyOrderElementsOf(project1ContractsWithMatchingCid.map { it.toModel() })
        }

        verify("must correctly fetch project 1 contracts with matching tags") {
            assertThat(
                repository.getAllByProjectId(
                    projectId = PROJECT_ID_1,
                    filters = ContractDeploymentRequestFilters(
                        contractIds = OrList(),
                        contractTags = OrList(
                            AndList(ContractTag("tag-1")),
                            AndList(ContractTag("tag-2"))
                        ),
                        contractImplements = OrList(),
                        deployedOnly = false
                    )
                )
            ).withMessage()
                .containsExactlyInAnyOrderElementsOf(project1ContractsWithMatchingTags.map { it.toModel() })
        }

        verify("must correctly fetch project 1 contracts with matching traits") {
            assertThat(
                repository.getAllByProjectId(
                    projectId = PROJECT_ID_1,
                    filters = ContractDeploymentRequestFilters(
                        contractIds = OrList(),
                        contractTags = OrList(),
                        contractImplements = OrList(
                            AndList(ContractTrait("trait-1")),
                            AndList(ContractTrait("trait-2"))
                        ),
                        deployedOnly = false
                    )
                )
            ).withMessage()
                .containsExactlyInAnyOrderElementsOf(project1ContractsWithMatchingTraits.map { it.toModel() })
        }

        verify("must correctly fetch project 2 contracts which match given filters") {
            assertThat(
                repository.getAllByProjectId(
                    projectId = PROJECT_ID_2,
                    filters = ContractDeploymentRequestFilters(
                        contractIds = OrList(ContractId("cid-1"), ContractId("cid-2"), ContractId("cid-3")),
                        contractTags = OrList(
                            AndList(ContractTag("tag-1"), ContractTag("tag-2")),
                            AndList(ContractTag("tag-3"))
                        ),
                        contractImplements = OrList(
                            AndList(ContractTrait("trait-1"), ContractTrait("trait-2")),
                            AndList(ContractTrait("trait-3"))
                        ),
                        deployedOnly = true
                    )
                )
            ).withMessage()
                .containsExactlyInAnyOrderElementsOf(project2MatchingContracts.map { it.toModel() })
        }
    }

    @Test
    fun mustCorrectlyStoreContractDeploymentRequest() {
        val id = UUID.randomUUID()
        val params = StoreContractDeploymentRequestParams(
            id = id,
            contractId = CONTRACT_ID,
            contractData = CONTRACT_DATA,
            contractTags = listOf(ContractTag("test-tag")),
            contractImplements = listOf(ContractTrait("test-trait")),
            deployerAddress = null,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            projectId = PROJECT_ID_1,
            createdAt = TestData.TIMESTAMP,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = DEPLOY_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = DEPLOY_SCREEN_AFTER_ACTION_MESSAGE
            )
        )

        val storedContractDeploymentRequest = suppose("contract deployment request is stored in database") {
            repository.store(params)
        }

        val expectedContractDeploymentRequest = ContractDeploymentRequest(
            id = id,
            contractId = CONTRACT_ID,
            contractData = CONTRACT_DATA,
            contractTags = listOf(ContractTag("test-tag")),
            contractImplements = listOf(ContractTrait("test-trait")),
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            projectId = PROJECT_ID_1,
            createdAt = TestData.TIMESTAMP,
            arbitraryData = ARBITRARY_DATA,
            screenBeforeActionMessage = DEPLOY_SCREEN_BEFORE_ACTION_MESSAGE,
            screenAfterActionMessage = DEPLOY_SCREEN_AFTER_ACTION_MESSAGE,
            contractAddress = null,
            deployerAddress = null,
            txHash = null
        )

        verify("storing contract deployment request returns correct result") {
            assertThat(storedContractDeploymentRequest).withMessage()
                .isEqualTo(expectedContractDeploymentRequest)
        }

        verify("contract deployment request was stored in database") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(expectedContractDeploymentRequest)
        }
    }

    @Test
    fun mustCorrectlySetTxInfoForContractDeploymentRequestWithNullTxHashAndContractAddress() {
        val id = UUID.randomUUID()
        val params = StoreContractDeploymentRequestParams(
            id = id,
            contractId = CONTRACT_ID,
            contractData = CONTRACT_DATA,
            contractTags = listOf(ContractTag("test-tag")),
            contractImplements = listOf(ContractTrait("test-trait")),
            deployerAddress = null,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            projectId = PROJECT_ID_1,
            createdAt = TestData.TIMESTAMP,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = DEPLOY_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = DEPLOY_SCREEN_AFTER_ACTION_MESSAGE
            )
        )

        suppose("contract deployment request is stored in database") {
            repository.store(params)
        }

        verify("setting txInfo will succeed") {
            assertThat(repository.setTxInfo(id, TX_HASH, CONTRACT_ADDRESS, DEPLOYER_ADDRESS)).withMessage()
                .isTrue()
        }

        verify("txInfo is correctly set in database") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(
                    ContractDeploymentRequest(
                        id = id,
                        contractId = CONTRACT_ID,
                        contractData = CONTRACT_DATA,
                        contractTags = listOf(ContractTag("test-tag")),
                        contractImplements = listOf(ContractTrait("test-trait")),
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        projectId = PROJECT_ID_1,
                        createdAt = TestData.TIMESTAMP,
                        arbitraryData = ARBITRARY_DATA,
                        screenBeforeActionMessage = DEPLOY_SCREEN_BEFORE_ACTION_MESSAGE,
                        screenAfterActionMessage = DEPLOY_SCREEN_AFTER_ACTION_MESSAGE,
                        contractAddress = CONTRACT_ADDRESS,
                        deployerAddress = DEPLOYER_ADDRESS,
                        txHash = TX_HASH
                    )
                )
        }
    }

    @Test
    fun mustNotUpdateDeployerAddressForContractDeploymentRequestWhenDeployerIsAlreadySet() {
        val id = UUID.randomUUID()
        val params = StoreContractDeploymentRequestParams(
            id = id,
            contractId = CONTRACT_ID,
            contractData = CONTRACT_DATA,
            contractTags = listOf(ContractTag("test-tag")),
            contractImplements = listOf(ContractTrait("test-trait")),
            deployerAddress = DEPLOYER_ADDRESS,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            projectId = PROJECT_ID_1,
            createdAt = TestData.TIMESTAMP,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = DEPLOY_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = DEPLOY_SCREEN_AFTER_ACTION_MESSAGE
            )
        )

        suppose("contract deployment request is stored in database") {
            repository.store(params)
        }

        verify("setting txInfo will succeed") {
            val ignoredDeployer = WalletAddress("f")
            assertThat(repository.setTxInfo(id, TX_HASH, CONTRACT_ADDRESS, ignoredDeployer)).withMessage()
                .isTrue()
        }

        verify("txHash was correctly set while contract deployer was not updated") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(
                    ContractDeploymentRequest(
                        id = id,
                        contractId = CONTRACT_ID,
                        contractData = CONTRACT_DATA,
                        contractTags = listOf(ContractTag("test-tag")),
                        contractImplements = listOf(ContractTrait("test-trait")),
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        projectId = PROJECT_ID_1,
                        createdAt = TestData.TIMESTAMP,
                        arbitraryData = ARBITRARY_DATA,
                        screenBeforeActionMessage = DEPLOY_SCREEN_BEFORE_ACTION_MESSAGE,
                        screenAfterActionMessage = DEPLOY_SCREEN_AFTER_ACTION_MESSAGE,
                        contractAddress = CONTRACT_ADDRESS,
                        deployerAddress = DEPLOYER_ADDRESS,
                        txHash = TX_HASH
                    )
                )
        }
    }

    @Test
    fun mustNotSetTxInfoForContractDeploymentRequestWhenTxHashAndContractAddressAreAlreadySet() {
        val id = UUID.randomUUID()
        val params = StoreContractDeploymentRequestParams(
            id = id,
            contractId = CONTRACT_ID,
            contractData = CONTRACT_DATA,
            contractTags = listOf(ContractTag("test-tag")),
            contractImplements = listOf(ContractTrait("test-trait")),
            deployerAddress = null,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            projectId = PROJECT_ID_1,
            createdAt = TestData.TIMESTAMP,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = DEPLOY_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = DEPLOY_SCREEN_AFTER_ACTION_MESSAGE
            )
        )

        suppose("contract deployment request is stored in database") {
            repository.store(params)
        }

        verify("setting txInfo will succeed") {
            assertThat(repository.setTxInfo(id, TX_HASH, CONTRACT_ADDRESS, DEPLOYER_ADDRESS)).withMessage()
                .isTrue()
        }

        verify("setting another txInfo will not succeed") {
            assertThat(
                repository.setTxInfo(
                    id = id,
                    txHash = TransactionHash("different-tx-hash"),
                    contractAddress = ContractAddress("dead"),
                    deployer = DEPLOYER_ADDRESS
                )
            ).withMessage()
                .isFalse()
        }

        verify("first txHash remains in database") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(
                    ContractDeploymentRequest(
                        id = id,
                        contractId = CONTRACT_ID,
                        contractData = CONTRACT_DATA,
                        contractTags = listOf(ContractTag("test-tag")),
                        contractImplements = listOf(ContractTrait("test-trait")),
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        projectId = PROJECT_ID_1,
                        createdAt = TestData.TIMESTAMP,
                        arbitraryData = ARBITRARY_DATA,
                        screenBeforeActionMessage = DEPLOY_SCREEN_BEFORE_ACTION_MESSAGE,
                        screenAfterActionMessage = DEPLOY_SCREEN_AFTER_ACTION_MESSAGE,
                        contractAddress = CONTRACT_ADDRESS,
                        deployerAddress = DEPLOYER_ADDRESS,
                        txHash = TX_HASH
                    )
                )
        }
    }

    private fun createRecord(
        id: UUID,
        projectId: UUID = PROJECT_ID_1,
        contractId: ContractId = CONTRACT_ID,
        contractTags: List<String> = emptyList(),
        contractImplements: List<String> = emptyList(),
        contractAddress: ContractAddress? = CONTRACT_ADDRESS,
        deployerAddress: WalletAddress? = DEPLOYER_ADDRESS,
        txHash: TransactionHash? = TX_HASH
    ) = ContractDeploymentRequestRecord(
        id = id,
        contractId = contractId,
        contractData = CONTRACT_DATA,
        contractTags = contractTags.toTypedArray(),
        contractImplements = contractImplements.toTypedArray(),
        chainId = CHAIN_ID,
        redirectUrl = REDIRECT_URL,
        projectId = projectId,
        createdAt = TestData.TIMESTAMP,
        arbitraryData = ARBITRARY_DATA,
        screenBeforeActionMessage = DEPLOY_SCREEN_BEFORE_ACTION_MESSAGE,
        screenAfterActionMessage = DEPLOY_SCREEN_AFTER_ACTION_MESSAGE,
        contractAddress = contractAddress,
        deployerAddress = deployerAddress,
        txHash = txHash
    )

    private fun ContractDeploymentRequestRecord.toModel() =
        ContractDeploymentRequest(
            id = id!!,
            contractId = contractId!!,
            contractData = contractData!!,
            contractTags = contractTags!!.map { ContractTag(it!!) },
            contractImplements = contractImplements!!.map { ContractTrait(it!!) },
            chainId = chainId!!,
            redirectUrl = redirectUrl!!,
            projectId = projectId!!,
            createdAt = createdAt!!,
            arbitraryData = arbitraryData,
            screenBeforeActionMessage = screenBeforeActionMessage,
            screenAfterActionMessage = screenAfterActionMessage,
            contractAddress = contractAddress,
            deployerAddress = deployerAddress,
            txHash = txHash
        )
}
