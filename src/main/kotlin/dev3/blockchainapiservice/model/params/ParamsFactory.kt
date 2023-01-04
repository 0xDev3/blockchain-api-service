package dev3.blockchainapiservice.model.params

import dev3.blockchainapiservice.features.api.access.model.result.Project
import dev3.blockchainapiservice.util.UtcDateTime
import java.util.UUID

interface ParamsFactory<P, R> {
    fun fromCreateParams(id: UUID, params: P, project: Project, createdAt: UtcDateTime): R
}
