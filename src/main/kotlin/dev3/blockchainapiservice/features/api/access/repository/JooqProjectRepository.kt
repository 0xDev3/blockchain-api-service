package dev3.blockchainapiservice.features.api.access.repository

import dev3.blockchainapiservice.exception.DuplicateIssuerContractAddressException
import dev3.blockchainapiservice.features.api.access.model.result.Project
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.generated.jooq.id.UserId
import dev3.blockchainapiservice.generated.jooq.tables.ProjectTable
import dev3.blockchainapiservice.generated.jooq.tables.records.ProjectRecord
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import mu.KLogging
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Repository

@Repository
class JooqProjectRepository(private val dslContext: DSLContext) : ProjectRepository {

    companion object : KLogging()

    override fun store(project: Project): Project {
        logger.info { "Store project: $project" }
        val record = ProjectRecord(
            id = project.id,
            ownerId = project.ownerId,
            issuerContractAddress = project.issuerContractAddress,
            baseRedirectUrl = project.baseRedirectUrl,
            chainId = project.chainId,
            customRpcUrl = project.customRpcUrl,
            createdAt = project.createdAt
        )

        try {
            dslContext.executeInsert(record)
        } catch (e: DuplicateKeyException) {
            throw DuplicateIssuerContractAddressException(project.issuerContractAddress, project.chainId)
        }

        return record.toModel()
    }

    override fun getById(id: ProjectId): Project? {
        logger.debug { "Get project by id: $id" }
        return dslContext.selectFrom(ProjectTable)
            .where(ProjectTable.ID.eq(id))
            .fetchOne { it.toModel() }
    }

    override fun getByIssuer(issuerContractAddress: ContractAddress, chainId: ChainId): Project? {
        logger.debug { "Get project by issuerContractAddress: $issuerContractAddress, chainId: $chainId" }
        return dslContext.selectFrom(ProjectTable)
            .where(
                DSL.and(
                    ProjectTable.ISSUER_CONTRACT_ADDRESS.eq(issuerContractAddress),
                    ProjectTable.CHAIN_ID.eq(chainId)
                )
            )
            .fetchOne { it.toModel() }
    }

    override fun getAllByOwnerId(ownerId: UserId): List<Project> {
        logger.info { "Get projects by ownerId: $ownerId" }
        return dslContext.selectFrom(ProjectTable)
            .where(ProjectTable.OWNER_ID.eq(ownerId))
            .orderBy(ProjectTable.CREATED_AT.asc())
            .fetch { it.toModel() }
    }

    private fun ProjectRecord.toModel(): Project =
        Project(
            id = id,
            ownerId = ownerId,
            issuerContractAddress = issuerContractAddress,
            baseRedirectUrl = baseRedirectUrl,
            chainId = chainId,
            customRpcUrl = customRpcUrl,
            createdAt = createdAt
        )
}
