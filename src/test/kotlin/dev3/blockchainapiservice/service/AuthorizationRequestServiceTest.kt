package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.exception.CannotAttachSignedMessageException
import dev3.blockchainapiservice.exception.ResourceNotFoundException
import dev3.blockchainapiservice.features.api.access.model.result.Project
import dev3.blockchainapiservice.features.wallet.authorization.model.params.CreateAuthorizationRequestParams
import dev3.blockchainapiservice.features.wallet.authorization.model.params.StoreAuthorizationRequestParams
import dev3.blockchainapiservice.features.wallet.authorization.model.result.AuthorizationRequest
import dev3.blockchainapiservice.features.wallet.authorization.repository.AuthorizationRequestRepository
import dev3.blockchainapiservice.features.wallet.authorization.service.AuthorizationRequestServiceImpl
import dev3.blockchainapiservice.features.wallet.authorization.service.SignatureCheckerService
import dev3.blockchainapiservice.generated.jooq.id.AuthorizationRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.generated.jooq.id.UserId
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.util.BaseUrl
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.SignedMessage
import dev3.blockchainapiservice.util.Status
import dev3.blockchainapiservice.util.WalletAddress
import dev3.blockchainapiservice.util.WithStatus
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.util.UUID

class AuthorizationRequestServiceTest : TestBase() {

    companion object {
        private val PROJECT = Project(
            id = ProjectId(UUID.randomUUID()),
            ownerId = UserId(UUID.randomUUID()),
            issuerContractAddress = ContractAddress("a"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = ChainId(1337L),
            customRpcUrl = "custom-rpc-url",
            createdAt = TestData.TIMESTAMP
        )
    }

    @Test
    fun mustSuccessfullyCreateAuthorizationRequest() {
        val uuid = AuthorizationRequestId(UUID.randomUUID())
        val uuidProvider = mock<UuidProvider>()

        suppose("some UUID will be returned") {
            call(uuidProvider.getRawUuid())
                .willReturn(uuid.value)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some timestamp will be returned") {
            call(utcDateTimeProvider.getUtcDateTime())
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
        val fullRedirectUrl = redirectUrl.replace("\${id}", uuid.value.toString())
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
            call(authorizationRequestRepository.store(databaseParams))
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
            expectThat(service.createAuthorizationRequest(createParams, PROJECT))
                .isEqualTo(databaseResponse)

            expectInteractions(authorizationRequestRepository) {
                once.store(databaseParams)
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionForNonExistentAuthorizationRequest() {
        val uuid = AuthorizationRequestId(UUID.randomUUID())
        val authorizationRequestRepository = mock<AuthorizationRequestRepository>()

        suppose("authorization request is not in database") {
            call(authorizationRequestRepository.getById(uuid))
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
            expectThrows<ResourceNotFoundException> {
                service.getAuthorizationRequest(uuid)
            }
        }
    }

    @Test
    fun mustReturnAuthorizationRequestWithPendingStatusWhenActualWalletAddressIsNull() {
        val uuid = AuthorizationRequestId(UUID.randomUUID())
        val authorizationRequest = AuthorizationRequest(
            id = uuid,
            projectId = ProjectId(UUID.randomUUID()),
            redirectUrl = "redirect-url/${uuid.value}",
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
            call(authorizationRequestRepository.getById(uuid))
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

            expectThat(result)
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

            expectInteractions(authorizationRequestRepository) {
                once.getById(uuid)
            }
        }
    }

    @Test
    fun mustReturnAuthorizationRequestWithPendingStatusWhenSignedMessageIsNull() {
        val uuid = AuthorizationRequestId(UUID.randomUUID())
        val authorizationRequest = AuthorizationRequest(
            id = uuid,
            projectId = ProjectId(UUID.randomUUID()),
            redirectUrl = "redirect-url/${uuid.value}",
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
            call(authorizationRequestRepository.getById(uuid))
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

            expectThat(result)
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

            expectInteractions(authorizationRequestRepository) {
                once.getById(uuid)
            }
        }
    }

    @Test
    fun mustReturnAuthorizationRequestWithFailedStatusWhenRequestedAndActualWalletAddressesDontMatch() {
        val uuid = AuthorizationRequestId(UUID.randomUUID())
        val authorizationRequest = AuthorizationRequest(
            id = uuid,
            projectId = ProjectId(UUID.randomUUID()),
            redirectUrl = "redirect-url/${uuid.value}",
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
            call(authorizationRequestRepository.getById(uuid))
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

            expectThat(result)
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

            expectInteractions(authorizationRequestRepository) {
                once.getById(uuid)
            }
        }
    }

    @Test
    fun mustReturnAuthorizationRequestWithFailedStatusWhenSignatureDoesntMatch() {
        val uuid = AuthorizationRequestId(UUID.randomUUID())
        val authorizationRequest = AuthorizationRequest(
            id = uuid,
            projectId = ProjectId(UUID.randomUUID()),
            redirectUrl = "redirect-url/${uuid.value}",
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
            call(authorizationRequestRepository.getById(uuid))
                .willReturn(authorizationRequest)
        }

        val signatureCheckerService = mock<SignatureCheckerService>()

        suppose("signature checker will return false") {
            call(
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

            expectThat(result)
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

            expectInteractions(authorizationRequestRepository) {
                once.getById(uuid)
            }
        }
    }

    @Test
    fun mustReturnAuthorizationRequestWithSuccessfulStatusWhenRequestedWalletAddressIsNull() {
        val uuid = AuthorizationRequestId(UUID.randomUUID())
        val authorizationRequest = AuthorizationRequest(
            id = uuid,
            projectId = ProjectId(UUID.randomUUID()),
            redirectUrl = "redirect-url/${uuid.value}",
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
            call(authorizationRequestRepository.getById(uuid))
                .willReturn(authorizationRequest)
        }

        val signatureCheckerService = mock<SignatureCheckerService>()

        suppose("signature checker will return true") {
            call(
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

            expectThat(result)
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

            expectInteractions(authorizationRequestRepository) {
                once.getById(uuid)
            }
        }
    }

    @Test
    fun mustReturnAuthorizationRequestWithSuccessfulStatusWhenRequestedWalletAddressIsSpecified() {
        val uuid = AuthorizationRequestId(UUID.randomUUID())
        val authorizationRequest = AuthorizationRequest(
            id = uuid,
            projectId = ProjectId(UUID.randomUUID()),
            redirectUrl = "redirect-url/${uuid.value}",
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
            call(authorizationRequestRepository.getById(uuid))
                .willReturn(authorizationRequest)
        }

        val signatureCheckerService = mock<SignatureCheckerService>()

        suppose("signature checker will return true") {
            call(
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

            expectThat(result)
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

            expectInteractions(authorizationRequestRepository) {
                once.getById(uuid)
            }
        }
    }

    @Test
    fun mustReturnAuthorizationRequestWithSuccessfulStatusAndDeleteItIfItsNotStoredIndefinitely() {
        val uuid = AuthorizationRequestId(UUID.randomUUID())
        val authorizationRequest = AuthorizationRequest(
            id = uuid,
            projectId = ProjectId(UUID.randomUUID()),
            redirectUrl = "redirect-url/${uuid.value}",
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
            call(authorizationRequestRepository.getById(uuid))
                .willReturn(authorizationRequest)
        }

        val signatureCheckerService = mock<SignatureCheckerService>()

        suppose("signature checker will return true") {
            call(
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

            expectThat(result)
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

            expectInteractions(authorizationRequestRepository) {
                once.getById(uuid)
                once.delete(uuid)
            }
        }
    }

    @Test
    fun mustCorrectlyReturnListOfAuthorizationRequestsByProjectId() {
        val uuid = AuthorizationRequestId(UUID.randomUUID())
        val authorizationRequest = AuthorizationRequest(
            id = uuid,
            projectId = ProjectId(UUID.randomUUID()),
            redirectUrl = "redirect-url/${uuid.value}",
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
            call(authorizationRequestRepository.getAllByProjectId(authorizationRequest.projectId))
                .willReturn(listOf(authorizationRequest))
        }

        val signatureCheckerService = mock<SignatureCheckerService>()

        suppose("signature checker will return true") {
            call(
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

            expectThat(result)
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
        val uuid = AuthorizationRequestId(UUID.randomUUID())
        val walletAddress = WalletAddress("a")
        val signedMessage = SignedMessage("signed-message")
        val authorizationRequestRepository = mock<AuthorizationRequestRepository>()

        suppose("signed message will be attached") {
            call(authorizationRequestRepository.setSignedMessage(uuid, walletAddress, signedMessage))
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

            expectInteractions(authorizationRequestRepository) {
                once.setSignedMessage(uuid, walletAddress, signedMessage)
            }
        }
    }

    @Test
    fun mustThrowCannotAttachSignedMessageExceptionWhenAttachingWalletAddressAndSignedMessageFails() {
        val uuid = AuthorizationRequestId(UUID.randomUUID())
        val walletAddress = WalletAddress("a")
        val signedMessage = SignedMessage("signed-message")
        val authorizationRequestRepository = mock<AuthorizationRequestRepository>()

        suppose("signed message will be attached") {
            call(authorizationRequestRepository.setSignedMessage(uuid, walletAddress, signedMessage))
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
            expectThrows<CannotAttachSignedMessageException> {
                service.attachWalletAddressAndSignedMessage(uuid, walletAddress, signedMessage)
            }

            expectInteractions(authorizationRequestRepository) {
                once.setSignedMessage(uuid, walletAddress, signedMessage)
            }
        }
    }
}
