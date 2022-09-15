package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.generated.jooq.tables.ImportedContractDecoratorTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ImportedContractDecoratorRecord
import com.ampnet.blockchainapiservice.model.json.ArtifactJson
import com.ampnet.blockchainapiservice.model.json.ManifestJson
import com.ampnet.blockchainapiservice.model.result.ContractDecorator
import com.ampnet.blockchainapiservice.util.ContractId
import mu.KLogging
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JooqImportedContractDecoratorRepository(
    private val dslContext: DSLContext
) : ImportedContractDecoratorRepository {

    companion object : KLogging()

    override fun store(
        id: UUID,
        contractId: ContractId,
        manifestJson: ManifestJson,
        artifactJson: ArtifactJson,
        infoMarkdown: String
    ): ContractDecorator {
        logger.info { "Store imported contract decorator, id: $id, contractId: $contractId" }
        val record = ImportedContractDecoratorRecord(
            id = id,
            contractId = contractId,
            manifestJson = manifestJson,
            artifactJson = artifactJson,
            infoMarkdown = infoMarkdown
        )
        dslContext.executeInsert(record)
        return ContractDecorator(contractId, artifactJson, manifestJson)
    }

    override fun getByContractId(contractId: ContractId): ContractDecorator? {
        logger.debug { "Get imported contract decorator by contract id: $contractId" }
        return dslContext.selectFrom(ImportedContractDecoratorTable.IMPORTED_CONTRACT_DECORATOR)
            .where(ImportedContractDecoratorTable.IMPORTED_CONTRACT_DECORATOR.CONTRACT_ID.eq(contractId))
            .fetchOne()
            ?.let {
                ContractDecorator(
                    id = it.contractId!!,
                    artifact = it.artifactJson!!,
                    manifest = it.manifestJson!!
                )
            }
    }
}
