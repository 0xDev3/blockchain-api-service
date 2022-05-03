package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.blockchain.properties.RpcUrlSpec
import com.ampnet.blockchainapiservice.config.binding.annotation.RpcUrlBinding
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.params.CreateSendErc20RequestParams
import com.ampnet.blockchainapiservice.model.request.AttachTransactionHashRequest
import com.ampnet.blockchainapiservice.model.request.CreateSendErc20Request
import com.ampnet.blockchainapiservice.model.response.SendErc20RequestResponse
import com.ampnet.blockchainapiservice.model.response.TransactionResponse
import com.ampnet.blockchainapiservice.service.SendErc20RequestService
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
class SendErc20RequestController(private val sendErc20RequestService: SendErc20RequestService) {

    @PostMapping("/send")
    fun createSendErc20Request(
        @RequestBody requestBody: CreateSendErc20Request
    ): ResponseEntity<SendErc20RequestResponse> {
        val params = CreateSendErc20RequestParams(
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

        val createdRequest = sendErc20RequestService.createSendErc20Request(params)

        return ResponseEntity.ok(
            SendErc20RequestResponse(
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
                    blockConfirmations = null
                )
            )
        )
    }

    @GetMapping("/send/{id}")
    fun getSendErc20Request(
        @PathVariable("id") id: UUID,
        @RpcUrlBinding rpcSpec: RpcUrlSpec
    ): ResponseEntity<SendErc20RequestResponse> {
        val sendRequest = sendErc20RequestService.getSendErc20Request(id, rpcSpec)

        return ResponseEntity.ok(
            SendErc20RequestResponse(
                id = sendRequest.id,
                status = sendRequest.status,
                chainId = sendRequest.chainId.value,
                tokenAddress = sendRequest.tokenAddress.rawValue,
                amount = sendRequest.tokenAmount.rawValue,
                senderAddress = sendRequest.tokenSenderAddress?.rawValue,
                recipientAddress = sendRequest.tokenRecipientAddress.rawValue,
                arbitraryData = sendRequest.arbitraryData,
                screenConfig = sendRequest.screenConfig.orEmpty(),
                redirectUrl = sendRequest.redirectUrl,
                sendTx = TransactionResponse(
                    txHash = sendRequest.transactionData.txHash?.value,
                    from = sendRequest.transactionData.fromAddress?.rawValue,
                    to = sendRequest.tokenAddress.rawValue,
                    data = sendRequest.transactionData.data.value,
                    blockConfirmations = sendRequest.transactionData.blockConfirmations
                )
            )
        )
    }

    @PutMapping("/send/{id}")
    fun attachTransactionHash(
        @PathVariable("id") id: UUID,
        @RequestBody requestBody: AttachTransactionHashRequest
    ) {
        sendErc20RequestService.attachTxHash(id, TransactionHash(requestBody.txHash))
    }
}
