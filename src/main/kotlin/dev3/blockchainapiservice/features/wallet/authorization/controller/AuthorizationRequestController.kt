package dev3.blockchainapiservice.features.wallet.authorization.controller

import dev3.blockchainapiservice.config.binding.annotation.ApiKeyBinding
import dev3.blockchainapiservice.config.interceptors.annotation.ApiReadLimitedMapping
import dev3.blockchainapiservice.config.interceptors.annotation.ApiWriteLimitedMapping
import dev3.blockchainapiservice.config.interceptors.annotation.IdType
import dev3.blockchainapiservice.features.api.access.model.result.Project
import dev3.blockchainapiservice.features.wallet.authorization.model.params.CreateAuthorizationRequestParams
import dev3.blockchainapiservice.features.wallet.authorization.model.request.CreateAuthorizationRequest
import dev3.blockchainapiservice.features.wallet.authorization.model.response.AuthorizationRequestResponse
import dev3.blockchainapiservice.features.wallet.authorization.model.response.AuthorizationRequestsResponse
import dev3.blockchainapiservice.features.wallet.authorization.service.AuthorizationRequestService
import dev3.blockchainapiservice.generated.jooq.id.AuthorizationRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.model.request.AttachSignedMessageRequest
import dev3.blockchainapiservice.util.SignedMessage
import dev3.blockchainapiservice.util.WalletAddress
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@Validated
@RestController
class AuthorizationRequestController(private val authorizationRequestService: AuthorizationRequestService) {

    @ApiWriteLimitedMapping(IdType.PROJECT_ID, RequestMethod.POST, "/v1/wallet-authorization")
    fun createAuthorizationRequest(
        @ApiKeyBinding project: Project,
        @Valid @RequestBody requestBody: CreateAuthorizationRequest
    ): ResponseEntity<AuthorizationRequestResponse> {
        val params = CreateAuthorizationRequestParams(requestBody)
        val createdRequest = authorizationRequestService.createAuthorizationRequest(params, project)
        return ResponseEntity.ok(AuthorizationRequestResponse(createdRequest))
    }

    @ApiReadLimitedMapping(IdType.AUTHORIZATION_REQUEST_ID, "/v1/wallet-authorization/{id}")
    fun getAuthorizationRequest(
        @PathVariable("id") id: AuthorizationRequestId
    ): ResponseEntity<AuthorizationRequestResponse> {
        val authorizationRequest = authorizationRequestService.getAuthorizationRequest(id)
        return ResponseEntity.ok(AuthorizationRequestResponse(authorizationRequest))
    }

    @ApiReadLimitedMapping(IdType.PROJECT_ID, "/v1/wallet-authorization/by-project/{projectId}")
    fun getAuthorizationRequestsByProjectId(
        @PathVariable("projectId") projectId: ProjectId
    ): ResponseEntity<AuthorizationRequestsResponse> {
        val authorizationRequests = authorizationRequestService.getAuthorizationRequestsByProjectId(projectId)
        return ResponseEntity.ok(
            AuthorizationRequestsResponse(authorizationRequests.map { AuthorizationRequestResponse(it) })
        )
    }

    @ApiWriteLimitedMapping(IdType.AUTHORIZATION_REQUEST_ID, RequestMethod.PUT, "/v1/wallet-authorization/{id}")
    fun attachSignedMessage(
        @PathVariable("id") id: AuthorizationRequestId,
        @Valid @RequestBody requestBody: AttachSignedMessageRequest
    ) {
        authorizationRequestService.attachWalletAddressAndSignedMessage(
            id = id,
            walletAddress = WalletAddress(requestBody.walletAddress),
            signedMessage = SignedMessage(requestBody.signedMessage)
        )
    }
}
