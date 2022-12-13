package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.blockchain.BlockchainService
import dev3.blockchainapiservice.blockchain.properties.ChainSpec
import dev3.blockchainapiservice.exception.CannotAttachTxInfoException
import dev3.blockchainapiservice.exception.ResourceNotFoundException
import dev3.blockchainapiservice.model.DeserializableEvent
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.model.params.CreateErc20LockRequestParams
import dev3.blockchainapiservice.model.params.StoreErc20LockRequestParams
import dev3.blockchainapiservice.model.result.BlockchainTransactionInfo
import dev3.blockchainapiservice.model.result.Erc20LockRequest
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.repository.Erc20LockRequestRepository
import dev3.blockchainapiservice.repository.ProjectRepository
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.BaseUrl
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.DurationSeconds
import dev3.blockchainapiservice.util.FunctionArgument
import dev3.blockchainapiservice.util.FunctionData
import dev3.blockchainapiservice.util.Status
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress
import dev3.blockchainapiservice.util.WithFunctionData
import dev3.blockchainapiservice.util.ZeroAddress
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

class Erc20LockRequestServiceTest : TestBase() {

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
        private val CREATE_PARAMS = CreateErc20LockRequestParams(
            redirectUrl = "redirect-url/\${id}",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.valueOf(123456L)),
            lockDuration = DurationSeconds(BigInteger.valueOf(123L)),
            lockContractAddress = ContractAddress("b"),
            tokenSenderAddress = WalletAddress("c"),
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            )
        )
        private val TX_HASH = TransactionHash("tx-hash")
        private val EVENTS = listOf<DeserializableEvent>()
    }

    @Test
    fun mustSuccessfullyCreateErc20LockRequest() {
        val uuidProvider = mock<UuidProvider>()
        val id = UUID.randomUUID()

        suppose("some UUID will be generated") {
            given(uuidProvider.getUuid())
                .willReturn(id)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some timestamp will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val functionEncoderService = mock<FunctionEncoderService>()
        val encodedData = FunctionData("encoded")

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "lock",
                    arguments = listOf(
                        FunctionArgument(CREATE_PARAMS.tokenAddress),
                        FunctionArgument(CREATE_PARAMS.tokenAmount),
                        FunctionArgument(CREATE_PARAMS.lockDuration),
                        FunctionArgument(id.toString()),
                        FunctionArgument(ZeroAddress)
                    )
                )
            )
                .willReturn(encodedData)
        }

        val erc20LockRequestRepository = mock<Erc20LockRequestRepository>()
        val redirectUrl = CREATE_PARAMS.redirectUrl!!

        val storeParams = StoreErc20LockRequestParams(
            id = id,
            projectId = PROJECT.id,
            chainId = PROJECT.chainId,
            redirectUrl = redirectUrl.replace("\${id}", id.toString()),
            tokenAddress = CREATE_PARAMS.tokenAddress,
            tokenAmount = CREATE_PARAMS.tokenAmount,
            lockDuration = CREATE_PARAMS.lockDuration,
            lockContractAddress = CREATE_PARAMS.lockContractAddress,
            tokenSenderAddress = CREATE_PARAMS.tokenSenderAddress,
            arbitraryData = CREATE_PARAMS.arbitraryData,
            screenConfig = CREATE_PARAMS.screenConfig,
            createdAt = TestData.TIMESTAMP
        )

        val storedRequest = Erc20LockRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = PROJECT.chainId,
            redirectUrl = storeParams.redirectUrl,
            tokenAddress = CREATE_PARAMS.tokenAddress,
            tokenAmount = CREATE_PARAMS.tokenAmount,
            lockDuration = CREATE_PARAMS.lockDuration,
            lockContractAddress = CREATE_PARAMS.lockContractAddress,
            tokenSenderAddress = CREATE_PARAMS.tokenSenderAddress,
            txHash = null,
            arbitraryData = CREATE_PARAMS.arbitraryData,
            screenConfig = CREATE_PARAMS.screenConfig,
            createdAt = TestData.TIMESTAMP
        )

        suppose("ERC20 lock request is stored in database") {
            given(erc20LockRequestRepository.store(storeParams))
                .willReturn(storedRequest)
        }

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20LockRequestRepository = erc20LockRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = uuidProvider,
                utcDateTimeProvider = utcDateTimeProvider,
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("ERC20 lock request is correctly created") {
            assertThat(service.createErc20LockRequest(CREATE_PARAMS, PROJECT)).withMessage()
                .isEqualTo(WithFunctionData(storedRequest, encodedData))

            verifyMock(erc20LockRequestRepository)
                .store(storeParams)
            verifyNoMoreInteractions(erc20LockRequestRepository)
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionForNonExistentErc20LockRequest() {
        val erc20LockRequestRepository = mock<Erc20LockRequestRepository>()

        suppose("ERC20 lock request does not exist in database") {
            given(erc20LockRequestRepository.getById(any()))
                .willReturn(null)
        }

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = mock(),
            erc20LockRequestRepository = erc20LockRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.getErc20LockRequest(id = UUID.randomUUID())
            }
        }
    }

    @Test
    fun mustReturnErc20LockRequestWithPendingStatusWhenErc20LockRequestHasNullTxHash() {
        val id = UUID.randomUUID()
        val lockRequest = Erc20LockRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = TestData.CHAIN_ID,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            lockDuration = CREATE_PARAMS.lockDuration,
            lockContractAddress = ContractAddress("b"),
            tokenSenderAddress = WalletAddress("c"),
            txHash = null,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val erc20LockRequestRepository = mock<Erc20LockRequestRepository>()

        suppose("ERC20 lock request exists in database") {
            given(erc20LockRequestRepository.getById(id))
                .willReturn(lockRequest)
        }

        val functionEncoderService = mock<FunctionEncoderService>()
        val encodedData = FunctionData("encoded")

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "lock",
                    arguments = listOf(
                        FunctionArgument(lockRequest.tokenAddress),
                        FunctionArgument(lockRequest.tokenAmount),
                        FunctionArgument(lockRequest.lockDuration),
                        FunctionArgument(id.toString()),
                        FunctionArgument(ZeroAddress)
                    )
                )
            )
                .willReturn(encodedData)
        }

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20LockRequestRepository = erc20LockRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = projectRepositoryMock(lockRequest.projectId)
        )

        verify("ERC20 lock request with pending status is returned") {
            assertThat(service.getErc20LockRequest(id)).withMessage()
                .isEqualTo(
                    lockRequest.withTransactionData(
                        status = Status.PENDING,
                        data = encodedData,
                        transactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20LockRequestWithPendingStatusWhenTransactionIsNotYetMined() {
        val id = UUID.randomUUID()
        val lockRequest = Erc20LockRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = TestData.CHAIN_ID,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            lockDuration = CREATE_PARAMS.lockDuration,
            lockContractAddress = ContractAddress("b"),
            tokenSenderAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val erc20LockRequestRepository = mock<Erc20LockRequestRepository>()

        suppose("ERC20 lock request exists in database") {
            given(erc20LockRequestRepository.getById(id))
                .willReturn(lockRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(lockRequest.chainId, null)

        suppose("transaction is not yet mined") {
            given(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH, EVENTS))
                .willReturn(null)
        }

        val functionEncoderService = mock<FunctionEncoderService>()
        val encodedData = FunctionData("encoded")

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "lock",
                    arguments = listOf(
                        FunctionArgument(lockRequest.tokenAddress),
                        FunctionArgument(lockRequest.tokenAmount),
                        FunctionArgument(lockRequest.lockDuration),
                        FunctionArgument(id.toString()),
                        FunctionArgument(ZeroAddress)
                    )
                )
            )
                .willReturn(encodedData)
        }

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20LockRequestRepository = erc20LockRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = projectRepositoryMock(lockRequest.projectId)
        )

        verify("ERC20 lock request with pending status is returned") {
            assertThat(service.getErc20LockRequest(id)).withMessage()
                .isEqualTo(
                    lockRequest.withTransactionData(
                        status = Status.PENDING,
                        data = encodedData,
                        transactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20LockRequestWithFailedStatusWhenTransactionIsNotSuccessful() {
        val id = UUID.randomUUID()
        val lockRequest = Erc20LockRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = TestData.CHAIN_ID,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            lockDuration = CREATE_PARAMS.lockDuration,
            lockContractAddress = ContractAddress("b"),
            tokenSenderAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val erc20LockRequestRepository = mock<Erc20LockRequestRepository>()

        suppose("ERC20 lock request exists in database") {
            given(erc20LockRequestRepository.getById(id))
                .willReturn(lockRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(lockRequest.chainId, null)
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = lockRequest.tokenSenderAddress!!,
            to = lockRequest.lockContractAddress,
            deployedContractAddress = null,
            data = encodedData,
            value = Balance.ZERO,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = false,
            events = emptyList()
        )

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH, EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "lock",
                    arguments = listOf(
                        FunctionArgument(lockRequest.tokenAddress),
                        FunctionArgument(lockRequest.tokenAmount),
                        FunctionArgument(lockRequest.lockDuration),
                        FunctionArgument(id.toString()),
                        FunctionArgument(ZeroAddress)
                    )
                )
            )
                .willReturn(encodedData)
        }

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20LockRequestRepository = erc20LockRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(lockRequest.projectId)
        )

        verify("ERC20 lock request with failed status is returned") {
            assertThat(service.getErc20LockRequest(id)).withMessage()
                .isEqualTo(
                    lockRequest.withTransactionData(
                        status = Status.FAILED,
                        data = encodedData,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20LockRequestWithFailedStatusWhenTransactionHasWrongToAddress() {
        val id = UUID.randomUUID()
        val lockRequest = Erc20LockRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = TestData.CHAIN_ID,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            lockDuration = CREATE_PARAMS.lockDuration,
            lockContractAddress = ContractAddress("b"),
            tokenSenderAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val erc20LockRequestRepository = mock<Erc20LockRequestRepository>()

        suppose("ERC20 lock request exists in database") {
            given(erc20LockRequestRepository.getById(id))
                .willReturn(lockRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(lockRequest.chainId, null)
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = lockRequest.tokenSenderAddress!!,
            to = WalletAddress("dead"),
            deployedContractAddress = null,
            data = encodedData,
            value = Balance.ZERO,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true,
            events = emptyList()
        )

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH, EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "lock",
                    arguments = listOf(
                        FunctionArgument(lockRequest.tokenAddress),
                        FunctionArgument(lockRequest.tokenAmount),
                        FunctionArgument(lockRequest.lockDuration),
                        FunctionArgument(id.toString()),
                        FunctionArgument(ZeroAddress)
                    )
                )
            )
                .willReturn(encodedData)
        }

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20LockRequestRepository = erc20LockRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(lockRequest.projectId)
        )

        verify("ERC20 lock request with failed status is returned") {
            assertThat(service.getErc20LockRequest(id)).withMessage()
                .isEqualTo(
                    lockRequest.withTransactionData(
                        status = Status.FAILED,
                        data = encodedData,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20LockRequestWithFailedStatusWhenTransactionHasWrongTxHash() {
        val id = UUID.randomUUID()
        val lockRequest = Erc20LockRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = TestData.CHAIN_ID,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            lockDuration = CREATE_PARAMS.lockDuration,
            lockContractAddress = ContractAddress("b"),
            tokenSenderAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val erc20LockRequestRepository = mock<Erc20LockRequestRepository>()

        suppose("ERC20 lock request exists in database") {
            given(erc20LockRequestRepository.getById(id))
                .willReturn(lockRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(lockRequest.chainId, null)
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TransactionHash("wrong-hash"),
            from = lockRequest.tokenSenderAddress!!,
            to = lockRequest.lockContractAddress,
            deployedContractAddress = null,
            data = encodedData,
            value = Balance.ZERO,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true,
            events = emptyList()
        )

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH, EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "lock",
                    arguments = listOf(
                        FunctionArgument(lockRequest.tokenAddress),
                        FunctionArgument(lockRequest.tokenAmount),
                        FunctionArgument(lockRequest.lockDuration),
                        FunctionArgument(id.toString()),
                        FunctionArgument(ZeroAddress)
                    )
                )
            )
                .willReturn(encodedData)
        }

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20LockRequestRepository = erc20LockRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(lockRequest.projectId)
        )

        verify("ERC20 lock request with failed status is returned") {
            assertThat(service.getErc20LockRequest(id)).withMessage()
                .isEqualTo(
                    lockRequest.withTransactionData(
                        status = Status.FAILED,
                        data = encodedData,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20LockRequestWithFailedStatusWhenTransactionHasWrongFromAddress() {
        val id = UUID.randomUUID()
        val lockRequest = Erc20LockRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = TestData.CHAIN_ID,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            lockDuration = CREATE_PARAMS.lockDuration,
            lockContractAddress = ContractAddress("b"),
            tokenSenderAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val erc20LockRequestRepository = mock<Erc20LockRequestRepository>()

        suppose("ERC20 lock request exists in database") {
            given(erc20LockRequestRepository.getById(id))
                .willReturn(lockRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(lockRequest.chainId, null)
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = WalletAddress("dead"),
            to = lockRequest.lockContractAddress,
            deployedContractAddress = null,
            data = encodedData,
            value = Balance.ZERO,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true,
            events = emptyList()
        )

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH, EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "lock",
                    arguments = listOf(
                        FunctionArgument(lockRequest.tokenAddress),
                        FunctionArgument(lockRequest.tokenAmount),
                        FunctionArgument(lockRequest.lockDuration),
                        FunctionArgument(id.toString()),
                        FunctionArgument(ZeroAddress)
                    )
                )
            )
                .willReturn(encodedData)
        }

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20LockRequestRepository = erc20LockRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(lockRequest.projectId)
        )

        verify("ERC20 lock request with failed status is returned") {
            assertThat(service.getErc20LockRequest(id)).withMessage()
                .isEqualTo(
                    lockRequest.withTransactionData(
                        status = Status.FAILED,
                        data = encodedData,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20LockRequestWithFailedStatusWhenTransactionHasWrongData() {
        val id = UUID.randomUUID()
        val lockRequest = Erc20LockRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = TestData.CHAIN_ID,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            lockDuration = CREATE_PARAMS.lockDuration,
            lockContractAddress = ContractAddress("b"),
            tokenSenderAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val erc20LockRequestRepository = mock<Erc20LockRequestRepository>()

        suppose("ERC20 lock request exists in database") {
            given(erc20LockRequestRepository.getById(id))
                .willReturn(lockRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(lockRequest.chainId, null)
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = lockRequest.tokenSenderAddress!!,
            to = lockRequest.lockContractAddress,
            deployedContractAddress = null,
            data = FunctionData("wrong-data"),
            value = Balance.ZERO,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true,
            events = emptyList()
        )

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH, EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "lock",
                    arguments = listOf(
                        FunctionArgument(lockRequest.tokenAddress),
                        FunctionArgument(lockRequest.tokenAmount),
                        FunctionArgument(lockRequest.lockDuration),
                        FunctionArgument(id.toString()),
                        FunctionArgument(ZeroAddress)
                    )
                )
            )
                .willReturn(encodedData)
        }

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20LockRequestRepository = erc20LockRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(lockRequest.projectId)
        )

        verify("ERC20 lock request with failed status is returned") {
            assertThat(service.getErc20LockRequest(id)).withMessage()
                .isEqualTo(
                    lockRequest.withTransactionData(
                        status = Status.FAILED,
                        data = encodedData,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20LockRequestWithSuccessfulStatusWhenFromAddressIsNull() {
        val id = UUID.randomUUID()
        val lockRequest = Erc20LockRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = TestData.CHAIN_ID,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            lockDuration = CREATE_PARAMS.lockDuration,
            lockContractAddress = ContractAddress("b"),
            tokenSenderAddress = null,
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val erc20LockRequestRepository = mock<Erc20LockRequestRepository>()

        suppose("ERC20 lock request exists in database") {
            given(erc20LockRequestRepository.getById(id))
                .willReturn(lockRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(lockRequest.chainId, null)
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = WalletAddress("0cafe0babe"),
            to = lockRequest.lockContractAddress,
            deployedContractAddress = null,
            data = encodedData,
            value = Balance.ZERO,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true,
            events = emptyList()
        )

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH, EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "lock",
                    arguments = listOf(
                        FunctionArgument(lockRequest.tokenAddress),
                        FunctionArgument(lockRequest.tokenAmount),
                        FunctionArgument(lockRequest.lockDuration),
                        FunctionArgument(id.toString()),
                        FunctionArgument(ZeroAddress)
                    )
                )
            )
                .willReturn(encodedData)
        }

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20LockRequestRepository = erc20LockRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(lockRequest.projectId)
        )

        verify("ERC20 lock request with successful status is returned") {
            assertThat(service.getErc20LockRequest(id)).withMessage()
                .isEqualTo(
                    lockRequest.withTransactionData(
                        status = Status.SUCCESS,
                        data = encodedData,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20LockRequestWithSuccessfulStatusWhenFromAddressIsSpecified() {
        val id = UUID.randomUUID()
        val lockRequest = Erc20LockRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = TestData.CHAIN_ID,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            lockDuration = CREATE_PARAMS.lockDuration,
            lockContractAddress = ContractAddress("b"),
            tokenSenderAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val erc20LockRequestRepository = mock<Erc20LockRequestRepository>()

        suppose("ERC20 lock request exists in database") {
            given(erc20LockRequestRepository.getById(id))
                .willReturn(lockRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(lockRequest.chainId, null)
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = lockRequest.tokenSenderAddress!!,
            to = lockRequest.lockContractAddress,
            deployedContractAddress = null,
            data = encodedData,
            value = Balance.ZERO,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true,
            events = emptyList()
        )

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH, EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "lock",
                    arguments = listOf(
                        FunctionArgument(lockRequest.tokenAddress),
                        FunctionArgument(lockRequest.tokenAmount),
                        FunctionArgument(lockRequest.lockDuration),
                        FunctionArgument(id.toString()),
                        FunctionArgument(ZeroAddress)
                    )
                )
            )
                .willReturn(encodedData)
        }

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20LockRequestRepository = erc20LockRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(lockRequest.projectId)
        )

        verify("ERC20 lock request with successful status is returned") {
            assertThat(service.getErc20LockRequest(id)).withMessage()
                .isEqualTo(
                    lockRequest.withTransactionData(
                        status = Status.SUCCESS,
                        data = encodedData,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyReturnListOfErc20LockRequestsByProjectId() {
        val id = UUID.randomUUID()
        val lockRequest = Erc20LockRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = TestData.CHAIN_ID,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            lockDuration = CREATE_PARAMS.lockDuration,
            lockContractAddress = ContractAddress("b"),
            tokenSenderAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val erc20LockRequestRepository = mock<Erc20LockRequestRepository>()

        suppose("ERC20 lock request exists in database") {
            given(erc20LockRequestRepository.getAllByProjectId(PROJECT.id))
                .willReturn(listOf(lockRequest))
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(lockRequest.chainId, null)
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = lockRequest.tokenSenderAddress!!,
            to = lockRequest.lockContractAddress,
            deployedContractAddress = null,
            data = encodedData,
            value = Balance.ZERO,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true,
            events = emptyList()
        )

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH, EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "lock",
                    arguments = listOf(
                        FunctionArgument(lockRequest.tokenAddress),
                        FunctionArgument(lockRequest.tokenAmount),
                        FunctionArgument(lockRequest.lockDuration),
                        FunctionArgument(id.toString()),
                        FunctionArgument(ZeroAddress)
                    )
                )
            )
                .willReturn(encodedData)
        }

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20LockRequestRepository = erc20LockRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(lockRequest.projectId)
        )

        verify("ERC20 lock request with successful status is returned") {
            assertThat(service.getErc20LockRequestsByProjectId(PROJECT.id))
                .withMessage()
                .isEqualTo(
                    listOf(
                        lockRequest.withTransactionData(
                            status = Status.SUCCESS,
                            data = encodedData,
                            transactionInfo = transactionInfo
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyReturnEmptyListOfErc20LockRequestsForNonExistentProject() {
        val projectId = UUID.randomUUID()
        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = mock(),
            erc20LockRequestRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = projectRepositoryMock(projectId)
        )

        verify("empty list is returned") {
            val result = service.getErc20LockRequestsByProjectId(projectId)

            assertThat(result).withMessage()
                .isEmpty()
        }
    }

    @Test
    fun mustSuccessfullyAttachTxInfo() {
        val erc20LockRequestRepository = mock<Erc20LockRequestRepository>()
        val id = UUID.randomUUID()
        val caller = WalletAddress("0xbc25524e0daacB1F149BA55279f593F5E3FB73e9")

        suppose("txInfo will be successfully attached to the request") {
            given(erc20LockRequestRepository.setTxInfo(id, TX_HASH, caller))
                .willReturn(true)
        }

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = mock(),
            erc20LockRequestRepository = erc20LockRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("txInfo was successfully attached") {
            service.attachTxInfo(id, TX_HASH, caller)

            verifyMock(erc20LockRequestRepository)
                .setTxInfo(id, TX_HASH, caller)
            verifyNoMoreInteractions(erc20LockRequestRepository)
        }
    }

    @Test
    fun mustThrowCannotAttachTxInfoExceptionWhenAttachingTxInfoFails() {
        val erc20LockRequestRepository = mock<Erc20LockRequestRepository>()
        val id = UUID.randomUUID()
        val caller = WalletAddress("0xbc25524e0daacB1F149BA55279f593F5E3FB73e9")

        suppose("attaching txInfo will fail") {
            given(erc20LockRequestRepository.setTxInfo(id, TX_HASH, caller))
                .willReturn(false)
        }

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = mock(),
            erc20LockRequestRepository = erc20LockRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("CannotAttachTxInfoException is thrown") {
            assertThrows<CannotAttachTxInfoException>(message) {
                service.attachTxInfo(id, TX_HASH, caller)
            }

            verifyMock(erc20LockRequestRepository)
                .setTxInfo(id, TX_HASH, caller)
            verifyNoMoreInteractions(erc20LockRequestRepository)
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
}
