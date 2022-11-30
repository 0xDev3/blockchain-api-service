package dev3.blockchainapiservice.repository

import dev3.blockchainapiservice.config.interceptors.annotation.IdType
import java.util.UUID

interface ProjectIdResolverRepository {
    fun getProjectId(idType: IdType, id: UUID): UUID?
}
