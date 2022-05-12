package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.blockchain.BlockchainService
import com.ampnet.blockchainapiservice.blockchain.properties.Chain
import com.ampnet.blockchainapiservice.blockchain.properties.ChainSpec
import com.ampnet.blockchainapiservice.blockchain.properties.RpcUrlSpec
import com.ampnet.blockchainapiservice.exception.CannotAttachSignedMessageException
import com.ampnet.blockchainapiservice.exception.IncompleteRequestException
import com.ampnet.blockchainapiservice.exception.NonExistentClientIdException
import com.ampnet.blockchainapiservice.exception.ResourceNotFoundException
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.params.CreateErc20BalanceRequestParams
import com.ampnet.blockchainapiservice.model.params.StoreErc20BalanceRequestParams
import com.ampnet.blockchainapiservice.model.result.ClientInfo
import com.ampnet.blockchainapiservice.model.result.Erc20BalanceRequest
import com.ampnet.blockchainapiservice.model.result.FullErc20BalanceRequest
import com.ampnet.blockchainapiservice.repository.ClientInfoRepository
import com.ampnet.blockchainapiservice.repository.Erc20BalanceRequestRepository
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.BlockNumber
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.Erc20Balance
import com.ampnet.blockchainapiservice.util.SignedMessage
import com.ampnet.blockchainapiservice.util.Status
import com.ampnet.blockchainapiservice.util.UtcDateTime
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import java.math.BigInteger
import java.util.UUID
import org.mockito.kotlin.verify as verifyMock

class Erc20BalanceRequestServiceTest : TestBase() {

    @Test
    fun mustSuccessfullyCreateErc20BalanceRequestWhenClientIdIsProvided() {
        val clientId = "test-client-id"
        val chainId = Chain.MATIC_TESTNET_MUMBAI.id
        val redirectUrl = "redirect-url/\${id}"
        val tokenAddress = ContractAddress("abc")
        val clientInfo = ClientInfo(
            clientId = clientId,
            chainId = chainId,
            sendRedirectUrl = null,
            balanceRedirectUrl = redirectUrl,
            tokenAddress = tokenAddress
        )
        val clientInfoRepository = mock<ClientInfoRepository>()

        suppose("some client info is fetched from database") {
            given(clientInfoRepository.getById(clientId))
                .willReturn(clientInfo)
        }

        val uuid = UUID.randomUUID()
        val uuidProvider = mock<UuidProvider>()

        suppose("some UUID will be returned") {
            given(uuidProvider.getUuid())
                .willReturn(uuid)
        }

        val createParams = CreateErc20BalanceRequestParams(
            clientId = clientId,
            chainId = null,
            redirectUrl = null,
            tokenAddress = null,
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
            chainId = chainId,
            redirectUrl = fullRedirectUrl,
            tokenAddress = tokenAddress,
            blockNumber = createParams.blockNumber,
            requestedWalletAddress = createParams.requestedWalletAddress,
            arbitraryData = createParams.arbitraryData,
            screenConfig = createParams.screenConfig
        )
        val databaseResponse = Erc20BalanceRequest(
            id = uuid,
            chainId = chainId,
            redirectUrl = fullRedirectUrl,
            tokenAddress = tokenAddress,
            blockNumber = createParams.blockNumber,
            requestedWalletAddress = createParams.requestedWalletAddress,
            actualWalletAddress = null,
            signedMessage = null,
            arbitraryData = createParams.arbitraryData,
            screenConfig = createParams.screenConfig
        )
        val erc20BalanceRequestRepository = mock<Erc20BalanceRequestRepository>()

        suppose("ERC20 balance request is stored in database") {
            given(erc20BalanceRequestRepository.store(databaseParams))
                .willReturn(databaseResponse)
        }

        val service = Erc20BalanceRequestServiceImpl(
            uuidProvider = uuidProvider,
            signatureCheckerService = mock(),
            blockchainService = mock(),
            clientInfoRepository = clientInfoRepository,
            erc20BalanceRequestRepository = erc20BalanceRequestRepository
        )

        verify("ERC20 balance request is correctly created") {
            assertThat(service.createErc20BalanceRequest(createParams)).withMessage()
                .isEqualTo(databaseResponse)

            verifyMock(erc20BalanceRequestRepository)
                .store(databaseParams)
            verifyNoMoreInteractions(erc20BalanceRequestRepository)
        }
    }

    @Test
    fun mustSuccessfullyCreateErc20BalanceRequestWhenClientIdIsNotProvided() {
        val uuid = UUID.randomUUID()
        val uuidProvider = mock<UuidProvider>()

        suppose("some UUID will be returned") {
            given(uuidProvider.getUuid())
                .willReturn(uuid)
        }

        val chainId = Chain.MATIC_TESTNET_MUMBAI.id
        val redirectUrl = "redirect-url/\${id}"
        val tokenAddress = ContractAddress("abc")
        val createParams = CreateErc20BalanceRequestParams(
            clientId = null,
            chainId = chainId,
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
            chainId = chainId,
            redirectUrl = fullRedirectUrl,
            tokenAddress = tokenAddress,
            blockNumber = createParams.blockNumber,
            requestedWalletAddress = createParams.requestedWalletAddress,
            arbitraryData = createParams.arbitraryData,
            screenConfig = createParams.screenConfig
        )
        val databaseResponse = Erc20BalanceRequest(
            id = uuid,
            chainId = chainId,
            redirectUrl = fullRedirectUrl,
            tokenAddress = tokenAddress,
            blockNumber = createParams.blockNumber,
            requestedWalletAddress = createParams.requestedWalletAddress,
            actualWalletAddress = null,
            signedMessage = null,
            arbitraryData = createParams.arbitraryData,
            screenConfig = createParams.screenConfig
        )
        val erc20BalanceRequestRepository = mock<Erc20BalanceRequestRepository>()

        suppose("ERC20 balance request is stored in database") {
            given(erc20BalanceRequestRepository.store(databaseParams))
                .willReturn(databaseResponse)
        }

        val service = Erc20BalanceRequestServiceImpl(
            uuidProvider = uuidProvider,
            signatureCheckerService = mock(),
            blockchainService = mock(),
            clientInfoRepository = mock(),
            erc20BalanceRequestRepository = erc20BalanceRequestRepository
        )

        verify("ERC20 balance request is correctly created") {
            assertThat(service.createErc20BalanceRequest(createParams)).withMessage()
                .isEqualTo(databaseResponse)

            verifyMock(erc20BalanceRequestRepository)
                .store(databaseParams)
            verifyNoMoreInteractions(erc20BalanceRequestRepository)
        }
    }

    @Test
    fun mustThrowNonExistentClientIdExceptionWhenClientInfoIsNotInDatabase() {
        val clientId = "test-client-id"
        val clientInfoRepository = mock<ClientInfoRepository>()

        suppose("client info is not in database") {
            given(clientInfoRepository.getById(clientId))
                .willReturn(null)
        }

        val createParams = CreateErc20BalanceRequestParams(
            clientId = clientId,
            chainId = null,
            redirectUrl = null,
            tokenAddress = null,
            blockNumber = BlockNumber(BigInteger.TEN),
            requestedWalletAddress = WalletAddress("def"),
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            )
        )

        val erc20BalanceRequestRepository = mock<Erc20BalanceRequestRepository>()
        val service = Erc20BalanceRequestServiceImpl(
            uuidProvider = mock(),
            signatureCheckerService = mock(),
            blockchainService = mock(),
            clientInfoRepository = clientInfoRepository,
            erc20BalanceRequestRepository = erc20BalanceRequestRepository
        )

        verify("NonExistentClientIdException is thrown") {
            assertThrows<NonExistentClientIdException>(message) {
                service.createErc20BalanceRequest(createParams)
            }

            verifyNoInteractions(erc20BalanceRequestRepository)
        }
    }

    @Test
    fun mustThrowIncompleteRequestExceptionWhenClientIdAndChainIdAreMissing() {
        val uuid = UUID.randomUUID()
        val uuidProvider = mock<UuidProvider>()

        suppose("some UUID will be returned") {
            given(uuidProvider.getUuid())
                .willReturn(uuid)
        }

        val createParams = CreateErc20BalanceRequestParams(
            clientId = null,
            chainId = null,
            redirectUrl = "redirect-url/\${id}",
            tokenAddress = ContractAddress("abc"),
            blockNumber = BlockNumber(BigInteger.TEN),
            requestedWalletAddress = WalletAddress("def"),
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            )
        )

        val erc20BalanceRequestRepository = mock<Erc20BalanceRequestRepository>()
        val service = Erc20BalanceRequestServiceImpl(
            uuidProvider = uuidProvider,
            signatureCheckerService = mock(),
            blockchainService = mock(),
            clientInfoRepository = mock(),
            erc20BalanceRequestRepository = erc20BalanceRequestRepository
        )

        verify("IncompleteRequestException is thrown") {
            assertThrows<IncompleteRequestException>(message) {
                service.createErc20BalanceRequest(createParams)
            }

            verifyNoInteractions(erc20BalanceRequestRepository)
        }
    }

    @Test
    fun mustThrowIncompleteRequestExceptionWhenClientIdAndRedirectUrlAreMissing() {
        val uuid = UUID.randomUUID()
        val uuidProvider = mock<UuidProvider>()

        suppose("some UUID will be returned") {
            given(uuidProvider.getUuid())
                .willReturn(uuid)
        }

        val createParams = CreateErc20BalanceRequestParams(
            clientId = null,
            chainId = Chain.MATIC_TESTNET_MUMBAI.id,
            redirectUrl = null,
            tokenAddress = ContractAddress("abc"),
            blockNumber = BlockNumber(BigInteger.TEN),
            requestedWalletAddress = WalletAddress("def"),
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            )
        )

        val erc20BalanceRequestRepository = mock<Erc20BalanceRequestRepository>()
        val service = Erc20BalanceRequestServiceImpl(
            uuidProvider = uuidProvider,
            signatureCheckerService = mock(),
            blockchainService = mock(),
            clientInfoRepository = mock(),
            erc20BalanceRequestRepository = erc20BalanceRequestRepository
        )

        verify("IncompleteRequestException is thrown") {
            assertThrows<IncompleteRequestException>(message) {
                service.createErc20BalanceRequest(createParams)
            }

            verifyNoInteractions(erc20BalanceRequestRepository)
        }
    }

    @Test
    fun mustThrowIncompleteRequestExceptionWhenClientIdAndTokenAddressAreMissing() {
        val uuid = UUID.randomUUID()
        val uuidProvider = mock<UuidProvider>()

        suppose("some UUID will be returned") {
            given(uuidProvider.getUuid())
                .willReturn(uuid)
        }

        val createParams = CreateErc20BalanceRequestParams(
            clientId = null,
            chainId = Chain.MATIC_TESTNET_MUMBAI.id,
            redirectUrl = "redirect-url/\${id}",
            tokenAddress = null,
            blockNumber = BlockNumber(BigInteger.TEN),
            requestedWalletAddress = WalletAddress("def"),
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            )
        )

        val erc20BalanceRequestRepository = mock<Erc20BalanceRequestRepository>()
        val service = Erc20BalanceRequestServiceImpl(
            uuidProvider = uuidProvider,
            signatureCheckerService = mock(),
            blockchainService = mock(),
            clientInfoRepository = mock(),
            erc20BalanceRequestRepository = erc20BalanceRequestRepository
        )

        verify("IncompleteRequestException is thrown") {
            assertThrows<IncompleteRequestException>(message) {
                service.createErc20BalanceRequest(createParams)
            }

            verifyNoInteractions(erc20BalanceRequestRepository)
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
            uuidProvider = mock(),
            signatureCheckerService = mock(),
            blockchainService = mock(),
            clientInfoRepository = mock(),
            erc20BalanceRequestRepository = erc20BalanceRequestRepository
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.getErc20BalanceRequest(uuid, RpcUrlSpec(null, null))
            }
        }
    }

    @Test
    fun mustReturnErc20BalanceRequestWithPendingStatusWhenActualWalletAddressIsNull() {
        val uuid = UUID.randomUUID()
        val erc20BalanceRequest = Erc20BalanceRequest(
            id = uuid,
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
            )
        )
        val erc20BalanceRequestRepository = mock<Erc20BalanceRequestRepository>()

        suppose("ERC20 balance request is returned from database") {
            given(erc20BalanceRequestRepository.getById(uuid))
                .willReturn(erc20BalanceRequest)
        }

        val service = Erc20BalanceRequestServiceImpl(
            uuidProvider = mock(),
            signatureCheckerService = mock(),
            blockchainService = mock(),
            clientInfoRepository = mock(),
            erc20BalanceRequestRepository = erc20BalanceRequestRepository
        )

        verify("ERC20 balance request with pending status is returned") {
            val result = service.getErc20BalanceRequest(uuid, RpcUrlSpec(null, null))

            assertThat(result).withMessage()
                .isEqualTo(
                    FullErc20BalanceRequest(
                        id = uuid,
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
                        signedMessage = null
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20BalanceRequestWithPendingStatusWhenSignedMessageIsNull() {
        val uuid = UUID.randomUUID()
        val erc20BalanceRequest = Erc20BalanceRequest(
            id = uuid,
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
            )
        )
        val erc20BalanceRequestRepository = mock<Erc20BalanceRequestRepository>()

        suppose("ERC20 balance request is returned from database") {
            given(erc20BalanceRequestRepository.getById(uuid))
                .willReturn(erc20BalanceRequest)
        }

        val rpcSpec = RpcUrlSpec(null, null)
        val balance = Erc20Balance(
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
                        rpcSpec = rpcSpec
                    ),
                    contractAddress = erc20BalanceRequest.tokenAddress,
                    walletAddress = erc20BalanceRequest.actualWalletAddress!!,
                    blockParameter = erc20BalanceRequest.blockNumber!!
                )
            ).willReturn(balance)
        }

        val service = Erc20BalanceRequestServiceImpl(
            uuidProvider = mock(),
            signatureCheckerService = mock(),
            blockchainService = blockchainService,
            clientInfoRepository = mock(),
            erc20BalanceRequestRepository = erc20BalanceRequestRepository
        )

        verify("ERC20 balance request with pending status is returned") {
            val result = service.getErc20BalanceRequest(uuid, rpcSpec)

            assertThat(result).withMessage()
                .isEqualTo(
                    FullErc20BalanceRequest(
                        id = uuid,
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
                        signedMessage = null
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20BalanceRequestWithFailedStatusWhenRequestedAndActualWalletAddressesDontMatch() {
        val uuid = UUID.randomUUID()
        val erc20BalanceRequest = Erc20BalanceRequest(
            id = uuid,
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
            )
        )
        val erc20BalanceRequestRepository = mock<Erc20BalanceRequestRepository>()

        suppose("ERC20 balance request is returned from database") {
            given(erc20BalanceRequestRepository.getById(uuid))
                .willReturn(erc20BalanceRequest)
        }

        val rpcSpec = RpcUrlSpec(null, null)
        val balance = Erc20Balance(
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
                        rpcSpec = rpcSpec
                    ),
                    contractAddress = erc20BalanceRequest.tokenAddress,
                    walletAddress = erc20BalanceRequest.actualWalletAddress!!,
                    blockParameter = erc20BalanceRequest.blockNumber!!
                )
            ).willReturn(balance)
        }

        val service = Erc20BalanceRequestServiceImpl(
            uuidProvider = mock(),
            signatureCheckerService = mock(),
            blockchainService = blockchainService,
            clientInfoRepository = mock(),
            erc20BalanceRequestRepository = erc20BalanceRequestRepository
        )

        verify("ERC20 balance request with failed status is returned") {
            val result = service.getErc20BalanceRequest(uuid, rpcSpec)

            assertThat(result).withMessage()
                .isEqualTo(
                    FullErc20BalanceRequest(
                        id = uuid,
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
                        signedMessage = erc20BalanceRequest.signedMessage
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20BalanceRequestWithFailedStatusWhenSignatureDoesntMatch() {
        val uuid = UUID.randomUUID()
        val erc20BalanceRequest = Erc20BalanceRequest(
            id = uuid,
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
            )
        )
        val erc20BalanceRequestRepository = mock<Erc20BalanceRequestRepository>()

        suppose("ERC20 balance request is returned from database") {
            given(erc20BalanceRequestRepository.getById(uuid))
                .willReturn(erc20BalanceRequest)
        }

        val rpcSpec = RpcUrlSpec(null, null)
        val balance = Erc20Balance(
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
                        rpcSpec = rpcSpec
                    ),
                    contractAddress = erc20BalanceRequest.tokenAddress,
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
            uuidProvider = mock(),
            signatureCheckerService = signatureCheckerService,
            blockchainService = blockchainService,
            clientInfoRepository = mock(),
            erc20BalanceRequestRepository = erc20BalanceRequestRepository
        )

        verify("ERC20 balance request with failed status is returned") {
            val result = service.getErc20BalanceRequest(uuid, rpcSpec)

            assertThat(result).withMessage()
                .isEqualTo(
                    FullErc20BalanceRequest(
                        id = uuid,
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
                        signedMessage = erc20BalanceRequest.signedMessage
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20BalanceRequestWithSuccessfulStatusWhenRequestedWalletAddressIsNull() {
        val uuid = UUID.randomUUID()
        val erc20BalanceRequest = Erc20BalanceRequest(
            id = uuid,
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
            )
        )
        val erc20BalanceRequestRepository = mock<Erc20BalanceRequestRepository>()

        suppose("ERC20 balance request is returned from database") {
            given(erc20BalanceRequestRepository.getById(uuid))
                .willReturn(erc20BalanceRequest)
        }

        val rpcSpec = RpcUrlSpec(null, null)
        val balance = Erc20Balance(
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
                        rpcSpec = rpcSpec
                    ),
                    contractAddress = erc20BalanceRequest.tokenAddress,
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
            uuidProvider = mock(),
            signatureCheckerService = signatureCheckerService,
            blockchainService = blockchainService,
            clientInfoRepository = mock(),
            erc20BalanceRequestRepository = erc20BalanceRequestRepository
        )

        verify("ERC20 balance request with successful status is returned") {
            val result = service.getErc20BalanceRequest(uuid, rpcSpec)

            assertThat(result).withMessage()
                .isEqualTo(
                    FullErc20BalanceRequest(
                        id = uuid,
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
                        signedMessage = erc20BalanceRequest.signedMessage
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20BalanceRequestWithSuccessfulStatusWhenRequestedWalletAddressIsSpecified() {
        val uuid = UUID.randomUUID()
        val erc20BalanceRequest = Erc20BalanceRequest(
            id = uuid,
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
            )
        )
        val erc20BalanceRequestRepository = mock<Erc20BalanceRequestRepository>()

        suppose("ERC20 balance request is returned from database") {
            given(erc20BalanceRequestRepository.getById(uuid))
                .willReturn(erc20BalanceRequest)
        }

        val rpcSpec = RpcUrlSpec(null, null)
        val balance = Erc20Balance(
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
                        rpcSpec = rpcSpec
                    ),
                    contractAddress = erc20BalanceRequest.tokenAddress,
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
            uuidProvider = mock(),
            signatureCheckerService = signatureCheckerService,
            blockchainService = blockchainService,
            clientInfoRepository = mock(),
            erc20BalanceRequestRepository = erc20BalanceRequestRepository
        )

        verify("ERC20 balance request with successful status is returned") {
            val result = service.getErc20BalanceRequest(uuid, rpcSpec)

            assertThat(result).withMessage()
                .isEqualTo(
                    FullErc20BalanceRequest(
                        id = uuid,
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
                        signedMessage = erc20BalanceRequest.signedMessage
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
            uuidProvider = mock(),
            signatureCheckerService = mock(),
            blockchainService = mock(),
            clientInfoRepository = mock(),
            erc20BalanceRequestRepository = erc20BalanceRequestRepository
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
            uuidProvider = mock(),
            signatureCheckerService = mock(),
            blockchainService = mock(),
            clientInfoRepository = mock(),
            erc20BalanceRequestRepository = erc20BalanceRequestRepository
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
}
