package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.blockchain.BlockchainService
import dev3.blockchainapiservice.blockchain.properties.ChainSpec
import dev3.blockchainapiservice.exception.CannotAttachTxInfoException
import dev3.blockchainapiservice.exception.ResourceNotFoundException
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.model.params.CreateAssetSendRequestParams
import dev3.blockchainapiservice.model.params.StoreAssetSendRequestParams
import dev3.blockchainapiservice.model.result.AssetSendRequest
import dev3.blockchainapiservice.model.result.BlockchainTransactionInfo
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.repository.AssetSendRequestRepository
import dev3.blockchainapiservice.repository.ProjectRepository
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.BaseUrl
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.FunctionArgument
import dev3.blockchainapiservice.util.FunctionData
import dev3.blockchainapiservice.util.PredefinedEvents
import dev3.blockchainapiservice.util.Status
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress
import dev3.blockchainapiservice.util.WithFunctionDataOrEthValue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import java.math.BigInteger
import java.util.UUID

class AssetSendRequestServiceTest : TestBase() {

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
        private val CREATE_PARAMS = CreateAssetSendRequestParams(
            redirectUrl = "redirect-url/\${id}",
            tokenAddress = ContractAddress("a"),
            assetAmount = Balance(BigInteger.valueOf(123456L)),
            assetSenderAddress = WalletAddress("b"),
            assetRecipientAddress = WalletAddress("c"),
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            )
        )
        private val TX_HASH = TransactionHash("tx-hash")
        private val TRANSFER_EVENTS = listOf(PredefinedEvents.ERC20_TRANSFER)
    }

    @Test
    fun mustSuccessfullyCreateAssetSendRequestForSomeToken() {
        val uuidProvider = mock<UuidProvider>()
        val uuid = UUID.randomUUID()

        suppose("some UUID will be generated") {
            call(uuidProvider.getUuid())
                .willReturn(uuid)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some timestamp will be returned") {
            call(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val functionEncoderService = mock<FunctionEncoderService>()
        val encodedData = FunctionData("encoded")

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "transfer",
                    arguments = listOf(
                        FunctionArgument(CREATE_PARAMS.assetRecipientAddress),
                        FunctionArgument(CREATE_PARAMS.assetAmount)
                    )
                )
            )
                .willReturn(encodedData)
        }

        val assetSendRequestRepository = mock<AssetSendRequestRepository>()
        val redirectUrl = CREATE_PARAMS.redirectUrl!!

        val storeParams = StoreAssetSendRequestParams(
            id = uuid,
            projectId = PROJECT.id,
            chainId = PROJECT.chainId,
            redirectUrl = redirectUrl.replace("\${id}", uuid.toString()),
            tokenAddress = CREATE_PARAMS.tokenAddress,
            assetAmount = CREATE_PARAMS.assetAmount,
            assetSenderAddress = CREATE_PARAMS.assetSenderAddress,
            assetRecipientAddress = CREATE_PARAMS.assetRecipientAddress,
            arbitraryData = CREATE_PARAMS.arbitraryData,
            screenConfig = CREATE_PARAMS.screenConfig,
            createdAt = TestData.TIMESTAMP
        )

        val storedRequest = AssetSendRequest(
            id = uuid,
            projectId = PROJECT.id,
            chainId = PROJECT.chainId,
            redirectUrl = storeParams.redirectUrl,
            tokenAddress = CREATE_PARAMS.tokenAddress,
            assetAmount = CREATE_PARAMS.assetAmount,
            assetSenderAddress = CREATE_PARAMS.assetSenderAddress,
            assetRecipientAddress = CREATE_PARAMS.assetRecipientAddress,
            txHash = null,
            arbitraryData = CREATE_PARAMS.arbitraryData,
            screenConfig = CREATE_PARAMS.screenConfig,
            createdAt = TestData.TIMESTAMP
        )

        suppose("asset send request is stored in database") {
            call(assetSendRequestRepository.store(storeParams))
                .willReturn(storedRequest)
        }

        val service = AssetSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetSendRequestRepository = assetSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = uuidProvider,
                utcDateTimeProvider = utcDateTimeProvider,
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("asset send request is correctly created") {
            expectThat(service.createAssetSendRequest(CREATE_PARAMS, PROJECT))
                .isEqualTo(WithFunctionDataOrEthValue(storedRequest, encodedData, null))

            expectInteractions(assetSendRequestRepository) {
                once.store(storeParams)
            }
        }
    }

    @Test
    fun mustSuccessfullyCreateAssetSendRequestForNativeAsset() {
        val uuidProvider = mock<UuidProvider>()
        val uuid = UUID.randomUUID()

        suppose("some UUID will be generated") {
            call(uuidProvider.getUuid())
                .willReturn(uuid)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some timestamp will be returned") {
            call(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val assetSendRequestRepository = mock<AssetSendRequestRepository>()
        val redirectUrl = CREATE_PARAMS.redirectUrl!!

        val storeParams = StoreAssetSendRequestParams(
            id = uuid,
            projectId = PROJECT.id,
            chainId = PROJECT.chainId,
            redirectUrl = redirectUrl.replace("\${id}", uuid.toString()),
            tokenAddress = null,
            assetAmount = CREATE_PARAMS.assetAmount,
            assetSenderAddress = CREATE_PARAMS.assetSenderAddress,
            assetRecipientAddress = CREATE_PARAMS.assetRecipientAddress,
            arbitraryData = CREATE_PARAMS.arbitraryData,
            screenConfig = CREATE_PARAMS.screenConfig,
            createdAt = TestData.TIMESTAMP
        )

        val storedRequest = AssetSendRequest(
            id = uuid,
            projectId = PROJECT.id,
            chainId = PROJECT.chainId,
            redirectUrl = storeParams.redirectUrl,
            tokenAddress = null,
            assetAmount = CREATE_PARAMS.assetAmount,
            assetSenderAddress = CREATE_PARAMS.assetSenderAddress,
            assetRecipientAddress = CREATE_PARAMS.assetRecipientAddress,
            txHash = null,
            arbitraryData = CREATE_PARAMS.arbitraryData,
            screenConfig = CREATE_PARAMS.screenConfig,
            createdAt = TestData.TIMESTAMP
        )

        suppose("asset send request is stored in database") {
            call(assetSendRequestRepository.store(storeParams))
                .willReturn(storedRequest)
        }

        val service = AssetSendRequestServiceImpl(
            functionEncoderService = mock(),
            assetSendRequestRepository = assetSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = uuidProvider,
                utcDateTimeProvider = utcDateTimeProvider,
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("asset send request is correctly created") {
            expectThat(service.createAssetSendRequest(CREATE_PARAMS.copy(tokenAddress = null), PROJECT))
                .isEqualTo(WithFunctionDataOrEthValue(storedRequest, null, CREATE_PARAMS.assetAmount))

            expectInteractions(assetSendRequestRepository) {
                once.store(storeParams)
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionForNonExistentAssetSendRequest() {
        val assetSendRequestRepository = mock<AssetSendRequestRepository>()

        suppose("asset send request does not exist in database") {
            call(assetSendRequestRepository.getById(any()))
                .willReturn(null)
        }

        val service = AssetSendRequestServiceImpl(
            functionEncoderService = mock(),
            assetSendRequestRepository = assetSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.getAssetSendRequest(id = UUID.randomUUID())
            }
        }
    }

    @Test
    fun mustReturnAssetSendRequestWithPendingStatusWhenAssetSendRequestHasNullTxHash() {
        val id = UUID.randomUUID()
        val sendRequest = AssetSendRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = TestData.CHAIN_ID,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            assetAmount = Balance(BigInteger.TEN),
            assetSenderAddress = WalletAddress("b"),
            assetRecipientAddress = WalletAddress("c"),
            txHash = null,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val assetSendRequestRepository = mock<AssetSendRequestRepository>()

        suppose("asset send request exists in database") {
            call(assetSendRequestRepository.getById(id))
                .willReturn(sendRequest)
        }

        val functionEncoderService = mock<FunctionEncoderService>()
        val encodedData = FunctionData("encoded")

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "transfer",
                    arguments = listOf(
                        FunctionArgument(sendRequest.assetRecipientAddress),
                        FunctionArgument(sendRequest.assetAmount)
                    )
                )
            )
                .willReturn(encodedData)
        }

        val service = AssetSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetSendRequestRepository = assetSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = projectRepositoryMock(sendRequest.projectId)
        )

        verify("asset send request with pending status is returned") {
            expectThat(service.getAssetSendRequest(id))
                .isEqualTo(
                    sendRequest.withTransactionData(
                        status = Status.PENDING,
                        data = encodedData,
                        value = null,
                        transactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetSendRequestWithPendingStatusWhenTransactionIsNotYetMined() {
        val id = UUID.randomUUID()
        val sendRequest = AssetSendRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = TestData.CHAIN_ID,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            assetAmount = Balance(BigInteger.TEN),
            assetSenderAddress = WalletAddress("b"),
            assetRecipientAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val assetSendRequestRepository = mock<AssetSendRequestRepository>()

        suppose("asset send request exists in database") {
            call(assetSendRequestRepository.getById(id))
                .willReturn(sendRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(sendRequest.chainId, null)

        suppose("transaction is not yet mined") {
            call(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH, TRANSFER_EVENTS))
                .willReturn(null)
        }

        val functionEncoderService = mock<FunctionEncoderService>()
        val encodedData = FunctionData("encoded")

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "transfer",
                    arguments = listOf(
                        FunctionArgument(sendRequest.assetRecipientAddress),
                        FunctionArgument(sendRequest.assetAmount)
                    )
                )
            )
                .willReturn(encodedData)
        }

        val service = AssetSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetSendRequestRepository = assetSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = projectRepositoryMock(sendRequest.projectId)
        )

        verify("asset send request with pending status is returned") {
            expectThat(service.getAssetSendRequest(id))
                .isEqualTo(
                    sendRequest.withTransactionData(
                        status = Status.PENDING,
                        data = encodedData,
                        value = null,
                        transactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetSendRequestWithFailedStatusWhenTransactionIsNotSuccessful() {
        val id = UUID.randomUUID()
        val sendRequest = AssetSendRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = TestData.CHAIN_ID,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            assetAmount = Balance(BigInteger.TEN),
            assetSenderAddress = WalletAddress("b"),
            assetRecipientAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val assetSendRequestRepository = mock<AssetSendRequestRepository>()

        suppose("asset send request exists in database") {
            call(assetSendRequestRepository.getById(id))
                .willReturn(sendRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(sendRequest.chainId, null)
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = sendRequest.assetSenderAddress!!,
            to = sendRequest.tokenAddress!!,
            deployedContractAddress = null,
            data = encodedData,
            value = Balance.ZERO,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = false,
            events = emptyList()
        )

        suppose("transaction is mined") {
            call(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH, TRANSFER_EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "transfer",
                    arguments = listOf(
                        FunctionArgument(sendRequest.assetRecipientAddress),
                        FunctionArgument(sendRequest.assetAmount)
                    )
                )
            )
                .willReturn(encodedData)
        }

        val service = AssetSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetSendRequestRepository = assetSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(sendRequest.projectId)
        )

        verify("asset send request with failed status is returned") {
            expectThat(service.getAssetSendRequest(id))
                .isEqualTo(
                    sendRequest.withTransactionData(
                        status = Status.FAILED,
                        data = encodedData,
                        value = null,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetSendRequestWithFailedStatusWhenTransactionHasWrongToAddress() {
        val id = UUID.randomUUID()
        val sendRequest = AssetSendRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = TestData.CHAIN_ID,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            assetAmount = Balance(BigInteger.TEN),
            assetSenderAddress = WalletAddress("b"),
            assetRecipientAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val assetSendRequestRepository = mock<AssetSendRequestRepository>()

        suppose("asset send request exists in database") {
            call(assetSendRequestRepository.getById(id))
                .willReturn(sendRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(sendRequest.chainId, null)
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = sendRequest.assetSenderAddress!!,
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
            call(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH, TRANSFER_EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "transfer",
                    arguments = listOf(
                        FunctionArgument(sendRequest.assetRecipientAddress),
                        FunctionArgument(sendRequest.assetAmount)
                    )
                )
            )
                .willReturn(encodedData)
        }

        val service = AssetSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetSendRequestRepository = assetSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(sendRequest.projectId)
        )

        verify("asset send request with failed status is returned") {
            expectThat(service.getAssetSendRequest(id))
                .isEqualTo(
                    sendRequest.withTransactionData(
                        status = Status.FAILED,
                        data = encodedData,
                        value = null,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetSendRequestWithFailedStatusWhenTransactionHasWrongTxHash() {
        val id = UUID.randomUUID()
        val sendRequest = AssetSendRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = TestData.CHAIN_ID,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            assetAmount = Balance(BigInteger.TEN),
            assetSenderAddress = WalletAddress("b"),
            assetRecipientAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val assetSendRequestRepository = mock<AssetSendRequestRepository>()

        suppose("asset send request exists in database") {
            call(assetSendRequestRepository.getById(id))
                .willReturn(sendRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(sendRequest.chainId, null)
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TransactionHash("wrong-hash"),
            from = sendRequest.assetSenderAddress!!,
            to = sendRequest.tokenAddress!!,
            deployedContractAddress = null,
            data = encodedData,
            value = Balance.ZERO,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true,
            events = emptyList()
        )

        suppose("transaction is mined") {
            call(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH, TRANSFER_EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "transfer",
                    arguments = listOf(
                        FunctionArgument(sendRequest.assetRecipientAddress),
                        FunctionArgument(sendRequest.assetAmount)
                    )
                )
            )
                .willReturn(encodedData)
        }

        val service = AssetSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetSendRequestRepository = assetSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(sendRequest.projectId)
        )

        verify("asset send request with failed status is returned") {
            expectThat(service.getAssetSendRequest(id))
                .isEqualTo(
                    sendRequest.withTransactionData(
                        status = Status.FAILED,
                        data = encodedData,
                        value = null,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetSendRequestWithFailedStatusWhenTransactionHasWrongFromAddress() {
        val id = UUID.randomUUID()
        val sendRequest = AssetSendRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = TestData.CHAIN_ID,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            assetAmount = Balance(BigInteger.TEN),
            assetSenderAddress = WalletAddress("b"),
            assetRecipientAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val assetSendRequestRepository = mock<AssetSendRequestRepository>()

        suppose("asset send request exists in database") {
            call(assetSendRequestRepository.getById(id))
                .willReturn(sendRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(sendRequest.chainId, null)
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = WalletAddress("dead"),
            to = sendRequest.tokenAddress!!,
            deployedContractAddress = null,
            data = encodedData,
            value = Balance.ZERO,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true,
            events = emptyList()
        )

        suppose("transaction is mined") {
            call(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH, TRANSFER_EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "transfer",
                    arguments = listOf(
                        FunctionArgument(sendRequest.assetRecipientAddress),
                        FunctionArgument(sendRequest.assetAmount)
                    )
                )
            )
                .willReturn(encodedData)
        }

        val service = AssetSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetSendRequestRepository = assetSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(sendRequest.projectId)
        )

        verify("asset send request with failed status is returned") {
            expectThat(service.getAssetSendRequest(id))
                .isEqualTo(
                    sendRequest.withTransactionData(
                        status = Status.FAILED,
                        data = encodedData,
                        value = null,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetSendRequestWithFailedStatusWhenTransactionHasWrongData() {
        val id = UUID.randomUUID()
        val sendRequest = AssetSendRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = TestData.CHAIN_ID,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            assetAmount = Balance(BigInteger.TEN),
            assetSenderAddress = WalletAddress("b"),
            assetRecipientAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val assetSendRequestRepository = mock<AssetSendRequestRepository>()

        suppose("asset send request exists in database") {
            call(assetSendRequestRepository.getById(id))
                .willReturn(sendRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(sendRequest.chainId, null)
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = sendRequest.assetSenderAddress!!,
            to = sendRequest.tokenAddress!!,
            deployedContractAddress = null,
            data = FunctionData("wrong-data"),
            value = Balance.ZERO,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true,
            events = emptyList()
        )

        suppose("transaction is mined") {
            call(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH, TRANSFER_EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "transfer",
                    arguments = listOf(
                        FunctionArgument(sendRequest.assetRecipientAddress),
                        FunctionArgument(sendRequest.assetAmount)
                    )
                )
            )
                .willReturn(encodedData)
        }

        val service = AssetSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetSendRequestRepository = assetSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(sendRequest.projectId)
        )

        verify("asset send request with failed status is returned") {
            expectThat(service.getAssetSendRequest(id))
                .isEqualTo(
                    sendRequest.withTransactionData(
                        status = Status.FAILED,
                        data = encodedData,
                        value = null,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetSendRequestWithFailedStatusWhenTransactionHasWrongRecipientAddressForNativeToken() {
        val id = UUID.randomUUID()
        val sendRequest = AssetSendRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = TestData.CHAIN_ID,
            redirectUrl = "test",
            tokenAddress = null,
            assetAmount = Balance(BigInteger.TEN),
            assetSenderAddress = WalletAddress("b"),
            assetRecipientAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val assetSendRequestRepository = mock<AssetSendRequestRepository>()

        suppose("asset send request exists in database") {
            call(assetSendRequestRepository.getById(id))
                .willReturn(sendRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(sendRequest.chainId, null)
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = sendRequest.assetSenderAddress!!,
            to = WalletAddress("dead"),
            deployedContractAddress = null,
            data = FunctionData.EMPTY,
            value = sendRequest.assetAmount,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true,
            events = emptyList()
        )

        suppose("transaction is mined") {
            call(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH, TRANSFER_EVENTS))
                .willReturn(transactionInfo)
        }

        val service = AssetSendRequestServiceImpl(
            functionEncoderService = mock(),
            assetSendRequestRepository = assetSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(sendRequest.projectId)
        )

        verify("asset send request with failed status is returned") {
            expectThat(service.getAssetSendRequest(id))
                .isEqualTo(
                    sendRequest.withTransactionData(
                        status = Status.FAILED,
                        data = null,
                        value = sendRequest.assetAmount,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetSendRequestWithFailedStatusWhenTransactionHasWrongValueForNativeToken() {
        val id = UUID.randomUUID()
        val sendRequest = AssetSendRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = TestData.CHAIN_ID,
            redirectUrl = "test",
            tokenAddress = null,
            assetAmount = Balance(BigInteger.TEN),
            assetSenderAddress = WalletAddress("b"),
            assetRecipientAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val assetSendRequestRepository = mock<AssetSendRequestRepository>()

        suppose("asset send request exists in database") {
            call(assetSendRequestRepository.getById(id))
                .willReturn(sendRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(sendRequest.chainId, null)
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = sendRequest.assetSenderAddress!!,
            to = sendRequest.assetRecipientAddress,
            deployedContractAddress = null,
            data = FunctionData.EMPTY,
            value = Balance(BigInteger.ONE),
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true,
            events = emptyList()
        )

        suppose("transaction is mined") {
            call(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH, TRANSFER_EVENTS))
                .willReturn(transactionInfo)
        }

        val service = AssetSendRequestServiceImpl(
            functionEncoderService = mock(),
            assetSendRequestRepository = assetSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(sendRequest.projectId)
        )

        verify("asset send request with failed status is returned") {
            expectThat(service.getAssetSendRequest(id))
                .isEqualTo(
                    sendRequest.withTransactionData(
                        status = Status.FAILED,
                        data = null,
                        value = sendRequest.assetAmount,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetSendRequestWithSuccessfulStatusWhenFromAddressIsNull() {
        val id = UUID.randomUUID()
        val sendRequest = AssetSendRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = TestData.CHAIN_ID,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            assetAmount = Balance(BigInteger.TEN),
            assetSenderAddress = null,
            assetRecipientAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val assetSendRequestRepository = mock<AssetSendRequestRepository>()

        suppose("asset send request exists in database") {
            call(assetSendRequestRepository.getById(id))
                .willReturn(sendRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(sendRequest.chainId, null)
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = WalletAddress("0cafe0babe"),
            to = sendRequest.tokenAddress!!,
            deployedContractAddress = null,
            data = encodedData,
            value = Balance.ZERO,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true,
            events = emptyList()
        )

        suppose("transaction is mined") {
            call(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH, TRANSFER_EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "transfer",
                    arguments = listOf(
                        FunctionArgument(sendRequest.assetRecipientAddress),
                        FunctionArgument(sendRequest.assetAmount)
                    )
                )
            )
                .willReturn(encodedData)
        }

        val service = AssetSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetSendRequestRepository = assetSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(sendRequest.projectId)
        )

        verify("asset send request with successful status is returned") {
            expectThat(service.getAssetSendRequest(id))
                .isEqualTo(
                    sendRequest.withTransactionData(
                        status = Status.SUCCESS,
                        data = encodedData,
                        value = null,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetSendRequestWithSuccessfulStatusWhenFromAddressIsSpecified() {
        val id = UUID.randomUUID()
        val sendRequest = AssetSendRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = TestData.CHAIN_ID,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            assetAmount = Balance(BigInteger.TEN),
            assetSenderAddress = WalletAddress("b"),
            assetRecipientAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val assetSendRequestRepository = mock<AssetSendRequestRepository>()

        suppose("asset send request exists in database") {
            call(assetSendRequestRepository.getById(id))
                .willReturn(sendRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(sendRequest.chainId, null)
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = sendRequest.assetSenderAddress!!,
            to = sendRequest.tokenAddress!!,
            deployedContractAddress = null,
            data = encodedData,
            value = Balance.ZERO,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true,
            events = emptyList()
        )

        suppose("transaction is mined") {
            call(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH, TRANSFER_EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "transfer",
                    arguments = listOf(
                        FunctionArgument(sendRequest.assetRecipientAddress),
                        FunctionArgument(sendRequest.assetAmount)
                    )
                )
            )
                .willReturn(encodedData)
        }

        val service = AssetSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetSendRequestRepository = assetSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(sendRequest.projectId)
        )

        verify("asset send request with successful status is returned") {
            expectThat(service.getAssetSendRequest(id))
                .isEqualTo(
                    sendRequest.withTransactionData(
                        status = Status.SUCCESS,
                        data = encodedData,
                        value = null,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetSendRequestWithSuccessfulStatusWhenFromAddressIsNullForNativeToken() {
        val id = UUID.randomUUID()
        val sendRequest = AssetSendRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = TestData.CHAIN_ID,
            redirectUrl = "test",
            tokenAddress = null,
            assetAmount = Balance(BigInteger.TEN),
            assetSenderAddress = null,
            assetRecipientAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val assetSendRequestRepository = mock<AssetSendRequestRepository>()

        suppose("asset send request exists in database") {
            call(assetSendRequestRepository.getById(id))
                .willReturn(sendRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(sendRequest.chainId, null)
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = WalletAddress("0cafe0babe"),
            to = sendRequest.assetRecipientAddress,
            deployedContractAddress = null,
            data = FunctionData.EMPTY,
            value = sendRequest.assetAmount,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true,
            events = emptyList()
        )

        suppose("transaction is mined") {
            call(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH, TRANSFER_EVENTS))
                .willReturn(transactionInfo)
        }

        val service = AssetSendRequestServiceImpl(
            functionEncoderService = mock(),
            assetSendRequestRepository = assetSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(sendRequest.projectId)
        )

        verify("asset send request with successful status is returned") {
            expectThat(service.getAssetSendRequest(id))
                .isEqualTo(
                    sendRequest.withTransactionData(
                        status = Status.SUCCESS,
                        data = null,
                        value = sendRequest.assetAmount,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetSendRequestWithSuccessfulStatusWhenFromAddressIsSpecifiedForNativeToken() {
        val id = UUID.randomUUID()
        val sendRequest = AssetSendRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = TestData.CHAIN_ID,
            redirectUrl = "test",
            tokenAddress = null,
            assetAmount = Balance(BigInteger.TEN),
            assetSenderAddress = WalletAddress("b"),
            assetRecipientAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val assetSendRequestRepository = mock<AssetSendRequestRepository>()

        suppose("asset send request exists in database") {
            call(assetSendRequestRepository.getById(id))
                .willReturn(sendRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(sendRequest.chainId, null)
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = sendRequest.assetSenderAddress!!,
            to = sendRequest.assetRecipientAddress,
            deployedContractAddress = null,
            data = FunctionData.EMPTY,
            value = sendRequest.assetAmount,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true,
            events = emptyList()
        )

        suppose("transaction is mined") {
            call(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH, TRANSFER_EVENTS))
                .willReturn(transactionInfo)
        }

        val service = AssetSendRequestServiceImpl(
            functionEncoderService = mock(),
            assetSendRequestRepository = assetSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(sendRequest.projectId)
        )

        verify("asset send request with successful status is returned") {
            expectThat(service.getAssetSendRequest(id))
                .isEqualTo(
                    sendRequest.withTransactionData(
                        status = Status.SUCCESS,
                        data = null,
                        value = sendRequest.assetAmount,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyReturnListOfAssetSendRequestsByProjectId() {
        val id = UUID.randomUUID()
        val sendRequest = AssetSendRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = TestData.CHAIN_ID,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            assetAmount = Balance(BigInteger.TEN),
            assetSenderAddress = WalletAddress("b"),
            assetRecipientAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val assetSendRequestRepository = mock<AssetSendRequestRepository>()

        suppose("asset send request exists in database") {
            call(assetSendRequestRepository.getAllByProjectId(PROJECT.id))
                .willReturn(listOf(sendRequest))
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(sendRequest.chainId, null)
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = sendRequest.assetSenderAddress!!,
            to = sendRequest.tokenAddress!!,
            deployedContractAddress = null,
            data = encodedData,
            value = Balance.ZERO,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true,
            events = emptyList()
        )

        suppose("transaction is mined") {
            call(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH, TRANSFER_EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "transfer",
                    arguments = listOf(
                        FunctionArgument(sendRequest.assetRecipientAddress),
                        FunctionArgument(sendRequest.assetAmount)
                    )
                )
            )
                .willReturn(encodedData)
        }

        val service = AssetSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetSendRequestRepository = assetSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(sendRequest.projectId)
        )

        verify("asset send request with successful status is returned") {
            expectThat(service.getAssetSendRequestsByProjectId(PROJECT.id))
                .isEqualTo(
                    listOf(
                        sendRequest.withTransactionData(
                            status = Status.SUCCESS,
                            data = encodedData,
                            value = null,
                            transactionInfo = transactionInfo
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyReturnEmptyListOfAssetSendRequestsForNonExistentProject() {
        val projectId = UUID.randomUUID()
        val service = AssetSendRequestServiceImpl(
            functionEncoderService = mock(),
            assetSendRequestRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = projectRepositoryMock(projectId)
        )

        verify("empty list is returned") {
            val result = service.getAssetSendRequestsByProjectId(projectId)

            expectThat(result)
                .isEmpty()
        }
    }

    @Test
    fun mustCorrectlyReturnListOfAssetSendRequestsBySenderAddress() {
        val id = UUID.randomUUID()
        val sender = WalletAddress("b")
        val sendRequest = AssetSendRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = TestData.CHAIN_ID,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            assetAmount = Balance(BigInteger.TEN),
            assetSenderAddress = sender,
            assetRecipientAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val assetSendRequestRepository = mock<AssetSendRequestRepository>()

        suppose("asset send request exists in database") {
            call(assetSendRequestRepository.getBySender(sender))
                .willReturn(listOf(sendRequest))
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(sendRequest.chainId, null)
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = sendRequest.assetSenderAddress!!,
            to = sendRequest.tokenAddress!!,
            deployedContractAddress = null,
            data = encodedData,
            value = Balance.ZERO,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true,
            events = emptyList()
        )

        suppose("transaction is mined") {
            call(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH, TRANSFER_EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "transfer",
                    arguments = listOf(
                        FunctionArgument(sendRequest.assetRecipientAddress),
                        FunctionArgument(sendRequest.assetAmount)
                    )
                )
            )
                .willReturn(encodedData)
        }

        val service = AssetSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetSendRequestRepository = assetSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(sendRequest.projectId)
        )

        verify("asset send request with successful status is returned") {
            expectThat(service.getAssetSendRequestsBySender(sender))
                .isEqualTo(
                    listOf(
                        sendRequest.withTransactionData(
                            status = Status.SUCCESS,
                            data = encodedData,
                            value = null,
                            transactionInfo = transactionInfo
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyReturnListOfAssetSendRequestsByRecipientAddress() {
        val id = UUID.randomUUID()
        val recipient = WalletAddress("c")
        val sendRequest = AssetSendRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = TestData.CHAIN_ID,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            assetAmount = Balance(BigInteger.TEN),
            assetSenderAddress = WalletAddress("b"),
            assetRecipientAddress = recipient,
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val assetSendRequestRepository = mock<AssetSendRequestRepository>()

        suppose("asset send request exists in database") {
            call(assetSendRequestRepository.getByRecipient(recipient))
                .willReturn(listOf(sendRequest))
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(sendRequest.chainId, null)
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = sendRequest.assetSenderAddress!!,
            to = sendRequest.tokenAddress!!,
            deployedContractAddress = null,
            data = encodedData,
            value = Balance.ZERO,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true,
            events = emptyList()
        )

        suppose("transaction is mined") {
            call(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH, TRANSFER_EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "transfer",
                    arguments = listOf(
                        FunctionArgument(sendRequest.assetRecipientAddress),
                        FunctionArgument(sendRequest.assetAmount)
                    )
                )
            )
                .willReturn(encodedData)
        }

        val service = AssetSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetSendRequestRepository = assetSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(sendRequest.projectId)
        )

        verify("asset send request with successful status is returned") {
            expectThat(service.getAssetSendRequestsByRecipient(recipient))
                .isEqualTo(
                    listOf(
                        sendRequest.withTransactionData(
                            status = Status.SUCCESS,
                            data = encodedData,
                            value = null,
                            transactionInfo = transactionInfo
                        )
                    )
                )
        }
    }

    @Test
    fun mustSuccessfullyAttachTxInfo() {
        val assetSendRequestRepository = mock<AssetSendRequestRepository>()
        val id = UUID.randomUUID()
        val caller = WalletAddress("0xbc25524e0daacB1F149BA55279f593F5E3FB73e9")

        suppose("txInfo will be successfully attached to the request") {
            call(assetSendRequestRepository.setTxInfo(id, TX_HASH, caller))
                .willReturn(true)
        }

        val service = AssetSendRequestServiceImpl(
            functionEncoderService = mock(),
            assetSendRequestRepository = assetSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("txInfo was successfully attached") {
            service.attachTxInfo(id, TX_HASH, caller)

            expectInteractions(assetSendRequestRepository) {
                once.setTxInfo(id, TX_HASH, caller)
            }
        }
    }

    @Test
    fun mustThrowCannotAttachTxInfoExceptionWhenAttachingTxInfoFails() {
        val assetSendRequestRepository = mock<AssetSendRequestRepository>()
        val id = UUID.randomUUID()
        val caller = WalletAddress("0xbc25524e0daacB1F149BA55279f593F5E3FB73e9")

        suppose("attaching txInfo will fail") {
            call(assetSendRequestRepository.setTxInfo(id, TX_HASH, caller))
                .willReturn(false)
        }

        val service = AssetSendRequestServiceImpl(
            functionEncoderService = mock(),
            assetSendRequestRepository = assetSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("CannotAttachTxInfoException is thrown") {
            expectThrows<CannotAttachTxInfoException> {
                service.attachTxInfo(id, TX_HASH, caller)
            }

            expectInteractions(assetSendRequestRepository) {
                once.setTxInfo(id, TX_HASH, caller)
            }
        }
    }

    private fun projectRepositoryMock(projectId: UUID): ProjectRepository {
        val projectRepository = mock<ProjectRepository>()

        suppose("some project will be returned") {
            call(projectRepository.getById(projectId))
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
        }

        return projectRepository
    }
}
