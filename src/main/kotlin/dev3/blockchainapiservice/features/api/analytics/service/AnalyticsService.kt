package dev3.blockchainapiservice.features.api.analytics.service

import dev3.blockchainapiservice.features.api.access.model.result.UserIdentifier
import dev3.blockchainapiservice.generated.jooq.id.ProjectId

interface AnalyticsService {
    fun postApiKeyCreatedEvent(
        userIdentifier: UserIdentifier,
        projectId: ProjectId,
        origin: String?,
        userAgent: String?,
        remoteAddr: String?
    )
}
