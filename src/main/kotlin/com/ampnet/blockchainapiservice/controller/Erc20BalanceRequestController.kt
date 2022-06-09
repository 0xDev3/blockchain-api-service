package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.blockchain.properties.RpcUrlSpec
import com.ampnet.blockchainapiservice.config.binding.annotation.RpcUrlBinding
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.params.CreateErc20BalanceRequestParams
import com.ampnet.blockchainapiservice.model.request.AttachSignedMessageRequest
import com.ampnet.blockchainapiservice.model.request.CreateErc20BalanceRequest
import com.ampnet.blockchainapiservice.model.response.BalanceResponse
import com.ampnet.blockchainapiservice.model.response.Erc20BalanceRequestResponse
import com.ampnet.blockchainapiservice.service.Erc20BalanceRequestService
import com.ampnet.blockchainapiservice.util.BlockNumber
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.SignedMessage
import com.ampnet.blockchainapiservice.util.Status
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
class Erc20BalanceRequestController(private val erc20BalanceRequestService: Erc20BalanceRequestService) {

    @PostMapping(
        value = [
            "/balance", // TODO deprecated
            "/v1/balance"
        ]
    )
    fun createErc20BalanceRequest(
        @RequestBody requestBody: CreateErc20BalanceRequest
    ): ResponseEntity<Erc20BalanceRequestResponse> {
        val params = CreateErc20BalanceRequestParams(
            clientId = requestBody.clientId,
            chainId = requestBody.chainId?.let { ChainId(it) },
            redirectUrl = requestBody.redirectUrl,
            tokenAddress = requestBody.tokenAddress?.let { ContractAddress(it) },
            blockNumber = requestBody.blockNumber?.let { BlockNumber(it) },
            requestedWalletAddress = requestBody.walletAddress?.let { WalletAddress(it) },
            arbitraryData = requestBody.arbitraryData,
            screenConfig = requestBody.screenConfig ?: ScreenConfig.EMPTY
        )

        val createdRequest = erc20BalanceRequestService.createErc20BalanceRequest(params)

        return ResponseEntity.ok(
            Erc20BalanceRequestResponse(
                id = createdRequest.id,
                status = Status.PENDING,
                chainId = createdRequest.chainId.value,
                redirectUrl = createdRequest.redirectUrl,
                tokenAddress = createdRequest.tokenAddress.rawValue,
                blockNumber = createdRequest.blockNumber?.value,
                walletAddress = createdRequest.requestedWalletAddress?.rawValue,
                arbitraryData = createdRequest.arbitraryData,
                screenConfig = createdRequest.screenConfig.orEmpty(),
                balance = null,
                messageToSign = createdRequest.messageToSign,
                signedMessage = createdRequest.signedMessage?.value
            )
        )
    }

    @GetMapping(
        value = [
            "/balance/{id}", // TODO deprecated
            "/v1/balance/{id}"
        ]
    )
    fun getErc20BalanceRequest(
        @PathVariable("id") id: UUID,
        @RpcUrlBinding rpcSpec: RpcUrlSpec
    ): ResponseEntity<Erc20BalanceRequestResponse> {
        val balanceRequest = erc20BalanceRequestService.getErc20BalanceRequest(id, rpcSpec)

        return ResponseEntity.ok(
            Erc20BalanceRequestResponse(
                id = balanceRequest.id,
                status = balanceRequest.status,
                chainId = balanceRequest.chainId.value,
                redirectUrl = balanceRequest.redirectUrl,
                tokenAddress = balanceRequest.tokenAddress.rawValue,
                blockNumber = balanceRequest.blockNumber?.value,
                walletAddress = balanceRequest.requestedWalletAddress?.rawValue,
                arbitraryData = balanceRequest.arbitraryData,
                screenConfig = balanceRequest.screenConfig.orEmpty(),
                balance = balanceRequest.balance?.let {
                    BalanceResponse(
                        wallet = it.wallet.rawValue,
                        blockNumber = it.blockNumber.value,
                        timestamp = it.timestamp.value,
                        amount = it.amount.rawValue
                    )
                },
                messageToSign = balanceRequest.messageToSign,
                signedMessage = balanceRequest.signedMessage?.value
            )
        )
    }

    @PutMapping(
        value = [
            "/balance/{id}", // TODO deprecated
            "/v1/balance/{id}"
        ]
    )
    fun attachSignedMessage(
        @PathVariable("id") id: UUID,
        @RequestBody requestBody: AttachSignedMessageRequest
    ) {
        erc20BalanceRequestService.attachWalletAddressAndSignedMessage(
            id = id,
            walletAddress = WalletAddress(requestBody.walletAddress),
            signedMessage = SignedMessage(requestBody.signedMessage)
        )
    }
}
