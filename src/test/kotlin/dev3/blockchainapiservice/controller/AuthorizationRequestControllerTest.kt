package dev3.blockchainapiservice.controller

import dev3.blockchainapiservice.JsonSchemaDocumentation
import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.generated.jooq.id.AuthorizationRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.generated.jooq.id.UserId
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.model.params.CreateAuthorizationRequestParams
import dev3.blockchainapiservice.model.request.AttachSignedMessageRequest
import dev3.blockchainapiservice.model.request.CreateAuthorizationRequest
import dev3.blockchainapiservice.model.response.AuthorizationRequestResponse
import dev3.blockchainapiservice.model.response.AuthorizationRequestsResponse
import dev3.blockchainapiservice.model.result.AuthorizationRequest
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.service.AuthorizationRequestService
import dev3.blockchainapiservice.util.BaseUrl
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.SignedMessage
import dev3.blockchainapiservice.util.Status
import dev3.blockchainapiservice.util.WalletAddress
import dev3.blockchainapiservice.util.WithStatus
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.http.ResponseEntity
import java.util.UUID

class AuthorizationRequestControllerTest : TestBase() {

    @Test
    fun mustCorrectlyCreateAuthorizationRequest() {
        val params = CreateAuthorizationRequestParams(
            redirectUrl = "redirect-url",
            requestedWalletAddress = WalletAddress("b"),
            messageToSign = "message-to-sign-override",
            storeIndefinitely = true,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            )
        )
        val result = AuthorizationRequest(
            id = AuthorizationRequestId(UUID.randomUUID()),
            projectId = ProjectId(UUID.randomUUID()),
            redirectUrl = params.redirectUrl!!,
            messageToSignOverride = params.messageToSign,
            storeIndefinitely = params.storeIndefinitely,
            requestedWalletAddress = params.requestedWalletAddress,
            actualWalletAddress = null,
            signedMessage = null,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = params.screenConfig,
            createdAt = TestData.TIMESTAMP
        )
        val project = Project(
            id = result.projectId,
            ownerId = UserId(UUID.randomUUID()),
            issuerContractAddress = ContractAddress("a"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = ChainId(1337L),
            customRpcUrl = "custom-rpc-url",
            createdAt = TestData.TIMESTAMP
        )
        val service = mock<AuthorizationRequestService>()

        suppose("authorization request will be created") {
            call(service.createAuthorizationRequest(params, project))
                .willReturn(result)
        }

        val controller = AuthorizationRequestController(service)

        verify("controller returns correct response") {
            val request = CreateAuthorizationRequest(
                redirectUrl = params.redirectUrl,
                messageToSign = params.messageToSign,
                storeIndefinitely = params.storeIndefinitely,
                walletAddress = params.requestedWalletAddress?.rawValue,
                arbitraryData = params.arbitraryData,
                screenConfig = params.screenConfig
            )
            val response = controller.createAuthorizationRequest(project, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        AuthorizationRequestResponse(
                            id = result.id,
                            projectId = project.id,
                            status = Status.PENDING,
                            redirectUrl = result.redirectUrl,
                            walletAddress = result.requestedWalletAddress?.rawValue,
                            arbitraryData = result.arbitraryData,
                            screenConfig = result.screenConfig.orEmpty(),
                            messageToSign = result.messageToSign,
                            signedMessage = result.signedMessage?.value,
                            createdAt = TestData.TIMESTAMP.value
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchAuthorizationRequest() {
        val id = AuthorizationRequestId(UUID.randomUUID())
        val service = mock<AuthorizationRequestService>()
        val result = WithStatus(
            value = AuthorizationRequest(
                id = id,
                projectId = ProjectId(UUID.randomUUID()),
                redirectUrl = "redirect-url",
                messageToSignOverride = "message-to-sign-override",
                storeIndefinitely = true,
                requestedWalletAddress = WalletAddress("def"),
                actualWalletAddress = WalletAddress("def"),
                arbitraryData = TestData.EMPTY_JSON_OBJECT,
                screenConfig = ScreenConfig(
                    beforeActionMessage = "before-action-message",
                    afterActionMessage = "after-action-message"
                ),
                signedMessage = SignedMessage("signed-message"),
                createdAt = TestData.TIMESTAMP
            ),
            status = Status.SUCCESS
        )

        suppose("some authorization request will be fetched") {
            call(service.getAuthorizationRequest(id))
                .willReturn(result)
        }

        val controller = AuthorizationRequestController(service)

        verify("controller returns correct response") {
            val response = controller.getAuthorizationRequest(id)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        AuthorizationRequestResponse(
                            id = result.value.id,
                            projectId = result.value.projectId,
                            status = result.status,
                            redirectUrl = result.value.redirectUrl,
                            walletAddress = result.value.requestedWalletAddress?.rawValue,
                            arbitraryData = result.value.arbitraryData,
                            screenConfig = result.value.screenConfig,
                            messageToSign = result.value.messageToSign,
                            signedMessage = result.value.signedMessage?.value,
                            createdAt = result.value.createdAt.value
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchAuthorizationRequestsByProjectId() {
        val id = AuthorizationRequestId(UUID.randomUUID())
        val projectId = ProjectId(UUID.randomUUID())
        val service = mock<AuthorizationRequestService>()
        val result =
            WithStatus(
                value = AuthorizationRequest(
                    id = id,
                    projectId = projectId,
                    redirectUrl = "redirect-url",
                    messageToSignOverride = "message-to-sign-override",
                    storeIndefinitely = true,
                    requestedWalletAddress = WalletAddress("def"),
                    actualWalletAddress = WalletAddress("def"),
                    arbitraryData = TestData.EMPTY_JSON_OBJECT,
                    screenConfig = ScreenConfig(
                        beforeActionMessage = "before-action-message",
                        afterActionMessage = "after-action-message"
                    ),
                    signedMessage = SignedMessage("signed-message"),
                    createdAt = TestData.TIMESTAMP
                ),
                status = Status.SUCCESS
            )

        suppose("some authorization requests will be fetched by project ID") {
            call(service.getAuthorizationRequestsByProjectId(projectId))
                .willReturn(listOf(result))
        }

        val controller = AuthorizationRequestController(service)

        verify("controller returns correct response") {
            val response = controller.getAuthorizationRequestsByProjectId(projectId)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        AuthorizationRequestsResponse(
                            listOf(
                                AuthorizationRequestResponse(
                                    id = result.value.id,
                                    projectId = result.value.projectId,
                                    status = result.status,
                                    redirectUrl = result.value.redirectUrl,
                                    walletAddress = result.value.requestedWalletAddress?.rawValue,
                                    arbitraryData = result.value.arbitraryData,
                                    screenConfig = result.value.screenConfig,
                                    messageToSign = result.value.messageToSign,
                                    signedMessage = result.value.signedMessage?.value,
                                    createdAt = result.value.createdAt.value
                                )
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyAttachSignedMessage() {
        val service = mock<AuthorizationRequestService>()
        val controller = AuthorizationRequestController(service)

        val id = AuthorizationRequestId(UUID.randomUUID())
        val walletAddress = WalletAddress("abc")
        val signedMessage = SignedMessage("signed-message")

        suppose("signed message will be attached") {
            val request = AttachSignedMessageRequest(walletAddress.rawValue, signedMessage.value)
            controller.attachSignedMessage(id, request)
            JsonSchemaDocumentation.createSchema(request.javaClass)
        }

        verify("signed message is correctly attached") {
            expectInteractions(service) {
                once.attachWalletAddressAndSignedMessage(id, walletAddress, signedMessage)
            }
        }
    }
}
