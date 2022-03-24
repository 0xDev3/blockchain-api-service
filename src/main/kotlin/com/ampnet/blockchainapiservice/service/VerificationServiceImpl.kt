package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.config.ApplicationProperties
import com.ampnet.blockchainapiservice.exception.BadSignatureException
import com.ampnet.blockchainapiservice.exception.ExpiredValidationMessageException
import com.ampnet.blockchainapiservice.exception.ResourceNotFoundException
import com.ampnet.blockchainapiservice.model.result.SignedVerificationMessage
import com.ampnet.blockchainapiservice.model.result.UnsignedVerificationMessage
import com.ampnet.blockchainapiservice.repository.SignedVerificationMessageRepository
import com.ampnet.blockchainapiservice.repository.UnsignedVerificationMessageRepository
import com.ampnet.blockchainapiservice.util.WalletAddress
import mu.KLogging
import org.kethereum.crypto.signedMessageToKey
import org.kethereum.crypto.toAddress
import org.kethereum.model.SignatureData
import org.springframework.stereotype.Service
import java.math.BigInteger
import java.util.UUID

@Service
class VerificationServiceImpl(
    private val unsignedVerificationMessageRepository: UnsignedVerificationMessageRepository,
    private val signedVerificationMessageRepository: SignedVerificationMessageRepository,
    private val uuidProvider: UuidProvider,
    private val utcDateTimeProvider: UtcDateTimeProvider,
    private val applicationProperties: ApplicationProperties
) : VerificationService {

    companion object : KLogging() {
        private const val EIP_191_MAGIC_BYTE = 0x19.toByte()
        private const val SIGNATURE_LENGTH = 132
        private const val R_START = 2
        private const val R_END = 66
        private const val S_START = 66
        private const val S_END = 130
        private const val V_START = 130
        private const val V_END = 132
        private const val HEX_RADIX = 16
        private val V_OFFSET = BigInteger.valueOf(27L)
    }

    override fun createUnsignedVerificationMessage(walletAddress: WalletAddress): UnsignedVerificationMessage {
        logger.info { "Creating unsigned verification message for wallet address: $walletAddress" }

        val message = UnsignedVerificationMessage(
            id = uuidProvider.getUuid(),
            walletAddress = walletAddress,
            createdAt = utcDateTimeProvider.getUtcDateTime(),
            validityDuration = applicationProperties.verification.unsignedMessageValidity
        )

        return unsignedVerificationMessageRepository.store(message)
    }

    override fun verifyAndStoreMessageSignature(messageId: UUID, signature: String): SignedVerificationMessage {
        logger.info { "Verifying message signature, messageId: $messageId, signature: $signature" }

        val unsignedMessage = unsignedVerificationMessageRepository.getById(messageId)
            ?: throw ResourceNotFoundException("Message not found for ID: $messageId")

        val now = utcDateTimeProvider.getUtcDateTime()

        if (unsignedMessage.isExpired(now)) {
            throw ExpiredValidationMessageException(messageId)
        }

        val signatureData = getSignatureData(signature)
        val eip919 = generateEip191Message(unsignedMessage.toStringMessage().toByteArray())
        val publicKey = signedMessageToKey(eip919, signatureData)
        val signatureAddress = WalletAddress(publicKey.toAddress().toString())

        if (signatureAddress != unsignedMessage.walletAddress) {
            throw BadSignatureException(
                "Public address of provided signature does not match expected signature address"
            )
        }

        val deletionResult = unsignedVerificationMessageRepository.deleteById(messageId)
        logger.debug { "Unsigned message with ID: $messageId was deleted: $deletionResult" }

        return signedVerificationMessageRepository.store(
            unsignedMessage.toSignedMessage(
                signature = signature,
                messageId = uuidProvider.getUuid(),
                now = now,
                validityDuration = applicationProperties.verification.signedMessageValidity
            )
        )
    }

    private fun generateEip191Message(message: ByteArray): ByteArray =
        byteArrayOf(EIP_191_MAGIC_BYTE) + ("Ethereum Signed Message:\n" + message.size).toByteArray() + message

    private fun BigInteger.withVOffset(): BigInteger =
        if (this == BigInteger.ZERO || this == BigInteger.ONE) {
            this + V_OFFSET
        } else {
            this
        }

    private fun getSignatureData(signature: String): SignatureData {
        if (signature.length != SIGNATURE_LENGTH) {
            throw BadSignatureException("Signature: $signature is of wrong length")
        }

        val r = signature.substring(R_START, R_END)
        val s = signature.substring(S_START, S_END)
        val v = signature.substring(V_START, V_END)

        try {
            return SignatureData(
                r = BigInteger(r, HEX_RADIX),
                s = BigInteger(s, HEX_RADIX),
                v = BigInteger(v, HEX_RADIX).withVOffset()
            )
        } catch (ex: NumberFormatException) {
            throw BadSignatureException("Signature: $signature is not a valid hex value")
        }
    }
}
