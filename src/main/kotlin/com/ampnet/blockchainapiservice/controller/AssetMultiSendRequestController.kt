package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.config.binding.annotation.ApiKeyBinding
import com.ampnet.blockchainapiservice.config.validation.ValidEthAddress
import com.ampnet.blockchainapiservice.model.params.CreateAssetMultiSendRequestParams
import com.ampnet.blockchainapiservice.model.request.AttachTransactionInfoRequest
import com.ampnet.blockchainapiservice.model.request.CreateAssetMultiSendRequest
import com.ampnet.blockchainapiservice.model.response.AssetMultiSendRequestResponse
import com.ampnet.blockchainapiservice.model.response.AssetMultiSendRequestsResponse
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.service.AssetMultiSendRequestService
import com.ampnet.blockchainapiservice.util.TransactionHash
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
class AssetMultiSendRequestController(private val assetMultiSendRequestService: AssetMultiSendRequestService) {

    @PostMapping("/v1/multi-send")
    fun createAssetMultiSendRequest(
        @ApiKeyBinding project: Project,
        @Valid @RequestBody requestBody: CreateAssetMultiSendRequest
    ): ResponseEntity<AssetMultiSendRequestResponse> {
        val params = CreateAssetMultiSendRequestParams(requestBody)
        val createdRequest = assetMultiSendRequestService.createAssetMultiSendRequest(params, project)
        return ResponseEntity.ok(AssetMultiSendRequestResponse(createdRequest))
    }

    @GetMapping("/v1/multi-send/{id}")
    fun getAssetMultiSendRequest(
        @PathVariable("id") id: UUID
    ): ResponseEntity<AssetMultiSendRequestResponse> {
        val request = assetMultiSendRequestService.getAssetMultiSendRequest(id)
        return ResponseEntity.ok(AssetMultiSendRequestResponse(request))
    }

    @GetMapping("/v1/multi-send/by-project/{projectId}")
    fun getAssetMultiSendRequestsByProjectId(
        @PathVariable("projectId") projectId: UUID
    ): ResponseEntity<AssetMultiSendRequestsResponse> {
        val requests = assetMultiSendRequestService.getAssetMultiSendRequestsByProjectId(projectId)
        return ResponseEntity.ok(AssetMultiSendRequestsResponse(requests.map { AssetMultiSendRequestResponse(it) }))
    }

    @GetMapping("/v1/multi-send/by-sender/{sender}")
    fun getAssetMultiSendRequestsBySender(
        @ValidEthAddress @PathVariable("sender") sender: String
    ): ResponseEntity<AssetMultiSendRequestsResponse> {
        val requests = assetMultiSendRequestService.getAssetMultiSendRequestsBySender(WalletAddress(sender))
        return ResponseEntity.ok(AssetMultiSendRequestsResponse(requests.map { AssetMultiSendRequestResponse(it) }))
    }

    @PutMapping("/v1/multi-send/{id}/approve")
    fun attachApproveTransactionInfo(
        @PathVariable("id") id: UUID,
        @Valid @RequestBody requestBody: AttachTransactionInfoRequest
    ) {
        assetMultiSendRequestService.attachApproveTxInfo(
            id = id,
            txHash = TransactionHash(requestBody.txHash),
            caller = WalletAddress(requestBody.callerAddress)
        )
    }

    @PutMapping("/v1/multi-send/{id}/disperse")
    fun attachDisperseTransactionInfo(
        @PathVariable("id") id: UUID,
        @Valid @RequestBody requestBody: AttachTransactionInfoRequest
    ) {
        assetMultiSendRequestService.attachDisperseTxInfo(
            id = id,
            txHash = TransactionHash(requestBody.txHash),
            caller = WalletAddress(requestBody.callerAddress)
        )
    }
}
