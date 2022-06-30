package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.blockchain.BlockchainService
import com.ampnet.blockchainapiservice.blockchain.properties.Chain
import com.ampnet.blockchainapiservice.blockchain.properties.ChainSpec
import com.ampnet.blockchainapiservice.exception.CannotAttachSignedMessageException
import com.ampnet.blockchainapiservice.exception.ResourceNotFoundException
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.params.CreateErc20BalanceRequestParams
import com.ampnet.blockchainapiservice.model.params.StoreErc20BalanceRequestParams
import com.ampnet.blockchainapiservice.model.result.Erc20BalanceRequest
import com.ampnet.blockchainapiservice.model.result.FullErc20BalanceRequest
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.repository.Erc20BalanceRequestRepository
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

class Erc20BalanceRequestServiceTest : TestBase() {

    @Test
    fun mustSuccessfullyCreateErc20BalanceRequest() {
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
        val createParams = CreateErc20BalanceRequestParams(
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
        val databaseParams = StoreErc20BalanceRequestParams(
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
        val databaseResponse = Erc20BalanceRequest(
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
        val erc20BalanceRequestRepository = mock<Erc20BalanceRequestRepository>()

        suppose("ERC20 balance request is stored in database") {
            given(erc20BalanceRequestRepository.store(databaseParams))
                .willReturn(databaseResponse)
        }

        val service = Erc20BalanceRequestServiceImpl(
            signatureCheckerService = mock(),
            blockchainService = mock(),
            erc20BalanceRequestRepository = erc20BalanceRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = uuidProvider,
                utcDateTimeProvider = utcDateTimeProvider,
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("ERC20 balance request is correctly created") {
            assertThat(service.createErc20BalanceRequest(createParams, project)).withMessage()
                .isEqualTo(databaseResponse)

            verifyMock(erc20BalanceRequestRepository)
                .store(databaseParams)
            verifyNoMoreInteractions(erc20BalanceRequestRepository)
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionForNonExistentErc20BalanceRequest() {
        val uuid = UUID.randomUUID()
        val erc20BalanceRequestRepository = mock<Erc20BalanceRequestRepository>()

        suppose("ERC20 balance request is not in database") {
            given(erc20BalanceRequestRepository.getById(uuid))
                .willReturn(null)
        }

        val service = Erc20BalanceRequestServiceImpl(
            signatureCheckerService = mock(),
            blockchainService = mock(),
            erc20BalanceRequestRepository = erc20BalanceRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.getErc20BalanceRequest(uuid)
            }
        }
    }

    @Test
    fun mustReturnErc20BalanceRequestWithPendingStatusWhenActualWalletAddressIsNull() {
        val uuid = UUID.randomUUID()
        val erc20BalanceRequest = Erc20BalanceRequest(
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
        val erc20BalanceRequestRepository = mock<Erc20BalanceRequestRepository>()

        suppose("ERC20 balance request is returned from database") {
            given(erc20BalanceRequestRepository.getById(uuid))
                .willReturn(erc20BalanceRequest)
        }

        val service = Erc20BalanceRequestServiceImpl(
            signatureCheckerService = mock(),
            blockchainService = mock(),
            erc20BalanceRequestRepository = erc20BalanceRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = projectRepositoryMockWithCustomRpcUrl(erc20BalanceRequest.projectId, null)
        )

        verify("ERC20 balance request with pending status is returned") {
            val result = service.getErc20BalanceRequest(uuid)

            assertThat(result).withMessage()
                .isEqualTo(
                    FullErc20BalanceRequest(
                        id = uuid,
                        projectId = erc20BalanceRequest.projectId,
                        status = Status.PENDING,
                        chainId = erc20BalanceRequest.chainId,
                        redirectUrl = erc20BalanceRequest.redirectUrl,
                        tokenAddress = erc20BalanceRequest.tokenAddress,
                        blockNumber = erc20BalanceRequest.blockNumber,
                        requestedWalletAddress = erc20BalanceRequest.requestedWalletAddress,
                        arbitraryData = erc20BalanceRequest.arbitraryData,
                        screenConfig = erc20BalanceRequest.screenConfig,
                        balance = null,
                        messageToSign = erc20BalanceRequest.messageToSign,
                        signedMessage = null,
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20BalanceRequestWithPendingStatusWhenSignedMessageIsNull() {
        val uuid = UUID.randomUUID()
        val erc20BalanceRequest = Erc20BalanceRequest(
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
        val erc20BalanceRequestRepository = mock<Erc20BalanceRequestRepository>()

        suppose("ERC20 balance request is returned from database") {
            given(erc20BalanceRequestRepository.getById(uuid))
                .willReturn(erc20BalanceRequest)
        }

        val customRpcUrl = "custom-rpc-url"
        val balance = AccountBalance(
            wallet = erc20BalanceRequest.actualWalletAddress!!,
            blockNumber = erc20BalanceRequest.blockNumber!!,
            timestamp = UtcDateTime.ofEpochSeconds(0L),
            amount = Balance(BigInteger.ONE)
        )
        val blockchainService = mock<BlockchainService>()

        suppose("blockchain service will return some ERC20 balance") {
            given(
                blockchainService.fetchErc20AccountBalance(
                    chainSpec = ChainSpec(
                        chainId = erc20BalanceRequest.chainId,
                        customRpcUrl = customRpcUrl
                    ),
                    contractAddress = erc20BalanceRequest.tokenAddress!!,
                    walletAddress = erc20BalanceRequest.actualWalletAddress!!,
                    blockParameter = erc20BalanceRequest.blockNumber!!
                )
            ).willReturn(balance)
        }

        val service = Erc20BalanceRequestServiceImpl(
            signatureCheckerService = mock(),
            blockchainService = blockchainService,
            erc20BalanceRequestRepository = erc20BalanceRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMockWithCustomRpcUrl(erc20BalanceRequest.projectId, customRpcUrl)
        )

        verify("ERC20 balance request with pending status is returned") {
            val result = service.getErc20BalanceRequest(uuid)

            assertThat(result).withMessage()
                .isEqualTo(
                    FullErc20BalanceRequest(
                        id = uuid,
                        projectId = erc20BalanceRequest.projectId,
                        status = Status.PENDING,
                        chainId = erc20BalanceRequest.chainId,
                        redirectUrl = erc20BalanceRequest.redirectUrl,
                        tokenAddress = erc20BalanceRequest.tokenAddress,
                        blockNumber = erc20BalanceRequest.blockNumber,
                        requestedWalletAddress = erc20BalanceRequest.requestedWalletAddress,
                        arbitraryData = erc20BalanceRequest.arbitraryData,
                        screenConfig = erc20BalanceRequest.screenConfig,
                        balance = balance,
                        messageToSign = erc20BalanceRequest.messageToSign,
                        signedMessage = null,
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20BalanceRequestWithFailedStatusWhenRequestedAndActualWalletAddressesDontMatch() {
        val uuid = UUID.randomUUID()
        val erc20BalanceRequest = Erc20BalanceRequest(
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
        val erc20BalanceRequestRepository = mock<Erc20BalanceRequestRepository>()

        suppose("ERC20 balance request is returned from database") {
            given(erc20BalanceRequestRepository.getById(uuid))
                .willReturn(erc20BalanceRequest)
        }

        val customRpcUrl = "custom-rpc-url"
        val balance = AccountBalance(
            wallet = erc20BalanceRequest.actualWalletAddress!!,
            blockNumber = erc20BalanceRequest.blockNumber!!,
            timestamp = UtcDateTime.ofEpochSeconds(0L),
            amount = Balance(BigInteger.ONE)
        )
        val blockchainService = mock<BlockchainService>()

        suppose("blockchain service will return some ERC20 balance") {
            given(
                blockchainService.fetchErc20AccountBalance(
                    chainSpec = ChainSpec(
                        chainId = erc20BalanceRequest.chainId,
                        customRpcUrl = customRpcUrl
                    ),
                    contractAddress = erc20BalanceRequest.tokenAddress!!,
                    walletAddress = erc20BalanceRequest.actualWalletAddress!!,
                    blockParameter = erc20BalanceRequest.blockNumber!!
                )
            ).willReturn(balance)
        }

        val service = Erc20BalanceRequestServiceImpl(
            signatureCheckerService = mock(),
            blockchainService = blockchainService,
            erc20BalanceRequestRepository = erc20BalanceRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMockWithCustomRpcUrl(erc20BalanceRequest.projectId, customRpcUrl)
        )

        verify("ERC20 balance request with failed status is returned") {
            val result = service.getErc20BalanceRequest(uuid)

            assertThat(result).withMessage()
                .isEqualTo(
                    FullErc20BalanceRequest(
                        id = uuid,
                        projectId = erc20BalanceRequest.projectId,
                        status = Status.FAILED,
                        chainId = erc20BalanceRequest.chainId,
                        redirectUrl = erc20BalanceRequest.redirectUrl,
                        tokenAddress = erc20BalanceRequest.tokenAddress,
                        blockNumber = erc20BalanceRequest.blockNumber,
                        requestedWalletAddress = erc20BalanceRequest.requestedWalletAddress,
                        arbitraryData = erc20BalanceRequest.arbitraryData,
                        screenConfig = erc20BalanceRequest.screenConfig,
                        balance = balance,
                        messageToSign = erc20BalanceRequest.messageToSign,
                        signedMessage = erc20BalanceRequest.signedMessage,
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20BalanceRequestWithFailedStatusWhenSignatureDoesntMatch() {
        val uuid = UUID.randomUUID()
        val erc20BalanceRequest = Erc20BalanceRequest(
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
        val erc20BalanceRequestRepository = mock<Erc20BalanceRequestRepository>()

        suppose("ERC20 balance request is returned from database") {
            given(erc20BalanceRequestRepository.getById(uuid))
                .willReturn(erc20BalanceRequest)
        }

        val customRpcUrl = "custom-rpc-url"
        val balance = AccountBalance(
            wallet = erc20BalanceRequest.actualWalletAddress!!,
            blockNumber = erc20BalanceRequest.blockNumber!!,
            timestamp = UtcDateTime.ofEpochSeconds(0L),
            amount = Balance(BigInteger.ONE)
        )
        val blockchainService = mock<BlockchainService>()

        suppose("blockchain service will return some ERC20 balance") {
            given(
                blockchainService.fetchErc20AccountBalance(
                    chainSpec = ChainSpec(
                        chainId = erc20BalanceRequest.chainId,
                        customRpcUrl = customRpcUrl
                    ),
                    contractAddress = erc20BalanceRequest.tokenAddress!!,
                    walletAddress = erc20BalanceRequest.actualWalletAddress!!,
                    blockParameter = erc20BalanceRequest.blockNumber!!
                )
            ).willReturn(balance)
        }

        val signatureCheckerService = mock<SignatureCheckerService>()

        suppose("signature checker will return false") {
            given(
                signatureCheckerService.signatureMatches(
                    message = erc20BalanceRequest.messageToSign,
                    signedMessage = erc20BalanceRequest.signedMessage!!,
                    signer = erc20BalanceRequest.actualWalletAddress!!
                )
            ).willReturn(false)
        }

        val service = Erc20BalanceRequestServiceImpl(
            signatureCheckerService = signatureCheckerService,
            blockchainService = blockchainService,
            erc20BalanceRequestRepository = erc20BalanceRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMockWithCustomRpcUrl(erc20BalanceRequest.projectId, customRpcUrl)
        )

        verify("ERC20 balance request with failed status is returned") {
            val result = service.getErc20BalanceRequest(uuid)

            assertThat(result).withMessage()
                .isEqualTo(
                    FullErc20BalanceRequest(
                        id = uuid,
                        projectId = erc20BalanceRequest.projectId,
                        status = Status.FAILED,
                        chainId = erc20BalanceRequest.chainId,
                        redirectUrl = erc20BalanceRequest.redirectUrl,
                        tokenAddress = erc20BalanceRequest.tokenAddress,
                        blockNumber = erc20BalanceRequest.blockNumber,
                        requestedWalletAddress = erc20BalanceRequest.requestedWalletAddress,
                        arbitraryData = erc20BalanceRequest.arbitraryData,
                        screenConfig = erc20BalanceRequest.screenConfig,
                        balance = balance,
                        messageToSign = erc20BalanceRequest.messageToSign,
                        signedMessage = erc20BalanceRequest.signedMessage,
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20BalanceRequestWithSuccessfulStatusWhenRequestedWalletAddressIsNull() {
        val uuid = UUID.randomUUID()
        val erc20BalanceRequest = Erc20BalanceRequest(
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
        val erc20BalanceRequestRepository = mock<Erc20BalanceRequestRepository>()

        suppose("ERC20 balance request is returned from database") {
            given(erc20BalanceRequestRepository.getById(uuid))
                .willReturn(erc20BalanceRequest)
        }

        val customRpcUrl = "custom-rpc-url"
        val balance = AccountBalance(
            wallet = erc20BalanceRequest.actualWalletAddress!!,
            blockNumber = erc20BalanceRequest.blockNumber!!,
            timestamp = UtcDateTime.ofEpochSeconds(0L),
            amount = Balance(BigInteger.ONE)
        )
        val blockchainService = mock<BlockchainService>()

        suppose("blockchain service will return some ERC20 balance") {
            given(
                blockchainService.fetchErc20AccountBalance(
                    chainSpec = ChainSpec(
                        chainId = erc20BalanceRequest.chainId,
                        customRpcUrl = customRpcUrl
                    ),
                    contractAddress = erc20BalanceRequest.tokenAddress!!,
                    walletAddress = erc20BalanceRequest.actualWalletAddress!!,
                    blockParameter = erc20BalanceRequest.blockNumber!!
                )
            ).willReturn(balance)
        }

        val signatureCheckerService = mock<SignatureCheckerService>()

        suppose("signature checker will return true") {
            given(
                signatureCheckerService.signatureMatches(
                    message = erc20BalanceRequest.messageToSign,
                    signedMessage = erc20BalanceRequest.signedMessage!!,
                    signer = erc20BalanceRequest.actualWalletAddress!!
                )
            ).willReturn(true)
        }

        val service = Erc20BalanceRequestServiceImpl(
            signatureCheckerService = signatureCheckerService,
            blockchainService = blockchainService,
            erc20BalanceRequestRepository = erc20BalanceRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMockWithCustomRpcUrl(erc20BalanceRequest.projectId, customRpcUrl)
        )

        verify("ERC20 balance request with successful status is returned") {
            val result = service.getErc20BalanceRequest(uuid)

            assertThat(result).withMessage()
                .isEqualTo(
                    FullErc20BalanceRequest(
                        id = uuid,
                        projectId = erc20BalanceRequest.projectId,
                        status = Status.SUCCESS,
                        chainId = erc20BalanceRequest.chainId,
                        redirectUrl = erc20BalanceRequest.redirectUrl,
                        tokenAddress = erc20BalanceRequest.tokenAddress,
                        blockNumber = erc20BalanceRequest.blockNumber,
                        requestedWalletAddress = erc20BalanceRequest.requestedWalletAddress,
                        arbitraryData = erc20BalanceRequest.arbitraryData,
                        screenConfig = erc20BalanceRequest.screenConfig,
                        balance = balance,
                        messageToSign = erc20BalanceRequest.messageToSign,
                        signedMessage = erc20BalanceRequest.signedMessage,
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20BalanceRequestWithSuccessfulStatusWhenRequestedWalletAddressIsSpecified() {
        val uuid = UUID.randomUUID()
        val erc20BalanceRequest = Erc20BalanceRequest(
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
        val erc20BalanceRequestRepository = mock<Erc20BalanceRequestRepository>()

        suppose("ERC20 balance request is returned from database") {
            given(erc20BalanceRequestRepository.getById(uuid))
                .willReturn(erc20BalanceRequest)
        }

        val customRpcUrl = "custom-rpc-url"
        val balance = AccountBalance(
            wallet = erc20BalanceRequest.actualWalletAddress!!,
            blockNumber = erc20BalanceRequest.blockNumber!!,
            timestamp = UtcDateTime.ofEpochSeconds(0L),
            amount = Balance(BigInteger.ONE)
        )
        val blockchainService = mock<BlockchainService>()

        suppose("blockchain service will return some ERC20 balance") {
            given(
                blockchainService.fetchErc20AccountBalance(
                    chainSpec = ChainSpec(
                        chainId = erc20BalanceRequest.chainId,
                        customRpcUrl = customRpcUrl
                    ),
                    contractAddress = erc20BalanceRequest.tokenAddress!!,
                    walletAddress = erc20BalanceRequest.actualWalletAddress!!,
                    blockParameter = erc20BalanceRequest.blockNumber!!
                )
            ).willReturn(balance)
        }

        val signatureCheckerService = mock<SignatureCheckerService>()

        suppose("signature checker will return true") {
            given(
                signatureCheckerService.signatureMatches(
                    message = erc20BalanceRequest.messageToSign,
                    signedMessage = erc20BalanceRequest.signedMessage!!,
                    signer = erc20BalanceRequest.actualWalletAddress!!
                )
            ).willReturn(true)
        }

        val service = Erc20BalanceRequestServiceImpl(
            signatureCheckerService = signatureCheckerService,
            blockchainService = blockchainService,
            erc20BalanceRequestRepository = erc20BalanceRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMockWithCustomRpcUrl(erc20BalanceRequest.projectId, customRpcUrl)
        )

        verify("ERC20 balance request with successful status is returned") {
            val result = service.getErc20BalanceRequest(uuid)

            assertThat(result).withMessage()
                .isEqualTo(
                    FullErc20BalanceRequest(
                        id = uuid,
                        projectId = erc20BalanceRequest.projectId,
                        status = Status.SUCCESS,
                        chainId = erc20BalanceRequest.chainId,
                        redirectUrl = erc20BalanceRequest.redirectUrl,
                        tokenAddress = erc20BalanceRequest.tokenAddress,
                        blockNumber = erc20BalanceRequest.blockNumber,
                        requestedWalletAddress = erc20BalanceRequest.requestedWalletAddress,
                        arbitraryData = erc20BalanceRequest.arbitraryData,
                        screenConfig = erc20BalanceRequest.screenConfig,
                        balance = balance,
                        messageToSign = erc20BalanceRequest.messageToSign,
                        signedMessage = erc20BalanceRequest.signedMessage,
                        createdAt = erc20BalanceRequest.createdAt
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20BalanceRequestWithSuccessfulStatusWhenRequestedWalletAddressIsNullForNativeToken() {
        val uuid = UUID.randomUUID()
        val erc20BalanceRequest = Erc20BalanceRequest(
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
        val erc20BalanceRequestRepository = mock<Erc20BalanceRequestRepository>()

        suppose("ERC20 balance request is returned from database") {
            given(erc20BalanceRequestRepository.getById(uuid))
                .willReturn(erc20BalanceRequest)
        }

        val customRpcUrl = "custom-rpc-url"
        val balance = AccountBalance(
            wallet = erc20BalanceRequest.actualWalletAddress!!,
            blockNumber = erc20BalanceRequest.blockNumber!!,
            timestamp = UtcDateTime.ofEpochSeconds(0L),
            amount = Balance(BigInteger.ONE)
        )
        val blockchainService = mock<BlockchainService>()

        suppose("blockchain service will return some ERC20 balance") {
            given(
                blockchainService.fetchAccountBalance(
                    chainSpec = ChainSpec(
                        chainId = erc20BalanceRequest.chainId,
                        customRpcUrl = customRpcUrl
                    ),
                    walletAddress = erc20BalanceRequest.actualWalletAddress!!,
                    blockParameter = erc20BalanceRequest.blockNumber!!
                )
            ).willReturn(balance)
        }

        val signatureCheckerService = mock<SignatureCheckerService>()

        suppose("signature checker will return true") {
            given(
                signatureCheckerService.signatureMatches(
                    message = erc20BalanceRequest.messageToSign,
                    signedMessage = erc20BalanceRequest.signedMessage!!,
                    signer = erc20BalanceRequest.actualWalletAddress!!
                )
            ).willReturn(true)
        }

        val service = Erc20BalanceRequestServiceImpl(
            signatureCheckerService = signatureCheckerService,
            blockchainService = blockchainService,
            erc20BalanceRequestRepository = erc20BalanceRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMockWithCustomRpcUrl(erc20BalanceRequest.projectId, customRpcUrl)
        )

        verify("ERC20 balance request with successful status is returned") {
            val result = service.getErc20BalanceRequest(uuid)

            assertThat(result).withMessage()
                .isEqualTo(
                    FullErc20BalanceRequest(
                        id = uuid,
                        projectId = erc20BalanceRequest.projectId,
                        status = Status.SUCCESS,
                        chainId = erc20BalanceRequest.chainId,
                        redirectUrl = erc20BalanceRequest.redirectUrl,
                        tokenAddress = null,
                        blockNumber = erc20BalanceRequest.blockNumber,
                        requestedWalletAddress = erc20BalanceRequest.requestedWalletAddress,
                        arbitraryData = erc20BalanceRequest.arbitraryData,
                        screenConfig = erc20BalanceRequest.screenConfig,
                        balance = balance,
                        messageToSign = erc20BalanceRequest.messageToSign,
                        signedMessage = erc20BalanceRequest.signedMessage,
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20BalanceRequestWithSuccessfulStatusWhenRequestedWalletAddressIsSpecifiedForNativeToken() {
        val uuid = UUID.randomUUID()
        val erc20BalanceRequest = Erc20BalanceRequest(
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
        val erc20BalanceRequestRepository = mock<Erc20BalanceRequestRepository>()

        suppose("ERC20 balance request is returned from database") {
            given(erc20BalanceRequestRepository.getById(uuid))
                .willReturn(erc20BalanceRequest)
        }

        val customRpcUrl = "custom-rpc-url"
        val balance = AccountBalance(
            wallet = erc20BalanceRequest.actualWalletAddress!!,
            blockNumber = erc20BalanceRequest.blockNumber!!,
            timestamp = UtcDateTime.ofEpochSeconds(0L),
            amount = Balance(BigInteger.ONE)
        )
        val blockchainService = mock<BlockchainService>()

        suppose("blockchain service will return some ERC20 balance") {
            given(
                blockchainService.fetchAccountBalance(
                    chainSpec = ChainSpec(
                        chainId = erc20BalanceRequest.chainId,
                        customRpcUrl = customRpcUrl
                    ),
                    walletAddress = erc20BalanceRequest.actualWalletAddress!!,
                    blockParameter = erc20BalanceRequest.blockNumber!!
                )
            ).willReturn(balance)
        }

        val signatureCheckerService = mock<SignatureCheckerService>()

        suppose("signature checker will return true") {
            given(
                signatureCheckerService.signatureMatches(
                    message = erc20BalanceRequest.messageToSign,
                    signedMessage = erc20BalanceRequest.signedMessage!!,
                    signer = erc20BalanceRequest.actualWalletAddress!!
                )
            ).willReturn(true)
        }

        val service = Erc20BalanceRequestServiceImpl(
            signatureCheckerService = signatureCheckerService,
            blockchainService = blockchainService,
            erc20BalanceRequestRepository = erc20BalanceRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMockWithCustomRpcUrl(erc20BalanceRequest.projectId, customRpcUrl)
        )

        verify("ERC20 balance request with successful status is returned") {
            val result = service.getErc20BalanceRequest(uuid)

            assertThat(result).withMessage()
                .isEqualTo(
                    FullErc20BalanceRequest(
                        id = uuid,
                        projectId = erc20BalanceRequest.projectId,
                        status = Status.SUCCESS,
                        chainId = erc20BalanceRequest.chainId,
                        redirectUrl = erc20BalanceRequest.redirectUrl,
                        tokenAddress = null,
                        blockNumber = erc20BalanceRequest.blockNumber,
                        requestedWalletAddress = erc20BalanceRequest.requestedWalletAddress,
                        arbitraryData = erc20BalanceRequest.arbitraryData,
                        screenConfig = erc20BalanceRequest.screenConfig,
                        balance = balance,
                        messageToSign = erc20BalanceRequest.messageToSign,
                        signedMessage = erc20BalanceRequest.signedMessage,
                        createdAt = erc20BalanceRequest.createdAt
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyReturnListOfErc20BalanceRequestsByProjectId() {
        val uuid = UUID.randomUUID()
        val erc20BalanceRequest = Erc20BalanceRequest(
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
        val erc20BalanceRequestRepository = mock<Erc20BalanceRequestRepository>()

        suppose("ERC20 balance request is returned from database") {
            given(erc20BalanceRequestRepository.getAllByProjectId(erc20BalanceRequest.projectId))
                .willReturn(listOf(erc20BalanceRequest))
        }

        val customRpcUrl = "custom-rpc-url"
        val balance = AccountBalance(
            wallet = erc20BalanceRequest.actualWalletAddress!!,
            blockNumber = erc20BalanceRequest.blockNumber!!,
            timestamp = UtcDateTime.ofEpochSeconds(0L),
            amount = Balance(BigInteger.ONE)
        )
        val blockchainService = mock<BlockchainService>()

        suppose("blockchain service will return some ERC20 balance") {
            given(
                blockchainService.fetchErc20AccountBalance(
                    chainSpec = ChainSpec(
                        chainId = erc20BalanceRequest.chainId,
                        customRpcUrl = customRpcUrl
                    ),
                    contractAddress = erc20BalanceRequest.tokenAddress!!,
                    walletAddress = erc20BalanceRequest.actualWalletAddress!!,
                    blockParameter = erc20BalanceRequest.blockNumber!!
                )
            ).willReturn(balance)
        }

        val signatureCheckerService = mock<SignatureCheckerService>()

        suppose("signature checker will return true") {
            given(
                signatureCheckerService.signatureMatches(
                    message = erc20BalanceRequest.messageToSign,
                    signedMessage = erc20BalanceRequest.signedMessage!!,
                    signer = erc20BalanceRequest.actualWalletAddress!!
                )
            ).willReturn(true)
        }

        val service = Erc20BalanceRequestServiceImpl(
            signatureCheckerService = signatureCheckerService,
            blockchainService = blockchainService,
            erc20BalanceRequestRepository = erc20BalanceRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMockWithCustomRpcUrl(erc20BalanceRequest.projectId, customRpcUrl)
        )

        verify("ERC20 balance request with successful status is returned") {
            val result = service.getErc20BalanceRequestsByProjectId(erc20BalanceRequest.projectId)

            assertThat(result).withMessage()
                .isEqualTo(
                    listOf(
                        FullErc20BalanceRequest(
                            id = uuid,
                            projectId = erc20BalanceRequest.projectId,
                            status = Status.SUCCESS,
                            chainId = erc20BalanceRequest.chainId,
                            redirectUrl = erc20BalanceRequest.redirectUrl,
                            tokenAddress = erc20BalanceRequest.tokenAddress,
                            blockNumber = erc20BalanceRequest.blockNumber,
                            requestedWalletAddress = erc20BalanceRequest.requestedWalletAddress,
                            arbitraryData = erc20BalanceRequest.arbitraryData,
                            screenConfig = erc20BalanceRequest.screenConfig,
                            balance = balance,
                            messageToSign = erc20BalanceRequest.messageToSign,
                            signedMessage = erc20BalanceRequest.signedMessage,
                            createdAt = erc20BalanceRequest.createdAt
                        )
                    )
                )
        }
    }

    @Test
    fun mustAttachWalletAddressAndSignedMessage() {
        val uuid = UUID.randomUUID()
        val walletAddress = WalletAddress("a")
        val signedMessage = SignedMessage("signed-message")
        val erc20BalanceRequestRepository = mock<Erc20BalanceRequestRepository>()

        suppose("signed message will be attached") {
            given(erc20BalanceRequestRepository.setSignedMessage(uuid, walletAddress, signedMessage))
                .willReturn(true)
        }

        val service = Erc20BalanceRequestServiceImpl(
            signatureCheckerService = mock(),
            blockchainService = mock(),
            erc20BalanceRequestRepository = erc20BalanceRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("wallet address and signed message are successfully attached") {
            service.attachWalletAddressAndSignedMessage(uuid, walletAddress, signedMessage)

            verifyMock(erc20BalanceRequestRepository)
                .setSignedMessage(uuid, walletAddress, signedMessage)
            verifyNoMoreInteractions(erc20BalanceRequestRepository)
        }
    }

    @Test
    fun mustThrowCannotAttachSignedMessageExceptionWhenAttachingWalletAddressAndSignedMessageFails() {
        val uuid = UUID.randomUUID()
        val walletAddress = WalletAddress("a")
        val signedMessage = SignedMessage("signed-message")
        val erc20BalanceRequestRepository = mock<Erc20BalanceRequestRepository>()

        suppose("signed message will be attached") {
            given(erc20BalanceRequestRepository.setSignedMessage(uuid, walletAddress, signedMessage))
                .willReturn(false)
        }

        val service = Erc20BalanceRequestServiceImpl(
            signatureCheckerService = mock(),
            blockchainService = mock(),
            erc20BalanceRequestRepository = erc20BalanceRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
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

            verifyMock(erc20BalanceRequestRepository)
                .setSignedMessage(uuid, walletAddress, signedMessage)
            verifyNoMoreInteractions(erc20BalanceRequestRepository)
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
