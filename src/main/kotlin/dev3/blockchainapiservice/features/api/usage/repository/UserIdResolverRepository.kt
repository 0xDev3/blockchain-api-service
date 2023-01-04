package dev3.blockchainapiservice.features.api.usage.repository

import dev3.blockchainapiservice.config.interceptors.annotation.IdType
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.generated.jooq.id.UserId
import java.util.UUID

interface UserIdResolverRepository {
    fun getByProjectId(projectId: ProjectId): UserId?
    fun getUserId(idType: IdType, id: UUID): UserId?
}
