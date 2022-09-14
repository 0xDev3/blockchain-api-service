package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.model.result.UserIdentifier
import java.util.UUID

interface AnalyticsService {
    fun postApiKeyCreatedEvent(
        userIdentifier: UserIdentifier,
        projectId: UUID,
        origin: String?,
        userAgent: String?,
        remoteAddr: String?
    )
}
