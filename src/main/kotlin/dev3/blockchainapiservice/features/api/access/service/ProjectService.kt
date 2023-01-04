package dev3.blockchainapiservice.features.api.access.service

import dev3.blockchainapiservice.features.api.access.model.params.CreateProjectParams
import dev3.blockchainapiservice.features.api.access.model.result.ApiKey
import dev3.blockchainapiservice.features.api.access.model.result.Project
import dev3.blockchainapiservice.features.api.access.model.result.UserIdentifier
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress

interface ProjectService {
    fun createProject(userIdentifier: UserIdentifier, params: CreateProjectParams): Project
    fun getProjectById(userIdentifier: UserIdentifier, id: ProjectId): Project
    fun getProjectByIssuer(
        userIdentifier: UserIdentifier,
        issuerContactAddress: ContractAddress,
        chainId: ChainId
    ): Project

    fun getAllProjectsForUser(userIdentifier: UserIdentifier): List<Project>
    fun getProjectApiKeys(userIdentifier: UserIdentifier, projectId: ProjectId): List<ApiKey>
    fun createApiKey(userIdentifier: UserIdentifier, projectId: ProjectId): ApiKey
}
