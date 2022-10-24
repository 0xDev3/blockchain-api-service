package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.model.filters.ContractDecoratorFilters
import com.ampnet.blockchainapiservice.model.json.ArtifactJson
import com.ampnet.blockchainapiservice.model.json.ManifestJson
import com.ampnet.blockchainapiservice.model.result.ContractDecorator
import com.ampnet.blockchainapiservice.util.ContractId
import com.ampnet.blockchainapiservice.util.InterfaceId
import com.ampnet.blockchainapiservice.util.UtcDateTime
import java.util.UUID

interface ImportedContractDecoratorRepository {
    @Suppress("LongParameterList")
    fun store(
        id: UUID,
        projectId: UUID,
        contractId: ContractId,
        manifestJson: ManifestJson,
        artifactJson: ArtifactJson,
        infoMarkdown: String,
        importedAt: UtcDateTime
    ): ContractDecorator

    fun updateInterfaces(
        contractId: ContractId,
        projectId: UUID,
        interfaces: List<InterfaceId>,
        manifest: ManifestJson
    ): Boolean

    fun getByContractIdAndProjectId(contractId: ContractId, projectId: UUID): ContractDecorator?
    fun getManifestJsonByContractIdAndProjectId(contractId: ContractId, projectId: UUID): ManifestJson?
    fun getArtifactJsonByContractIdAndProjectId(contractId: ContractId, projectId: UUID): ArtifactJson?
    fun getInfoMarkdownByContractIdAndProjectId(contractId: ContractId, projectId: UUID): String?
    fun getAll(projectId: UUID, filters: ContractDecoratorFilters): List<ContractDecorator>
    fun getAllManifestJsonFiles(projectId: UUID, filters: ContractDecoratorFilters): List<ManifestJson>
    fun getAllArtifactJsonFiles(projectId: UUID, filters: ContractDecoratorFilters): List<ArtifactJson>
    fun getAllInfoMarkdownFiles(projectId: UUID, filters: ContractDecoratorFilters): List<String>
}
