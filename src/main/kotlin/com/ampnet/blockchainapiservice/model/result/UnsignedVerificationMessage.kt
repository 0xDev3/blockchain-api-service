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

    fun isValid(now: UtcDateTime): Boolean {
        return now.isBefore(validUntil)
    }

    fun toSignedMessage(signature: String, now: UtcDateTime, validityDuration: Duration): SignedVerificationMessage =
        SignedVerificationMessage(
            id = id,
            walletAddress = walletAddress,
            signature = signature,
            createdAt = createdAt,
            verifiedAt = now,
            validUntil = now + validityDuration
        )

    fun toStringMessage(): String =
        "By signing this message, I verify that I'm the owner of wallet address ${walletAddress.rawValue}." +
            " Message ID: $id, timestamp: ${createdAt.value.toInstant().toEpochMilli()}"
}
