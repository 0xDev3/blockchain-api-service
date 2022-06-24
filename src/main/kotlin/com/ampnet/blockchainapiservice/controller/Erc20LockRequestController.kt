package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.blockchain.properties.RpcUrlSpec
import com.ampnet.blockchainapiservice.config.binding.annotation.ApiKeyBinding
import com.ampnet.blockchainapiservice.config.binding.annotation.RpcUrlBinding
import com.ampnet.blockchainapiservice.model.params.CreateErc20LockRequestParams
import com.ampnet.blockchainapiservice.model.request.AttachTransactionHashRequest
import com.ampnet.blockchainapiservice.model.request.CreateErc20LockRequest
import com.ampnet.blockchainapiservice.model.response.Erc20LockRequestResponse
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.service.Erc20LockRequestService
import com.ampnet.blockchainapiservice.util.TransactionHash
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
        @PathVariable("id") id: UUID,
        @RpcUrlBinding rpcSpec: RpcUrlSpec
    ): ResponseEntity<Erc20LockRequestResponse> {
        val lockRequest = erc20LockRequestService.getErc20LockRequest(id, rpcSpec)
        return ResponseEntity.ok(Erc20LockRequestResponse(lockRequest))
    }

    @PutMapping("/v1/lock/{id}")
    fun attachTransactionHash(
        @PathVariable("id") id: UUID,
        @RequestBody requestBody: AttachTransactionHashRequest
    ) {
        erc20LockRequestService.attachTxHash(id, TransactionHash(requestBody.txHash))
    }
}
