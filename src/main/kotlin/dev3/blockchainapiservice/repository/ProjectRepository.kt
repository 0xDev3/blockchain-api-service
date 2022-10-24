package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import java.util.UUID

interface ProjectRepository {
    fun store(project: Project): Project
    fun getById(id: UUID): Project?
    fun getByIssuer(issuerContractAddress: ContractAddress, chainId: ChainId): Project?
    fun getAllByOwnerId(ownerId: UUID): List<Project>
}
