package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.blockchain.properties.RpcUrlSpec
import com.ampnet.blockchainapiservice.config.binding.annotation.RpcUrlBinding
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.params.CreateErc20LockRequestParams
import com.ampnet.blockchainapiservice.model.request.AttachTransactionHashRequest
import com.ampnet.blockchainapiservice.model.request.CreateErc20LockRequest
import com.ampnet.blockchainapiservice.model.response.Erc20LockRequestResponse
import com.ampnet.blockchainapiservice.model.response.TransactionResponse
import com.ampnet.blockchainapiservice.service.Erc20LockRequestService
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.DurationSeconds
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
class Erc20LockRequestController(private val erc20LockRequestService: Erc20LockRequestService) {

    @PostMapping("/v1/lock")
    fun createErc20LockRequest(
        @RequestBody requestBody: CreateErc20LockRequest
    ): ResponseEntity<Erc20LockRequestResponse> {
        val params = CreateErc20LockRequestParams(
            clientId = requestBody.clientId,
            chainId = requestBody.chainId?.let { ChainId(it) },
            redirectUrl = requestBody.redirectUrl,
            tokenAddress = requestBody.tokenAddress?.let { ContractAddress(it) },
            tokenAmount = Balance(requestBody.amount),
            lockDuration = DurationSeconds(requestBody.lockDurationInSeconds),
            lockContractAddress = ContractAddress(requestBody.lockContractAddress),
            tokenSenderAddress = requestBody.senderAddress?.let { WalletAddress(it) },
            arbitraryData = requestBody.arbitraryData,
            screenConfig = requestBody.screenConfig ?: ScreenConfig.EMPTY
        )

        val createdRequest = erc20LockRequestService.createErc20LockRequest(params)

        return ResponseEntity.ok(
            Erc20LockRequestResponse(
                id = createdRequest.value.id,
                status = Status.PENDING,
                chainId = createdRequest.value.chainId.value,
                tokenAddress = createdRequest.value.tokenAddress.rawValue,
                amount = createdRequest.value.tokenAmount.rawValue,
                lockDurationInSeconds = createdRequest.value.lockDuration.rawValue,
                unlocksAt = null,
                lockContractAddress = createdRequest.value.lockContractAddress.rawValue,
                senderAddress = createdRequest.value.tokenSenderAddress?.rawValue,
                arbitraryData = createdRequest.value.arbitraryData,
                screenConfig = createdRequest.value.screenConfig.orEmpty(),
                redirectUrl = createdRequest.value.redirectUrl,
                lockTx = TransactionResponse(
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

    @GetMapping("/v1/lock/{id}")
    fun getErc20LockRequest(
        @PathVariable("id") id: UUID,
        @RpcUrlBinding rpcSpec: RpcUrlSpec
    ): ResponseEntity<Erc20LockRequestResponse> {
        val lockRequest = erc20LockRequestService.getErc20LockRequest(id, rpcSpec)

        return ResponseEntity.ok(
            Erc20LockRequestResponse(
                id = lockRequest.value.id,
                status = lockRequest.status,
                chainId = lockRequest.value.chainId.value,
                tokenAddress = lockRequest.value.tokenAddress.rawValue,
                amount = lockRequest.value.tokenAmount.rawValue,
                lockDurationInSeconds = lockRequest.value.lockDuration.rawValue,
                unlocksAt = lockRequest.transactionData.timestamp?.plus(lockRequest.value.lockDuration)?.value,
                lockContractAddress = lockRequest.value.lockContractAddress.rawValue,
                senderAddress = lockRequest.value.tokenSenderAddress?.rawValue,
                arbitraryData = lockRequest.value.arbitraryData,
                screenConfig = lockRequest.value.screenConfig.orEmpty(),
                redirectUrl = lockRequest.value.redirectUrl,
                lockTx = TransactionResponse(
                    txHash = lockRequest.transactionData.txHash?.value,
                    from = lockRequest.transactionData.fromAddress?.rawValue,
                    to = lockRequest.transactionData.toAddress.rawValue,
                    data = lockRequest.transactionData.data.value,
                    blockConfirmations = lockRequest.transactionData.blockConfirmations,
                    timestamp = lockRequest.transactionData.timestamp?.value
                )
            )
        )
    }

    @PutMapping("/v1/lock/{id}")
    fun attachTransactionHash(
        @PathVariable("id") id: UUID,
        @RequestBody requestBody: AttachTransactionHashRequest
    ) {
        erc20LockRequestService.attachTxHash(id, TransactionHash(requestBody.txHash))
    }
}
