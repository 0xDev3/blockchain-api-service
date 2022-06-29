package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.config.binding.annotation.ApiKeyBinding
import com.ampnet.blockchainapiservice.model.params.CreateErc20LockRequestParams
import com.ampnet.blockchainapiservice.model.request.AttachTransactionInfoRequest
import com.ampnet.blockchainapiservice.model.request.CreateErc20LockRequest
import com.ampnet.blockchainapiservice.model.response.Erc20LockRequestResponse
import com.ampnet.blockchainapiservice.model.response.Erc20LockRequestsResponse
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.service.Erc20LockRequestService
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class Erc20LockRequestController(private val erc20LockRequestService: Erc20LockRequestService) {

    @PostMapping("/v1/lock")
    fun createErc20LockRequest(
        @ApiKeyBinding project: Project,
        @RequestBody requestBody: CreateErc20LockRequest
    ): ResponseEntity<Erc20LockRequestResponse> {
        val params = CreateErc20LockRequestParams(requestBody)
        val createdRequest = erc20LockRequestService.createErc20LockRequest(params, project)
        return ResponseEntity.ok(Erc20LockRequestResponse(createdRequest))
    }

    @GetMapping("/v1/lock/{id}")
    fun getErc20LockRequest(
        @PathVariable("id") id: UUID
    ): ResponseEntity<Erc20LockRequestResponse> {
        val lockRequest = erc20LockRequestService.getErc20LockRequest(id)
        return ResponseEntity.ok(Erc20LockRequestResponse(lockRequest))
    }

    @GetMapping("/v1/lock/by-project/{projectId}")
    fun getErc20LockRequestsByProjectId(
        @PathVariable("projectId") projectId: UUID
    ): ResponseEntity<Erc20LockRequestsResponse> {
        val lockRequests = erc20LockRequestService.getErc20LockRequestsByProjectId(projectId)
        return ResponseEntity.ok(Erc20LockRequestsResponse(lockRequests.map { Erc20LockRequestResponse(it) }))
    }

    @PutMapping("/v1/lock/{id}")
    fun attachTransactionInfo(
        @PathVariable("id") id: UUID,
        @RequestBody requestBody: AttachTransactionInfoRequest
    ) {
        erc20LockRequestService.attachTxInfo(
            id,
            TransactionHash(requestBody.txHash),
            WalletAddress(requestBody.callerAddress)
        )
    }
}
