package dev3.blockchainapiservice.repository

import dev3.blockchainapiservice.model.filters.ContractDecoratorFilters
import dev3.blockchainapiservice.model.json.ArtifactJson
import dev3.blockchainapiservice.model.json.ManifestJson
import dev3.blockchainapiservice.model.result.ContractDecorator
import dev3.blockchainapiservice.util.ContractId
import dev3.blockchainapiservice.util.InterfaceId
import dev3.blockchainapiservice.util.UtcDateTime
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
