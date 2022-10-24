package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.JsonSchemaDocumentation
import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.params.CreateAuthorizationRequestParams
import com.ampnet.blockchainapiservice.model.request.AttachSignedMessageRequest
import com.ampnet.blockchainapiservice.model.request.CreateAuthorizationRequest
import com.ampnet.blockchainapiservice.model.response.AuthorizationRequestResponse
import com.ampnet.blockchainapiservice.model.response.AuthorizationRequestsResponse
import com.ampnet.blockchainapiservice.model.result.AuthorizationRequest
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.service.AuthorizationRequestService
import com.ampnet.blockchainapiservice.util.BaseUrl
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.SignedMessage
import com.ampnet.blockchainapiservice.util.Status
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.ampnet.blockchainapiservice.util.WithStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoMoreInteractions
import org.springframework.http.ResponseEntity
import java.util.UUID
import org.mockito.kotlin.verify as verifyMock

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
            id = UUID.randomUUID(),
            projectId = UUID.randomUUID(),
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
            ownerId = UUID.randomUUID(),
            issuerContractAddress = ContractAddress("a"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = ChainId(1337L),
            customRpcUrl = "custom-rpc-url",
            createdAt = TestData.TIMESTAMP
        )
        val service = mock<AuthorizationRequestService>()

        suppose("authorization request will be created") {
            given(service.createAuthorizationRequest(params, project))
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

            assertThat(response).withMessage()
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
        val id = UUID.randomUUID()
        val service = mock<AuthorizationRequestService>()
        val result = WithStatus(
            value = AuthorizationRequest(
                id = id,
                projectId = UUID.randomUUID(),
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
            given(service.getAuthorizationRequest(id))
                .willReturn(result)
        }

        val controller = AuthorizationRequestController(service)

        verify("controller returns correct response") {
            val response = controller.getAuthorizationRequest(id)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
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
        val id = UUID.randomUUID()
        val projectId = UUID.randomUUID()
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
            given(service.getAuthorizationRequestsByProjectId(projectId))
                .willReturn(listOf(result))
        }

        val controller = AuthorizationRequestController(service)

        verify("controller returns correct response") {
            val response = controller.getAuthorizationRequestsByProjectId(projectId)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
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

        val id = UUID.randomUUID()
        val walletAddress = WalletAddress("abc")
        val signedMessage = SignedMessage("signed-message")

        suppose("signed message will be attached") {
            val request = AttachSignedMessageRequest(walletAddress.rawValue, signedMessage.value)
            controller.attachSignedMessage(id, request)
            JsonSchemaDocumentation.createSchema(request.javaClass)
        }

        verify("signed message is correctly attached") {
            verifyMock(service)
                .attachWalletAddressAndSignedMessage(id, walletAddress, signedMessage)

            verifyNoMoreInteractions(service)
        }
    }
}
