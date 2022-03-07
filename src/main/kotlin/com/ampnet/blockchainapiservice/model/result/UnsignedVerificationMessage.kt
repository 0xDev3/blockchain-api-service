package com.ampnet.blockchainapiservice.model.result

import com.ampnet.blockchainapiservice.util.WalletAddress
import java.time.Duration
import java.time.OffsetDateTime
import java.util.UUID

data class UnsignedVerificationMessage(
    val id: UUID,
    val walletAddress: WalletAddress,
    val createdAt: OffsetDateTime,
    val validUntil: OffsetDateTime
) {
    constructor(id: UUID, walletAddress: WalletAddress, createdAt: OffsetDateTime, validityDuration: Duration) :
        this(id, walletAddress, createdAt, createdAt.plus(validityDuration))

    fun isValid(now: OffsetDateTime): Boolean {
        return now.isBefore(validUntil)
    }
}
