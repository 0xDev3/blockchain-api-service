package dev3.blockchainapiservice.repository

import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import java.util.UUID

interface ProjectRepository {
    fun store(project: Project): Project
    fun getById(id: UUID): Project?
    fun getByIssuer(issuerContractAddress: ContractAddress, chainId: ChainId): Project?
    fun getAllByOwnerId(ownerId: UUID): List<Project>
}
