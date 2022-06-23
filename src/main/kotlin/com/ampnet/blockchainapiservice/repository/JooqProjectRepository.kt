package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.generated.jooq.tables.ProjectTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.interfaces.IProjectRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ProjectRecord
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.util.ContractAddress
import mu.KLogging
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JooqProjectRepository(private val dslContext: DSLContext) : ProjectRepository {

    companion object : KLogging()

    override fun store(project: Project): Project {
        logger.info { "Store project: $project" }
        val record = ProjectRecord(
            id = project.id,
            ownerId = project.ownerId,
            issuerContractAddress = project.issuerContractAddress,
            redirectUrl = project.redirectUrl,
            chainId = project.chainId,
            customRpcUrl = project.customRpcUrl,
            createdAt = project.createdAt
        )
        dslContext.executeInsert(record)
        return record.toModel()
    }

    override fun getById(id: UUID): Project? {
        logger.debug { "Get project by id: $id" }
        return dslContext.selectFrom(ProjectTable.PROJECT)
            .where(ProjectTable.PROJECT.ID.eq(id))
            .fetchOne { it.toModel() }
    }

    override fun getByIssuerContractAddress(issuerContractAddress: ContractAddress): Project? {
        logger.debug { "Get project by issuerContractAddress: $issuerContractAddress" }
        return dslContext.selectFrom(ProjectTable.PROJECT)
            .where(ProjectTable.PROJECT.ISSUER_CONTRACT_ADDRESS.eq(issuerContractAddress))
            .fetchOne { it.toModel() }
    }

    override fun getAllByOwnerId(ownerId: UUID): List<Project> {
        logger.info { "Get projects by ownerId: $ownerId" }
        return dslContext.selectFrom(ProjectTable.PROJECT)
            .where(ProjectTable.PROJECT.OWNER_ID.eq(ownerId))
            .orderBy(ProjectTable.PROJECT.CREATED_AT.asc())
            .fetch { it.toModel() }
    }

    private fun IProjectRecord.toModel(): Project =
        Project(
            id = id!!,
            ownerId = ownerId!!,
            issuerContractAddress = issuerContractAddress!!,
            redirectUrl = redirectUrl!!,
            chainId = chainId!!,
            customRpcUrl = customRpcUrl,
            createdAt = createdAt!!
        )
}
