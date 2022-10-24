package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.blockchain.BlockchainService
import com.ampnet.blockchainapiservice.blockchain.properties.Chain
import com.ampnet.blockchainapiservice.blockchain.properties.ChainSpec
import com.ampnet.blockchainapiservice.exception.CannotAttachSignedMessageException
import com.ampnet.blockchainapiservice.exception.ResourceNotFoundException
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.params.CreateAssetBalanceRequestParams
import com.ampnet.blockchainapiservice.model.params.StoreAssetBalanceRequestParams
import com.ampnet.blockchainapiservice.model.result.AssetBalanceRequest
import com.ampnet.blockchainapiservice.model.result.FullAssetBalanceRequest
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.repository.AssetBalanceRequestRepository
import com.ampnet.blockchainapiservice.repository.ProjectRepository
import com.ampnet.blockchainapiservice.util.AccountBalance
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.BaseUrl
import com.ampnet.blockchainapiservice.util.BlockNumber
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.SignedMessage
import com.ampnet.blockchainapiservice.util.Status
import com.ampnet.blockchainapiservice.util.UtcDateTime
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoMoreInteractions
import java.math.BigInteger
import java.util.UUID
import org.mockito.kotlin.verify as verifyMock

class AssetBalanceRequestServiceTest : TestBase() {

    @Test
    fun mustSuccessfullyCreateAssetBalanceRequest() {
        val uuid = UUID.randomUUID()
        val uuidProvider = mock<UuidProvider>()

        suppose("some UUID will be returned") {
            given(uuidProvider.getUuid())
                .willReturn(uuid)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some timestamp will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val project = Project(
            id = UUID.randomUUID(),
            ownerId = UUID.randomUUID(),
            issuerContractAddress = ContractAddress("a"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = ChainId(1337L),
            customRpcUrl = "custom-rpc-url",
            createdAt = TestData.TIMESTAMP
        )

        val redirectUrl = "redirect-url/\${id}"
        val tokenAddress = ContractAddress("abc")
        val createParams = CreateAssetBalanceRequestParams(
            redirectUrl = redirectUrl,
            tokenAddress = tokenAddress,
            blockNumber = BlockNumber(BigInteger.TEN),
            requestedWalletAddress = WalletAddress("def"),
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            )
        )
        val fullRedirectUrl = redirectUrl.replace("\${id}", uuid.toString())
        val databaseParams = StoreAssetBalanceRequestParams(
            id = uuid,
            projectId = project.id,
            chainId = project.chainId,
            redirectUrl = fullRedirectUrl,
            tokenAddress = tokenAddress,
            blockNumber = createParams.blockNumber,
            requestedWalletAddress = createParams.requestedWalletAddress,
            arbitraryData = createParams.arbitraryData,
            screenConfig = createParams.screenConfig,
            createdAt = TestData.TIMESTAMP
        )
        val databaseResponse = AssetBalanceRequest(
            id = uuid,
            projectId = project.id,
            chainId = project.chainId,
            redirectUrl = fullRedirectUrl,
            tokenAddress = tokenAddress,
            blockNumber = createParams.blockNumber,
            requestedWalletAddress = createParams.requestedWalletAddress,
            actualWalletAddress = null,
            signedMessage = null,
            arbitraryData = createParams.arbitraryData,
            screenConfig = createParams.screenConfig,
            createdAt = TestData.TIMESTAMP
        )
        val assetBalanceRequestRepository = mock<AssetBalanceRequestRepository>()

        suppose("asset balance request is stored in database") {
            given(assetBalanceRequestRepository.store(databaseParams))
                .willReturn(databaseResponse)
        }

        val service = AssetBalanceRequestServiceImpl(
            signatureCheckerService = mock(),
            blockchainService = mock(),
            assetBalanceRequestRepository = assetBalanceRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = uuidProvider,
                utcDateTimeProvider = utcDateTimeProvider,
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("asset balance request is correctly created") {
            assertThat(service.createAssetBalanceRequest(createParams, project)).withMessage()
                .isEqualTo(databaseResponse)

            verifyMock(assetBalanceRequestRepository)
                .store(databaseParams)
            verifyNoMoreInteractions(assetBalanceRequestRepository)
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionForNonExistentAssetBalanceRequest() {
        val uuid = UUID.randomUUID()
        val assetBalanceRequestRepository = mock<AssetBalanceRequestRepository>()

        suppose("asset balance request is not in database") {
            given(assetBalanceRequestRepository.getById(uuid))
                .willReturn(null)
        }

        val service = AssetBalanceRequestServiceImpl(
            signatureCheckerService = mock(),
            blockchainService = mock(),
            assetBalanceRequestRepository = assetBalanceRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.getAssetBalanceRequest(uuid)
            }
        }
    }

    @Test
    fun mustReturnAssetBalanceRequestWithPendingStatusWhenActualWalletAddressIsNull() {
        val uuid = UUID.randomUUID()
        val assetBalanceRequest = AssetBalanceRequest(
            id = uuid,
            projectId = UUID.randomUUID(),
            chainId = Chain.MATIC_TESTNET_MUMBAI.id,
            redirectUrl = "redirect-url/$uuid",
            tokenAddress = ContractAddress("abc"),
            blockNumber = BlockNumber(BigInteger.TEN),
            requestedWalletAddress = WalletAddress("def"),
            actualWalletAddress = null,
            signedMessage = null,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val assetBalanceRequestRepository = mock<AssetBalanceRequestRepository>()

        suppose("asset balance request is returned from database") {
            given(assetBalanceRequestRepository.getById(uuid))
                .willReturn(assetBalanceRequest)
        }

        val service = AssetBalanceRequestServiceImpl(
            signatureCheckerService = mock(),
            blockchainService = mock(),
            assetBalanceRequestRepository = assetBalanceRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = projectRepositoryMockWithCustomRpcUrl(assetBalanceRequest.projectId, null)
        )

        verify("asset balance request with pending status is returned") {
            val result = service.getAssetBalanceRequest(uuid)

            assertThat(result).withMessage()
                .isEqualTo(
                    FullAssetBalanceRequest(
                        id = uuid,
                        projectId = assetBalanceRequest.projectId,
                        status = Status.PENDING,
                        chainId = assetBalanceRequest.chainId,
                        redirectUrl = assetBalanceRequest.redirectUrl,
                        tokenAddress = assetBalanceRequest.tokenAddress,
                        blockNumber = assetBalanceRequest.blockNumber,
                        requestedWalletAddress = assetBalanceRequest.requestedWalletAddress,
                        arbitraryData = assetBalanceRequest.arbitraryData,
                        screenConfig = assetBalanceRequest.screenConfig,
                        balance = null,
                        messageToSign = assetBalanceRequest.messageToSign,
                        signedMessage = null,
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetBalanceRequestWithPendingStatusWhenSignedMessageIsNull() {
        val uuid = UUID.randomUUID()
        val assetBalanceRequest = AssetBalanceRequest(
            id = uuid,
            projectId = UUID.randomUUID(),
            chainId = Chain.MATIC_TESTNET_MUMBAI.id,
            redirectUrl = "redirect-url/$uuid",
            tokenAddress = ContractAddress("abc"),
            blockNumber = BlockNumber(BigInteger.TEN),
            requestedWalletAddress = WalletAddress("def"),
            actualWalletAddress = WalletAddress("fff"),
            signedMessage = null,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val assetBalanceRequestRepository = mock<AssetBalanceRequestRepository>()

        suppose("asset balance request is returned from database") {
            given(assetBalanceRequestRepository.getById(uuid))
                .willReturn(assetBalanceRequest)
        }

        val customRpcUrl = "custom-rpc-url"
        val balance = AccountBalance(
            wallet = assetBalanceRequest.actualWalletAddress!!,
            blockNumber = assetBalanceRequest.blockNumber!!,
            timestamp = UtcDateTime.ofEpochSeconds(0L),
            amount = Balance(BigInteger.ONE)
        )
        val blockchainService = mock<BlockchainService>()

        suppose("blockchain service will return some asset balance") {
            given(
                blockchainService.fetchErc20AccountBalance(
                    chainSpec = ChainSpec(
                        chainId = assetBalanceRequest.chainId,
                        customRpcUrl = customRpcUrl
                    ),
                    contractAddress = assetBalanceRequest.tokenAddress!!,
                    walletAddress = assetBalanceRequest.actualWalletAddress!!,
                    blockParameter = assetBalanceRequest.blockNumber!!
                )
            ).willReturn(balance)
        }

        val service = AssetBalanceRequestServiceImpl(
            signatureCheckerService = mock(),
            blockchainService = blockchainService,
            assetBalanceRequestRepository = assetBalanceRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMockWithCustomRpcUrl(assetBalanceRequest.projectId, customRpcUrl)
        )

        verify("asset balance request with pending status is returned") {
            val result = service.getAssetBalanceRequest(uuid)

            assertThat(result).withMessage()
                .isEqualTo(
                    FullAssetBalanceRequest(
                        id = uuid,
                        projectId = assetBalanceRequest.projectId,
                        status = Status.PENDING,
                        chainId = assetBalanceRequest.chainId,
                        redirectUrl = assetBalanceRequest.redirectUrl,
                        tokenAddress = assetBalanceRequest.tokenAddress,
                        blockNumber = assetBalanceRequest.blockNumber,
                        requestedWalletAddress = assetBalanceRequest.requestedWalletAddress,
                        arbitraryData = assetBalanceRequest.arbitraryData,
                        screenConfig = assetBalanceRequest.screenConfig,
                        balance = balance,
                        messageToSign = assetBalanceRequest.messageToSign,
                        signedMessage = null,
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetBalanceRequestWithFailedStatusWhenRequestedAndActualWalletAddressesDontMatch() {
        val uuid = UUID.randomUUID()
        val assetBalanceRequest = AssetBalanceRequest(
            id = uuid,
            projectId = UUID.randomUUID(),
            chainId = Chain.MATIC_TESTNET_MUMBAI.id,
            redirectUrl = "redirect-url/$uuid",
            tokenAddress = ContractAddress("abc"),
            blockNumber = BlockNumber(BigInteger.TEN),
            requestedWalletAddress = WalletAddress("def"),
            actualWalletAddress = WalletAddress("fff"),
            signedMessage = SignedMessage("signed-message"),
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val assetBalanceRequestRepository = mock<AssetBalanceRequestRepository>()

        suppose("asset balance request is returned from database") {
            given(assetBalanceRequestRepository.getById(uuid))
                .willReturn(assetBalanceRequest)
        }

        val customRpcUrl = "custom-rpc-url"
        val balance = AccountBalance(
            wallet = assetBalanceRequest.actualWalletAddress!!,
            blockNumber = assetBalanceRequest.blockNumber!!,
            timestamp = UtcDateTime.ofEpochSeconds(0L),
            amount = Balance(BigInteger.ONE)
        )
        val blockchainService = mock<BlockchainService>()

        suppose("blockchain service will return some asset balance") {
            given(
                blockchainService.fetchErc20AccountBalance(
                    chainSpec = ChainSpec(
                        chainId = assetBalanceRequest.chainId,
                        customRpcUrl = customRpcUrl
                    ),
                    contractAddress = assetBalanceRequest.tokenAddress!!,
                    walletAddress = assetBalanceRequest.actualWalletAddress!!,
                    blockParameter = assetBalanceRequest.blockNumber!!
                )
            ).willReturn(balance)
        }

        val service = AssetBalanceRequestServiceImpl(
            signatureCheckerService = mock(),
            blockchainService = blockchainService,
            assetBalanceRequestRepository = assetBalanceRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMockWithCustomRpcUrl(assetBalanceRequest.projectId, customRpcUrl)
        )

        verify("asset balance request with failed status is returned") {
            val result = service.getAssetBalanceRequest(uuid)

            assertThat(result).withMessage()
                .isEqualTo(
                    FullAssetBalanceRequest(
                        id = uuid,
                        projectId = assetBalanceRequest.projectId,
                        status = Status.FAILED,
                        chainId = assetBalanceRequest.chainId,
                        redirectUrl = assetBalanceRequest.redirectUrl,
                        tokenAddress = assetBalanceRequest.tokenAddress,
                        blockNumber = assetBalanceRequest.blockNumber,
                        requestedWalletAddress = assetBalanceRequest.requestedWalletAddress,
                        arbitraryData = assetBalanceRequest.arbitraryData,
                        screenConfig = assetBalanceRequest.screenConfig,
                        balance = balance,
                        messageToSign = assetBalanceRequest.messageToSign,
                        signedMessage = assetBalanceRequest.signedMessage,
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetBalanceRequestWithFailedStatusWhenSignatureDoesntMatch() {
        val uuid = UUID.randomUUID()
        val assetBalanceRequest = AssetBalanceRequest(
            id = uuid,
            projectId = UUID.randomUUID(),
            chainId = Chain.MATIC_TESTNET_MUMBAI.id,
            redirectUrl = "redirect-url/$uuid",
            tokenAddress = ContractAddress("abc"),
            blockNumber = BlockNumber(BigInteger.TEN),
            requestedWalletAddress = WalletAddress("def"),
            actualWalletAddress = WalletAddress("def"),
            signedMessage = SignedMessage("signed-message"),
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val assetBalanceRequestRepository = mock<AssetBalanceRequestRepository>()

        suppose("asset balance request is returned from database") {
            given(assetBalanceRequestRepository.getById(uuid))
                .willReturn(assetBalanceRequest)
        }

        val customRpcUrl = "custom-rpc-url"
        val balance = AccountBalance(
            wallet = assetBalanceRequest.actualWalletAddress!!,
            blockNumber = assetBalanceRequest.blockNumber!!,
            timestamp = UtcDateTime.ofEpochSeconds(0L),
            amount = Balance(BigInteger.ONE)
        )
        val blockchainService = mock<BlockchainService>()

        suppose("blockchain service will return some asset balance") {
            given(
                blockchainService.fetchErc20AccountBalance(
                    chainSpec = ChainSpec(
                        chainId = assetBalanceRequest.chainId,
                        customRpcUrl = customRpcUrl
                    ),
                    contractAddress = assetBalanceRequest.tokenAddress!!,
                    walletAddress = assetBalanceRequest.actualWalletAddress!!,
                    blockParameter = assetBalanceRequest.blockNumber!!
                )
            ).willReturn(balance)
        }

        val signatureCheckerService = mock<SignatureCheckerService>()

        suppose("signature checker will return false") {
            given(
                signatureCheckerService.signatureMatches(
                    message = assetBalanceRequest.messageToSign,
                    signedMessage = assetBalanceRequest.signedMessage!!,
                    signer = assetBalanceRequest.actualWalletAddress!!
                )
            ).willReturn(false)
        }

        val service = AssetBalanceRequestServiceImpl(
            signatureCheckerService = signatureCheckerService,
            blockchainService = blockchainService,
            assetBalanceRequestRepository = assetBalanceRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMockWithCustomRpcUrl(assetBalanceRequest.projectId, customRpcUrl)
        )

        verify("asset balance request with failed status is returned") {
            val result = service.getAssetBalanceRequest(uuid)

            assertThat(result).withMessage()
                .isEqualTo(
                    FullAssetBalanceRequest(
                        id = uuid,
                        projectId = assetBalanceRequest.projectId,
                        status = Status.FAILED,
                        chainId = assetBalanceRequest.chainId,
                        redirectUrl = assetBalanceRequest.redirectUrl,
                        tokenAddress = assetBalanceRequest.tokenAddress,
                        blockNumber = assetBalanceRequest.blockNumber,
                        requestedWalletAddress = assetBalanceRequest.requestedWalletAddress,
                        arbitraryData = assetBalanceRequest.arbitraryData,
                        screenConfig = assetBalanceRequest.screenConfig,
                        balance = balance,
                        messageToSign = assetBalanceRequest.messageToSign,
                        signedMessage = assetBalanceRequest.signedMessage,
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetBalanceRequestWithSuccessfulStatusWhenRequestedWalletAddressIsNull() {
        val uuid = UUID.randomUUID()
        val assetBalanceRequest = AssetBalanceRequest(
            id = uuid,
            projectId = UUID.randomUUID(),
            chainId = Chain.MATIC_TESTNET_MUMBAI.id,
            redirectUrl = "redirect-url/$uuid",
            tokenAddress = ContractAddress("abc"),
            blockNumber = BlockNumber(BigInteger.TEN),
            requestedWalletAddress = null,
            actualWalletAddress = WalletAddress("def"),
            signedMessage = SignedMessage("signed-message"),
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val assetBalanceRequestRepository = mock<AssetBalanceRequestRepository>()

        suppose("asset balance request is returned from database") {
            given(assetBalanceRequestRepository.getById(uuid))
                .willReturn(assetBalanceRequest)
        }

        val customRpcUrl = "custom-rpc-url"
        val balance = AccountBalance(
            wallet = assetBalanceRequest.actualWalletAddress!!,
            blockNumber = assetBalanceRequest.blockNumber!!,
            timestamp = UtcDateTime.ofEpochSeconds(0L),
            amount = Balance(BigInteger.ONE)
        )
        val blockchainService = mock<BlockchainService>()

        suppose("blockchain service will return some asset balance") {
            given(
                blockchainService.fetchErc20AccountBalance(
                    chainSpec = ChainSpec(
                        chainId = assetBalanceRequest.chainId,
                        customRpcUrl = customRpcUrl
                    ),
                    contractAddress = assetBalanceRequest.tokenAddress!!,
                    walletAddress = assetBalanceRequest.actualWalletAddress!!,
                    blockParameter = assetBalanceRequest.blockNumber!!
                )
            ).willReturn(balance)
        }

        val signatureCheckerService = mock<SignatureCheckerService>()

        suppose("signature checker will return true") {
            given(
                signatureCheckerService.signatureMatches(
                    message = assetBalanceRequest.messageToSign,
                    signedMessage = assetBalanceRequest.signedMessage!!,
                    signer = assetBalanceRequest.actualWalletAddress!!
                )
            ).willReturn(true)
        }

        val service = AssetBalanceRequestServiceImpl(
            signatureCheckerService = signatureCheckerService,
            blockchainService = blockchainService,
            assetBalanceRequestRepository = assetBalanceRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMockWithCustomRpcUrl(assetBalanceRequest.projectId, customRpcUrl)
        )

        verify("asset balance request with successful status is returned") {
            val result = service.getAssetBalanceRequest(uuid)

            assertThat(result).withMessage()
                .isEqualTo(
                    FullAssetBalanceRequest(
                        id = uuid,
                        projectId = assetBalanceRequest.projectId,
                        status = Status.SUCCESS,
                        chainId = assetBalanceRequest.chainId,
                        redirectUrl = assetBalanceRequest.redirectUrl,
                        tokenAddress = assetBalanceRequest.tokenAddress,
                        blockNumber = assetBalanceRequest.blockNumber,
                        requestedWalletAddress = assetBalanceRequest.requestedWalletAddress,
                        arbitraryData = assetBalanceRequest.arbitraryData,
                        screenConfig = assetBalanceRequest.screenConfig,
                        balance = balance,
                        messageToSign = assetBalanceRequest.messageToSign,
                        signedMessage = assetBalanceRequest.signedMessage,
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetBalanceRequestWithSuccessfulStatusWhenRequestedWalletAddressIsSpecified() {
        val uuid = UUID.randomUUID()
        val assetBalanceRequest = AssetBalanceRequest(
            id = uuid,
            projectId = UUID.randomUUID(),
            chainId = Chain.MATIC_TESTNET_MUMBAI.id,
            redirectUrl = "redirect-url/$uuid",
            tokenAddress = ContractAddress("abc"),
            blockNumber = BlockNumber(BigInteger.TEN),
            requestedWalletAddress = WalletAddress("def"),
            actualWalletAddress = WalletAddress("def"),
            signedMessage = SignedMessage("signed-message"),
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val assetBalanceRequestRepository = mock<AssetBalanceRequestRepository>()

        suppose("asset balance request is returned from database") {
            given(assetBalanceRequestRepository.getById(uuid))
                .willReturn(assetBalanceRequest)
        }

        val customRpcUrl = "custom-rpc-url"
        val balance = AccountBalance(
            wallet = assetBalanceRequest.actualWalletAddress!!,
            blockNumber = assetBalanceRequest.blockNumber!!,
            timestamp = UtcDateTime.ofEpochSeconds(0L),
            amount = Balance(BigInteger.ONE)
        )
        val blockchainService = mock<BlockchainService>()

        suppose("blockchain service will return some asset balance") {
            given(
                blockchainService.fetchErc20AccountBalance(
                    chainSpec = ChainSpec(
                        chainId = assetBalanceRequest.chainId,
                        customRpcUrl = customRpcUrl
                    ),
                    contractAddress = assetBalanceRequest.tokenAddress!!,
                    walletAddress = assetBalanceRequest.actualWalletAddress!!,
                    blockParameter = assetBalanceRequest.blockNumber!!
                )
            ).willReturn(balance)
        }

        val signatureCheckerService = mock<SignatureCheckerService>()

        suppose("signature checker will return true") {
            given(
                signatureCheckerService.signatureMatches(
                    message = assetBalanceRequest.messageToSign,
                    signedMessage = assetBalanceRequest.signedMessage!!,
                    signer = assetBalanceRequest.actualWalletAddress!!
                )
            ).willReturn(true)
        }

        val service = AssetBalanceRequestServiceImpl(
            signatureCheckerService = signatureCheckerService,
            blockchainService = blockchainService,
            assetBalanceRequestRepository = assetBalanceRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMockWithCustomRpcUrl(assetBalanceRequest.projectId, customRpcUrl)
        )

        verify("asset balance request with successful status is returned") {
            val result = service.getAssetBalanceRequest(uuid)

            assertThat(result).withMessage()
                .isEqualTo(
                    FullAssetBalanceRequest(
                        id = uuid,
                        projectId = assetBalanceRequest.projectId,
                        status = Status.SUCCESS,
                        chainId = assetBalanceRequest.chainId,
                        redirectUrl = assetBalanceRequest.redirectUrl,
                        tokenAddress = assetBalanceRequest.tokenAddress,
                        blockNumber = assetBalanceRequest.blockNumber,
                        requestedWalletAddress = assetBalanceRequest.requestedWalletAddress,
                        arbitraryData = assetBalanceRequest.arbitraryData,
                        screenConfig = assetBalanceRequest.screenConfig,
                        balance = balance,
                        messageToSign = assetBalanceRequest.messageToSign,
                        signedMessage = assetBalanceRequest.signedMessage,
                        createdAt = assetBalanceRequest.createdAt
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetBalanceRequestWithSuccessfulStatusWhenRequestedWalletAddressIsNullForNativeToken() {
        val uuid = UUID.randomUUID()
        val assetBalanceRequest = AssetBalanceRequest(
            id = uuid,
            projectId = UUID.randomUUID(),
            chainId = Chain.MATIC_TESTNET_MUMBAI.id,
            redirectUrl = "redirect-url/$uuid",
            tokenAddress = null,
            blockNumber = BlockNumber(BigInteger.TEN),
            requestedWalletAddress = null,
            actualWalletAddress = WalletAddress("def"),
            signedMessage = SignedMessage("signed-message"),
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val assetBalanceRequestRepository = mock<AssetBalanceRequestRepository>()

        suppose("asset balance request is returned from database") {
            given(assetBalanceRequestRepository.getById(uuid))
                .willReturn(assetBalanceRequest)
        }

        val customRpcUrl = "custom-rpc-url"
        val balance = AccountBalance(
            wallet = assetBalanceRequest.actualWalletAddress!!,
            blockNumber = assetBalanceRequest.blockNumber!!,
            timestamp = UtcDateTime.ofEpochSeconds(0L),
            amount = Balance(BigInteger.ONE)
        )
        val blockchainService = mock<BlockchainService>()

        suppose("blockchain service will return some asset balance") {
            given(
                blockchainService.fetchAccountBalance(
                    chainSpec = ChainSpec(
                        chainId = assetBalanceRequest.chainId,
                        customRpcUrl = customRpcUrl
                    ),
                    walletAddress = assetBalanceRequest.actualWalletAddress!!,
                    blockParameter = assetBalanceRequest.blockNumber!!
                )
            ).willReturn(balance)
        }

        val signatureCheckerService = mock<SignatureCheckerService>()

        suppose("signature checker will return true") {
            given(
                signatureCheckerService.signatureMatches(
                    message = assetBalanceRequest.messageToSign,
                    signedMessage = assetBalanceRequest.signedMessage!!,
                    signer = assetBalanceRequest.actualWalletAddress!!
                )
            ).willReturn(true)
        }

        val service = AssetBalanceRequestServiceImpl(
            signatureCheckerService = signatureCheckerService,
            blockchainService = blockchainService,
            assetBalanceRequestRepository = assetBalanceRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMockWithCustomRpcUrl(assetBalanceRequest.projectId, customRpcUrl)
        )

        verify("asset balance request with successful status is returned") {
            val result = service.getAssetBalanceRequest(uuid)

            assertThat(result).withMessage()
                .isEqualTo(
                    FullAssetBalanceRequest(
                        id = uuid,
                        projectId = assetBalanceRequest.projectId,
                        status = Status.SUCCESS,
                        chainId = assetBalanceRequest.chainId,
                        redirectUrl = assetBalanceRequest.redirectUrl,
                        tokenAddress = null,
                        blockNumber = assetBalanceRequest.blockNumber,
                        requestedWalletAddress = assetBalanceRequest.requestedWalletAddress,
                        arbitraryData = assetBalanceRequest.arbitraryData,
                        screenConfig = assetBalanceRequest.screenConfig,
                        balance = balance,
                        messageToSign = assetBalanceRequest.messageToSign,
                        signedMessage = assetBalanceRequest.signedMessage,
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetBalanceRequestWithSuccessfulStatusWhenRequestedWalletAddressIsSpecifiedForNativeToken() {
        val uuid = UUID.randomUUID()
        val assetBalanceRequest = AssetBalanceRequest(
            id = uuid,
            projectId = UUID.randomUUID(),
            chainId = Chain.MATIC_TESTNET_MUMBAI.id,
            redirectUrl = "redirect-url/$uuid",
            tokenAddress = null,
            blockNumber = BlockNumber(BigInteger.TEN),
            requestedWalletAddress = WalletAddress("def"),
            actualWalletAddress = WalletAddress("def"),
            signedMessage = SignedMessage("signed-message"),
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val assetBalanceRequestRepository = mock<AssetBalanceRequestRepository>()

        suppose("asset balance request is returned from database") {
            given(assetBalanceRequestRepository.getById(uuid))
                .willReturn(assetBalanceRequest)
        }

        val customRpcUrl = "custom-rpc-url"
        val balance = AccountBalance(
            wallet = assetBalanceRequest.actualWalletAddress!!,
            blockNumber = assetBalanceRequest.blockNumber!!,
            timestamp = UtcDateTime.ofEpochSeconds(0L),
            amount = Balance(BigInteger.ONE)
        )
        val blockchainService = mock<BlockchainService>()

        suppose("blockchain service will return some asset balance") {
            given(
                blockchainService.fetchAccountBalance(
                    chainSpec = ChainSpec(
                        chainId = assetBalanceRequest.chainId,
                        customRpcUrl = customRpcUrl
                    ),
                    walletAddress = assetBalanceRequest.actualWalletAddress!!,
                    blockParameter = assetBalanceRequest.blockNumber!!
                )
            ).willReturn(balance)
        }

        val signatureCheckerService = mock<SignatureCheckerService>()

        suppose("signature checker will return true") {
            given(
                signatureCheckerService.signatureMatches(
                    message = assetBalanceRequest.messageToSign,
                    signedMessage = assetBalanceRequest.signedMessage!!,
                    signer = assetBalanceRequest.actualWalletAddress!!
                )
            ).willReturn(true)
        }

        val service = AssetBalanceRequestServiceImpl(
            signatureCheckerService = signatureCheckerService,
            blockchainService = blockchainService,
            assetBalanceRequestRepository = assetBalanceRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMockWithCustomRpcUrl(assetBalanceRequest.projectId, customRpcUrl)
        )

        verify("asset balance request with successful status is returned") {
            val result = service.getAssetBalanceRequest(uuid)

            assertThat(result).withMessage()
                .isEqualTo(
                    FullAssetBalanceRequest(
                        id = uuid,
                        projectId = assetBalanceRequest.projectId,
                        status = Status.SUCCESS,
                        chainId = assetBalanceRequest.chainId,
                        redirectUrl = assetBalanceRequest.redirectUrl,
                        tokenAddress = null,
                        blockNumber = assetBalanceRequest.blockNumber,
                        requestedWalletAddress = assetBalanceRequest.requestedWalletAddress,
                        arbitraryData = assetBalanceRequest.arbitraryData,
                        screenConfig = assetBalanceRequest.screenConfig,
                        balance = balance,
                        messageToSign = assetBalanceRequest.messageToSign,
                        signedMessage = assetBalanceRequest.signedMessage,
                        createdAt = assetBalanceRequest.createdAt
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyReturnListOfAssetBalanceRequestsByProjectId() {
        val uuid = UUID.randomUUID()
        val assetBalanceRequest = AssetBalanceRequest(
            id = uuid,
            projectId = UUID.randomUUID(),
            chainId = Chain.MATIC_TESTNET_MUMBAI.id,
            redirectUrl = "redirect-url/$uuid",
            tokenAddress = ContractAddress("abc"),
            blockNumber = BlockNumber(BigInteger.TEN),
            requestedWalletAddress = WalletAddress("def"),
            actualWalletAddress = WalletAddress("def"),
            signedMessage = SignedMessage("signed-message"),
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val assetBalanceRequestRepository = mock<AssetBalanceRequestRepository>()

        suppose("asset balance request is returned from database") {
            given(assetBalanceRequestRepository.getAllByProjectId(assetBalanceRequest.projectId))
                .willReturn(listOf(assetBalanceRequest))
        }

        val customRpcUrl = "custom-rpc-url"
        val balance = AccountBalance(
            wallet = assetBalanceRequest.actualWalletAddress!!,
            blockNumber = assetBalanceRequest.blockNumber!!,
            timestamp = UtcDateTime.ofEpochSeconds(0L),
            amount = Balance(BigInteger.ONE)
        )
        val blockchainService = mock<BlockchainService>()

        suppose("blockchain service will return some asset balance") {
            given(
                blockchainService.fetchErc20AccountBalance(
                    chainSpec = ChainSpec(
                        chainId = assetBalanceRequest.chainId,
                        customRpcUrl = customRpcUrl
                    ),
                    contractAddress = assetBalanceRequest.tokenAddress!!,
                    walletAddress = assetBalanceRequest.actualWalletAddress!!,
                    blockParameter = assetBalanceRequest.blockNumber!!
                )
            ).willReturn(balance)
        }

        val signatureCheckerService = mock<SignatureCheckerService>()

        suppose("signature checker will return true") {
            given(
                signatureCheckerService.signatureMatches(
                    message = assetBalanceRequest.messageToSign,
                    signedMessage = assetBalanceRequest.signedMessage!!,
                    signer = assetBalanceRequest.actualWalletAddress!!
                )
            ).willReturn(true)
        }

        val service = AssetBalanceRequestServiceImpl(
            signatureCheckerService = signatureCheckerService,
            blockchainService = blockchainService,
            assetBalanceRequestRepository = assetBalanceRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMockWithCustomRpcUrl(assetBalanceRequest.projectId, customRpcUrl)
        )

        verify("asset balance request with successful status is returned") {
            val result = service.getAssetBalanceRequestsByProjectId(assetBalanceRequest.projectId)

            assertThat(result).withMessage()
                .isEqualTo(
                    listOf(
                        FullAssetBalanceRequest(
                            id = uuid,
                            projectId = assetBalanceRequest.projectId,
                            status = Status.SUCCESS,
                            chainId = assetBalanceRequest.chainId,
                            redirectUrl = assetBalanceRequest.redirectUrl,
                            tokenAddress = assetBalanceRequest.tokenAddress,
                            blockNumber = assetBalanceRequest.blockNumber,
                            requestedWalletAddress = assetBalanceRequest.requestedWalletAddress,
                            arbitraryData = assetBalanceRequest.arbitraryData,
                            screenConfig = assetBalanceRequest.screenConfig,
                            balance = balance,
                            messageToSign = assetBalanceRequest.messageToSign,
                            signedMessage = assetBalanceRequest.signedMessage,
                            createdAt = assetBalanceRequest.createdAt
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyReturnEmptyListOfAssetBalanceRequestsForNonExistentProject() {
        val projectId = UUID.randomUUID()
        val service = AssetBalanceRequestServiceImpl(
            signatureCheckerService = mock(),
            blockchainService = mock(),
            assetBalanceRequestRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = projectRepositoryMockWithCustomRpcUrl(projectId, null)
        )

        verify("empty list is returned") {
            val result = service.getAssetBalanceRequestsByProjectId(projectId)

            assertThat(result).withMessage()
                .isEmpty()
        }
    }

    @Test
    fun mustAttachWalletAddressAndSignedMessage() {
        val uuid = UUID.randomUUID()
        val walletAddress = WalletAddress("a")
        val signedMessage = SignedMessage("signed-message")
        val assetBalanceRequestRepository = mock<AssetBalanceRequestRepository>()

        suppose("signed message will be attached") {
            given(assetBalanceRequestRepository.setSignedMessage(uuid, walletAddress, signedMessage))
                .willReturn(true)
        }

        val service = AssetBalanceRequestServiceImpl(
            signatureCheckerService = mock(),
            blockchainService = mock(),
            assetBalanceRequestRepository = assetBalanceRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("wallet address and signed message are successfully attached") {
            service.attachWalletAddressAndSignedMessage(uuid, walletAddress, signedMessage)

            verifyMock(assetBalanceRequestRepository)
                .setSignedMessage(uuid, walletAddress, signedMessage)
            verifyNoMoreInteractions(assetBalanceRequestRepository)
        }
    }

    @Test
    fun mustThrowCannotAttachSignedMessageExceptionWhenAttachingWalletAddressAndSignedMessageFails() {
        val uuid = UUID.randomUUID()
        val walletAddress = WalletAddress("a")
        val signedMessage = SignedMessage("signed-message")
        val assetBalanceRequestRepository = mock<AssetBalanceRequestRepository>()

        suppose("signed message will be attached") {
            given(assetBalanceRequestRepository.setSignedMessage(uuid, walletAddress, signedMessage))
                .willReturn(false)
        }

        val service = AssetBalanceRequestServiceImpl(
            signatureCheckerService = mock(),
            blockchainService = mock(),
            assetBalanceRequestRepository = assetBalanceRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("CannotAttachSignedMessageException is thrown") {
            assertThrows<CannotAttachSignedMessageException>(message) {
                service.attachWalletAddressAndSignedMessage(uuid, walletAddress, signedMessage)
            }

            verifyMock(assetBalanceRequestRepository)
                .setSignedMessage(uuid, walletAddress, signedMessage)
            verifyNoMoreInteractions(assetBalanceRequestRepository)
        }
    }

    private fun projectRepositoryMockWithCustomRpcUrl(projectId: UUID, customRpcUrl: String?): ProjectRepository {
        val projectRepository = mock<ProjectRepository>()

        given(projectRepository.getById(projectId))
            .willReturn(
                Project(
                    id = projectId,
                    ownerId = UUID.randomUUID(),
                    issuerContractAddress = ContractAddress("dead"),
                    baseRedirectUrl = BaseUrl(""),
                    chainId = ChainId(0L),
                    customRpcUrl = customRpcUrl,
                    createdAt = TestData.TIMESTAMP
                )
            )

        return projectRepository
    }
}
