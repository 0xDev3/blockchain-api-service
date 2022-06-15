package com.ampnet.blockchainapiservice.model.result

import com.ampnet.blockchainapiservice.util.UtcDateTime
import java.util.UUID

data class ApiKey(
    val id: UUID,
    val projectId: UUID,
    val apiKey: String,
    val createdAt: UtcDateTime
)
