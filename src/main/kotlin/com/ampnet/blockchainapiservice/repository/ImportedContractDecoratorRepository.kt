package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.model.json.ArtifactJson
import com.ampnet.blockchainapiservice.model.json.ManifestJson
import com.ampnet.blockchainapiservice.model.result.ContractDecorator
import com.ampnet.blockchainapiservice.util.ContractId
import java.util.UUID

interface ImportedContractDecoratorRepository {
    fun store(
        id: UUID,
        contractId: ContractId,
        manifestJson: ManifestJson,
        artifactJson: ArtifactJson,
        infoMarkdown: String
    ): ContractDecorator

    fun getByContractId(contractId: ContractId): ContractDecorator?
}
