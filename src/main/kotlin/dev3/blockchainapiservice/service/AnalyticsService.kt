package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.model.result.UserIdentifier
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
