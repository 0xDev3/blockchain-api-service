package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.generated.jooq.tables.ContractMetadataTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ContractMetadataRecord
import com.ampnet.blockchainapiservice.model.result.ContractMetadata
import com.ampnet.blockchainapiservice.util.ContractId
import com.ampnet.blockchainapiservice.util.InterfaceId
import mu.KLogging
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JooqContractMetadataRepository(private val dslContext: DSLContext) : ContractMetadataRepository {

    companion object : KLogging()

    override fun createOrUpdate(contractMetadata: ContractMetadata): Boolean {
        logger.info { "Create or update contract metadata: $contractMetadata" }
        val tags = contractMetadata.contractTags.map { it.value }.toTypedArray()
        val implements = contractMetadata.contractImplements.map { it.value }.toTypedArray()

        return dslContext.insertInto(ContractMetadataTable)
            .set(
                ContractMetadataRecord(
                    id = contractMetadata.id,
                    contractId = contractMetadata.contractId,
                    contractTags = tags,
                    contractImplements = implements,
                    name = contractMetadata.name,
                    description = contractMetadata.description,
                    projectId = contractMetadata.projectId
                )
            )
            .onConflict(ContractMetadataTable.CONTRACT_ID, ContractMetadataTable.PROJECT_ID)
            .doUpdate()
            .set(ContractMetadataTable.NAME, contractMetadata.name)
            .set(ContractMetadataTable.DESCRIPTION, contractMetadata.description)
            .set(ContractMetadataTable.CONTRACT_TAGS, tags)
            .set(ContractMetadataTable.CONTRACT_IMPLEMENTS, implements)
            .execute() > 0
    }

    override fun updateInterfaces(contractId: ContractId, projectId: UUID, interfaces: List<InterfaceId>): Boolean {
        logger.info {
            "Update contract metadata interfaces, contractId: $contractId, projectId: $projectId," +
                " interfaces: $interfaces"
        }

        return dslContext.update(ContractMetadataTable)
            .set(ContractMetadataTable.CONTRACT_IMPLEMENTS, interfaces.map { it.value }.toTypedArray())
            .where(
                DSL.and(
                    ContractMetadataTable.CONTRACT_ID.eq(contractId),
                    ContractMetadataTable.PROJECT_ID.eq(projectId)
                )
            )
            .execute() > 0
    }

    override fun exists(contractId: ContractId, projectId: UUID): Boolean {
        logger.debug { "Check if contract metadata exists, contractId: $contractId, projectId: $projectId" }
        return dslContext.fetchExists(
            ContractMetadataTable,
            DSL.and(
                ContractMetadataTable.CONTRACT_ID.eq(contractId),
                ContractMetadataTable.PROJECT_ID.eq(projectId)
            )
        )
    }
}
