package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.generated.jooq.tables.ContractMetadataTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ContractMetadataRecord
import com.ampnet.blockchainapiservice.model.result.ContractMetadata
import com.ampnet.blockchainapiservice.util.ContractId
import mu.KLogging
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JooqContractMetadataRepository(private val dslContext: DSLContext) : ContractMetadataRepository {

    companion object : KLogging() {
        private val TABLE = ContractMetadataTable.CONTRACT_METADATA
    }

    override fun createOrUpdate(contractMetadata: ContractMetadata): Boolean {
        logger.info { "Create or update contract metadata: $contractMetadata" }
        val tags = contractMetadata.contractTags.map { it.value }.toTypedArray()
        val implements = contractMetadata.contractImplements.map { it.value }.toTypedArray()

        return dslContext.insertInto(TABLE)
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
            .onConflict(TABLE.CONTRACT_ID, TABLE.PROJECT_ID)
            .doUpdate()
            .set(TABLE.NAME, contractMetadata.name)
            .set(TABLE.DESCRIPTION, contractMetadata.description)
            .set(TABLE.CONTRACT_TAGS, tags)
            .set(TABLE.CONTRACT_IMPLEMENTS, implements)
            .execute() > 0
    }

    override fun exists(contractId: ContractId, projectId: UUID): Boolean {
        logger.debug { "Check if contract metadata exists, contractId: $contractId, projectId: $projectId" }
        return dslContext.fetchExists(
            TABLE,
            DSL.and(
                TABLE.CONTRACT_ID.eq(contractId),
                TABLE.PROJECT_ID.eq(projectId)
            )
        )
    }
}
