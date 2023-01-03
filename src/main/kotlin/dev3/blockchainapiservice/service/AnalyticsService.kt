package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.model.result.UserIdentifier

interface AnalyticsService {
    fun postApiKeyCreatedEvent(
        userIdentifier: UserIdentifier,
        projectId: ProjectId,
        origin: String?,
        userAgent: String?,
        remoteAddr: String?
    )
}
