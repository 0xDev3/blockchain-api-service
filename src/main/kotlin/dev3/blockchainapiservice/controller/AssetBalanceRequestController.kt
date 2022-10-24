package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.config.binding.annotation.ApiKeyBinding
import com.ampnet.blockchainapiservice.model.params.CreateAssetBalanceRequestParams
import com.ampnet.blockchainapiservice.model.request.AttachSignedMessageRequest
import com.ampnet.blockchainapiservice.model.request.CreateAssetBalanceRequest
import com.ampnet.blockchainapiservice.model.response.AssetBalanceRequestResponse
import com.ampnet.blockchainapiservice.model.response.AssetBalanceRequestsResponse
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.service.AssetBalanceRequestService
import com.ampnet.blockchainapiservice.util.SignedMessage
import com.ampnet.blockchainapiservice.util.WalletAddress
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
class AssetBalanceRequestController(private val assetBalanceRequestService: AssetBalanceRequestService) {

    @PostMapping("/v1/balance")
    fun createAssetBalanceRequest(
        @ApiKeyBinding project: Project,
        @Valid @RequestBody requestBody: CreateAssetBalanceRequest
    ): ResponseEntity<AssetBalanceRequestResponse> {
        val params = CreateAssetBalanceRequestParams(requestBody)
        val createdRequest = assetBalanceRequestService.createAssetBalanceRequest(params, project)
        return ResponseEntity.ok(AssetBalanceRequestResponse(createdRequest))
    }

    @GetMapping("/v1/balance/{id}")
    fun getAssetBalanceRequest(
        @PathVariable("id") id: UUID
    ): ResponseEntity<AssetBalanceRequestResponse> {
        val balanceRequest = assetBalanceRequestService.getAssetBalanceRequest(id)
        return ResponseEntity.ok(AssetBalanceRequestResponse(balanceRequest))
    }

    @GetMapping("/v1/balance/by-project/{projectId}")
    fun getAssetBalanceRequestsByProjectId(
        @PathVariable("projectId") projectId: UUID
    ): ResponseEntity<AssetBalanceRequestsResponse> {
        val balanceRequests = assetBalanceRequestService.getAssetBalanceRequestsByProjectId(projectId)
        return ResponseEntity.ok(AssetBalanceRequestsResponse(balanceRequests.map { AssetBalanceRequestResponse(it) }))
    }

    @PutMapping("/v1/balance/{id}")
    fun attachSignedMessage(
        @PathVariable("id") id: UUID,
        @Valid @RequestBody requestBody: AttachSignedMessageRequest
    ) {
        assetBalanceRequestService.attachWalletAddressAndSignedMessage(
            id = id,
            walletAddress = WalletAddress(requestBody.walletAddress),
            signedMessage = SignedMessage(requestBody.signedMessage)
        )
    }
}
