package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.blockchain.BlockchainService
import com.ampnet.blockchainapiservice.blockchain.properties.ChainSpec
import com.ampnet.blockchainapiservice.exception.CannotAttachTxInfoException
import com.ampnet.blockchainapiservice.exception.ResourceNotFoundException
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.params.CreateAssetMultiSendRequestParams
import com.ampnet.blockchainapiservice.model.params.StoreAssetMultiSendRequestParams
import com.ampnet.blockchainapiservice.model.result.AssetMultiSendRequest
import com.ampnet.blockchainapiservice.model.result.BlockchainTransactionInfo
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.repository.AssetMultiSendRequestRepository
import com.ampnet.blockchainapiservice.repository.ProjectRepository
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.BaseUrl
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.FunctionArgument
import com.ampnet.blockchainapiservice.util.FunctionData
import com.ampnet.blockchainapiservice.util.Status
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.ampnet.blockchainapiservice.util.WithFunctionDataOrEthValue
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

class AssetMultiSendRequestServiceTest : TestBase() {

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
        private val CREATE_PARAMS = CreateAssetMultiSendRequestParams(
            redirectUrl = "redirect-url/\${id}",
            tokenAddress = ContractAddress("a"),
            disperseContractAddress = ContractAddress("b"),
            assetAmounts = listOf(Balance(BigInteger.valueOf(123456L)), Balance(BigInteger.valueOf(789L))),
            assetRecipientAddresses = listOf(WalletAddress("c"), WalletAddress("d")),
            itemNames = listOf("item1", null),
            assetSenderAddress = WalletAddress("e"),
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            approveScreenConfig = ScreenConfig(
                beforeActionMessage = "approve-before-action-message",
                afterActionMessage = "approve-after-action-message"
            ),
            disperseScreenConfig = ScreenConfig(
                beforeActionMessage = "disperse-before-action-message",
                afterActionMessage = "disperse-after-action-message"
            )
        )
        private val TOTAL_TOKEN_AMOUNT = Balance(CREATE_PARAMS.assetAmounts.sumOf { it.rawValue })
        private val APPROVE_TX_HASH = TransactionHash("approve-tx-hash")
        private val DISPERSE_TX_HASH = TransactionHash("disperse-tx-hash")
        private val ID = UUID.randomUUID()
        private val STORE_PARAMS = StoreAssetMultiSendRequestParams(
            id = ID,
            projectId = PROJECT.id,
            chainId = PROJECT.chainId,
            redirectUrl = CREATE_PARAMS.redirectUrl!!.replace("\${id}", ID.toString()),
            tokenAddress = CREATE_PARAMS.tokenAddress,
            disperseContractAddress = CREATE_PARAMS.disperseContractAddress,
            assetAmounts = CREATE_PARAMS.assetAmounts,
            assetRecipientAddresses = CREATE_PARAMS.assetRecipientAddresses,
            itemNames = CREATE_PARAMS.itemNames,
            assetSenderAddress = CREATE_PARAMS.assetSenderAddress,
            arbitraryData = CREATE_PARAMS.arbitraryData,
            approveScreenConfig = CREATE_PARAMS.approveScreenConfig,
            disperseScreenConfig = CREATE_PARAMS.disperseScreenConfig,
            createdAt = TestData.TIMESTAMP
        )
        private val STORED_REQUEST = AssetMultiSendRequest(
            id = ID,
            projectId = PROJECT.id,
            chainId = PROJECT.chainId,
            redirectUrl = STORE_PARAMS.redirectUrl,
            tokenAddress = STORE_PARAMS.tokenAddress,
            disperseContractAddress = STORE_PARAMS.disperseContractAddress,
            assetAmounts = STORE_PARAMS.assetAmounts,
            assetRecipientAddresses = STORE_PARAMS.assetRecipientAddresses,
            itemNames = STORE_PARAMS.itemNames,
            assetSenderAddress = STORE_PARAMS.assetSenderAddress,
            approveTxHash = null,
            disperseTxHash = null,
            arbitraryData = STORE_PARAMS.arbitraryData,
            approveScreenConfig = STORE_PARAMS.approveScreenConfig,
            disperseScreenConfig = STORE_PARAMS.disperseScreenConfig,
            createdAt = TestData.TIMESTAMP
        )
        private val ENCODED_APPROVE_DATA = FunctionData("encoded-approve")
        private val APPROVE_TX_INFO = BlockchainTransactionInfo(
            hash = APPROVE_TX_HASH,
            from = STORED_REQUEST.assetSenderAddress!!,
            to = STORED_REQUEST.tokenAddress!!,
            deployedContractAddress = null,
            data = ENCODED_APPROVE_DATA,
            value = Balance.ZERO,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true
        )
        private val ENCODED_DISPERSE_ETHER_DATA = FunctionData("encoded-disperse-ether")
        private val DISPERSE_ETHER_TX_INFO = BlockchainTransactionInfo(
            hash = DISPERSE_TX_HASH,
            from = STORED_REQUEST.assetSenderAddress!!,
            to = STORED_REQUEST.disperseContractAddress,
            deployedContractAddress = null,
            data = ENCODED_DISPERSE_ETHER_DATA,
            value = TOTAL_TOKEN_AMOUNT,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true
        )
        private val ENCODED_DISPERSE_TOKEN_DATA = FunctionData("encoded-disperse-token")
        private val DISPERSE_TOKEN_TX_INFO = DISPERSE_ETHER_TX_INFO.copy(
            data = ENCODED_DISPERSE_TOKEN_DATA,
            value = Balance.ZERO
        )
    }

    @Test
    fun mustSuccessfullyCreateAssetMultiSendRequestForSomeToken() {
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

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "approve",
                    arguments = listOf(
                        FunctionArgument(CREATE_PARAMS.disperseContractAddress),
                        FunctionArgument(TOTAL_TOKEN_AMOUNT)
                    )
                )
            )
                .willReturn(ENCODED_APPROVE_DATA)
        }

        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()

        suppose("asset multi-send request is stored in database") {
            given(assetMultiSendRequestRepository.store(STORE_PARAMS))
                .willReturn(STORED_REQUEST)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = uuidProvider,
                utcDateTimeProvider = utcDateTimeProvider,
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("asset multi-send request is correctly created") {
            assertThat(service.createAssetMultiSendRequest(CREATE_PARAMS, PROJECT)).withMessage()
                .isEqualTo(WithFunctionDataOrEthValue(STORED_REQUEST, ENCODED_APPROVE_DATA, null))

            verifyMock(assetMultiSendRequestRepository)
                .store(STORE_PARAMS)
            verifyNoMoreInteractions(assetMultiSendRequestRepository)
        }
    }

    @Test
    fun mustSuccessfullyCreateAssetMultiSendRequestForNativeAsset() {
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

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "disperseEther",
                    arguments = listOf(
                        FunctionArgument.fromAddresses(CREATE_PARAMS.assetRecipientAddresses),
                        FunctionArgument.fromUint256s(CREATE_PARAMS.assetAmounts)
                    )
                )
            )
                .willReturn(ENCODED_DISPERSE_ETHER_DATA)
        }

        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()

        val storeParams = STORE_PARAMS.copy(tokenAddress = null)
        val storedRequest = STORED_REQUEST.copy(tokenAddress = null)

        suppose("asset multi-send request is stored in database") {
            given(assetMultiSendRequestRepository.store(storeParams))
                .willReturn(storedRequest)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = uuidProvider,
                utcDateTimeProvider = utcDateTimeProvider,
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("asset multi-send request is correctly created") {
            assertThat(service.createAssetMultiSendRequest(CREATE_PARAMS.copy(tokenAddress = null), PROJECT))
                .withMessage()
                .isEqualTo(WithFunctionDataOrEthValue(storedRequest, ENCODED_DISPERSE_ETHER_DATA, TOTAL_TOKEN_AMOUNT))

            verifyMock(assetMultiSendRequestRepository)
                .store(storeParams)
            verifyNoMoreInteractions(assetMultiSendRequestRepository)
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionForNonExistentAssetMultiSendRequest() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()

        suppose("asset multi-send request does not exist in database") {
            given(assetMultiSendRequestRepository.getById(any()))
                .willReturn(null)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = mock(),
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.getAssetMultiSendRequest(id = UUID.randomUUID())
            }
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithPendingApproveStatusWhenAssetMultiSendRequestHasNullApproveTxHash() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()

        suppose("asset multi-send request exists in database") {
            given(assetMultiSendRequestRepository.getById(ID))
                .willReturn(STORED_REQUEST)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "approve",
                    arguments = listOf(
                        FunctionArgument(CREATE_PARAMS.disperseContractAddress),
                        FunctionArgument(TOTAL_TOKEN_AMOUNT)
                    )
                )
            )
                .willReturn(ENCODED_APPROVE_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = projectRepositoryMock(STORED_REQUEST.projectId)
        )

        verify("asset multi-send request with pending approve status is returned") {
            assertThat(service.getAssetMultiSendRequest(ID)).withMessage()
                .isEqualTo(
                    STORED_REQUEST.withMultiTransactionData(
                        approveStatus = Status.PENDING,
                        approveData = ENCODED_APPROVE_DATA,
                        approveTransactionInfo = null,
                        disperseStatus = null,
                        disperseData = null,
                        disperseValue = null,
                        disperseTransactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithPendingDisperseStatusWhenAssetMultiSendRequestHasNullDisperseTxHash() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val storedRequest = STORED_REQUEST.copy(tokenAddress = null)

        suppose("asset multi-send request exists in database") {
            given(assetMultiSendRequestRepository.getById(ID))
                .willReturn(storedRequest)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "disperseEther",
                    arguments = listOf(
                        FunctionArgument.fromAddresses(CREATE_PARAMS.assetRecipientAddresses),
                        FunctionArgument.fromUint256s(CREATE_PARAMS.assetAmounts)
                    )
                )
            )
                .willReturn(ENCODED_DISPERSE_ETHER_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = projectRepositoryMock(storedRequest.projectId)
        )

        verify("asset multi-send request with pending disperse status is returned") {
            assertThat(service.getAssetMultiSendRequest(ID)).withMessage()
                .isEqualTo(
                    storedRequest.withMultiTransactionData(
                        approveStatus = null,
                        approveData = null,
                        approveTransactionInfo = null,
                        disperseStatus = Status.PENDING,
                        disperseData = ENCODED_DISPERSE_ETHER_DATA,
                        disperseValue = TOTAL_TOKEN_AMOUNT,
                        disperseTransactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithPendingApproveStatusWhenApproveTransactionIsNotYetMined() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val storedRequest = STORED_REQUEST.copy(approveTxHash = APPROVE_TX_HASH)

        suppose("asset multi-send request exists in database") {
            given(assetMultiSendRequestRepository.getById(ID))
                .willReturn(storedRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(storedRequest.chainId, null)

        suppose("transaction is not yet mined") {
            given(blockchainService.fetchTransactionInfo(chainSpec, APPROVE_TX_HASH))
                .willReturn(null)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "approve",
                    arguments = listOf(
                        FunctionArgument(CREATE_PARAMS.disperseContractAddress),
                        FunctionArgument(TOTAL_TOKEN_AMOUNT)
                    )
                )
            )
                .willReturn(ENCODED_APPROVE_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(storedRequest.projectId)
        )

        verify("asset multi-send request with pending approve status is returned") {
            assertThat(service.getAssetMultiSendRequest(ID)).withMessage()
                .isEqualTo(
                    storedRequest.withMultiTransactionData(
                        approveStatus = Status.PENDING,
                        approveData = ENCODED_APPROVE_DATA,
                        approveTransactionInfo = null,
                        disperseStatus = null,
                        disperseData = null,
                        disperseValue = null,
                        disperseTransactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithPendingDisperseStatusWhenDisperseTransactionIsNotYetMined() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val storedRequest = STORED_REQUEST.copy(tokenAddress = null, disperseTxHash = DISPERSE_TX_HASH)

        suppose("asset multi-send request exists in database") {
            given(assetMultiSendRequestRepository.getById(ID))
                .willReturn(storedRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(storedRequest.chainId, null)

        suppose("transaction is not yet mined") {
            given(blockchainService.fetchTransactionInfo(chainSpec, DISPERSE_TX_HASH))
                .willReturn(null)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "disperseEther",
                    arguments = listOf(
                        FunctionArgument.fromAddresses(CREATE_PARAMS.assetRecipientAddresses),
                        FunctionArgument.fromUint256s(CREATE_PARAMS.assetAmounts)
                    )
                )
            )
                .willReturn(ENCODED_DISPERSE_ETHER_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(storedRequest.projectId)
        )

        verify("asset multi-send request with pending disperse status is returned") {
            assertThat(service.getAssetMultiSendRequest(ID)).withMessage()
                .isEqualTo(
                    storedRequest.withMultiTransactionData(
                        approveStatus = null,
                        approveData = null,
                        approveTransactionInfo = null,
                        disperseStatus = Status.PENDING,
                        disperseData = ENCODED_DISPERSE_ETHER_DATA,
                        disperseValue = TOTAL_TOKEN_AMOUNT,
                        disperseTransactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithFailedApproveStatusWhenApproveTransactionIsNotSuccessful() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val storedRequest = STORED_REQUEST.copy(approveTxHash = APPROVE_TX_HASH)

        suppose("asset multi-send request exists in database") {
            given(assetMultiSendRequestRepository.getById(ID))
                .willReturn(storedRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(storedRequest.chainId, null)
        val transactionInfo = APPROVE_TX_INFO.copy(success = false)

        suppose("transaction is returned") {
            given(blockchainService.fetchTransactionInfo(chainSpec, APPROVE_TX_HASH))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "approve",
                    arguments = listOf(
                        FunctionArgument(CREATE_PARAMS.disperseContractAddress),
                        FunctionArgument(TOTAL_TOKEN_AMOUNT)
                    )
                )
            )
                .willReturn(ENCODED_APPROVE_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(storedRequest.projectId)
        )

        verify("asset multi-send request with failed approve status is returned") {
            assertThat(service.getAssetMultiSendRequest(ID)).withMessage()
                .isEqualTo(
                    storedRequest.withMultiTransactionData(
                        approveStatus = Status.FAILED,
                        approveData = ENCODED_APPROVE_DATA,
                        approveTransactionInfo = transactionInfo,
                        disperseStatus = null,
                        disperseData = null,
                        disperseValue = null,
                        disperseTransactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithFailedDisperseStatusWhenDisperseTransactionIsNotSuccessful() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val storedRequest = STORED_REQUEST.copy(tokenAddress = null, disperseTxHash = DISPERSE_TX_HASH)

        suppose("asset multi-send request exists in database") {
            given(assetMultiSendRequestRepository.getById(ID))
                .willReturn(storedRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(storedRequest.chainId, null)
        val transactionInfo = DISPERSE_ETHER_TX_INFO.copy(success = false)

        suppose("transaction is returned") {
            given(blockchainService.fetchTransactionInfo(chainSpec, DISPERSE_TX_HASH))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "disperseEther",
                    arguments = listOf(
                        FunctionArgument.fromAddresses(CREATE_PARAMS.assetRecipientAddresses),
                        FunctionArgument.fromUint256s(CREATE_PARAMS.assetAmounts)
                    )
                )
            )
                .willReturn(ENCODED_DISPERSE_ETHER_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(storedRequest.projectId)
        )

        verify("asset multi-send request with failed disperse status is returned") {
            assertThat(service.getAssetMultiSendRequest(ID)).withMessage()
                .isEqualTo(
                    storedRequest.withMultiTransactionData(
                        approveStatus = null,
                        approveData = null,
                        approveTransactionInfo = null,
                        disperseStatus = Status.FAILED,
                        disperseData = ENCODED_DISPERSE_ETHER_DATA,
                        disperseValue = TOTAL_TOKEN_AMOUNT,
                        disperseTransactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithFailedApproveStatusWhenApproveTransactionHasWrongTxHash() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val storedRequest = STORED_REQUEST.copy(approveTxHash = APPROVE_TX_HASH)

        suppose("asset multi-send request exists in database") {
            given(assetMultiSendRequestRepository.getById(ID))
                .willReturn(storedRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(storedRequest.chainId, null)
        val transactionInfo = APPROVE_TX_INFO.copy(hash = TransactionHash("wrong"))

        suppose("transaction is returned") {
            given(blockchainService.fetchTransactionInfo(chainSpec, APPROVE_TX_HASH))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "approve",
                    arguments = listOf(
                        FunctionArgument(CREATE_PARAMS.disperseContractAddress),
                        FunctionArgument(TOTAL_TOKEN_AMOUNT)
                    )
                )
            )
                .willReturn(ENCODED_APPROVE_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(storedRequest.projectId)
        )

        verify("asset multi-send request with failed approve status is returned") {
            assertThat(service.getAssetMultiSendRequest(ID)).withMessage()
                .isEqualTo(
                    storedRequest.withMultiTransactionData(
                        approveStatus = Status.FAILED,
                        approveData = ENCODED_APPROVE_DATA,
                        approveTransactionInfo = transactionInfo,
                        disperseStatus = null,
                        disperseData = null,
                        disperseValue = null,
                        disperseTransactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithFailedDisperseStatusWhenDisperseTransactionHasWrongTxHash() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val storedRequest = STORED_REQUEST.copy(tokenAddress = null, disperseTxHash = DISPERSE_TX_HASH)

        suppose("asset multi-send request exists in database") {
            given(assetMultiSendRequestRepository.getById(ID))
                .willReturn(storedRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(storedRequest.chainId, null)
        val transactionInfo = DISPERSE_ETHER_TX_INFO.copy(hash = TransactionHash("wrong"))

        suppose("transaction is returned") {
            given(blockchainService.fetchTransactionInfo(chainSpec, DISPERSE_TX_HASH))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "disperseEther",
                    arguments = listOf(
                        FunctionArgument.fromAddresses(CREATE_PARAMS.assetRecipientAddresses),
                        FunctionArgument.fromUint256s(CREATE_PARAMS.assetAmounts)
                    )
                )
            )
                .willReturn(ENCODED_DISPERSE_ETHER_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(storedRequest.projectId)
        )

        verify("asset multi-send request with failed disperse status is returned") {
            assertThat(service.getAssetMultiSendRequest(ID)).withMessage()
                .isEqualTo(
                    storedRequest.withMultiTransactionData(
                        approveStatus = null,
                        approveData = null,
                        approveTransactionInfo = null,
                        disperseStatus = Status.FAILED,
                        disperseData = ENCODED_DISPERSE_ETHER_DATA,
                        disperseValue = TOTAL_TOKEN_AMOUNT,
                        disperseTransactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithFailedApproveStatusWhenApproveTransactionHasMismatchingFromAddress() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val storedRequest = STORED_REQUEST.copy(approveTxHash = APPROVE_TX_HASH)

        suppose("asset multi-send request exists in database") {
            given(assetMultiSendRequestRepository.getById(ID))
                .willReturn(storedRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(storedRequest.chainId, null)
        val transactionInfo = APPROVE_TX_INFO.copy(from = WalletAddress("dead"))

        suppose("transaction is returned") {
            given(blockchainService.fetchTransactionInfo(chainSpec, APPROVE_TX_HASH))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "approve",
                    arguments = listOf(
                        FunctionArgument(CREATE_PARAMS.disperseContractAddress),
                        FunctionArgument(TOTAL_TOKEN_AMOUNT)
                    )
                )
            )
                .willReturn(ENCODED_APPROVE_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(storedRequest.projectId)
        )

        verify("asset multi-send request with failed approve status is returned") {
            assertThat(service.getAssetMultiSendRequest(ID)).withMessage()
                .isEqualTo(
                    storedRequest.withMultiTransactionData(
                        approveStatus = Status.FAILED,
                        approveData = ENCODED_APPROVE_DATA,
                        approveTransactionInfo = transactionInfo,
                        disperseStatus = null,
                        disperseData = null,
                        disperseValue = null,
                        disperseTransactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithFailedDisperseStatusWhenDisperseTransactionHasMismatchingFromAddress() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val storedRequest = STORED_REQUEST.copy(tokenAddress = null, disperseTxHash = DISPERSE_TX_HASH)

        suppose("asset multi-send request exists in database") {
            given(assetMultiSendRequestRepository.getById(ID))
                .willReturn(storedRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(storedRequest.chainId, null)
        val transactionInfo = DISPERSE_ETHER_TX_INFO.copy(from = WalletAddress("dead"))

        suppose("transaction is returned") {
            given(blockchainService.fetchTransactionInfo(chainSpec, DISPERSE_TX_HASH))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "disperseEther",
                    arguments = listOf(
                        FunctionArgument.fromAddresses(CREATE_PARAMS.assetRecipientAddresses),
                        FunctionArgument.fromUint256s(CREATE_PARAMS.assetAmounts)
                    )
                )
            )
                .willReturn(ENCODED_DISPERSE_ETHER_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(storedRequest.projectId)
        )

        verify("asset multi-send request with failed disperse status is returned") {
            assertThat(service.getAssetMultiSendRequest(ID)).withMessage()
                .isEqualTo(
                    storedRequest.withMultiTransactionData(
                        approveStatus = null,
                        approveData = null,
                        approveTransactionInfo = null,
                        disperseStatus = Status.FAILED,
                        disperseData = ENCODED_DISPERSE_ETHER_DATA,
                        disperseValue = TOTAL_TOKEN_AMOUNT,
                        disperseTransactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithFailedApproveStatusWhenApproveTransactionHasMismatchingToAddress() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val storedRequest = STORED_REQUEST.copy(approveTxHash = APPROVE_TX_HASH)

        suppose("asset multi-send request exists in database") {
            given(assetMultiSendRequestRepository.getById(ID))
                .willReturn(storedRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(storedRequest.chainId, null)
        val transactionInfo = APPROVE_TX_INFO.copy(to = WalletAddress("dead"))

        suppose("transaction is returned") {
            given(blockchainService.fetchTransactionInfo(chainSpec, APPROVE_TX_HASH))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "approve",
                    arguments = listOf(
                        FunctionArgument(CREATE_PARAMS.disperseContractAddress),
                        FunctionArgument(TOTAL_TOKEN_AMOUNT)
                    )
                )
            )
                .willReturn(ENCODED_APPROVE_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(storedRequest.projectId)
        )

        verify("asset multi-send request with failed approve status is returned") {
            assertThat(service.getAssetMultiSendRequest(ID)).withMessage()
                .isEqualTo(
                    storedRequest.withMultiTransactionData(
                        approveStatus = Status.FAILED,
                        approveData = ENCODED_APPROVE_DATA,
                        approveTransactionInfo = transactionInfo,
                        disperseStatus = null,
                        disperseData = null,
                        disperseValue = null,
                        disperseTransactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithFailedDisperseStatusWhenDisperseTransactionHasMismatchingToAddress() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val storedRequest = STORED_REQUEST.copy(tokenAddress = null, disperseTxHash = DISPERSE_TX_HASH)

        suppose("asset multi-send request exists in database") {
            given(assetMultiSendRequestRepository.getById(ID))
                .willReturn(storedRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(storedRequest.chainId, null)
        val transactionInfo = DISPERSE_ETHER_TX_INFO.copy(to = WalletAddress("dead"))

        suppose("transaction is returned") {
            given(blockchainService.fetchTransactionInfo(chainSpec, DISPERSE_TX_HASH))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "disperseEther",
                    arguments = listOf(
                        FunctionArgument.fromAddresses(CREATE_PARAMS.assetRecipientAddresses),
                        FunctionArgument.fromUint256s(CREATE_PARAMS.assetAmounts)
                    )
                )
            )
                .willReturn(ENCODED_DISPERSE_ETHER_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(storedRequest.projectId)
        )

        verify("asset multi-send request with failed disperse status is returned") {
            assertThat(service.getAssetMultiSendRequest(ID)).withMessage()
                .isEqualTo(
                    storedRequest.withMultiTransactionData(
                        approveStatus = null,
                        approveData = null,
                        approveTransactionInfo = null,
                        disperseStatus = Status.FAILED,
                        disperseData = ENCODED_DISPERSE_ETHER_DATA,
                        disperseValue = TOTAL_TOKEN_AMOUNT,
                        disperseTransactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithFailedApproveStatusWhenApproveTransactionHasNonNullDeployedCtrAddr() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val storedRequest = STORED_REQUEST.copy(approveTxHash = APPROVE_TX_HASH)

        suppose("asset multi-send request exists in database") {
            given(assetMultiSendRequestRepository.getById(ID))
                .willReturn(storedRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(storedRequest.chainId, null)
        val transactionInfo = APPROVE_TX_INFO.copy(deployedContractAddress = ContractAddress("dead"))

        suppose("transaction is returned") {
            given(blockchainService.fetchTransactionInfo(chainSpec, APPROVE_TX_HASH))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "approve",
                    arguments = listOf(
                        FunctionArgument(CREATE_PARAMS.disperseContractAddress),
                        FunctionArgument(TOTAL_TOKEN_AMOUNT)
                    )
                )
            )
                .willReturn(ENCODED_APPROVE_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(storedRequest.projectId)
        )

        verify("asset multi-send request with failed approve status is returned") {
            assertThat(service.getAssetMultiSendRequest(ID)).withMessage()
                .isEqualTo(
                    storedRequest.withMultiTransactionData(
                        approveStatus = Status.FAILED,
                        approveData = ENCODED_APPROVE_DATA,
                        approveTransactionInfo = transactionInfo,
                        disperseStatus = null,
                        disperseData = null,
                        disperseValue = null,
                        disperseTransactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithFailedDisperseStatusWhenDisperseTransactionHasNonNullDeployedContractAddr() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val storedRequest = STORED_REQUEST.copy(tokenAddress = null, disperseTxHash = DISPERSE_TX_HASH)

        suppose("asset multi-send request exists in database") {
            given(assetMultiSendRequestRepository.getById(ID))
                .willReturn(storedRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(storedRequest.chainId, null)
        val transactionInfo = DISPERSE_ETHER_TX_INFO.copy(deployedContractAddress = ContractAddress("dead"))

        suppose("transaction is returned") {
            given(blockchainService.fetchTransactionInfo(chainSpec, DISPERSE_TX_HASH))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "disperseEther",
                    arguments = listOf(
                        FunctionArgument.fromAddresses(CREATE_PARAMS.assetRecipientAddresses),
                        FunctionArgument.fromUint256s(CREATE_PARAMS.assetAmounts)
                    )
                )
            )
                .willReturn(ENCODED_DISPERSE_ETHER_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(storedRequest.projectId)
        )

        verify("asset multi-send request with failed disperse status is returned") {
            assertThat(service.getAssetMultiSendRequest(ID)).withMessage()
                .isEqualTo(
                    storedRequest.withMultiTransactionData(
                        approveStatus = null,
                        approveData = null,
                        approveTransactionInfo = null,
                        disperseStatus = Status.FAILED,
                        disperseData = ENCODED_DISPERSE_ETHER_DATA,
                        disperseValue = TOTAL_TOKEN_AMOUNT,
                        disperseTransactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithFailedApproveStatusWhenApproveTransactionHasMismatchingData() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val storedRequest = STORED_REQUEST.copy(approveTxHash = APPROVE_TX_HASH)

        suppose("asset multi-send request exists in database") {
            given(assetMultiSendRequestRepository.getById(ID))
                .willReturn(storedRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(storedRequest.chainId, null)
        val transactionInfo = APPROVE_TX_INFO.copy(data = FunctionData("mismatching"))

        suppose("transaction is returned") {
            given(blockchainService.fetchTransactionInfo(chainSpec, APPROVE_TX_HASH))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "approve",
                    arguments = listOf(
                        FunctionArgument(CREATE_PARAMS.disperseContractAddress),
                        FunctionArgument(TOTAL_TOKEN_AMOUNT)
                    )
                )
            )
                .willReturn(ENCODED_APPROVE_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(storedRequest.projectId)
        )

        verify("asset multi-send request with failed approve status is returned") {
            assertThat(service.getAssetMultiSendRequest(ID)).withMessage()
                .isEqualTo(
                    storedRequest.withMultiTransactionData(
                        approveStatus = Status.FAILED,
                        approveData = ENCODED_APPROVE_DATA,
                        approveTransactionInfo = transactionInfo,
                        disperseStatus = null,
                        disperseData = null,
                        disperseValue = null,
                        disperseTransactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithFailedDisperseStatusWhenDisperseTransactionHasMismatchingData() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val storedRequest = STORED_REQUEST.copy(tokenAddress = null, disperseTxHash = DISPERSE_TX_HASH)

        suppose("asset multi-send request exists in database") {
            given(assetMultiSendRequestRepository.getById(ID))
                .willReturn(storedRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(storedRequest.chainId, null)
        val transactionInfo = DISPERSE_ETHER_TX_INFO.copy(data = FunctionData("mismatching"))

        suppose("transaction is returned") {
            given(blockchainService.fetchTransactionInfo(chainSpec, DISPERSE_TX_HASH))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "disperseEther",
                    arguments = listOf(
                        FunctionArgument.fromAddresses(CREATE_PARAMS.assetRecipientAddresses),
                        FunctionArgument.fromUint256s(CREATE_PARAMS.assetAmounts)
                    )
                )
            )
                .willReturn(ENCODED_DISPERSE_ETHER_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(storedRequest.projectId)
        )

        verify("asset multi-send request with failed disperse status is returned") {
            assertThat(service.getAssetMultiSendRequest(ID)).withMessage()
                .isEqualTo(
                    storedRequest.withMultiTransactionData(
                        approveStatus = null,
                        approveData = null,
                        approveTransactionInfo = null,
                        disperseStatus = Status.FAILED,
                        disperseData = ENCODED_DISPERSE_ETHER_DATA,
                        disperseValue = TOTAL_TOKEN_AMOUNT,
                        disperseTransactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithFailedApproveStatusWhenApproveTransactionHasMismatchingValue() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val storedRequest = STORED_REQUEST.copy(approveTxHash = APPROVE_TX_HASH)

        suppose("asset multi-send request exists in database") {
            given(assetMultiSendRequestRepository.getById(ID))
                .willReturn(storedRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(storedRequest.chainId, null)
        val transactionInfo = APPROVE_TX_INFO.copy(value = Balance(BigInteger.ONE))

        suppose("transaction is returned") {
            given(blockchainService.fetchTransactionInfo(chainSpec, APPROVE_TX_HASH))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "approve",
                    arguments = listOf(
                        FunctionArgument(CREATE_PARAMS.disperseContractAddress),
                        FunctionArgument(TOTAL_TOKEN_AMOUNT)
                    )
                )
            )
                .willReturn(ENCODED_APPROVE_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(storedRequest.projectId)
        )

        verify("asset multi-send request with failed approve status is returned") {
            assertThat(service.getAssetMultiSendRequest(ID)).withMessage()
                .isEqualTo(
                    storedRequest.withMultiTransactionData(
                        approveStatus = Status.FAILED,
                        approveData = ENCODED_APPROVE_DATA,
                        approveTransactionInfo = transactionInfo,
                        disperseStatus = null,
                        disperseData = null,
                        disperseValue = null,
                        disperseTransactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithFailedDisperseStatusWhenDisperseTransactionHasMismatchingValue() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val storedRequest = STORED_REQUEST.copy(tokenAddress = null, disperseTxHash = DISPERSE_TX_HASH)

        suppose("asset multi-send request exists in database") {
            given(assetMultiSendRequestRepository.getById(ID))
                .willReturn(storedRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(storedRequest.chainId, null)
        val transactionInfo = DISPERSE_ETHER_TX_INFO.copy(value = Balance.ZERO)

        suppose("transaction is returned") {
            given(blockchainService.fetchTransactionInfo(chainSpec, DISPERSE_TX_HASH))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "disperseEther",
                    arguments = listOf(
                        FunctionArgument.fromAddresses(CREATE_PARAMS.assetRecipientAddresses),
                        FunctionArgument.fromUint256s(CREATE_PARAMS.assetAmounts)
                    )
                )
            )
                .willReturn(ENCODED_DISPERSE_ETHER_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(storedRequest.projectId)
        )

        verify("asset multi-send request with failed disperse status is returned") {
            assertThat(service.getAssetMultiSendRequest(ID)).withMessage()
                .isEqualTo(
                    storedRequest.withMultiTransactionData(
                        approveStatus = null,
                        approveData = null,
                        approveTransactionInfo = null,
                        disperseStatus = Status.FAILED,
                        disperseData = ENCODED_DISPERSE_ETHER_DATA,
                        disperseValue = TOTAL_TOKEN_AMOUNT,
                        disperseTransactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithSuccessfulApproveStatusWhenSenderIsNotNull() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val storedRequest = STORED_REQUEST.copy(approveTxHash = APPROVE_TX_HASH)

        suppose("asset multi-send request exists in database") {
            given(assetMultiSendRequestRepository.getById(ID))
                .willReturn(storedRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(storedRequest.chainId, null)
        val transactionInfo = APPROVE_TX_INFO

        suppose("transaction is returned") {
            given(blockchainService.fetchTransactionInfo(chainSpec, APPROVE_TX_HASH))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded for approve transaction") {
            given(
                functionEncoderService.encode(
                    functionName = "approve",
                    arguments = listOf(
                        FunctionArgument(CREATE_PARAMS.disperseContractAddress),
                        FunctionArgument(TOTAL_TOKEN_AMOUNT)
                    )
                )
            )
                .willReturn(ENCODED_APPROVE_DATA)
        }

        suppose("function data will be encoded for disperse transaction") {
            given(
                functionEncoderService.encode(
                    functionName = "disperseToken",
                    arguments = listOf(
                        FunctionArgument(CREATE_PARAMS.tokenAddress!!),
                        FunctionArgument.fromAddresses(CREATE_PARAMS.assetRecipientAddresses),
                        FunctionArgument.fromUint256s(CREATE_PARAMS.assetAmounts)
                    )
                )
            )
                .willReturn(ENCODED_DISPERSE_TOKEN_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(storedRequest.projectId)
        )

        verify("asset multi-send request with successful approve status is returned") {
            assertThat(service.getAssetMultiSendRequest(ID)).withMessage()
                .isEqualTo(
                    storedRequest.withMultiTransactionData(
                        approveStatus = Status.SUCCESS,
                        approveData = ENCODED_APPROVE_DATA,
                        approveTransactionInfo = transactionInfo,
                        disperseStatus = Status.PENDING,
                        disperseData = ENCODED_DISPERSE_TOKEN_DATA,
                        disperseValue = Balance.ZERO,
                        disperseTransactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithSuccessfulDisperseStatusWhenSenderIsNotNull() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val storedRequest = STORED_REQUEST.copy(tokenAddress = null, disperseTxHash = DISPERSE_TX_HASH)

        suppose("asset multi-send request exists in database") {
            given(assetMultiSendRequestRepository.getById(ID))
                .willReturn(storedRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(storedRequest.chainId, null)
        val transactionInfo = DISPERSE_ETHER_TX_INFO

        suppose("transaction is returned") {
            given(blockchainService.fetchTransactionInfo(chainSpec, DISPERSE_TX_HASH))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "disperseEther",
                    arguments = listOf(
                        FunctionArgument.fromAddresses(CREATE_PARAMS.assetRecipientAddresses),
                        FunctionArgument.fromUint256s(CREATE_PARAMS.assetAmounts)
                    )
                )
            )
                .willReturn(ENCODED_DISPERSE_ETHER_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(storedRequest.projectId)
        )

        verify("asset multi-send request with successful disperse status is returned") {
            assertThat(service.getAssetMultiSendRequest(ID)).withMessage()
                .isEqualTo(
                    storedRequest.withMultiTransactionData(
                        approveStatus = null,
                        approveData = null,
                        approveTransactionInfo = null,
                        disperseStatus = Status.SUCCESS,
                        disperseData = ENCODED_DISPERSE_ETHER_DATA,
                        disperseValue = TOTAL_TOKEN_AMOUNT,
                        disperseTransactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithSuccessfulApproveStatusWhenSenderIsNull() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val storedRequest = STORED_REQUEST.copy(approveTxHash = APPROVE_TX_HASH, assetSenderAddress = null)

        suppose("asset multi-send request exists in database") {
            given(assetMultiSendRequestRepository.getById(ID))
                .willReturn(storedRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(storedRequest.chainId, null)
        val transactionInfo = APPROVE_TX_INFO

        suppose("transaction is returned") {
            given(blockchainService.fetchTransactionInfo(chainSpec, APPROVE_TX_HASH))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded for approve transaction") {
            given(
                functionEncoderService.encode(
                    functionName = "approve",
                    arguments = listOf(
                        FunctionArgument(CREATE_PARAMS.disperseContractAddress),
                        FunctionArgument(TOTAL_TOKEN_AMOUNT)
                    )
                )
            )
                .willReturn(ENCODED_APPROVE_DATA)
        }

        suppose("function data will be encoded for disperse transaction") {
            given(
                functionEncoderService.encode(
                    functionName = "disperseToken",
                    arguments = listOf(
                        FunctionArgument(CREATE_PARAMS.tokenAddress!!),
                        FunctionArgument.fromAddresses(CREATE_PARAMS.assetRecipientAddresses),
                        FunctionArgument.fromUint256s(CREATE_PARAMS.assetAmounts)
                    )
                )
            )
                .willReturn(ENCODED_DISPERSE_TOKEN_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(storedRequest.projectId)
        )

        verify("asset multi-send request with pending successful status is returned") {
            assertThat(service.getAssetMultiSendRequest(ID)).withMessage()
                .isEqualTo(
                    storedRequest.withMultiTransactionData(
                        approveStatus = Status.SUCCESS,
                        approveData = ENCODED_APPROVE_DATA,
                        approveTransactionInfo = transactionInfo,
                        disperseStatus = Status.PENDING,
                        disperseData = ENCODED_DISPERSE_TOKEN_DATA,
                        disperseValue = Balance.ZERO,
                        disperseTransactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithSuccessfulDisperseStatusWhenSenderIsNull() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val storedRequest = STORED_REQUEST.copy(
            tokenAddress = null,
            disperseTxHash = DISPERSE_TX_HASH,
            assetSenderAddress = null
        )

        suppose("asset multi-send request exists in database") {
            given(assetMultiSendRequestRepository.getById(ID))
                .willReturn(storedRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(storedRequest.chainId, null)
        val transactionInfo = DISPERSE_ETHER_TX_INFO

        suppose("transaction is returned") {
            given(blockchainService.fetchTransactionInfo(chainSpec, DISPERSE_TX_HASH))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "disperseEther",
                    arguments = listOf(
                        FunctionArgument.fromAddresses(CREATE_PARAMS.assetRecipientAddresses),
                        FunctionArgument.fromUint256s(CREATE_PARAMS.assetAmounts)
                    )
                )
            )
                .willReturn(ENCODED_DISPERSE_ETHER_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(storedRequest.projectId)
        )

        verify("asset multi-send request with successful disperse status is returned") {
            assertThat(service.getAssetMultiSendRequest(ID)).withMessage()
                .isEqualTo(
                    storedRequest.withMultiTransactionData(
                        approveStatus = null,
                        approveData = null,
                        approveTransactionInfo = null,
                        disperseStatus = Status.SUCCESS,
                        disperseData = ENCODED_DISPERSE_ETHER_DATA,
                        disperseValue = TOTAL_TOKEN_AMOUNT,
                        disperseTransactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithSuccessfulApproveStatusAndDisperseStatus() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val storedRequest = STORED_REQUEST.copy(
            approveTxHash = APPROVE_TX_HASH,
            disperseTxHash = DISPERSE_TX_HASH
        )

        suppose("asset multi-send request exists in database") {
            given(assetMultiSendRequestRepository.getById(ID))
                .willReturn(storedRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(storedRequest.chainId, null)

        suppose("approve transaction is returned") {
            given(blockchainService.fetchTransactionInfo(chainSpec, APPROVE_TX_HASH))
                .willReturn(APPROVE_TX_INFO)
        }

        suppose("disperse transaction is returned") {
            given(blockchainService.fetchTransactionInfo(chainSpec, DISPERSE_TX_HASH))
                .willReturn(DISPERSE_TOKEN_TX_INFO)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded for approve transaction") {
            given(
                functionEncoderService.encode(
                    functionName = "approve",
                    arguments = listOf(
                        FunctionArgument(CREATE_PARAMS.disperseContractAddress),
                        FunctionArgument(TOTAL_TOKEN_AMOUNT)
                    )
                )
            )
                .willReturn(ENCODED_APPROVE_DATA)
        }

        suppose("function data will be encoded for disperse transaction") {
            given(
                functionEncoderService.encode(
                    functionName = "disperseToken",
                    arguments = listOf(
                        FunctionArgument(CREATE_PARAMS.tokenAddress!!),
                        FunctionArgument.fromAddresses(CREATE_PARAMS.assetRecipientAddresses),
                        FunctionArgument.fromUint256s(CREATE_PARAMS.assetAmounts)
                    )
                )
            )
                .willReturn(ENCODED_DISPERSE_TOKEN_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(storedRequest.projectId)
        )

        verify("asset multi-send request with successful approve status is returned") {
            assertThat(service.getAssetMultiSendRequest(ID)).withMessage()
                .isEqualTo(
                    storedRequest.withMultiTransactionData(
                        approveStatus = Status.SUCCESS,
                        approveData = ENCODED_APPROVE_DATA,
                        approveTransactionInfo = APPROVE_TX_INFO,
                        disperseStatus = Status.SUCCESS,
                        disperseData = ENCODED_DISPERSE_TOKEN_DATA,
                        disperseValue = Balance.ZERO,
                        disperseTransactionInfo = DISPERSE_TOKEN_TX_INFO
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyReturnListOfAssetMultiSendRequestsByProjectId() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()

        suppose("asset multi-send request exists in database") {
            given(assetMultiSendRequestRepository.getAllByProjectId(PROJECT.id))
                .willReturn(listOf(STORED_REQUEST))
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "approve",
                    arguments = listOf(
                        FunctionArgument(CREATE_PARAMS.disperseContractAddress),
                        FunctionArgument(TOTAL_TOKEN_AMOUNT)
                    )
                )
            )
                .willReturn(ENCODED_APPROVE_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = projectRepositoryMock(STORED_REQUEST.projectId)
        )

        verify("asset multi-send request is returned") {
            assertThat(service.getAssetMultiSendRequestsByProjectId(PROJECT.id))
                .withMessage()
                .isEqualTo(
                    listOf(
                        STORED_REQUEST.withMultiTransactionData(
                            approveStatus = Status.PENDING,
                            approveData = ENCODED_APPROVE_DATA,
                            approveTransactionInfo = null,
                            disperseStatus = null,
                            disperseData = null,
                            disperseValue = null,
                            disperseTransactionInfo = null
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyReturnEmptyListOfAssetMultiSendRequestsForNonExistentProject() {
        val projectId = UUID.randomUUID()
        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = mock(),
            assetMultiSendRequestRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = projectRepositoryMock(projectId)
        )

        verify("empty list is returned") {
            val result = service.getAssetMultiSendRequestsByProjectId(projectId)

            assertThat(result).withMessage()
                .isEmpty()
        }
    }

    @Test
    fun mustCorrectlyReturnListOfAssetMultiSendRequestsBySenderAddress() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()

        suppose("asset multi-send request exists in database") {
            given(assetMultiSendRequestRepository.getBySender(STORED_REQUEST.assetSenderAddress!!))
                .willReturn(listOf(STORED_REQUEST))
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "approve",
                    arguments = listOf(
                        FunctionArgument(CREATE_PARAMS.disperseContractAddress),
                        FunctionArgument(TOTAL_TOKEN_AMOUNT)
                    )
                )
            )
                .willReturn(ENCODED_APPROVE_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = projectRepositoryMock(STORED_REQUEST.projectId)
        )

        verify("asset multi-send request with successful status is returned") {
            assertThat(service.getAssetMultiSendRequestsBySender(STORED_REQUEST.assetSenderAddress!!)).withMessage()
                .isEqualTo(
                    listOf(
                        STORED_REQUEST.withMultiTransactionData(
                            approveStatus = Status.PENDING,
                            approveData = ENCODED_APPROVE_DATA,
                            approveTransactionInfo = null,
                            disperseStatus = null,
                            disperseData = null,
                            disperseValue = null,
                            disperseTransactionInfo = null
                        )
                    )
                )
        }
    }

    @Test
    fun mustSuccessfullyAttachApproveTxInfo() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val id = UUID.randomUUID()
        val caller = WalletAddress("0xbc25524e0daacB1F149BA55279f593F5E3FB73e9")

        suppose("approve txInfo will be successfully attached to the request") {
            given(assetMultiSendRequestRepository.setApproveTxInfo(id, APPROVE_TX_HASH, caller))
                .willReturn(true)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = mock(),
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("approve txInfo was successfully attached") {
            service.attachApproveTxInfo(id, APPROVE_TX_HASH, caller)

            verifyMock(assetMultiSendRequestRepository)
                .setApproveTxInfo(id, APPROVE_TX_HASH, caller)
            verifyNoMoreInteractions(assetMultiSendRequestRepository)
        }
    }

    @Test
    fun mustThrowCannotAttachTxInfoExceptionWhenAttachingApproveTxInfoFails() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val id = UUID.randomUUID()
        val caller = WalletAddress("0xbc25524e0daacB1F149BA55279f593F5E3FB73e9")

        suppose("attaching approve txInfo will fail") {
            given(assetMultiSendRequestRepository.setApproveTxInfo(id, APPROVE_TX_HASH, caller))
                .willReturn(false)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = mock(),
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("CannotAttachTxInfoException is thrown") {
            assertThrows<CannotAttachTxInfoException>(message) {
                service.attachApproveTxInfo(id, APPROVE_TX_HASH, caller)
            }

            verifyMock(assetMultiSendRequestRepository)
                .setApproveTxInfo(id, APPROVE_TX_HASH, caller)
            verifyNoMoreInteractions(assetMultiSendRequestRepository)
        }
    }

    @Test
    fun mustSuccessfullyDisperseApproveTxInfo() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val id = UUID.randomUUID()
        val caller = WalletAddress("0xbc25524e0daacB1F149BA55279f593F5E3FB73e9")

        suppose("disperse txInfo will be successfully attached to the request") {
            given(assetMultiSendRequestRepository.setDisperseTxInfo(id, DISPERSE_TX_HASH, caller))
                .willReturn(true)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = mock(),
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("disperse txInfo was successfully attached") {
            service.attachDisperseTxInfo(id, DISPERSE_TX_HASH, caller)

            verifyMock(assetMultiSendRequestRepository)
                .setDisperseTxInfo(id, DISPERSE_TX_HASH, caller)
            verifyNoMoreInteractions(assetMultiSendRequestRepository)
        }
    }

    @Test
    fun mustThrowCannotAttachTxInfoExceptionWhenAttachingDisperseTxInfoFails() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val id = UUID.randomUUID()
        val caller = WalletAddress("0xbc25524e0daacB1F149BA55279f593F5E3FB73e9")

        suppose("attaching disperse txInfo will fail") {
            given(assetMultiSendRequestRepository.setDisperseTxInfo(id, APPROVE_TX_HASH, caller))
                .willReturn(false)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = mock(),
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("CannotAttachTxInfoException is thrown") {
            assertThrows<CannotAttachTxInfoException>(message) {
                service.attachDisperseTxInfo(id, APPROVE_TX_HASH, caller)
            }

            verifyMock(assetMultiSendRequestRepository)
                .setDisperseTxInfo(id, APPROVE_TX_HASH, caller)
            verifyNoMoreInteractions(assetMultiSendRequestRepository)
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
