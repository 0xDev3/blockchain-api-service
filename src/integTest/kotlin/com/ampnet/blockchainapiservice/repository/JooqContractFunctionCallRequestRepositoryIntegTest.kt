package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.generated.jooq.enums.UserIdentifierType
import com.ampnet.blockchainapiservice.generated.jooq.tables.interfaces.IContractFunctionCallRequestRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ContractDeploymentRequestRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ContractFunctionCallRequestRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ContractMetadataRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ProjectRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.UserIdentifierRecord
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.filters.ContractFunctionCallRequestFilters
import com.ampnet.blockchainapiservice.model.params.StoreContractFunctionCallRequestParams
import com.ampnet.blockchainapiservice.model.result.ContractFunctionCallRequest
import com.ampnet.blockchainapiservice.testcontainers.SharedTestContainers
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.BaseUrl
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.ContractBinaryData
import com.ampnet.blockchainapiservice.util.ContractId
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
import org.springframework.test.annotation.DirtiesContext
import java.math.BigInteger
import java.util.UUID

@JooqTest
@Import(JooqContractFunctionCallRequestRepository::class)
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JooqContractFunctionCallRequestRepositoryIntegTest : TestBase() {

    companion object {
        private val PROJECT_ID_1 = UUID.randomUUID()
        private val PROJECT_ID_2 = UUID.randomUUID()
        private val OWNER_ID = UUID.randomUUID()
        private val DEPLOYED_CONTRACT_ID = UUID.randomUUID()
        private val CONTRACT_ADDRESS = ContractAddress("1337")
        private const val FUNCTION_NAME = "balanceOf"
        private val ETH_AMOUNT = Balance(BigInteger("10000"))
        private val CHAIN_ID = ChainId(1337L)
        private const val REDIRECT_URL = "redirect-url"
        private val ARBITRARY_DATA = TestData.EMPTY_JSON_OBJECT
        private const val SCREEN_BEFORE_ACTION_MESSAGE = "screen-before-action-message"
        private const val SCREEN_AFTER_ACTION_MESSAGE = "screen-after-action-message"
        private val CALLER_ADDRESS = WalletAddress("123")
        private val TX_HASH = TransactionHash("tx-hash")
    }

    @Suppress("unused")
    private val postgresContainer = SharedTestContainers.postgresContainer

    @Autowired
    private lateinit var repository: JooqContractFunctionCallRequestRepository

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

        val metadataId = UUID.randomUUID()

        dslContext.executeInsert(
            ContractMetadataRecord(
                id = metadataId,
                contractId = ContractId("contract-id"),
                contractTags = emptyArray(),
                contractImplements = emptyArray(),
            )
        )

        dslContext.executeInsert(
            ContractDeploymentRequestRecord(
                id = DEPLOYED_CONTRACT_ID,
                alias = UUID.randomUUID().toString(),
                contractMetadataId = metadataId,
                contractData = ContractBinaryData("00"),
                constructorParams = TestData.EMPTY_JSON_ARRAY,
                initialEthAmount = Balance.ZERO,
                chainId = CHAIN_ID,
                redirectUrl = REDIRECT_URL,
                projectId = PROJECT_ID_1,
                createdAt = TestData.TIMESTAMP,
                arbitraryData = ARBITRARY_DATA,
                screenBeforeActionMessage = SCREEN_BEFORE_ACTION_MESSAGE,
                screenAfterActionMessage = SCREEN_AFTER_ACTION_MESSAGE,
                contractAddress = CONTRACT_ADDRESS,
                deployerAddress = CALLER_ADDRESS,
                txHash = TX_HASH,
                imported = false
            )
        )
    }

    @Test
    fun mustCorrectlyFetchContractFunctionCallRequestById() {
        val id = UUID.randomUUID()
        val record = createRecord(id)

        suppose("some contract function call request exists in database") {
            dslContext.executeInsert(record)
        }

        verify("contract function call request is correctly fetched by ID") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(record.toModel())
        }
    }

    @Test
    fun mustReturnNullWhenFetchingNonExistentContractFunctionCallRequestById() {
        verify("null is returned when fetching non-existent contract function call request") {
            val result = repository.getById(UUID.randomUUID())

            assertThat(result).withMessage()
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyFetchContractFunctionCallRequestsByProjectIdAndFilters() {
        val project1ContractsWithMatchingDeployedContractIdAndAddress = listOf(
            createRecord(id = UUID.randomUUID()),
            createRecord(id = UUID.randomUUID())
        )
        val project1ContractsWithMissingDeployedContractId = listOf(
            createRecord(id = UUID.randomUUID(), deployedContractId = null),
            createRecord(id = UUID.randomUUID(), deployedContractId = null)
        )
        val project1ContractsWithNonMatchingContractAddress = listOf(
            createRecord(id = UUID.randomUUID(), contractAddress = ContractAddress("dead")),
            createRecord(id = UUID.randomUUID(), contractAddress = ContractAddress("dead"))
        )
        val project2ContractsWithMatchingDeployedContractIdAndAddress = listOf(
            createRecord(id = UUID.randomUUID(), projectId = PROJECT_ID_2),
            createRecord(id = UUID.randomUUID(), projectId = PROJECT_ID_2)
        )
        val project2ContractsWithMissingDeployedContractId = listOf(
            createRecord(id = UUID.randomUUID(), deployedContractId = null, projectId = PROJECT_ID_2),
            createRecord(id = UUID.randomUUID(), deployedContractId = null, projectId = PROJECT_ID_2)
        )
        val project2ContractsWithNonMatchingContractAddress = listOf(
            createRecord(id = UUID.randomUUID(), contractAddress = ContractAddress("dead"), projectId = PROJECT_ID_2),
            createRecord(id = UUID.randomUUID(), contractAddress = ContractAddress("dead"), projectId = PROJECT_ID_2)
        )

        suppose("some contract function call requests exist in database") {
            dslContext.batchInsert(
                project1ContractsWithMatchingDeployedContractIdAndAddress +
                    project1ContractsWithMissingDeployedContractId + project1ContractsWithNonMatchingContractAddress +
                    project2ContractsWithMatchingDeployedContractIdAndAddress +
                    project2ContractsWithMissingDeployedContractId + project2ContractsWithNonMatchingContractAddress
            ).execute()
        }

        verify("must correctly fetch project 1 contract function calls with matching contract ID and address") {
            assertThat(
                repository.getAllByProjectId(
                    projectId = PROJECT_ID_1,
                    filters = ContractFunctionCallRequestFilters(
                        deployedContractId = DEPLOYED_CONTRACT_ID,
                        contractAddress = CONTRACT_ADDRESS
                    )
                )
            ).withMessage()
                .containsExactlyInAnyOrderElementsOf(
                    models(project1ContractsWithMatchingDeployedContractIdAndAddress)
                )
        }

        verify("must correctly fetch project 1 contract function calls with matching contract ID") {
            assertThat(
                repository.getAllByProjectId(
                    projectId = PROJECT_ID_1,
                    filters = ContractFunctionCallRequestFilters(
                        deployedContractId = DEPLOYED_CONTRACT_ID,
                        contractAddress = null
                    )
                )
            ).withMessage()
                .containsExactlyInAnyOrderElementsOf(
                    models(
                        project1ContractsWithMatchingDeployedContractIdAndAddress,
                        project1ContractsWithNonMatchingContractAddress
                    )
                )
        }

        verify("must correctly fetch project 1 contract function calls with matching contract address") {
            assertThat(
                repository.getAllByProjectId(
                    projectId = PROJECT_ID_1,
                    filters = ContractFunctionCallRequestFilters(
                        deployedContractId = null,
                        contractAddress = CONTRACT_ADDRESS
                    )
                )
            ).withMessage()
                .containsExactlyInAnyOrderElementsOf(
                    models(
                        project1ContractsWithMatchingDeployedContractIdAndAddress,
                        project1ContractsWithMissingDeployedContractId
                    )
                )
        }

        verify("must correctly fetch all project 1 contract function calls") {
            assertThat(
                repository.getAllByProjectId(
                    projectId = PROJECT_ID_1,
                    filters = ContractFunctionCallRequestFilters(
                        deployedContractId = null,
                        contractAddress = null
                    )
                )
            ).withMessage()
                .containsExactlyInAnyOrderElementsOf(
                    models(
                        project1ContractsWithMatchingDeployedContractIdAndAddress,
                        project1ContractsWithMissingDeployedContractId,
                        project1ContractsWithNonMatchingContractAddress
                    )
                )
        }

        verify("must correctly fetch project 2 contract function calls with matching contract ID and address") {
            assertThat(
                repository.getAllByProjectId(
                    projectId = PROJECT_ID_2,
                    filters = ContractFunctionCallRequestFilters(
                        deployedContractId = DEPLOYED_CONTRACT_ID,
                        contractAddress = CONTRACT_ADDRESS
                    )
                )
            ).withMessage()
                .containsExactlyInAnyOrderElementsOf(
                    models(project2ContractsWithMatchingDeployedContractIdAndAddress)
                )
        }

        verify("must correctly fetch project 2 contract function calls with matching contract ID") {
            assertThat(
                repository.getAllByProjectId(
                    projectId = PROJECT_ID_2,
                    filters = ContractFunctionCallRequestFilters(
                        deployedContractId = DEPLOYED_CONTRACT_ID,
                        contractAddress = null
                    )
                )
            ).withMessage()
                .containsExactlyInAnyOrderElementsOf(
                    models(
                        project2ContractsWithMatchingDeployedContractIdAndAddress,
                        project2ContractsWithNonMatchingContractAddress
                    )
                )
        }

        verify("must correctly fetch project 2 contract function calls with matching contract address") {
            assertThat(
                repository.getAllByProjectId(
                    projectId = PROJECT_ID_2,
                    filters = ContractFunctionCallRequestFilters(
                        deployedContractId = null,
                        contractAddress = CONTRACT_ADDRESS
                    )
                )
            ).withMessage()
                .containsExactlyInAnyOrderElementsOf(
                    models(
                        project2ContractsWithMatchingDeployedContractIdAndAddress,
                        project2ContractsWithMissingDeployedContractId
                    )
                )
        }

        verify("must correctly fetch all project 2 contract function calls") {
            assertThat(
                repository.getAllByProjectId(
                    projectId = PROJECT_ID_2,
                    filters = ContractFunctionCallRequestFilters(
                        deployedContractId = null,
                        contractAddress = null
                    )
                )
            ).withMessage()
                .containsExactlyInAnyOrderElementsOf(
                    models(
                        project2ContractsWithMatchingDeployedContractIdAndAddress,
                        project2ContractsWithMissingDeployedContractId,
                        project2ContractsWithNonMatchingContractAddress
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyStoreContractFunctionCallRequest() {
        val id = UUID.randomUUID()
        val params = StoreContractFunctionCallRequestParams(
            id = id,
            deployedContractId = DEPLOYED_CONTRACT_ID,
            contractAddress = CONTRACT_ADDRESS,
            functionName = FUNCTION_NAME,
            functionParams = TestData.EMPTY_JSON_ARRAY,
            ethAmount = ETH_AMOUNT,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            projectId = PROJECT_ID_1,
            createdAt = TestData.TIMESTAMP,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = SCREEN_AFTER_ACTION_MESSAGE
            ),
            callerAddress = CALLER_ADDRESS
        )

        val storedContractFunctionCallRequest = suppose("contract function call request is stored in database") {
            repository.store(params)
        }

        val expectedContractFunctionCallRequest = ContractFunctionCallRequest(
            id = id,
            deployedContractId = DEPLOYED_CONTRACT_ID,
            contractAddress = CONTRACT_ADDRESS,
            functionName = FUNCTION_NAME,
            functionParams = TestData.EMPTY_JSON_ARRAY,
            ethAmount = ETH_AMOUNT,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            projectId = PROJECT_ID_1,
            createdAt = TestData.TIMESTAMP,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = SCREEN_AFTER_ACTION_MESSAGE
            ),
            callerAddress = CALLER_ADDRESS,
            txHash = null
        )

        verify("storing contract function call request returns correct result") {
            assertThat(storedContractFunctionCallRequest).withMessage()
                .isEqualTo(expectedContractFunctionCallRequest)
        }

        verify("contract function call request was stored in database") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(expectedContractFunctionCallRequest)
        }
    }

    @Test
    fun mustCorrectlySetTxInfoForContractFunctionCallRequestWithNullTxHash() {
        val id = UUID.randomUUID()
        val params = StoreContractFunctionCallRequestParams(
            id = id,
            deployedContractId = DEPLOYED_CONTRACT_ID,
            contractAddress = CONTRACT_ADDRESS,
            functionName = FUNCTION_NAME,
            functionParams = TestData.EMPTY_JSON_ARRAY,
            ethAmount = ETH_AMOUNT,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            projectId = PROJECT_ID_1,
            createdAt = TestData.TIMESTAMP,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = SCREEN_AFTER_ACTION_MESSAGE
            ),
            callerAddress = null
        )

        suppose("contract function call request is stored in database") {
            repository.store(params)
        }

        verify("setting txInfo will succeed") {
            assertThat(repository.setTxInfo(id, TX_HASH, CALLER_ADDRESS)).withMessage()
                .isTrue()
        }

        verify("txInfo is correctly set in database") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(
                    ContractFunctionCallRequest(
                        id = id,
                        deployedContractId = DEPLOYED_CONTRACT_ID,
                        contractAddress = CONTRACT_ADDRESS,
                        functionName = FUNCTION_NAME,
                        functionParams = TestData.EMPTY_JSON_ARRAY,
                        ethAmount = ETH_AMOUNT,
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        projectId = PROJECT_ID_1,
                        createdAt = TestData.TIMESTAMP,
                        arbitraryData = ARBITRARY_DATA,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        callerAddress = CALLER_ADDRESS,
                        txHash = TX_HASH,
                    )
                )
        }
    }

    @Test
    fun mustNotUpdateCallerAddressForContractFunctionCallRequestWhenCallerIsAlreadySet() {
        val id = UUID.randomUUID()
        val params = StoreContractFunctionCallRequestParams(
            id = id,
            deployedContractId = DEPLOYED_CONTRACT_ID,
            contractAddress = CONTRACT_ADDRESS,
            functionName = FUNCTION_NAME,
            functionParams = TestData.EMPTY_JSON_ARRAY,
            ethAmount = ETH_AMOUNT,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            projectId = PROJECT_ID_1,
            createdAt = TestData.TIMESTAMP,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = SCREEN_AFTER_ACTION_MESSAGE
            ),
            callerAddress = CALLER_ADDRESS
        )

        suppose("contract function call request is stored in database") {
            repository.store(params)
        }

        verify("setting txInfo will succeed") {
            val ignoredCaller = WalletAddress("f")
            assertThat(repository.setTxInfo(id, TX_HASH, ignoredCaller)).withMessage()
                .isTrue()
        }

        verify("txHash was correctly set while contract function caller was not updated") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(
                    ContractFunctionCallRequest(
                        id = id,
                        deployedContractId = DEPLOYED_CONTRACT_ID,
                        contractAddress = CONTRACT_ADDRESS,
                        functionName = FUNCTION_NAME,
                        functionParams = TestData.EMPTY_JSON_ARRAY,
                        ethAmount = ETH_AMOUNT,
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        projectId = PROJECT_ID_1,
                        createdAt = TestData.TIMESTAMP,
                        arbitraryData = ARBITRARY_DATA,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        callerAddress = CALLER_ADDRESS,
                        txHash = TX_HASH,
                    )
                )
        }
    }

    @Test
    fun mustNotSetTxInfoForContractFunctionCallRequestWhenTxHashIsAlreadySet() {
        val id = UUID.randomUUID()
        val params = StoreContractFunctionCallRequestParams(
            id = id,
            deployedContractId = DEPLOYED_CONTRACT_ID,
            contractAddress = CONTRACT_ADDRESS,
            functionName = FUNCTION_NAME,
            functionParams = TestData.EMPTY_JSON_ARRAY,
            ethAmount = ETH_AMOUNT,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            projectId = PROJECT_ID_1,
            createdAt = TestData.TIMESTAMP,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = SCREEN_AFTER_ACTION_MESSAGE
            ),
            callerAddress = null
        )

        suppose("contract function call request is stored in database") {
            repository.store(params)
        }

        verify("setting txInfo will succeed") {
            assertThat(repository.setTxInfo(id, TX_HASH, CALLER_ADDRESS)).withMessage()
                .isTrue()
        }

        verify("setting another txInfo will not succeed") {
            assertThat(
                repository.setTxInfo(
                    id = id,
                    txHash = TransactionHash("different-tx-hash"),
                    caller = CALLER_ADDRESS
                )
            ).withMessage()
                .isFalse()
        }

        verify("first txHash remains in database") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(
                    ContractFunctionCallRequest(
                        id = id,
                        deployedContractId = DEPLOYED_CONTRACT_ID,
                        contractAddress = CONTRACT_ADDRESS,
                        functionName = FUNCTION_NAME,
                        functionParams = TestData.EMPTY_JSON_ARRAY,
                        ethAmount = ETH_AMOUNT,
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        projectId = PROJECT_ID_1,
                        createdAt = TestData.TIMESTAMP,
                        arbitraryData = ARBITRARY_DATA,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        callerAddress = CALLER_ADDRESS,
                        txHash = TX_HASH,
                    )
                )
        }
    }

    private fun createRecord(
        id: UUID,
        deployedContractId: UUID? = DEPLOYED_CONTRACT_ID,
        contractAddress: ContractAddress = CONTRACT_ADDRESS,
        projectId: UUID = PROJECT_ID_1
    ) = ContractFunctionCallRequestRecord(
        id = id,
        deployedContractId = deployedContractId,
        contractAddress = contractAddress,
        functionName = FUNCTION_NAME,
        functionParams = TestData.EMPTY_JSON_ARRAY,
        ethAmount = ETH_AMOUNT,
        chainId = CHAIN_ID,
        redirectUrl = REDIRECT_URL,
        projectId = projectId,
        createdAt = TestData.TIMESTAMP,
        arbitraryData = ARBITRARY_DATA,
        screenBeforeActionMessage = SCREEN_BEFORE_ACTION_MESSAGE,
        screenAfterActionMessage = SCREEN_AFTER_ACTION_MESSAGE,
        callerAddress = CALLER_ADDRESS,
        txHash = TX_HASH
    )

    private fun IContractFunctionCallRequestRecord.toModel() =
        ContractFunctionCallRequest(
            id = id!!,
            deployedContractId = deployedContractId,
            contractAddress = contractAddress!!,
            functionName = functionName!!,
            functionParams = functionParams!!,
            ethAmount = ethAmount!!,
            chainId = chainId!!,
            redirectUrl = redirectUrl!!,
            projectId = projectId!!,
            createdAt = createdAt!!,
            arbitraryData = arbitraryData,
            screenConfig = ScreenConfig(
                beforeActionMessage = screenBeforeActionMessage,
                afterActionMessage = screenAfterActionMessage
            ),
            callerAddress = callerAddress,
            txHash = txHash,
        )

    private fun models(vararg records: List<IContractFunctionCallRequestRecord>): List<ContractFunctionCallRequest> =
        records.flatMap { it }.map { it.toModel() }
}
