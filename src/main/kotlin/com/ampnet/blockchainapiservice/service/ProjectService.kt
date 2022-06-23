package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.model.params.CreateProjectParams
import com.ampnet.blockchainapiservice.model.result.ApiKey
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.model.result.UserIdentifier
import com.ampnet.blockchainapiservice.util.ContractAddress
import java.util.UUID

interface ProjectService {
    fun createProject(userIdentifier: UserIdentifier, params: CreateProjectParams): Project
    fun getProjectById(userIdentifier: UserIdentifier, id: UUID): Project
    fun getProjectByIssuerAddress(userIdentifier: UserIdentifier, issuerContactAddress: ContractAddress): Project
    fun getAllProjectsForUser(userIdentifier: UserIdentifier): List<Project>
    fun getProjectApiKeys(userIdentifier: UserIdentifier, projectId: UUID): List<ApiKey>
    fun createApiKey(userIdentifier: UserIdentifier, projectId: UUID): ApiKey
}
