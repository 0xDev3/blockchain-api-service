package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.blockchain.BlockchainService
import com.ampnet.blockchainapiservice.blockchain.properties.Chain
import com.ampnet.blockchainapiservice.blockchain.properties.ChainSpec
import com.ampnet.blockchainapiservice.exception.CannotAttachTxInfoException
import com.ampnet.blockchainapiservice.exception.ResourceNotFoundException
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.params.CreateErc20LockRequestParams
import com.ampnet.blockchainapiservice.model.params.StoreErc20LockRequestParams
import com.ampnet.blockchainapiservice.model.result.BlockchainTransactionInfo
import com.ampnet.blockchainapiservice.model.result.Erc20LockRequest
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.repository.Erc20LockRequestRepository
import com.ampnet.blockchainapiservice.repository.ProjectRepository
import com.ampnet.blockchainapiservice.util.AbiType.AbiType
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.BaseUrl
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.DurationSeconds
import com.ampnet.blockchainapiservice.util.EthereumString
import com.ampnet.blockchainapiservice.util.FunctionArgument
import com.ampnet.blockchainapiservice.util.FunctionData
import com.ampnet.blockchainapiservice.util.Status
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.ampnet.blockchainapiservice.util.WithFunctionData
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
                        FunctionArgument(abiType = AbiType.Address, value = CREATE_PARAMS.tokenAddress),
                        FunctionArgument(abiType = AbiType.Uint256, value = CREATE_PARAMS.tokenAmount),
                        FunctionArgument(abiType = AbiType.Uint256, value = CREATE_PARAMS.lockDuration),
                        FunctionArgument(abiType = AbiType.Utf8String, value = EthereumString(id.toString())),
                        FunctionArgument(abiType = AbiType.Address, value = ZeroAddress)
                    ),
                    abiOutputTypes = emptyList()
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
            chainId = Chain.HARDHAT_TESTNET.id,
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
                        FunctionArgument(abiType = AbiType.Address, value = lockRequest.tokenAddress),
                        FunctionArgument(abiType = AbiType.Uint256, value = lockRequest.tokenAmount),
                        FunctionArgument(abiType = AbiType.Uint256, value = lockRequest.lockDuration),
                        FunctionArgument(abiType = AbiType.Utf8String, value = EthereumString(id.toString())),
                        FunctionArgument(abiType = AbiType.Address, value = ZeroAddress)
                    ),
                    abiOutputTypes = emptyList()
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
            chainId = Chain.HARDHAT_TESTNET.id,
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
            given(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH))
                .willReturn(null)
        }

        val functionEncoderService = mock<FunctionEncoderService>()
        val encodedData = FunctionData("encoded")

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "lock",
                    arguments = listOf(
                        FunctionArgument(abiType = AbiType.Address, value = lockRequest.tokenAddress),
                        FunctionArgument(abiType = AbiType.Uint256, value = lockRequest.tokenAmount),
                        FunctionArgument(abiType = AbiType.Uint256, value = lockRequest.lockDuration),
                        FunctionArgument(abiType = AbiType.Utf8String, value = EthereumString(id.toString())),
                        FunctionArgument(abiType = AbiType.Address, value = ZeroAddress)
                    ),
                    abiOutputTypes = emptyList()
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
            chainId = Chain.HARDHAT_TESTNET.id,
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
            value = Balance(BigInteger.ZERO),
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = false
        )

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "lock",
                    arguments = listOf(
                        FunctionArgument(abiType = AbiType.Address, value = lockRequest.tokenAddress),
                        FunctionArgument(abiType = AbiType.Uint256, value = lockRequest.tokenAmount),
                        FunctionArgument(abiType = AbiType.Uint256, value = lockRequest.lockDuration),
                        FunctionArgument(abiType = AbiType.Utf8String, value = EthereumString(id.toString())),
                        FunctionArgument(abiType = AbiType.Address, value = ZeroAddress)
                    ),
                    abiOutputTypes = emptyList()
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
            chainId = Chain.HARDHAT_TESTNET.id,
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
            value = Balance(BigInteger.ZERO),
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true
        )

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "lock",
                    arguments = listOf(
                        FunctionArgument(abiType = AbiType.Address, value = lockRequest.tokenAddress),
                        FunctionArgument(abiType = AbiType.Uint256, value = lockRequest.tokenAmount),
                        FunctionArgument(abiType = AbiType.Uint256, value = lockRequest.lockDuration),
                        FunctionArgument(abiType = AbiType.Utf8String, value = EthereumString(id.toString())),
                        FunctionArgument(abiType = AbiType.Address, value = ZeroAddress)
                    ),
                    abiOutputTypes = emptyList()
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
            chainId = Chain.HARDHAT_TESTNET.id,
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
            value = Balance(BigInteger.ZERO),
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true
        )

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "lock",
                    arguments = listOf(
                        FunctionArgument(abiType = AbiType.Address, value = lockRequest.tokenAddress),
                        FunctionArgument(abiType = AbiType.Uint256, value = lockRequest.tokenAmount),
                        FunctionArgument(abiType = AbiType.Uint256, value = lockRequest.lockDuration),
                        FunctionArgument(abiType = AbiType.Utf8String, value = EthereumString(id.toString())),
                        FunctionArgument(abiType = AbiType.Address, value = ZeroAddress)
                    ),
                    abiOutputTypes = emptyList()
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
            chainId = Chain.HARDHAT_TESTNET.id,
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
            value = Balance(BigInteger.ZERO),
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true
        )

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "lock",
                    arguments = listOf(
                        FunctionArgument(abiType = AbiType.Address, value = lockRequest.tokenAddress),
                        FunctionArgument(abiType = AbiType.Uint256, value = lockRequest.tokenAmount),
                        FunctionArgument(abiType = AbiType.Uint256, value = lockRequest.lockDuration),
                        FunctionArgument(abiType = AbiType.Utf8String, value = EthereumString(id.toString())),
                        FunctionArgument(abiType = AbiType.Address, value = ZeroAddress)
                    ),
                    abiOutputTypes = emptyList()
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
            chainId = Chain.HARDHAT_TESTNET.id,
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
            value = Balance(BigInteger.ZERO),
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true
        )

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "lock",
                    arguments = listOf(
                        FunctionArgument(abiType = AbiType.Address, value = lockRequest.tokenAddress),
                        FunctionArgument(abiType = AbiType.Uint256, value = lockRequest.tokenAmount),
                        FunctionArgument(abiType = AbiType.Uint256, value = lockRequest.lockDuration),
                        FunctionArgument(abiType = AbiType.Utf8String, value = EthereumString(id.toString())),
                        FunctionArgument(abiType = AbiType.Address, value = ZeroAddress)
                    ),
                    abiOutputTypes = emptyList()
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
            chainId = Chain.HARDHAT_TESTNET.id,
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
            value = Balance(BigInteger.ZERO),
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true
        )

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "lock",
                    arguments = listOf(
                        FunctionArgument(abiType = AbiType.Address, value = lockRequest.tokenAddress),
                        FunctionArgument(abiType = AbiType.Uint256, value = lockRequest.tokenAmount),
                        FunctionArgument(abiType = AbiType.Uint256, value = lockRequest.lockDuration),
                        FunctionArgument(abiType = AbiType.Utf8String, value = EthereumString(id.toString())),
                        FunctionArgument(abiType = AbiType.Address, value = ZeroAddress)
                    ),
                    abiOutputTypes = emptyList()
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
            chainId = Chain.HARDHAT_TESTNET.id,
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
            value = Balance(BigInteger.ZERO),
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true
        )

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "lock",
                    arguments = listOf(
                        FunctionArgument(abiType = AbiType.Address, value = lockRequest.tokenAddress),
                        FunctionArgument(abiType = AbiType.Uint256, value = lockRequest.tokenAmount),
                        FunctionArgument(abiType = AbiType.Uint256, value = lockRequest.lockDuration),
                        FunctionArgument(abiType = AbiType.Utf8String, value = EthereumString(id.toString())),
                        FunctionArgument(abiType = AbiType.Address, value = ZeroAddress)
                    ),
                    abiOutputTypes = emptyList()
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
            chainId = Chain.HARDHAT_TESTNET.id,
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
            value = Balance(BigInteger.ZERO),
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true
        )

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "lock",
                    arguments = listOf(
                        FunctionArgument(abiType = AbiType.Address, value = lockRequest.tokenAddress),
                        FunctionArgument(abiType = AbiType.Uint256, value = lockRequest.tokenAmount),
                        FunctionArgument(abiType = AbiType.Uint256, value = lockRequest.lockDuration),
                        FunctionArgument(abiType = AbiType.Utf8String, value = EthereumString(id.toString())),
                        FunctionArgument(abiType = AbiType.Address, value = ZeroAddress)
                    ),
                    abiOutputTypes = emptyList()
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
