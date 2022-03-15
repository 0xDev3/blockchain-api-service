package com.ampnet.blockchainapiservice.model.response

import java.time.OffsetDateTime
import java.util.UUID

data class VerifySignedMessageResponse(
    val id: UUID,
    val signature: String,
    val validUntil: OffsetDateTime
)
