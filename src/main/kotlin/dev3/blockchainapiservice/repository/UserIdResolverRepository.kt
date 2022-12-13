package dev3.blockchainapiservice.repository

import dev3.blockchainapiservice.config.interceptors.annotation.IdType
import java.util.UUID

interface UserIdResolverRepository {
    fun getUserId(idType: IdType, id: UUID): UUID?
}
