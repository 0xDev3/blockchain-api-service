package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.exception.DuplicateIssuerContractAddressException
import com.ampnet.blockchainapiservice.generated.jooq.tables.ProjectTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ProjectRecord
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import mu.KLogging
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JooqProjectRepository(private val dslContext: DSLContext) : ProjectRepository {

    companion object : KLogging() {
        private val TABLE = ProjectTable.PROJECT
    }

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

    override fun getById(id: UUID): Project? {
        logger.debug { "Get project by id: $id" }
        return dslContext.selectFrom(TABLE)
            .where(TABLE.ID.eq(id))
            .fetchOne { it.toModel() }
    }

    override fun getByIssuer(issuerContractAddress: ContractAddress, chainId: ChainId): Project? {
        logger.debug { "Get project by issuerContractAddress: $issuerContractAddress, chainId: $chainId" }
        return dslContext.selectFrom(TABLE)
            .where(
                DSL.and(
                    TABLE.ISSUER_CONTRACT_ADDRESS.eq(issuerContractAddress),
                    TABLE.CHAIN_ID.eq(chainId)
                )
            )
            .fetchOne { it.toModel() }
    }

    override fun getAllByOwnerId(ownerId: UUID): List<Project> {
        logger.info { "Get projects by ownerId: $ownerId" }
        return dslContext.selectFrom(TABLE)
            .where(TABLE.OWNER_ID.eq(ownerId))
            .orderBy(TABLE.CREATED_AT.asc())
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
