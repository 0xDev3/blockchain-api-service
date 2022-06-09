package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.blockchain.properties.RpcUrlSpec
import com.ampnet.blockchainapiservice.config.binding.annotation.RpcUrlBinding
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.params.CreateErc20SendRequestParams
import com.ampnet.blockchainapiservice.model.request.AttachTransactionHashRequest
import com.ampnet.blockchainapiservice.model.request.CreateErc20SendRequest
import com.ampnet.blockchainapiservice.model.response.Erc20SendRequestResponse
import com.ampnet.blockchainapiservice.model.response.TransactionResponse
import com.ampnet.blockchainapiservice.service.Erc20SendRequestService
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.Status
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

    @PostMapping(
        value = [
            "/send", // TODO deprecated
            "/v1/send"
        ]
    )
    fun createErc20SendRequest(
        @RequestBody requestBody: CreateErc20SendRequest
    ): ResponseEntity<Erc20SendRequestResponse> {
        val params = CreateErc20SendRequestParams(
            clientId = requestBody.clientId,
            chainId = requestBody.chainId?.let { ChainId(it) },
            redirectUrl = requestBody.redirectUrl,
            tokenAddress = requestBody.tokenAddress?.let { ContractAddress(it) },
            tokenAmount = Balance(requestBody.amount),
            tokenSenderAddress = requestBody.senderAddress?.let { WalletAddress(it) },
            tokenRecipientAddress = WalletAddress(requestBody.recipientAddress),
            arbitraryData = requestBody.arbitraryData,
            screenConfig = requestBody.screenConfig ?: ScreenConfig.EMPTY
        )

        val createdRequest = erc20SendRequestService.createErc20SendRequest(params)

        return ResponseEntity.ok(
            Erc20SendRequestResponse(
                id = createdRequest.value.id,
                status = Status.PENDING,
                chainId = createdRequest.value.chainId.value,
                tokenAddress = createdRequest.value.tokenAddress.rawValue,
                amount = createdRequest.value.tokenAmount.rawValue,
                senderAddress = createdRequest.value.tokenSenderAddress?.rawValue,
                recipientAddress = createdRequest.value.tokenRecipientAddress.rawValue,
                arbitraryData = createdRequest.value.arbitraryData,
                screenConfig = createdRequest.value.screenConfig.orEmpty(),
                redirectUrl = createdRequest.value.redirectUrl,
                sendTx = TransactionResponse(
                    txHash = null,
                    from = createdRequest.value.tokenSenderAddress?.rawValue,
                    to = createdRequest.value.tokenAddress.rawValue,
                    data = createdRequest.data.value,
                    blockConfirmations = null,
                    timestamp = null
                )
            )
        )
    }

    @GetMapping(
        value = [
            "/send/{id}", // TODO deprecated
            "/v1/send/{id}"
        ]
    )
    fun getErc20SendRequest(
        @PathVariable("id") id: UUID,
        @RpcUrlBinding rpcSpec: RpcUrlSpec
    ): ResponseEntity<Erc20SendRequestResponse> {
        val sendRequest = erc20SendRequestService.getErc20SendRequest(id, rpcSpec)

        return ResponseEntity.ok(
            Erc20SendRequestResponse(
                id = sendRequest.value.id,
                status = sendRequest.status,
                chainId = sendRequest.value.chainId.value,
                tokenAddress = sendRequest.value.tokenAddress.rawValue,
                amount = sendRequest.value.tokenAmount.rawValue,
                senderAddress = sendRequest.value.tokenSenderAddress?.rawValue,
                recipientAddress = sendRequest.value.tokenRecipientAddress.rawValue,
                arbitraryData = sendRequest.value.arbitraryData,
                screenConfig = sendRequest.value.screenConfig.orEmpty(),
                redirectUrl = sendRequest.value.redirectUrl,
                sendTx = TransactionResponse(
                    txHash = sendRequest.transactionData.txHash?.value,
                    from = sendRequest.transactionData.fromAddress?.rawValue,
                    to = sendRequest.transactionData.toAddress.rawValue,
                    data = sendRequest.transactionData.data.value,
                    blockConfirmations = sendRequest.transactionData.blockConfirmations,
                    timestamp = sendRequest.transactionData.timestamp?.value
                )
            )
        )
    }

    @PutMapping(
        value = [
            "/send/{id}", // TODO deprecated
            "/v1/send/{id}"
        ]
    )
    fun attachTransactionHash(
        @PathVariable("id") id: UUID,
        @RequestBody requestBody: AttachTransactionHashRequest
    ) {
        erc20SendRequestService.attachTxHash(id, TransactionHash(requestBody.txHash))
    }
}
