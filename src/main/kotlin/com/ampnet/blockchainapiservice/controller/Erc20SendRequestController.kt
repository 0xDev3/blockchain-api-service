package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.blockchain.properties.RpcUrlSpec
import com.ampnet.blockchainapiservice.config.binding.annotation.ApiKeyBinding
import com.ampnet.blockchainapiservice.config.binding.annotation.RpcUrlBinding
import com.ampnet.blockchainapiservice.model.params.CreateErc20SendRequestParams
import com.ampnet.blockchainapiservice.model.request.AttachTransactionHashRequest
import com.ampnet.blockchainapiservice.model.request.CreateErc20SendRequest
import com.ampnet.blockchainapiservice.model.response.Erc20SendRequestResponse
import com.ampnet.blockchainapiservice.model.response.Erc20SendRequestsResponse
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.service.Erc20SendRequestService
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
class Erc20SendRequestController(private val erc20SendRequestService: Erc20SendRequestService) {

    @PostMapping("/v1/send")
    fun createErc20SendRequest(
        @ApiKeyBinding project: Project,
        @RequestBody requestBody: CreateErc20SendRequest
    ): ResponseEntity<Erc20SendRequestResponse> {
        val params = CreateErc20SendRequestParams(requestBody)
        val createdRequest = erc20SendRequestService.createErc20SendRequest(params, project)
        return ResponseEntity.ok(Erc20SendRequestResponse(createdRequest))
    }

    @GetMapping("/v1/send/{id}")
    fun getErc20SendRequest(
        @PathVariable("id") id: UUID,
        @RpcUrlBinding rpcSpec: RpcUrlSpec
    ): ResponseEntity<Erc20SendRequestResponse> {
        val sendRequest = erc20SendRequestService.getErc20SendRequest(id, rpcSpec)
        return ResponseEntity.ok(Erc20SendRequestResponse(sendRequest))
    }

    @GetMapping("/v1/send/by-sender/{sender}")
    fun getErc20SendRequestsBySender(
        @PathVariable("sender") sender: WalletAddress,
        @RpcUrlBinding rpcSpec: RpcUrlSpec
    ): ResponseEntity<Erc20SendRequestsResponse> {
        val sendRequests = erc20SendRequestService.getErc20SendRequestsBySender(sender, rpcSpec)
        return ResponseEntity.ok(Erc20SendRequestsResponse(sendRequests.map { Erc20SendRequestResponse(it) }))
    }

    @GetMapping("/v1/send/by-recipient/{recipient}")
    fun getErc20SendRequestsByRecipient(
        @PathVariable("recipient") recipient: WalletAddress,
        @RpcUrlBinding rpcSpec: RpcUrlSpec
    ): ResponseEntity<Erc20SendRequestsResponse> {
        val sendRequests = erc20SendRequestService.getErc20SendRequestsByRecipient(recipient, rpcSpec)
        return ResponseEntity.ok(Erc20SendRequestsResponse(sendRequests.map { Erc20SendRequestResponse(it) }))
    }

    @PutMapping("/v1/send/{id}")
    fun attachTransactionHash(
        @PathVariable("id") id: UUID,
        @RequestBody requestBody: AttachTransactionHashRequest
    ) {
        erc20SendRequestService.attachTxHash(id, TransactionHash(requestBody.txHash))
    }
}
