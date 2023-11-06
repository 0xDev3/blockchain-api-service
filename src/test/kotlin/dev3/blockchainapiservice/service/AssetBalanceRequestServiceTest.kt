package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.blockchain.BlockchainService
import dev3.blockchainapiservice.blockchain.properties.ChainSpec
import dev3.blockchainapiservice.exception.CannotAttachSignedMessageException
import dev3.blockchainapiservice.exception.ResourceNotFoundException
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.model.params.CreateAssetBalanceRequestParams
import dev3.blockchainapiservice.model.params.StoreAssetBalanceRequestParams
import dev3.blockchainapiservice.model.result.AssetBalanceRequest
import dev3.blockchainapiservice.model.result.FullAssetBalanceRequest
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.repository.AssetBalanceRequestRepository
import dev3.blockchainapiservice.repository.ProjectRepository
import dev3.blockchainapiservice.util.AccountBalance
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.BaseUrl
import dev3.blockchainapiservice.util.BlockNumber
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.SignedMessage
import dev3.blockchainapiservice.util.Status
import dev3.blockchainapiservice.util.UtcDateTime
import dev3.blockchainapiservice.util.WalletAddress
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.math.BigInteger
import java.util.UUID

class AssetBalanceRequestServiceTest : TestBase() {

    @Test
    fun mustSuccessfullyCreateAssetBalanceRequest() {
        val uuid = UUID.randomUUID()
        val uuidProvider = mock<UuidProvider>()

        suppose("some UUID will be returned") {
            call(uuidProvider.getUuid())
                .willReturn(uuid)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some timestamp will be returned") {
            call(utcDateTimeProvider.getUtcDateTime())
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
            call(assetBalanceRequestRepository.store(databaseParams))
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
            expectThat(service.createAssetBalanceRequest(createParams, project))
                .isEqualTo(databaseResponse)

            expectInteractions(assetBalanceRequestRepository) {
                once.store(databaseParams)
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionForNonExistentAssetBalanceRequest() {
        val uuid = UUID.randomUUID()
        val assetBalanceRequestRepository = mock<AssetBalanceRequestRepository>()

        suppose("asset balance request is not in database") {
            call(assetBalanceRequestRepository.getById(uuid))
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
            expectThrows<ResourceNotFoundException> {
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
            chainId = TestData.CHAIN_ID,
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
            call(assetBalanceRequestRepository.getById(uuid))
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

            expectThat(result)
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
            chainId = TestData.CHAIN_ID,
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
            call(assetBalanceRequestRepository.getById(uuid))
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
            call(
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

            expectThat(result)
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
            chainId = TestData.CHAIN_ID,
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
            call(assetBalanceRequestRepository.getById(uuid))
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
            call(
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

            expectThat(result)
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
            chainId = TestData.CHAIN_ID,
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
            call(assetBalanceRequestRepository.getById(uuid))
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
            call(
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
            call(
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

            expectThat(result)
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
            chainId = TestData.CHAIN_ID,
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
            call(assetBalanceRequestRepository.getById(uuid))
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
            call(
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
            call(
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

            expectThat(result)
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
            chainId = TestData.CHAIN_ID,
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
            call(assetBalanceRequestRepository.getById(uuid))
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
            call(
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
            call(
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

            expectThat(result)
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
            chainId = TestData.CHAIN_ID,
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
            call(assetBalanceRequestRepository.getById(uuid))
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
            call(
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
            call(
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

            expectThat(result)
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
            chainId = TestData.CHAIN_ID,
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
            call(assetBalanceRequestRepository.getById(uuid))
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
            call(
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
            call(
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

            expectThat(result)
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
            chainId = TestData.CHAIN_ID,
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
            call(assetBalanceRequestRepository.getAllByProjectId(assetBalanceRequest.projectId))
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
            call(
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
            call(
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

            expectThat(result)
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

            expectThat(result)
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
            call(assetBalanceRequestRepository.setSignedMessage(uuid, walletAddress, signedMessage))
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

            expectInteractions(assetBalanceRequestRepository) {
                once.setSignedMessage(uuid, walletAddress, signedMessage)
            }
        }
    }

    @Test
    fun mustThrowCannotAttachSignedMessageExceptionWhenAttachingWalletAddressAndSignedMessageFails() {
        val uuid = UUID.randomUUID()
        val walletAddress = WalletAddress("a")
        val signedMessage = SignedMessage("signed-message")
        val assetBalanceRequestRepository = mock<AssetBalanceRequestRepository>()

        suppose("signed message will be attached") {
            call(assetBalanceRequestRepository.setSignedMessage(uuid, walletAddress, signedMessage))
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
            expectThrows<CannotAttachSignedMessageException> {
                service.attachWalletAddressAndSignedMessage(uuid, walletAddress, signedMessage)
            }

            expectInteractions(assetBalanceRequestRepository) {
                once.setSignedMessage(uuid, walletAddress, signedMessage)
            }
        }
    }

    private fun projectRepositoryMockWithCustomRpcUrl(projectId: UUID, customRpcUrl: String?): ProjectRepository {
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
                        customRpcUrl = customRpcUrl,
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }

        return projectRepository
    }
}
