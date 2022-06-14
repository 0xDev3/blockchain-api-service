package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.model.result.Project
import java.util.UUID

interface ProjectRepository {
    fun store(project: Project): Project
    fun getById(id: UUID): Project?
}
