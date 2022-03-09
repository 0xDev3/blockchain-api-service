package com.ampnet.blockchainapiservice.model.response

import java.time.OffsetDateTime
import java.util.UUID

data class GenerateVerificationMessageResponse(
    val id: UUID,
    val message: String,
    val validUntil: OffsetDateTime
)
