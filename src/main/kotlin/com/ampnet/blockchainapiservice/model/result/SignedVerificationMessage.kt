package com.ampnet.blockchainapiservice.model.result

import com.ampnet.blockchainapiservice.util.UtcDateTime
import com.ampnet.blockchainapiservice.util.WalletAddress
import java.util.UUID

data class SignedVerificationMessage(
    val id: UUID,
    val walletAddress: WalletAddress,
    val signature: String,
    val createdAt: UtcDateTime,
    val verifiedAt: UtcDateTime,
    val validUntil: UtcDateTime
) {
    fun isExpired(now: UtcDateTime): Boolean {
        return now.isAfter(validUntil)
    }
}
