package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.model.result.Project
import java.util.UUID

interface ProjectRepository {
    fun getById(id: UUID): Project?
    fun create(project: Project): Project
}
