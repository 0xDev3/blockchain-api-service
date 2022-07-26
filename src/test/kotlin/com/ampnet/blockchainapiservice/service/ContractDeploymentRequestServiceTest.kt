package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.blockchain.BlockchainService
import com.ampnet.blockchainapiservice.blockchain.properties.ChainSpec
import com.ampnet.blockchainapiservice.exception.CannotAttachTxInfoException
import com.ampnet.blockchainapiservice.exception.ResourceNotFoundException
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.filters.ContractDeploymentRequestFilters
import com.ampnet.blockchainapiservice.model.filters.OrList
import com.ampnet.blockchainapiservice.model.params.CreateContractDeploymentRequestParams
import com.ampnet.blockchainapiservice.model.params.StoreContractDeploymentRequestParams
import com.ampnet.blockchainapiservice.model.result.BlockchainTransactionInfo
import com.ampnet.blockchainapiservice.model.result.ContractDecorator
import com.ampnet.blockchainapiservice.model.result.ContractDeploymentRequest
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.repository.ContractDecoratorRepository
import com.ampnet.blockchainapiservice.repository.ContractDeploymentRequestRepository
import com.ampnet.blockchainapiservice.repository.ContractMetadataRepository
import com.ampnet.blockchainapiservice.repository.ProjectRepository
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.BaseUrl
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.ContractBinaryData
import com.ampnet.blockchainapiservice.util.ContractId
import com.ampnet.blockchainapiservice.util.ContractTag
import com.ampnet.blockchainapiservice.util.ContractTrait
import com.ampnet.blockchainapiservice.util.FunctionArgument
import com.ampnet.blockchainapiservice.util.FunctionData
import com.ampnet.blockchainapiservice.util.Status
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.ampnet.blockchainapiservice.util.ZeroAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoMoreInteractions
import java.math.BigInteger
import java.util.UUID
import org.mockito.kotlin.verify as verifyMock

class ContractDeploymentRequestServiceTest : TestBase() {

    companion object {
        private val PROJECT = Project(
            id = UUID.randomUUID(),
            ownerId = UUID.randomUUID(),
            issuerContractAddress = ContractAddress("a"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = ChainId(1337L),
            customRpcUrl = "custom-rpc-url",
            createdAt = TestData.TIMESTAMP
        )
        private val CONTRACT_ID = ContractId("contract-id")
        private val CREATE_PARAMS = CreateContractDeploymentRequestParams(
            alias = "alias",
            contractId = CONTRACT_ID,
            constructorParams = listOf(FunctionArgument("test")),
            deployerAddress = WalletAddress("a"),
            initialEthAmount = Balance(BigInteger("10000")),
            redirectUrl = "redirect-url/\${id}",
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            )
        )
        private val CONTRACT_DECORATOR = ContractDecorator(
            id = CONTRACT_ID,
            binary = ContractBinaryData("12"),
            tags = listOf(ContractTag("test-tag")),
            implements = listOf(ContractTrait("test-trait")),
            constructors = emptyList(),
            functions = emptyList(),
            events = emptyList()
        )
        private val ID = UUID.randomUUID()
        private val ENCODED_CONSTRUCTOR = FunctionData("0x1234")
        private val STORE_PARAMS = StoreContractDeploymentRequestParams(
            id = ID,
            alias = CREATE_PARAMS.alias,
            contractId = CONTRACT_ID,
            contractData = ContractBinaryData(CONTRACT_DECORATOR.binary.value + ENCODED_CONSTRUCTOR.withoutPrefix),
            constructorParams = TestData.EMPTY_JSON_ARRAY,
            deployerAddress = CREATE_PARAMS.deployerAddress,
            initialEthAmount = CREATE_PARAMS.initialEthAmount,
            chainId = PROJECT.chainId,
            redirectUrl = CREATE_PARAMS.redirectUrl!!.replace("\${id}", ID.toString()),
            projectId = PROJECT.id,
            createdAt = TestData.TIMESTAMP,
            arbitraryData = CREATE_PARAMS.arbitraryData,
            screenConfig = CREATE_PARAMS.screenConfig
        )
        private val TX_HASH = TransactionHash("tx-hash")
        private val CONTRACT_ADDRESS = ContractAddress("cafebabe")
        private val STORED_REQUEST = ContractDeploymentRequest(
            id = ID,
            alias = STORE_PARAMS.alias,
            contractId = CONTRACT_ID,
            contractData = STORE_PARAMS.contractData,
            constructorParams = TestData.EMPTY_JSON_ARRAY,
            contractTags = CONTRACT_DECORATOR.tags,
            contractImplements = CONTRACT_DECORATOR.implements,
            initialEthAmount = STORE_PARAMS.initialEthAmount,
            chainId = STORE_PARAMS.chainId,
            redirectUrl = STORE_PARAMS.redirectUrl,
            projectId = PROJECT.id,
            createdAt = TestData.TIMESTAMP,
            arbitraryData = STORE_PARAMS.arbitraryData,
            screenConfig = STORE_PARAMS.screenConfig,
            contractAddress = CONTRACT_ADDRESS,
            deployerAddress = STORE_PARAMS.deployerAddress,
            txHash = TX_HASH
        )
        val CHAIN_SPEC = ChainSpec(STORED_REQUEST.chainId, null)
        val TRANSACTION_INFO = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = STORED_REQUEST.deployerAddress!!,
            to = ZeroAddress,
            deployedContractAddress = CONTRACT_ADDRESS,
            data = FunctionData(STORED_REQUEST.contractData.value),
            value = STORED_REQUEST.initialEthAmount,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true
        )
    }

    @Test
    fun mustSuccessfullyCreateContractDeploymentRequest() {
        val contractDecoratorRepository = mock<ContractDecoratorRepository>()

        suppose("some contract decorator will be returned") {
            given(contractDecoratorRepository.getById(CONTRACT_ID))
                .willReturn(CONTRACT_DECORATOR)
        }

        val uuidProvider = mock<UuidProvider>()

        suppose("some UUID will be generated") {
            given(uuidProvider.getUuid())
                .willReturn(ID)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some timestamp will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("constructor will be encoded") {
            given(functionEncoderService.encodeConstructor(arguments = CREATE_PARAMS.constructorParams))
                .willReturn(ENCODED_CONSTRUCTOR)
        }

        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("contract deployment request is stored in database") {
            given(contractDeploymentRequestRepository.store(STORE_PARAMS))
                .willReturn(STORED_REQUEST)
        }

        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepositoryMock(exists = true),
            contractDecoratorRepository = contractDecoratorRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = uuidProvider,
                utcDateTimeProvider = utcDateTimeProvider,
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("contract deployment request is correctly created") {
            assertThat(service.createContractDeploymentRequest(CREATE_PARAMS, PROJECT)).withMessage()
                .isEqualTo(STORED_REQUEST)

            verifyMock(contractDeploymentRequestRepository)
                .store(STORE_PARAMS)
            verifyNoMoreInteractions(contractDeploymentRequestRepository)
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionForNonExistentContractDecorator() {
        val contractDecoratorRepository = mock<ContractDecoratorRepository>()

        suppose("null will be returned when fetching contract decorator") {
            given(contractDecoratorRepository.getById(CONTRACT_ID))
                .willReturn(null)
        }

        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = mock(),
            contractDecoratorRepository = contractDecoratorRepository,
            contractMetadataRepository = contractMetadataRepositoryMock(exists = true),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.createContractDeploymentRequest(CREATE_PARAMS, PROJECT)
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionForNonExistentContractMetadata() {
        val contractDecoratorRepository = mock<ContractDecoratorRepository>()

        suppose("some contract decorator will be returned") {
            given(contractDecoratorRepository.getById(CONTRACT_ID))
                .willReturn(CONTRACT_DECORATOR)
        }

        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = mock(),
            contractMetadataRepository = contractMetadataRepositoryMock(exists = false),
            contractDecoratorRepository = contractDecoratorRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.createContractDeploymentRequest(CREATE_PARAMS, PROJECT)
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionForNonExistentContractDeploymentRequest() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("contract deployment request does not exist in database") {
            given(contractDeploymentRequestRepository.getById(any()))
                .willReturn(null)
        }

        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepositoryMock(exists = true),
            contractDecoratorRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.getContractDeploymentRequest(id = UUID.randomUUID())
            }
        }
    }

    @Test
    fun mustReturnContractDeploymentRequestWithPendingStatusWhenContractDeploymentRequestHasNullTxHash() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()
        val request = STORED_REQUEST.copy(
            txHash = null,
            contractAddress = null
        )

        suppose("contract deployment request exists in database") {
            given(contractDeploymentRequestRepository.getById(ID))
                .willReturn(request)
        }

        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepositoryMock(exists = true),
            contractDecoratorRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = projectRepositoryMock(PROJECT.id)
        )

        verify("contract deployment request with pending status is returned") {
            assertThat(service.getContractDeploymentRequest(ID)).withMessage()
                .isEqualTo(
                    request.withTransactionData(
                        status = Status.PENDING,
                        transactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnContractDeploymentRequestWithPendingStatusWhenTransactionIsNotYetMined() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("contract deployment request exists in database") {
            given(contractDeploymentRequestRepository.getById(ID))
                .willReturn(STORED_REQUEST)
        }

        val blockchainService = mock<BlockchainService>()

        suppose("transaction is not yet mined") {
            given(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH))
                .willReturn(null)
        }

        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepositoryMock(exists = true),
            contractDecoratorRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(PROJECT.id)
        )

        verify("contract deployment request with pending status is returned") {
            assertThat(service.getContractDeploymentRequest(ID)).withMessage()
                .isEqualTo(
                    STORED_REQUEST.withTransactionData(
                        status = Status.PENDING,
                        transactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnContractDeploymentRequestWithFailedStatusWhenTransactionIsNotSuccessful() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("contract deployment request exists in database") {
            given(contractDeploymentRequestRepository.getById(ID))
                .willReturn(STORED_REQUEST)
        }

        val blockchainService = mock<BlockchainService>()
        val transactionInfo = TRANSACTION_INFO.copy(success = false)

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH))
                .willReturn(transactionInfo)
        }

        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepositoryMock(exists = true),
            contractDecoratorRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(PROJECT.id)
        )

        verify("contract deployment request with failed status is returned") {
            assertThat(service.getContractDeploymentRequest(ID)).withMessage()
                .isEqualTo(
                    STORED_REQUEST.withTransactionData(
                        status = Status.FAILED,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnContractDeploymentRequestWithFailedStatusWhenTransactionHasWrongTxHash() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("contract deployment request exists in database") {
            given(contractDeploymentRequestRepository.getById(ID))
                .willReturn(STORED_REQUEST)
        }

        val blockchainService = mock<BlockchainService>()
        val transactionInfo = TRANSACTION_INFO.copy(hash = TransactionHash("wrong"))

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH))
                .willReturn(transactionInfo)
        }

        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepositoryMock(exists = true),
            contractDecoratorRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(PROJECT.id)
        )

        verify("contract deployment request with failed status is returned") {
            assertThat(service.getContractDeploymentRequest(ID)).withMessage()
                .isEqualTo(
                    STORED_REQUEST.withTransactionData(
                        status = Status.FAILED,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnContractDeploymentRequestWithFailedStatusWhenTransactionHasWrongDeployerAddress() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("contract deployment request exists in database") {
            given(contractDeploymentRequestRepository.getById(ID))
                .willReturn(STORED_REQUEST)
        }

        val blockchainService = mock<BlockchainService>()
        val transactionInfo = TRANSACTION_INFO.copy(from = WalletAddress("1337"))

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH))
                .willReturn(transactionInfo)
        }

        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepositoryMock(exists = true),
            contractDecoratorRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(PROJECT.id)
        )

        verify("contract deployment request with failed status is returned") {
            assertThat(service.getContractDeploymentRequest(ID)).withMessage()
                .isEqualTo(
                    STORED_REQUEST.withTransactionData(
                        status = Status.FAILED,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnContractDeploymentRequestWithFailedStatusWhenTransactionHasNullContractAddress() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("contract deployment request exists in database") {
            given(contractDeploymentRequestRepository.getById(ID))
                .willReturn(STORED_REQUEST)
        }

        val blockchainService = mock<BlockchainService>()
        val transactionInfo = TRANSACTION_INFO.copy(deployedContractAddress = null)

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH))
                .willReturn(transactionInfo)
        }

        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepositoryMock(exists = true),
            contractDecoratorRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(PROJECT.id)
        )

        verify("contract deployment request with failed status is returned") {
            assertThat(service.getContractDeploymentRequest(ID)).withMessage()
                .isEqualTo(
                    STORED_REQUEST.withTransactionData(
                        status = Status.FAILED,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnContractDeploymentRequestWithFailedStatusWhenTransactionHasWrongContractAddress() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("contract deployment request exists in database") {
            given(contractDeploymentRequestRepository.getById(ID))
                .willReturn(STORED_REQUEST)
        }

        val blockchainService = mock<BlockchainService>()
        val transactionInfo = TRANSACTION_INFO.copy(deployedContractAddress = ContractAddress("1337"))

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH))
                .willReturn(transactionInfo)
        }

        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepositoryMock(exists = true),
            contractDecoratorRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(PROJECT.id)
        )

        verify("contract deployment request with failed status is returned") {
            assertThat(service.getContractDeploymentRequest(ID)).withMessage()
                .isEqualTo(
                    STORED_REQUEST.withTransactionData(
                        status = Status.FAILED,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnContractDeploymentRequestWithFailedStatusWhenTransactionHasWrongData() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("contract deployment request exists in database") {
            given(contractDeploymentRequestRepository.getById(ID))
                .willReturn(STORED_REQUEST)
        }

        val blockchainService = mock<BlockchainService>()
        val transactionInfo = TRANSACTION_INFO.copy(data = FunctionData("wrong"))

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH))
                .willReturn(transactionInfo)
        }

        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepositoryMock(exists = true),
            contractDecoratorRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(PROJECT.id)
        )

        verify("contract deployment request with failed status is returned") {
            assertThat(service.getContractDeploymentRequest(ID)).withMessage()
                .isEqualTo(
                    STORED_REQUEST.withTransactionData(
                        status = Status.FAILED,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnContractDeploymentRequestWithFailedStatusWhenTransactionHasWrongValue() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("contract deployment request exists in database") {
            given(contractDeploymentRequestRepository.getById(ID))
                .willReturn(STORED_REQUEST)
        }

        val blockchainService = mock<BlockchainService>()
        val transactionInfo = TRANSACTION_INFO.copy(value = Balance(BigInteger("123456789")))

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH))
                .willReturn(transactionInfo)
        }

        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepositoryMock(exists = true),
            contractDecoratorRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(PROJECT.id)
        )

        verify("contract deployment request with failed status is returned") {
            assertThat(service.getContractDeploymentRequest(ID)).withMessage()
                .isEqualTo(
                    STORED_REQUEST.withTransactionData(
                        status = Status.FAILED,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnContractDeploymentRequestWithSuccessfulStatusWhenDeployerAddressIsNull() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()
        val request = STORED_REQUEST.copy(deployerAddress = null)

        suppose("contract deployment request exists in database") {
            given(contractDeploymentRequestRepository.getById(ID))
                .willReturn(request)
        }

        val blockchainService = mock<BlockchainService>()

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH))
                .willReturn(TRANSACTION_INFO)
        }

        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepositoryMock(exists = true),
            contractDecoratorRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(PROJECT.id)
        )

        verify("contract deployment request with successful status is returned") {
            assertThat(service.getContractDeploymentRequest(ID)).withMessage()
                .isEqualTo(
                    request.withTransactionData(
                        status = Status.SUCCESS,
                        transactionInfo = TRANSACTION_INFO
                    )
                )
        }
    }

    @Test
    fun mustReturnContractDeploymentRequestWithSuccessfulStatusWhenDeployerAddressIsNotNull() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("contract deployment request exists in database") {
            given(contractDeploymentRequestRepository.getById(ID))
                .willReturn(STORED_REQUEST)
        }

        val blockchainService = mock<BlockchainService>()

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH))
                .willReturn(TRANSACTION_INFO)
        }

        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepositoryMock(exists = true),
            contractDecoratorRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(PROJECT.id)
        )

        verify("contract deployment request with successful status is returned") {
            assertThat(service.getContractDeploymentRequest(ID)).withMessage()
                .isEqualTo(
                    STORED_REQUEST.withTransactionData(
                        status = Status.SUCCESS,
                        transactionInfo = TRANSACTION_INFO
                    )
                )
        }
    }

    @Test
    fun mustReturnContractDeploymentRequestWithSuccessfulStatusAndSetContractAddress() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("contract deployment request exists in database") {
            given(contractDeploymentRequestRepository.getById(ID))
                .willReturn(STORED_REQUEST.copy(contractAddress = null))
        }

        suppose("setting contract address will succeed") {
            given(contractDeploymentRequestRepository.setContractAddress(ID, CONTRACT_ADDRESS))
                .willReturn(true)
        }

        val blockchainService = mock<BlockchainService>()

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH))
                .willReturn(TRANSACTION_INFO)
        }

        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepositoryMock(exists = true),
            contractDecoratorRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(PROJECT.id)
        )

        verify("contract deployment request with successful status is returned") {
            assertThat(service.getContractDeploymentRequest(ID)).withMessage()
                .isEqualTo(
                    STORED_REQUEST.withTransactionData(
                        status = Status.SUCCESS,
                        transactionInfo = TRANSACTION_INFO
                    )
                )

            verifyMock(contractDeploymentRequestRepository)
                .getById(ID)
            verifyMock(contractDeploymentRequestRepository)
                .setContractAddress(ID, CONTRACT_ADDRESS)
            verifyNoMoreInteractions(contractDeploymentRequestRepository)
        }
    }

    @Test
    fun mustCorrectlyReturnListOfContractDeploymentRequestsByProjectId() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()
        val filters = ContractDeploymentRequestFilters(
            contractIds = OrList(),
            contractTags = OrList(),
            contractImplements = OrList(),
            deployedOnly = false
        )

        val pendingRequest = STORED_REQUEST.copy(contractAddress = null, txHash = TransactionHash("other-tx-hash"))

        suppose("contract deployment request exists in database") {
            given(contractDeploymentRequestRepository.getAllByProjectId(PROJECT.id, filters))
                .willReturn(
                    listOf(
                        STORED_REQUEST,
                        pendingRequest
                    )
                )
        }

        val blockchainService = mock<BlockchainService>()

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH))
                .willReturn(TRANSACTION_INFO)
        }

        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepositoryMock(exists = true),
            contractDecoratorRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(PROJECT.id)
        )

        verify("contract deployment request with successful status is returned") {
            assertThat(service.getContractDeploymentRequestsByProjectIdAndFilters(PROJECT.id, filters))
                .withMessage()
                .isEqualTo(
                    listOf(
                        STORED_REQUEST.withTransactionData(
                            status = Status.SUCCESS,
                            transactionInfo = TRANSACTION_INFO
                        ),
                        pendingRequest.withTransactionData(
                            status = Status.PENDING,
                            transactionInfo = null
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyReturnListOfContractDeploymentRequestsByProjectIdAndDeployedFilter() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()
        val filters = ContractDeploymentRequestFilters(
            contractIds = OrList(),
            contractTags = OrList(),
            contractImplements = OrList(),
            deployedOnly = true
        )

        suppose("contract deployment request exists in database") {
            given(contractDeploymentRequestRepository.getAllByProjectId(PROJECT.id, filters))
                .willReturn(
                    listOf(
                        STORED_REQUEST,
                        STORED_REQUEST.copy(contractAddress = null, txHash = TransactionHash("other-tx-hash"))
                    )
                )
        }

        val blockchainService = mock<BlockchainService>()

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH))
                .willReturn(TRANSACTION_INFO)
        }

        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepositoryMock(exists = true),
            contractDecoratorRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(PROJECT.id)
        )

        verify("contract deployment request with successful status is returned") {
            assertThat(service.getContractDeploymentRequestsByProjectIdAndFilters(PROJECT.id, filters))
                .withMessage()
                .isEqualTo(
                    listOf(
                        STORED_REQUEST.withTransactionData(
                            status = Status.SUCCESS,
                            transactionInfo = TRANSACTION_INFO
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyReturnEmptyListOfContractDeploymentRequestsForNonExistentProject() {
        val projectId = UUID.randomUUID()
        val filters = ContractDeploymentRequestFilters(
            contractIds = OrList(),
            contractTags = OrList(),
            contractImplements = OrList(),
            deployedOnly = false
        )
        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = mock(),
            contractMetadataRepository = contractMetadataRepositoryMock(exists = true),
            contractDecoratorRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = projectRepositoryMock(projectId)
        )

        verify("empty list is returned") {
            val result = service.getContractDeploymentRequestsByProjectIdAndFilters(PROJECT.id, filters)

            assertThat(result).withMessage()
                .isEmpty()
        }
    }

    @Test
    fun mustSuccessfullyAttachTxInfo() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()
        val deployer = WalletAddress("0xbc25524e0daacB1F149BA55279f593F5E3FB73e9")

        suppose("txInfo will be successfully attached to the request") {
            given(contractDeploymentRequestRepository.setTxInfo(ID, TX_HASH, deployer))
                .willReturn(true)
        }

        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepositoryMock(exists = true),
            contractDecoratorRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = projectRepositoryMock(PROJECT.id)
        )

        verify("txInfo was successfully attached") {
            service.attachTxInfo(ID, TX_HASH, deployer)

            verifyMock(contractDeploymentRequestRepository)
                .setTxInfo(ID, TX_HASH, deployer)
            verifyNoMoreInteractions(contractDeploymentRequestRepository)
        }
    }

    @Test
    fun mustThrowCannotAttachTxInfoExceptionWhenAttachingTxInfoFails() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()
        val deployer = WalletAddress("0xbc25524e0daacB1F149BA55279f593F5E3FB73e9")

        suppose("attaching txInfo will fail") {
            given(contractDeploymentRequestRepository.setTxInfo(ID, TX_HASH, deployer))
                .willReturn(false)
        }

        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepositoryMock(exists = true),
            contractDecoratorRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = projectRepositoryMock(PROJECT.id)
        )

        verify("CannotAttachTxInfoException is thrown") {
            assertThrows<CannotAttachTxInfoException>(message) {
                service.attachTxInfo(ID, TX_HASH, deployer)
            }

            verifyMock(contractDeploymentRequestRepository)
                .setTxInfo(ID, TX_HASH, deployer)
            verifyNoMoreInteractions(contractDeploymentRequestRepository)
        }
    }

    private fun projectRepositoryMock(projectId: UUID): ProjectRepository {
        val projectRepository = mock<ProjectRepository>()

        given(projectRepository.getById(projectId))
            .willReturn(
                Project(
                    id = projectId,
                    ownerId = UUID.randomUUID(),
                    issuerContractAddress = ContractAddress("dead"),
                    baseRedirectUrl = BaseUrl(""),
                    chainId = ChainId(0L),
                    customRpcUrl = null,
                    createdAt = TestData.TIMESTAMP
                )
            )

        return projectRepository
    }

    private fun contractMetadataRepositoryMock(exists: Boolean): ContractMetadataRepository {
        val contractMetadataRepository = mock<ContractMetadataRepository>()

        given(contractMetadataRepository.exists(CONTRACT_ID))
            .willReturn(exists)

        return contractMetadataRepository
    }
}
