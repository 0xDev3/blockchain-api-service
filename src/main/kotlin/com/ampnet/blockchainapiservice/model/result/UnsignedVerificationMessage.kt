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
}
