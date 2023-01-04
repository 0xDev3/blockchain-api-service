package dev3.blockchainapiservice.features.api.access.repository

import dev3.blockchainapiservice.features.api.access.model.result.Project
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.generated.jooq.id.UserId
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress

interface ProjectRepository {
    fun store(project: Project): Project
    fun getById(id: ProjectId): Project?
    fun getByIssuer(issuerContractAddress: ContractAddress, chainId: ChainId): Project?
    fun getAllByOwnerId(ownerId: UserId): List<Project>
}
