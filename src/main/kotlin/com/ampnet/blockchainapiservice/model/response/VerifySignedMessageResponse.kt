package com.ampnet.blockchainapiservice.model.response

import com.ampnet.blockchainapiservice.util.UtcDateTime
import java.util.UUID

data class VerifySignedMessageResponse(
    val id: UUID,
    val signature: String,
    val validUntil: UtcDateTime
)
