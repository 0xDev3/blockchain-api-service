package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.generated.jooq.tables.ContractMetadataTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ContractMetadataRecord
import com.ampnet.blockchainapiservice.util.ContractId
import com.ampnet.blockchainapiservice.util.ContractTag
import com.ampnet.blockchainapiservice.util.ContractTrait
import mu.KLogging
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JooqContractMetadataRepository(private val dslContext: DSLContext) : ContractMetadataRepository {

    companion object : KLogging()

    override fun createOrUpdate(
        id: UUID,
        name: String?,
        description: String?,
        contractId: ContractId,
        contractTags: List<ContractTag>,
        contractImplements: List<ContractTrait>
    ): Boolean {
        logger.info {
            "Create or update contract metadata, id: $id, name: $name, description: $description," +
                " contractId: $contractId, contractTags: $contractTags, contractImplements: $contractImplements"
        }
        val tags: Array<String?> = contractTags.map { it.value }.toTypedArray()
        val implements: Array<String?> = contractImplements.map { it.value }.toTypedArray()

        return dslContext.insertInto(ContractMetadataTable.CONTRACT_METADATA)
            .set(
                ContractMetadataRecord(
                    id = id,
                    name = name,
                    description = description,
                    contractId = contractId,
                    contractTags = tags,
                    contractImplements = implements
                )
            )
            .onConflict(ContractMetadataTable.CONTRACT_METADATA.CONTRACT_ID)
            .doUpdate()
            .set(ContractMetadataTable.CONTRACT_METADATA.CONTRACT_TAGS, tags)
            .set(ContractMetadataTable.CONTRACT_METADATA.CONTRACT_IMPLEMENTS, implements)
            .execute() > 0
    }

    override fun exists(contractId: ContractId): Boolean {
        logger.debug { "Check if contract metadata exists: $contractId" }
        return dslContext.fetchExists(
            ContractMetadataTable.CONTRACT_METADATA,
            ContractMetadataTable.CONTRACT_METADATA.CONTRACT_ID.eq(contractId)
        )
    }
}
