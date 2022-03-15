package com.ampnet.blockchainapiservice

import com.ampnet.blockchainapiservice.model.result.UnsignedVerificationMessage
import com.ampnet.blockchainapiservice.util.UtcDateTime
import com.ampnet.blockchainapiservice.util.WalletAddress
import java.time.Duration
import java.time.OffsetDateTime
import java.util.UUID

object TestData {

    val UNSIGNED_MESSAGE = UnsignedVerificationMessage(
        id = UUID.fromString("7d86b0ac-a9a6-40fc-ac6d-2a29ca687f73"),
        walletAddress = WalletAddress("0x865f603F42ca1231e5B5F90e15663b0FE19F0b21"),
        createdAt = UtcDateTime(OffsetDateTime.parse("2022-01-01T00:00:00Z")),
        validUntil = UtcDateTime(OffsetDateTime.parse("2022-01-01T02:00:00Z"))
    )

    // this message was signed using Metamask
    val SIGNED_MESSAGE = UNSIGNED_MESSAGE.toSignedMessage(
        signature = "0x2601a91eed301102ca423ffc36e43b4dc096bb556ecfb83f508047b34ab7236f4cd1eaaae98ee8eac9cde62988f062" +
            "891f3c84e241d320cf338bdfc17a51bc131b",
        now = UtcDateTime(OffsetDateTime.parse("2022-01-01T01:00:00Z")),
        validityDuration = Duration.ofHours(1L)
    )

    const val TOO_SHORT_SIGNATURE = "0x"
    const val INVALID_SIGNATURE = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
        "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"

    // also signed using Metamask, but using another address
    const val OTHER_SIGNATURE = "0x4f6e4a316147dcc797a89cf6163643a5f0315dbed5fbf8976f2af1ada260468c53411db1d501550f17" +
        "1463322dbcf8427c9b34ff40513bec90d46b75b910b7c71b"
}
