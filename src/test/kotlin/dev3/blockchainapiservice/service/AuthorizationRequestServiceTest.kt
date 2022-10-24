package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.exception.CannotAttachSignedMessageException
import com.ampnet.blockchainapiservice.exception.ResourceNotFoundException
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.params.CreateAuthorizationRequestParams
import com.ampnet.blockchainapiservice.model.params.StoreAuthorizationRequestParams
import com.ampnet.blockchainapiservice.model.result.AuthorizationRequest
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.repository.AuthorizationRequestRepository
import com.ampnet.blockchainapiservice.util.BaseUrl
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.SignedMessage
import com.ampnet.blockchainapiservice.util.Status
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.ampnet.blockchainapiservice.util.WithStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoMoreInteractions
import java.util.UUID
import org.mockito.kotlin.verify as verifyMock

class AuthorizationRequestServiceTest : TestBase() {

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
    }

    @Test
    fun mustSuccessfullyCreateAuthorizationRequest() {
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

        val redirectUrl = "redirect-url/\${id}"
        val createParams = CreateAuthorizationRequestParams(
            redirectUrl = redirectUrl,
            messageToSign = "message-to-sign-override",
            storeIndefinitely = true,
            requestedWalletAddress = WalletAddress("def"),
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            )
        )
        val fullRedirectUrl = redirectUrl.replace("\${id}", uuid.toString())
        val databaseParams = StoreAuthorizationRequestParams(
            id = uuid,
            projectId = PROJECT.id,
            redirectUrl = fullRedirectUrl,
            messageToSignOverride = createParams.messageToSign,
            storeIndefinitely = createParams.storeIndefinitely,
            requestedWalletAddress = createParams.requestedWalletAddress,
            arbitraryData = createParams.arbitraryData,
            screenConfig = createParams.screenConfig,
            createdAt = TestData.TIMESTAMP
        )
        val databaseResponse = AuthorizationRequest(
            id = uuid,
            projectId = PROJECT.id,
            redirectUrl = fullRedirectUrl,
            messageToSignOverride = createParams.messageToSign,
            storeIndefinitely = createParams.storeIndefinitely,
            requestedWalletAddress = createParams.requestedWalletAddress,
            actualWalletAddress = null,
            signedMessage = null,
            arbitraryData = createParams.arbitraryData,
            screenConfig = createParams.screenConfig,
            createdAt = TestData.TIMESTAMP
        )
        val authorizationRequestRepository = mock<AuthorizationRequestRepository>()

        suppose("authorization request is stored in database") {
            given(authorizationRequestRepository.store(databaseParams))
                .willReturn(databaseResponse)
        }

        val service = AuthorizationRequestServiceImpl(
            signatureCheckerService = mock(),
            authorizationRequestRepository = authorizationRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = uuidProvider,
                utcDateTimeProvider = utcDateTimeProvider,
                blockchainService = mock()
            )
        )

        verify("authorization request is correctly created") {
            assertThat(service.createAuthorizationRequest(createParams, PROJECT)).withMessage()
                .isEqualTo(databaseResponse)

            verifyMock(authorizationRequestRepository)
                .store(databaseParams)
            verifyNoMoreInteractions(authorizationRequestRepository)
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionForNonExistentAuthorizationRequest() {
        val uuid = UUID.randomUUID()
        val authorizationRequestRepository = mock<AuthorizationRequestRepository>()

        suppose("authorization request is not in database") {
            given(authorizationRequestRepository.getById(uuid))
                .willReturn(null)
        }

        val service = AuthorizationRequestServiceImpl(
            signatureCheckerService = mock(),
            authorizationRequestRepository = authorizationRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            )
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.getAuthorizationRequest(uuid)
            }
        }
    }

    @Test
    fun mustReturnAuthorizationRequestWithPendingStatusWhenActualWalletAddressIsNull() {
        val uuid = UUID.randomUUID()
        val authorizationRequest = AuthorizationRequest(
            id = uuid,
            projectId = UUID.randomUUID(),
            redirectUrl = "redirect-url/$uuid",
            messageToSignOverride = "message-to-sign-override",
            storeIndefinitely = true,
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
        val authorizationRequestRepository = mock<AuthorizationRequestRepository>()

        suppose("authorization request is returned from database") {
            given(authorizationRequestRepository.getById(uuid))
                .willReturn(authorizationRequest)
        }

        val service = AuthorizationRequestServiceImpl(
            signatureCheckerService = mock(),
            authorizationRequestRepository = authorizationRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            )
        )

        verify("authorization request with pending status is returned") {
            val result = service.getAuthorizationRequest(uuid)

            assertThat(result).withMessage()
                .isEqualTo(
                    WithStatus(
                        value = AuthorizationRequest(
                            id = uuid,
                            projectId = authorizationRequest.projectId,
                            redirectUrl = authorizationRequest.redirectUrl,
                            messageToSignOverride = authorizationRequest.messageToSignOverride,
                            storeIndefinitely = authorizationRequest.storeIndefinitely,
                            requestedWalletAddress = authorizationRequest.requestedWalletAddress,
                            actualWalletAddress = authorizationRequest.actualWalletAddress,
                            arbitraryData = authorizationRequest.arbitraryData,
                            screenConfig = authorizationRequest.screenConfig,
                            signedMessage = null,
                            createdAt = TestData.TIMESTAMP
                        ),
                        status = Status.PENDING
                    )
                )

            verifyMock(authorizationRequestRepository)
                .getById(uuid)
            verifyNoMoreInteractions(authorizationRequestRepository)
        }
    }

    @Test
    fun mustReturnAuthorizationRequestWithPendingStatusWhenSignedMessageIsNull() {
        val uuid = UUID.randomUUID()
        val authorizationRequest = AuthorizationRequest(
            id = uuid,
            projectId = UUID.randomUUID(),
            redirectUrl = "redirect-url/$uuid",
            messageToSignOverride = "message-to-sign-override",
            storeIndefinitely = true,
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
        val authorizationRequestRepository = mock<AuthorizationRequestRepository>()

        suppose("authorization request is returned from database") {
            given(authorizationRequestRepository.getById(uuid))
                .willReturn(authorizationRequest)
        }

        val service = AuthorizationRequestServiceImpl(
            signatureCheckerService = mock(),
            authorizationRequestRepository = authorizationRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            )
        )

        verify("authorization request with pending status is returned") {
            val result = service.getAuthorizationRequest(uuid)

            assertThat(result).withMessage()
                .isEqualTo(
                    WithStatus(
                        value =
                        AuthorizationRequest(
                            id = uuid,
                            projectId = authorizationRequest.projectId,
                            redirectUrl = authorizationRequest.redirectUrl,
                            messageToSignOverride = authorizationRequest.messageToSignOverride,
                            storeIndefinitely = authorizationRequest.storeIndefinitely,
                            requestedWalletAddress = authorizationRequest.requestedWalletAddress,
                            actualWalletAddress = authorizationRequest.actualWalletAddress,
                            arbitraryData = authorizationRequest.arbitraryData,
                            screenConfig = authorizationRequest.screenConfig,
                            signedMessage = null,
                            createdAt = TestData.TIMESTAMP
                        ),
                        status = Status.PENDING,
                    )
                )

            verifyMock(authorizationRequestRepository)
                .getById(uuid)
            verifyNoMoreInteractions(authorizationRequestRepository)
        }
    }

    @Test
    fun mustReturnAuthorizationRequestWithFailedStatusWhenRequestedAndActualWalletAddressesDontMatch() {
        val uuid = UUID.randomUUID()
        val authorizationRequest = AuthorizationRequest(
            id = uuid,
            projectId = UUID.randomUUID(),
            redirectUrl = "redirect-url/$uuid",
            messageToSignOverride = "message-to-sign-override",
            storeIndefinitely = true,
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
        val authorizationRequestRepository = mock<AuthorizationRequestRepository>()

        suppose("authorization request is returned from database") {
            given(authorizationRequestRepository.getById(uuid))
                .willReturn(authorizationRequest)
        }

        val service = AuthorizationRequestServiceImpl(
            signatureCheckerService = mock(),
            authorizationRequestRepository = authorizationRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            )
        )

        verify("authorization request with failed status is returned") {
            val result = service.getAuthorizationRequest(uuid)

            assertThat(result).withMessage()
                .isEqualTo(
                    WithStatus(
                        value =
                        AuthorizationRequest(
                            id = uuid,
                            projectId = authorizationRequest.projectId,
                            redirectUrl = authorizationRequest.redirectUrl,
                            messageToSignOverride = authorizationRequest.messageToSignOverride,
                            storeIndefinitely = authorizationRequest.storeIndefinitely,
                            requestedWalletAddress = authorizationRequest.requestedWalletAddress,
                            actualWalletAddress = authorizationRequest.actualWalletAddress,
                            arbitraryData = authorizationRequest.arbitraryData,
                            screenConfig = authorizationRequest.screenConfig,
                            signedMessage = authorizationRequest.signedMessage,
                            createdAt = TestData.TIMESTAMP
                        ),
                        status = Status.FAILED
                    )
                )

            verifyMock(authorizationRequestRepository)
                .getById(uuid)
            verifyNoMoreInteractions(authorizationRequestRepository)
        }
    }

    @Test
    fun mustReturnAuthorizationRequestWithFailedStatusWhenSignatureDoesntMatch() {
        val uuid = UUID.randomUUID()
        val authorizationRequest = AuthorizationRequest(
            id = uuid,
            projectId = UUID.randomUUID(),
            redirectUrl = "redirect-url/$uuid",
            messageToSignOverride = "message-to-sign-override",
            storeIndefinitely = true,
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
        val authorizationRequestRepository = mock<AuthorizationRequestRepository>()

        suppose("authorization request is returned from database") {
            given(authorizationRequestRepository.getById(uuid))
                .willReturn(authorizationRequest)
        }

        val signatureCheckerService = mock<SignatureCheckerService>()

        suppose("signature checker will return false") {
            given(
                signatureCheckerService.signatureMatches(
                    message = authorizationRequest.messageToSign,
                    signedMessage = authorizationRequest.signedMessage!!,
                    signer = authorizationRequest.actualWalletAddress!!
                )
            ).willReturn(false)
        }

        val service = AuthorizationRequestServiceImpl(
            signatureCheckerService = signatureCheckerService,
            authorizationRequestRepository = authorizationRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            )
        )

        verify("authorization request with failed status is returned") {
            val result = service.getAuthorizationRequest(uuid)

            assertThat(result).withMessage()
                .isEqualTo(
                    WithStatus(
                        value = AuthorizationRequest(
                            id = uuid,
                            projectId = authorizationRequest.projectId,
                            redirectUrl = authorizationRequest.redirectUrl,
                            messageToSignOverride = authorizationRequest.messageToSignOverride,
                            storeIndefinitely = authorizationRequest.storeIndefinitely,
                            requestedWalletAddress = authorizationRequest.requestedWalletAddress,
                            actualWalletAddress = authorizationRequest.actualWalletAddress,
                            arbitraryData = authorizationRequest.arbitraryData,
                            screenConfig = authorizationRequest.screenConfig,
                            signedMessage = authorizationRequest.signedMessage,
                            createdAt = TestData.TIMESTAMP
                        ),
                        status = Status.FAILED
                    )
                )

            verifyMock(authorizationRequestRepository)
                .getById(uuid)
            verifyNoMoreInteractions(authorizationRequestRepository)
        }
    }

    @Test
    fun mustReturnAuthorizationRequestWithSuccessfulStatusWhenRequestedWalletAddressIsNull() {
        val uuid = UUID.randomUUID()
        val authorizationRequest = AuthorizationRequest(
            id = uuid,
            projectId = UUID.randomUUID(),
            redirectUrl = "redirect-url/$uuid",
            messageToSignOverride = "message-to-sign-override",
            storeIndefinitely = true,
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
        val authorizationRequestRepository = mock<AuthorizationRequestRepository>()

        suppose("authorization request is returned from database") {
            given(authorizationRequestRepository.getById(uuid))
                .willReturn(authorizationRequest)
        }

        val signatureCheckerService = mock<SignatureCheckerService>()

        suppose("signature checker will return true") {
            given(
                signatureCheckerService.signatureMatches(
                    message = authorizationRequest.messageToSign,
                    signedMessage = authorizationRequest.signedMessage!!,
                    signer = authorizationRequest.actualWalletAddress!!
                )
            ).willReturn(true)
        }

        val service = AuthorizationRequestServiceImpl(
            signatureCheckerService = signatureCheckerService,
            authorizationRequestRepository = authorizationRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            )
        )

        verify("authorization request with successful status is returned") {
            val result = service.getAuthorizationRequest(uuid)

            assertThat(result).withMessage()
                .isEqualTo(
                    WithStatus(
                        value = AuthorizationRequest(
                            id = uuid,
                            projectId = authorizationRequest.projectId,
                            redirectUrl = authorizationRequest.redirectUrl,
                            messageToSignOverride = authorizationRequest.messageToSignOverride,
                            storeIndefinitely = authorizationRequest.storeIndefinitely,
                            requestedWalletAddress = authorizationRequest.requestedWalletAddress,
                            actualWalletAddress = authorizationRequest.actualWalletAddress,
                            arbitraryData = authorizationRequest.arbitraryData,
                            screenConfig = authorizationRequest.screenConfig,
                            signedMessage = authorizationRequest.signedMessage,
                            createdAt = TestData.TIMESTAMP
                        ),
                        status = Status.SUCCESS
                    )
                )

            verifyMock(authorizationRequestRepository)
                .getById(uuid)
            verifyNoMoreInteractions(authorizationRequestRepository)
        }
    }

    @Test
    fun mustReturnAuthorizationRequestWithSuccessfulStatusWhenRequestedWalletAddressIsSpecified() {
        val uuid = UUID.randomUUID()
        val authorizationRequest = AuthorizationRequest(
            id = uuid,
            projectId = UUID.randomUUID(),
            redirectUrl = "redirect-url/$uuid",
            messageToSignOverride = "message-to-sign-override",
            storeIndefinitely = true,
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
        val authorizationRequestRepository = mock<AuthorizationRequestRepository>()

        suppose("authorization request is returned from database") {
            given(authorizationRequestRepository.getById(uuid))
                .willReturn(authorizationRequest)
        }

        val signatureCheckerService = mock<SignatureCheckerService>()

        suppose("signature checker will return true") {
            given(
                signatureCheckerService.signatureMatches(
                    message = authorizationRequest.messageToSign,
                    signedMessage = authorizationRequest.signedMessage!!,
                    signer = authorizationRequest.actualWalletAddress!!
                )
            ).willReturn(true)
        }

        val service = AuthorizationRequestServiceImpl(
            signatureCheckerService = signatureCheckerService,
            authorizationRequestRepository = authorizationRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            )
        )

        verify("authorization request with successful status is returned") {
            val result = service.getAuthorizationRequest(uuid)

            assertThat(result).withMessage()
                .isEqualTo(
                    WithStatus(
                        value = AuthorizationRequest(
                            id = uuid,
                            projectId = authorizationRequest.projectId,
                            redirectUrl = authorizationRequest.redirectUrl,
                            messageToSignOverride = authorizationRequest.messageToSignOverride,
                            storeIndefinitely = authorizationRequest.storeIndefinitely,
                            requestedWalletAddress = authorizationRequest.requestedWalletAddress,
                            actualWalletAddress = authorizationRequest.actualWalletAddress,
                            arbitraryData = authorizationRequest.arbitraryData,
                            screenConfig = authorizationRequest.screenConfig,
                            signedMessage = authorizationRequest.signedMessage,
                            createdAt = authorizationRequest.createdAt
                        ),
                        status = Status.SUCCESS
                    )
                )

            verifyMock(authorizationRequestRepository)
                .getById(uuid)
            verifyNoMoreInteractions(authorizationRequestRepository)
        }
    }

    @Test
    fun mustReturnAuthorizationRequestWithSuccessfulStatusAndDeleteItIfItsNotStoredIndefinitely() {
        val uuid = UUID.randomUUID()
        val authorizationRequest = AuthorizationRequest(
            id = uuid,
            projectId = UUID.randomUUID(),
            redirectUrl = "redirect-url/$uuid",
            messageToSignOverride = "message-to-sign-override",
            storeIndefinitely = false,
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
        val authorizationRequestRepository = mock<AuthorizationRequestRepository>()

        suppose("authorization request is returned from database") {
            given(authorizationRequestRepository.getById(uuid))
                .willReturn(authorizationRequest)
        }

        val signatureCheckerService = mock<SignatureCheckerService>()

        suppose("signature checker will return true") {
            given(
                signatureCheckerService.signatureMatches(
                    message = authorizationRequest.messageToSign,
                    signedMessage = authorizationRequest.signedMessage!!,
                    signer = authorizationRequest.actualWalletAddress!!
                )
            ).willReturn(true)
        }

        val service = AuthorizationRequestServiceImpl(
            signatureCheckerService = signatureCheckerService,
            authorizationRequestRepository = authorizationRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            )
        )

        verify("authorization request with successful status is returned") {
            val result = service.getAuthorizationRequest(uuid)

            assertThat(result).withMessage()
                .isEqualTo(
                    WithStatus(
                        value = AuthorizationRequest(
                            id = uuid,
                            projectId = authorizationRequest.projectId,
                            redirectUrl = authorizationRequest.redirectUrl,
                            messageToSignOverride = authorizationRequest.messageToSignOverride,
                            storeIndefinitely = authorizationRequest.storeIndefinitely,
                            requestedWalletAddress = authorizationRequest.requestedWalletAddress,
                            actualWalletAddress = authorizationRequest.actualWalletAddress,
                            arbitraryData = authorizationRequest.arbitraryData,
                            screenConfig = authorizationRequest.screenConfig,
                            signedMessage = authorizationRequest.signedMessage,
                            createdAt = authorizationRequest.createdAt
                        ),
                        status = Status.SUCCESS
                    )
                )

            verifyMock(authorizationRequestRepository)
                .getById(uuid)
            verifyMock(authorizationRequestRepository)
                .delete(uuid)
            verifyNoMoreInteractions(authorizationRequestRepository)
        }
    }

    @Test
    fun mustCorrectlyReturnListOfAuthorizationRequestsByProjectId() {
        val uuid = UUID.randomUUID()
        val authorizationRequest = AuthorizationRequest(
            id = uuid,
            projectId = UUID.randomUUID(),
            redirectUrl = "redirect-url/$uuid",
            messageToSignOverride = "message-to-sign-override",
            storeIndefinitely = true,
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
        val authorizationRequestRepository = mock<AuthorizationRequestRepository>()

        suppose("authorization request is returned from database") {
            given(authorizationRequestRepository.getAllByProjectId(authorizationRequest.projectId))
                .willReturn(listOf(authorizationRequest))
        }

        val signatureCheckerService = mock<SignatureCheckerService>()

        suppose("signature checker will return true") {
            given(
                signatureCheckerService.signatureMatches(
                    message = authorizationRequest.messageToSign,
                    signedMessage = authorizationRequest.signedMessage!!,
                    signer = authorizationRequest.actualWalletAddress!!
                )
            ).willReturn(true)
        }

        val service = AuthorizationRequestServiceImpl(
            signatureCheckerService = signatureCheckerService,
            authorizationRequestRepository = authorizationRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            )
        )

        verify("authorization request with successful status is returned") {
            val result = service.getAuthorizationRequestsByProjectId(authorizationRequest.projectId)

            assertThat(result).withMessage()
                .isEqualTo(
                    listOf(
                        WithStatus(
                            value = AuthorizationRequest(
                                id = uuid,
                                projectId = authorizationRequest.projectId,
                                redirectUrl = authorizationRequest.redirectUrl,
                                messageToSignOverride = authorizationRequest.messageToSignOverride,
                                storeIndefinitely = authorizationRequest.storeIndefinitely,
                                requestedWalletAddress = authorizationRequest.requestedWalletAddress,
                                actualWalletAddress = authorizationRequest.actualWalletAddress,
                                arbitraryData = authorizationRequest.arbitraryData,
                                screenConfig = authorizationRequest.screenConfig,
                                signedMessage = authorizationRequest.signedMessage,
                                createdAt = authorizationRequest.createdAt
                            ),
                            status = Status.SUCCESS
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
        val authorizationRequestRepository = mock<AuthorizationRequestRepository>()

        suppose("signed message will be attached") {
            given(authorizationRequestRepository.setSignedMessage(uuid, walletAddress, signedMessage))
                .willReturn(true)
        }

        val service = AuthorizationRequestServiceImpl(
            signatureCheckerService = mock(),
            authorizationRequestRepository = authorizationRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            )
        )

        verify("wallet address and signed message are successfully attached") {
            service.attachWalletAddressAndSignedMessage(uuid, walletAddress, signedMessage)

            verifyMock(authorizationRequestRepository)
                .setSignedMessage(uuid, walletAddress, signedMessage)
            verifyNoMoreInteractions(authorizationRequestRepository)
        }
    }

    @Test
    fun mustThrowCannotAttachSignedMessageExceptionWhenAttachingWalletAddressAndSignedMessageFails() {
        val uuid = UUID.randomUUID()
        val walletAddress = WalletAddress("a")
        val signedMessage = SignedMessage("signed-message")
        val authorizationRequestRepository = mock<AuthorizationRequestRepository>()

        suppose("signed message will be attached") {
            given(authorizationRequestRepository.setSignedMessage(uuid, walletAddress, signedMessage))
                .willReturn(false)
        }

        val service = AuthorizationRequestServiceImpl(
            signatureCheckerService = mock(),
            authorizationRequestRepository = authorizationRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            )
        )

        verify("CannotAttachSignedMessageException is thrown") {
            assertThrows<CannotAttachSignedMessageException>(message) {
                service.attachWalletAddressAndSignedMessage(uuid, walletAddress, signedMessage)
            }

            verifyMock(authorizationRequestRepository)
                .setSignedMessage(uuid, walletAddress, signedMessage)
            verifyNoMoreInteractions(authorizationRequestRepository)
        }
    }
}
