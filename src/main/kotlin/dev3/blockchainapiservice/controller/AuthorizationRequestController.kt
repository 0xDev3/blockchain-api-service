package dev3.blockchainapiservice.controller

import dev3.blockchainapiservice.config.binding.annotation.ApiKeyBinding
import dev3.blockchainapiservice.model.params.CreateAuthorizationRequestParams
import dev3.blockchainapiservice.model.request.AttachSignedMessageRequest
import dev3.blockchainapiservice.model.request.CreateAuthorizationRequest
import dev3.blockchainapiservice.model.response.AuthorizationRequestResponse
import dev3.blockchainapiservice.model.response.AuthorizationRequestsResponse
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.service.AuthorizationRequestService
import dev3.blockchainapiservice.util.SignedMessage
import dev3.blockchainapiservice.util.WalletAddress
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import javax.validation.Valid

@Validated
@RestController
class AuthorizationRequestController(private val authorizationRequestService: AuthorizationRequestService) {

    @PostMapping("/v1/wallet-authorization")
    fun createAuthorizationRequest(
        @ApiKeyBinding project: Project,
        @Valid @RequestBody requestBody: CreateAuthorizationRequest
    ): ResponseEntity<AuthorizationRequestResponse> {
        val params = CreateAuthorizationRequestParams(requestBody)
        val createdRequest = authorizationRequestService.createAuthorizationRequest(params, project)
        return ResponseEntity.ok(AuthorizationRequestResponse(createdRequest))
    }

    @GetMapping("/v1/wallet-authorization/{id}")
    fun getAuthorizationRequest(
        @PathVariable("id") id: UUID
    ): ResponseEntity<AuthorizationRequestResponse> {
        val authorizationRequest = authorizationRequestService.getAuthorizationRequest(id)
        return ResponseEntity.ok(AuthorizationRequestResponse(authorizationRequest))
    }

    @GetMapping("/v1/wallet-authorization/by-project/{projectId}")
    fun getAuthorizationRequestsByProjectId(
        @PathVariable("projectId") projectId: UUID
    ): ResponseEntity<AuthorizationRequestsResponse> {
        val authorizationRequests = authorizationRequestService.getAuthorizationRequestsByProjectId(projectId)
        return ResponseEntity.ok(
            AuthorizationRequestsResponse(authorizationRequests.map { AuthorizationRequestResponse(it) })
        )
    }

    @PutMapping("/v1/wallet-authorization/{id}")
    fun attachSignedMessage(
        @PathVariable("id") id: UUID,
        @Valid @RequestBody requestBody: AttachSignedMessageRequest
    ) {
        authorizationRequestService.attachWalletAddressAndSignedMessage(
            id = id,
            walletAddress = WalletAddress(requestBody.walletAddress),
            signedMessage = SignedMessage(requestBody.signedMessage)
        )
    }
}
