package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.model.request.VerifySignedMessageRequest
import com.ampnet.blockchainapiservice.model.response.GenerateVerificationMessageResponse
import com.ampnet.blockchainapiservice.model.response.VerifySignedMessageResponse
import com.ampnet.blockchainapiservice.service.VerificationService
import com.ampnet.blockchainapiservice.util.WalletAddress
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class VerificationController(private val verificationService: VerificationService) {

    companion object : KLogging()

    @PostMapping("/verification/generate/{walletAddress}")
    fun generateVerificationMessage(
        @PathVariable walletAddress: String
    ): ResponseEntity<GenerateVerificationMessageResponse> {
        logger.info { "Request generation of verification message for wallet address: $walletAddress" }
        val unsignedMessage = verificationService.createUnsignedVerificationMessage(WalletAddress(walletAddress))
        return ResponseEntity.ok(
            GenerateVerificationMessageResponse(
                id = unsignedMessage.id,
                message = unsignedMessage.toStringMessage(),
                validUntil = unsignedMessage.validUntil.value
            )
        )
    }

    @PostMapping("/verification/signature/{messageId}")
    fun verifySignedMessage(
        @PathVariable messageId: UUID,
        @RequestBody requestBody: VerifySignedMessageRequest
    ): ResponseEntity<VerifySignedMessageResponse> {
        logger.info { "Validating signature for messageId: $messageId, signature: ${requestBody.signature}" }
        val signedMessage = verificationService.verifyAndStoreMessageSignature(messageId, requestBody.signature)
        return ResponseEntity.ok(
            VerifySignedMessageResponse(
                id = signedMessage.id,
                signature = signedMessage.signature,
                validUntil = signedMessage.validUntil.value
            )
        )
    }
}
