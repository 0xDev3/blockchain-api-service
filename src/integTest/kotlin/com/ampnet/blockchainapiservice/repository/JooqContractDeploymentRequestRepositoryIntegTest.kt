package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.exception.AliasAlreadyInUseException
import com.ampnet.blockchainapiservice.generated.jooq.enums.UserIdentifierType
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ContractDeploymentRequestRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ContractMetadataRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ProjectRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.UserIdentifierRecord
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.filters.AndList
import com.ampnet.blockchainapiservice.model.filters.ContractDeploymentRequestFilters
import com.ampnet.blockchainapiservice.model.filters.OrList
import com.ampnet.blockchainapiservice.model.params.StoreContractDeploymentRequestParams
import com.ampnet.blockchainapiservice.model.result.ContractDeploymentRequest
import com.ampnet.blockchainapiservice.testcontainers.SharedTestContainers
import com.ampnet.blockchainapiservice.util.Balance
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
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import java.math.BigInteger
import java.util.UUID

@JooqTest
@Import(JooqContractDeploymentRequestRepository::class)
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JooqContractDeploymentRequestRepositoryIntegTest : TestBase() {

    companion object {
        private val PROJECT_ID_1 = UUID.randomUUID()
        private val PROJECT_ID_2 = UUID.randomUUID()
        private val OWNER_ID = UUID.randomUUID()
        private const val ALIAS = "alias"
        private const val NAME = "name"
        private const val DESCRIPTION = "description"
        private val CONTRACT_ID = ContractId("contract-id")
        private val CONTRACT_DATA = ContractBinaryData("00")
        private val INITIAL_ETH_AMOUNT = Balance(BigInteger("10000"))
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
    private val postgresContainer = SharedTestContainers.postgresContainer

    @Autowired
    private lateinit var repository: JooqContractDeploymentRequestRepository

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
        val metadata = createMetadataRecord()
        val record = createRecord(id, metadata)

        suppose("some contract deployment request exists in database") {
            dslContext.executeInsert(metadata)
            dslContext.executeInsert(record)
        }

        verify("contract deployment request is correctly fetched by ID") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(record.toModel(metadata))
        }
    }

    @Test
    fun mustReturnNullWhenFetchingNonExistentContractDeploymentRequestById() {
        verify("null is returned when fetching non-existent contract deployment request by id") {
            val result = repository.getById(UUID.randomUUID())

            assertThat(result).withMessage()
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyFetchContractDeploymentRequestByAliasAndProjectId() {
        val metadata = createMetadataRecord()
        val record = createRecord(UUID.randomUUID(), metadata)

        suppose("some contract deployment request exists in database") {
            dslContext.executeInsert(metadata)
            dslContext.executeInsert(record)
        }

        verify("contract deployment request is correctly fetched by alias") {
            val result = repository.getByAliasAndProjectId(record.alias!!, record.projectId!!)

            assertThat(result).withMessage()
                .isEqualTo(record.toModel(metadata))
        }
    }

    @Test
    fun mustReturnNullWhenFetchingNonExistentContractDeploymentRequestByAliasAndProjectId() {
        verify("null is returned when fetching non-existent contract deployment request by alias and project id") {
            val result = repository.getByAliasAndProjectId("non-existent-alias", UUID.randomUUID())

            assertThat(result).withMessage()
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyFetchContractDeploymentRequestsByProjectIdAndFilters() {
        val cid1Metadata = createMetadataRecord(contractId = ContractId("cid-1"))
        val cid2Metadata = createMetadataRecord(contractId = ContractId("cid-2"))
        val cid3Metadata = createMetadataRecord(contractId = ContractId("cid-3"))
        val ignoredCidMetadata = createMetadataRecord(contractId = ContractId("ignored-cid"))
        val tag1Metadata = createMetadataRecord(contractId = ContractId("cid-tag-1"), tags = listOf("tag-1"))
        val tag2Metadata = createMetadataRecord(contractId = ContractId("cid-tag-2"), tags = listOf("tag-2"))
        val tag2AndIgnoredTagMetadata = createMetadataRecord(
            contractId = ContractId("cid-tag-2-ignored"),
            tags = listOf("tag-2", "ignored-tag")
        )
        val ignoredTagMetadata = createMetadataRecord(
            contractId = ContractId("cid-ignored-tag"),
            tags = listOf("ignored-tag")
        )
        val trait1Metadata = createMetadataRecord(contractId = ContractId("cid-trait-1"), traits = listOf("trait-1"))
        val trait2Metadata = createMetadataRecord(contractId = ContractId("cid-trait-2"), traits = listOf("trait-2"))
        val trait2AndIgnoredTraitMetadata = createMetadataRecord(
            contractId = ContractId("cid-trait-2-ignored"),
            traits = listOf("trait-2", "ignored-trait")
        )
        val ignoredTraitMetadata = createMetadataRecord(
            contractId = ContractId("cid-ignored-trait"),
            traits = listOf("ignored-trait")
        )
        val project2Metadata1 = createMetadataRecord(
            contractId = ContractId("cid-1-project-2"),
            tags = listOf("tag-1", "tag-2"),
            traits = listOf("trait-1", "trait-2")
        )
        val project2Metadata2 = createMetadataRecord(
            contractId = ContractId("cid-2-project-2"),
            tags = listOf("tag-1", "tag-2"),
            traits = listOf("trait-1", "trait-2")
        )
        val project2Metadata3 = createMetadataRecord(
            contractId = ContractId("cid-3-project-2"),
            tags = listOf("ignored-tag", "tag-3"),
            traits = listOf("ignored-trait", "trait-3")
        )
        val project2NonMatchingMetadata1 = createMetadataRecord(
            contractId = ContractId("cid-1-project-2-no-tx-hash"),
            tags = listOf("tag-1", "tag-2"),
            traits = listOf("trait-1", "trait-2")
        )
        val project2NonMatchingMetadata2 = createMetadataRecord(
            contractId = ContractId("cid-2-project-2-ignored"),
            tags = listOf("tag-1", "tag-2"),
            traits = listOf("trait-1", "trait-2")
        )
        val project2NonMatchingMetadata3 = createMetadataRecord(
            contractId = ContractId("cid-3-project-2-missing-tag"),
            tags = listOf("tag-1"),
            traits = listOf("ignored-trait", "trait-1", "trait-2")
        )
        val project2NonMatchingMetadata4 = createMetadataRecord(
            contractId = ContractId("cid-4-project-2-missing-trait"),
            tags = listOf("ignored-tag", "tag-1", "tag-2"),
            traits = listOf("trait-1")
        )
        val metadataById = listOf(
            cid1Metadata,
            cid2Metadata,
            cid3Metadata,
            ignoredCidMetadata,
            tag1Metadata,
            tag2Metadata,
            tag2AndIgnoredTagMetadata,
            ignoredTagMetadata,
            trait1Metadata,
            trait2Metadata,
            trait2AndIgnoredTraitMetadata,
            ignoredTraitMetadata,
            project2Metadata1,
            project2Metadata2,
            project2Metadata3,
            project2NonMatchingMetadata1,
            project2NonMatchingMetadata2,
            project2NonMatchingMetadata3,
            project2NonMatchingMetadata4
        ).associateBy { it.id }

        suppose("metadata records are inserted into the database") {
            dslContext.batchInsert(metadataById.values).execute()
        }

        fun uuid() = UUID.randomUUID()

        val project1ContractsWithMatchingCid = listOf(
            createRecord(id = uuid(), projectId = PROJECT_ID_1, metadata = cid1Metadata),
            createRecord(id = uuid(), projectId = PROJECT_ID_1, metadata = cid2Metadata),
            createRecord(id = uuid(), projectId = PROJECT_ID_1, metadata = cid3Metadata)
        )
        val project1NonDeployedContractsWithMatchingCid = listOf(
            createRecord(id = uuid(), projectId = PROJECT_ID_1, metadata = cid1Metadata, txHash = null),
            createRecord(id = uuid(), projectId = PROJECT_ID_1, metadata = cid2Metadata, txHash = null),
            createRecord(id = uuid(), projectId = PROJECT_ID_1, metadata = cid3Metadata, txHash = null)
        )
        val project1ContractsWithNonMatchingCid = listOf(
            createRecord(id = uuid(), projectId = PROJECT_ID_1, metadata = ignoredCidMetadata),
            createRecord(id = uuid(), projectId = PROJECT_ID_1, metadata = ignoredCidMetadata)
        )
        val project1ContractsWithMatchingTags = listOf(
            createRecord(id = uuid(), projectId = PROJECT_ID_1, metadata = tag1Metadata),
            createRecord(id = uuid(), projectId = PROJECT_ID_1, metadata = tag2Metadata),
            createRecord(id = uuid(), projectId = PROJECT_ID_1, metadata = tag2AndIgnoredTagMetadata)
        )
        val project1ContractsWithNonMatchingTags = listOf(
            createRecord(id = uuid(), projectId = PROJECT_ID_1, metadata = ignoredTagMetadata),
            createRecord(id = uuid(), projectId = PROJECT_ID_1, metadata = ignoredTagMetadata)
        )
        val project1ContractsWithMatchingTraits = listOf(
            createRecord(id = uuid(), projectId = PROJECT_ID_1, metadata = trait1Metadata),
            createRecord(id = uuid(), projectId = PROJECT_ID_1, metadata = trait2Metadata),
            createRecord(id = uuid(), projectId = PROJECT_ID_1, metadata = trait2AndIgnoredTraitMetadata)
        )
        val project1ContractsWithNonMatchingTraits = listOf(
            createRecord(id = uuid(), projectId = PROJECT_ID_1, metadata = ignoredTraitMetadata),
            createRecord(id = uuid(), projectId = PROJECT_ID_1, metadata = ignoredTraitMetadata)
        )

        val project2MatchingContracts = listOf(
            createRecord(id = uuid(), projectId = PROJECT_ID_2, metadata = project2Metadata1),
            createRecord(id = uuid(), projectId = PROJECT_ID_2, metadata = project2Metadata2),
            createRecord(id = uuid(), projectId = PROJECT_ID_2, metadata = project2Metadata3)
        )
        val project2NonMatchingContracts = listOf(
            createRecord(id = uuid(), projectId = PROJECT_ID_2, metadata = project2NonMatchingMetadata1, txHash = null),
            createRecord(id = uuid(), projectId = PROJECT_ID_2, metadata = project2NonMatchingMetadata2),
            createRecord(id = uuid(), projectId = PROJECT_ID_2, metadata = project2NonMatchingMetadata3),
            createRecord(id = uuid(), projectId = PROJECT_ID_2, metadata = project2NonMatchingMetadata4)
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
                        .map { it.toModel(metadataById[it.contractMetadataId!!]!!) }
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
                .containsExactlyInAnyOrderElementsOf(
                    project1ContractsWithMatchingCid.map { it.toModel(metadataById[it.contractMetadataId!!]!!) }
                )
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
                .containsExactlyInAnyOrderElementsOf(
                    project1ContractsWithMatchingTags.map { it.toModel(metadataById[it.contractMetadataId!!]!!) }
                )
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
                .containsExactlyInAnyOrderElementsOf(
                    project1ContractsWithMatchingTraits.map { it.toModel(metadataById[it.contractMetadataId!!]!!) }
                )
        }

        verify("must correctly fetch project 2 contracts which match given filters") {
            assertThat(
                repository.getAllByProjectId(
                    projectId = PROJECT_ID_2,
                    filters = ContractDeploymentRequestFilters(
                        contractIds = OrList(
                            ContractId("cid-1-project-2"),
                            ContractId("cid-2-project-2"),
                            ContractId("cid-3-project-2"),
                            ContractId("cid-1-project-2-no-tx-hash"),
                            ContractId("cid-3-project-2-missing-tag"),
                            ContractId("cid-4-project-2-missing-trait")
                        ),
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
                .containsExactlyInAnyOrderElementsOf(
                    project2MatchingContracts.map { it.toModel(metadataById[it.contractMetadataId!!]!!) }
                )
        }
    }

    @Test
    fun mustCorrectlyStoreContractDeploymentRequest() {
        suppose("some contract metadata is in database") {
            dslContext.executeInsert(
                createMetadataRecord(
                    tags = listOf("test-tag"),
                    traits = listOf("test-trait"),
                )
            )
        }

        val id = UUID.randomUUID()
        val params = StoreContractDeploymentRequestParams(
            id = id,
            alias = ALIAS,
            contractId = CONTRACT_ID,
            contractData = CONTRACT_DATA,
            constructorParams = TestData.EMPTY_JSON_ARRAY,
            deployerAddress = null,
            initialEthAmount = INITIAL_ETH_AMOUNT,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            projectId = PROJECT_ID_1,
            createdAt = TestData.TIMESTAMP,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = DEPLOY_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = DEPLOY_SCREEN_AFTER_ACTION_MESSAGE
            ),
            imported = false
        )

        val storedContractDeploymentRequest = suppose("contract deployment request is stored in database") {
            repository.store(params)
        }

        val expectedContractDeploymentRequest = ContractDeploymentRequest(
            id = id,
            alias = ALIAS,
            name = NAME,
            description = DESCRIPTION,
            contractId = CONTRACT_ID,
            contractData = CONTRACT_DATA,
            constructorParams = TestData.EMPTY_JSON_ARRAY,
            contractTags = listOf(ContractTag("test-tag")),
            contractImplements = listOf(ContractTrait("test-trait")),
            initialEthAmount = INITIAL_ETH_AMOUNT,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            projectId = PROJECT_ID_1,
            createdAt = TestData.TIMESTAMP,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = DEPLOY_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = DEPLOY_SCREEN_AFTER_ACTION_MESSAGE
            ),
            contractAddress = null,
            deployerAddress = null,
            txHash = null,
            imported = false
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

        verify("storing contract deployment request with conflicting alias throws AliasAlreadyInUseException") {
            assertThrows<AliasAlreadyInUseException>(message) {
                repository.store(params.copy(id = UUID.randomUUID()))
            }
        }
    }

    @Test
    fun mustCorrectlyMarkContractDeploymentRequestAsDeletedById() {
        val id = UUID.randomUUID()
        val metadata = createMetadataRecord()
        val record = createRecord(id, metadata)

        suppose("some contract deployment request exists in database") {
            dslContext.executeInsert(metadata)
            dslContext.executeInsert(record)
        }

        verify("contract deployment request is correctly fetched by ID") {
            assertThat(repository.getById(id)).withMessage()
                .isEqualTo(record.toModel(metadata))
        }

        val deletionResult = suppose("contract deployment request is marked as deleted") {
            repository.markAsDeleted(id)
        }

        verify("contract deployment request was successfully marked as deleted in the database") {
            assertThat(deletionResult).withMessage()
                .isTrue()
            assertThat(repository.getById(id)).withMessage()
                .isNull()
        }
    }

    @Test
    fun mustCorrectlySetTxInfoForContractDeploymentRequestWithNullTxHash() {
        suppose("some contract metadata is in database") {
            dslContext.executeInsert(
                createMetadataRecord(
                    tags = listOf("test-tag"),
                    traits = listOf("test-trait"),
                )
            )
        }

        val id = UUID.randomUUID()
        val params = StoreContractDeploymentRequestParams(
            id = id,
            alias = ALIAS,
            contractId = CONTRACT_ID,
            contractData = CONTRACT_DATA,
            constructorParams = TestData.EMPTY_JSON_ARRAY,
            deployerAddress = null,
            initialEthAmount = INITIAL_ETH_AMOUNT,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            projectId = PROJECT_ID_1,
            createdAt = TestData.TIMESTAMP,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = DEPLOY_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = DEPLOY_SCREEN_AFTER_ACTION_MESSAGE
            ),
            imported = false
        )

        suppose("contract deployment request is stored in database") {
            repository.store(params)
        }

        verify("setting txInfo will succeed") {
            assertThat(repository.setTxInfo(id, TX_HASH, DEPLOYER_ADDRESS)).withMessage()
                .isTrue()
        }

        verify("txInfo is correctly set in database") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(
                    ContractDeploymentRequest(
                        id = id,
                        alias = ALIAS,
                        name = NAME,
                        description = DESCRIPTION,
                        contractId = CONTRACT_ID,
                        contractData = CONTRACT_DATA,
                        constructorParams = TestData.EMPTY_JSON_ARRAY,
                        contractTags = listOf(ContractTag("test-tag")),
                        contractImplements = listOf(ContractTrait("test-trait")),
                        initialEthAmount = INITIAL_ETH_AMOUNT,
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        projectId = PROJECT_ID_1,
                        createdAt = TestData.TIMESTAMP,
                        arbitraryData = ARBITRARY_DATA,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = DEPLOY_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = DEPLOY_SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        contractAddress = null,
                        deployerAddress = DEPLOYER_ADDRESS,
                        txHash = TX_HASH,
                        imported = false
                    )
                )
        }
    }

    @Test
    fun mustNotUpdateDeployerAddressForContractDeploymentRequestWhenDeployerIsAlreadySet() {
        suppose("some contract metadata is in database") {
            dslContext.executeInsert(
                createMetadataRecord(
                    tags = listOf("test-tag"),
                    traits = listOf("test-trait"),
                )
            )
        }

        val id = UUID.randomUUID()
        val params = StoreContractDeploymentRequestParams(
            id = id,
            alias = ALIAS,
            contractId = CONTRACT_ID,
            contractData = CONTRACT_DATA,
            constructorParams = TestData.EMPTY_JSON_ARRAY,
            initialEthAmount = INITIAL_ETH_AMOUNT,
            deployerAddress = DEPLOYER_ADDRESS,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            projectId = PROJECT_ID_1,
            createdAt = TestData.TIMESTAMP,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = DEPLOY_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = DEPLOY_SCREEN_AFTER_ACTION_MESSAGE
            ),
            imported = false
        )

        suppose("contract deployment request is stored in database") {
            repository.store(params)
        }

        verify("setting txInfo will succeed") {
            val ignoredDeployer = WalletAddress("f")
            assertThat(repository.setTxInfo(id, TX_HASH, ignoredDeployer)).withMessage()
                .isTrue()
        }

        verify("txHash was correctly set while contract deployer was not updated") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(
                    ContractDeploymentRequest(
                        id = id,
                        alias = ALIAS,
                        name = NAME,
                        description = DESCRIPTION,
                        contractId = CONTRACT_ID,
                        contractData = CONTRACT_DATA,
                        constructorParams = TestData.EMPTY_JSON_ARRAY,
                        contractTags = listOf(ContractTag("test-tag")),
                        contractImplements = listOf(ContractTrait("test-trait")),
                        initialEthAmount = INITIAL_ETH_AMOUNT,
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        projectId = PROJECT_ID_1,
                        createdAt = TestData.TIMESTAMP,
                        arbitraryData = ARBITRARY_DATA,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = DEPLOY_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = DEPLOY_SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        contractAddress = null,
                        deployerAddress = DEPLOYER_ADDRESS,
                        txHash = TX_HASH,
                        imported = false
                    )
                )
        }
    }

    @Test
    fun mustNotSetTxInfoForContractDeploymentRequestWhenTxHashIsAlreadySet() {
        suppose("some contract metadata is in database") {
            dslContext.executeInsert(
                createMetadataRecord(
                    tags = listOf("test-tag"),
                    traits = listOf("test-trait"),
                )
            )
        }

        val id = UUID.randomUUID()
        val params = StoreContractDeploymentRequestParams(
            id = id,
            alias = ALIAS,
            contractId = CONTRACT_ID,
            contractData = CONTRACT_DATA,
            constructorParams = TestData.EMPTY_JSON_ARRAY,
            initialEthAmount = INITIAL_ETH_AMOUNT,
            deployerAddress = null,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            projectId = PROJECT_ID_1,
            createdAt = TestData.TIMESTAMP,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = DEPLOY_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = DEPLOY_SCREEN_AFTER_ACTION_MESSAGE
            ),
            imported = false
        )

        suppose("contract deployment request is stored in database") {
            repository.store(params)
        }

        verify("setting txInfo will succeed") {
            assertThat(repository.setTxInfo(id, TX_HASH, DEPLOYER_ADDRESS)).withMessage()
                .isTrue()
        }

        verify("setting another txInfo will not succeed") {
            assertThat(
                repository.setTxInfo(
                    id = id,
                    txHash = TransactionHash("different-tx-hash"),
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
                        alias = ALIAS,
                        name = NAME,
                        description = DESCRIPTION,
                        contractId = CONTRACT_ID,
                        contractData = CONTRACT_DATA,
                        constructorParams = TestData.EMPTY_JSON_ARRAY,
                        contractTags = listOf(ContractTag("test-tag")),
                        contractImplements = listOf(ContractTrait("test-trait")),
                        initialEthAmount = INITIAL_ETH_AMOUNT,
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        projectId = PROJECT_ID_1,
                        createdAt = TestData.TIMESTAMP,
                        arbitraryData = ARBITRARY_DATA,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = DEPLOY_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = DEPLOY_SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        contractAddress = null,
                        deployerAddress = DEPLOYER_ADDRESS,
                        txHash = TX_HASH,
                        imported = false
                    )
                )
        }
    }

    @Test
    fun mustCorrectlySetContractAddressForContractDeploymentRequestWithNullContractAddress() {
        suppose("some contract metadata is in database") {
            dslContext.executeInsert(
                createMetadataRecord(
                    tags = listOf("test-tag"),
                    traits = listOf("test-trait"),
                )
            )
        }

        val id = UUID.randomUUID()
        val params = StoreContractDeploymentRequestParams(
            id = id,
            alias = ALIAS,
            contractId = CONTRACT_ID,
            contractData = CONTRACT_DATA,
            constructorParams = TestData.EMPTY_JSON_ARRAY,
            deployerAddress = null,
            initialEthAmount = INITIAL_ETH_AMOUNT,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            projectId = PROJECT_ID_1,
            createdAt = TestData.TIMESTAMP,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = DEPLOY_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = DEPLOY_SCREEN_AFTER_ACTION_MESSAGE
            ),
            imported = false
        )

        suppose("contract deployment request is stored in database") {
            repository.store(params)
        }

        verify("setting contract address will succeed") {
            assertThat(repository.setContractAddress(id, CONTRACT_ADDRESS)).withMessage()
                .isTrue()
        }

        verify("contract address is correctly set in database") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(
                    ContractDeploymentRequest(
                        id = id,
                        alias = ALIAS,
                        name = NAME,
                        description = DESCRIPTION,
                        contractId = CONTRACT_ID,
                        contractData = CONTRACT_DATA,
                        constructorParams = TestData.EMPTY_JSON_ARRAY,
                        contractTags = listOf(ContractTag("test-tag")),
                        contractImplements = listOf(ContractTrait("test-trait")),
                        initialEthAmount = INITIAL_ETH_AMOUNT,
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        projectId = PROJECT_ID_1,
                        createdAt = TestData.TIMESTAMP,
                        arbitraryData = ARBITRARY_DATA,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = DEPLOY_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = DEPLOY_SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        contractAddress = CONTRACT_ADDRESS,
                        deployerAddress = null,
                        txHash = null,
                        imported = false
                    )
                )
        }
    }

    @Test
    fun mustNotSetContractAddressForContractDeploymentRequestWhenContractAddressIsAlreadySet() {
        suppose("some contract metadata is in database") {
            dslContext.executeInsert(
                createMetadataRecord(
                    tags = listOf("test-tag"),
                    traits = listOf("test-trait"),
                )
            )
        }

        val id = UUID.randomUUID()
        val params = StoreContractDeploymentRequestParams(
            id = id,
            alias = ALIAS,
            contractId = CONTRACT_ID,
            contractData = CONTRACT_DATA,
            constructorParams = TestData.EMPTY_JSON_ARRAY,
            initialEthAmount = INITIAL_ETH_AMOUNT,
            deployerAddress = null,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            projectId = PROJECT_ID_1,
            createdAt = TestData.TIMESTAMP,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = DEPLOY_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = DEPLOY_SCREEN_AFTER_ACTION_MESSAGE
            ),
            imported = false
        )

        suppose("contract deployment request is stored in database") {
            repository.store(params)
        }

        verify("setting contract address will succeed") {
            assertThat(repository.setContractAddress(id, CONTRACT_ADDRESS)).withMessage()
                .isTrue()
        }

        verify("setting another contract address will not succeed") {
            assertThat(repository.setContractAddress(id, ContractAddress("dead"))).withMessage()
                .isFalse()
        }

        verify("first contract address remains in database") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(
                    ContractDeploymentRequest(
                        id = id,
                        alias = ALIAS,
                        name = NAME,
                        description = DESCRIPTION,
                        contractId = CONTRACT_ID,
                        contractData = CONTRACT_DATA,
                        constructorParams = TestData.EMPTY_JSON_ARRAY,
                        contractTags = listOf(ContractTag("test-tag")),
                        contractImplements = listOf(ContractTrait("test-trait")),
                        initialEthAmount = INITIAL_ETH_AMOUNT,
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        projectId = PROJECT_ID_1,
                        createdAt = TestData.TIMESTAMP,
                        arbitraryData = ARBITRARY_DATA,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = DEPLOY_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = DEPLOY_SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        contractAddress = CONTRACT_ADDRESS,
                        deployerAddress = null,
                        txHash = null,
                        imported = false
                    )
                )
        }
    }

    private fun createMetadataRecord(
        contractId: ContractId = CONTRACT_ID,
        tags: List<String> = emptyList(),
        traits: List<String> = emptyList()
    ) = ContractMetadataRecord(
        id = UUID.randomUUID(),
        name = NAME,
        description = DESCRIPTION,
        contractId = contractId,
        contractTags = tags.toTypedArray(),
        contractImplements = traits.toTypedArray(),
    )

    private fun createRecord(
        id: UUID,
        metadata: ContractMetadataRecord,
        projectId: UUID = PROJECT_ID_1,
        contractAddress: ContractAddress? = CONTRACT_ADDRESS,
        deployerAddress: WalletAddress? = DEPLOYER_ADDRESS,
        txHash: TransactionHash? = TX_HASH
    ) = ContractDeploymentRequestRecord(
        id = id,
        alias = UUID.randomUUID().toString(),
        contractMetadataId = metadata.id,
        contractData = CONTRACT_DATA,
        constructorParams = TestData.EMPTY_JSON_ARRAY,
        initialEthAmount = INITIAL_ETH_AMOUNT,
        chainId = CHAIN_ID,
        redirectUrl = REDIRECT_URL,
        projectId = projectId,
        createdAt = TestData.TIMESTAMP,
        arbitraryData = ARBITRARY_DATA,
        screenBeforeActionMessage = DEPLOY_SCREEN_BEFORE_ACTION_MESSAGE,
        screenAfterActionMessage = DEPLOY_SCREEN_AFTER_ACTION_MESSAGE,
        contractAddress = contractAddress,
        deployerAddress = deployerAddress,
        txHash = txHash,
        imported = false,
        deleted = false
    )

    private fun ContractDeploymentRequestRecord.toModel(metadata: ContractMetadataRecord) =
        ContractDeploymentRequest(
            id = id!!,
            alias = alias!!,
            name = NAME,
            description = DESCRIPTION,
            contractId = metadata.contractId!!,
            contractData = contractData!!,
            constructorParams = constructorParams!!,
            contractTags = metadata.contractTags!!.map { ContractTag(it!!) },
            contractImplements = metadata.contractImplements!!.map { ContractTrait(it!!) },
            initialEthAmount = INITIAL_ETH_AMOUNT,
            chainId = chainId!!,
            redirectUrl = redirectUrl!!,
            projectId = projectId!!,
            createdAt = createdAt!!,
            arbitraryData = arbitraryData,
            screenConfig = ScreenConfig(
                beforeActionMessage = DEPLOY_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = DEPLOY_SCREEN_AFTER_ACTION_MESSAGE
            ),
            contractAddress = contractAddress,
            deployerAddress = deployerAddress,
            txHash = txHash,
            imported = false
        )
}
