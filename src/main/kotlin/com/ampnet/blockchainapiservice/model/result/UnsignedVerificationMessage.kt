package com.ampnet.blockchainapiservice.model.result

import com.ampnet.blockchainapiservice.util.UtcDateTime
import com.ampnet.blockchainapiservice.util.WalletAddress
import java.time.Duration
import java.util.UUID

data class UnsignedVerificationMessage(
    val id: UUID,
    val walletAddress: WalletAddress,
    val createdAt: UtcDateTime,
    val validUntil: UtcDateTime
) {
    constructor(id: UUID, walletAddress: WalletAddress, createdAt: UtcDateTime, validityDuration: Duration) :
        this(id, walletAddress, createdAt, createdAt + validityDuration)

    fun isExpired(now: UtcDateTime): Boolean {
        return now.isAfter(validUntil)
    }

    fun toSignedMessage(
        signature: String,
        messageId: UUID,
        now: UtcDateTime,
        validityDuration: Duration
    ): SignedVerificationMessage =
        SignedVerificationMessage(
            id = messageId,
            walletAddress = walletAddress,
            signature = signature,
            signedId = id, // original message ID is the one which is used in the message to sign
            createdAt = createdAt,
            verifiedAt = now,
            validUntil = now + validityDuration
        )

    fun toStringMessage(): String =
        "By signing this message, I verify that I'm the owner of wallet address ${walletAddress.rawValue}." +
            " Message ID: $id, timestamp: ${createdAt.value.toInstant().toEpochMilli()}"
}
