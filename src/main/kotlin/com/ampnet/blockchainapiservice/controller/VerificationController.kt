package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.model.response.GenerateVerificationMessageResponse
import com.ampnet.blockchainapiservice.service.VerificationService
import com.ampnet.blockchainapiservice.util.WalletAddress
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class VerificationController(private val verificationService: VerificationService) {

    companion object : KLogging()

    @PostMapping("/verification/{walletAddress}/generate")
    fun generateVerificationMessage(
        @PathVariable walletAddress: String
    ): ResponseEntity<GenerateVerificationMessageResponse> {
        logger.info { "Request generation of verification message for wallet address: $walletAddress" }
        val message = verificationService.createUnsignedVerificationMessage(WalletAddress(walletAddress))
        return ResponseEntity.ok(
            GenerateVerificationMessageResponse(
                id = message.id,
                message = message.toStringMessage(),
                validUntil = message.validUntil.value
            )
        )
    }
}
