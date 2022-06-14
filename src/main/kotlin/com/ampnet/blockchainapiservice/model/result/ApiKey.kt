package com.ampnet.blockchainapiservice.model.result

import java.util.UUID

data class ApiKey(
    val id: UUID,
    val projectId: UUID,
    val apiKey: String
)
