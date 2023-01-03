package dev3.blockchainapiservice.repository

import dev3.blockchainapiservice.generated.jooq.id.ImportedContractDecoratorId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.model.filters.ContractDecoratorFilters
import dev3.blockchainapiservice.model.json.ArtifactJson
import dev3.blockchainapiservice.model.json.ManifestJson
import dev3.blockchainapiservice.model.result.ContractDecorator
import dev3.blockchainapiservice.util.ContractId
import dev3.blockchainapiservice.util.InterfaceId
import dev3.blockchainapiservice.util.UtcDateTime

interface ImportedContractDecoratorRepository {
    @Suppress("LongParameterList")
    fun store(
        id: ImportedContractDecoratorId,
        projectId: ProjectId,
        contractId: ContractId,
        manifestJson: ManifestJson,
        artifactJson: ArtifactJson,
        infoMarkdown: String,
        importedAt: UtcDateTime,
        previewOnly: Boolean
    ): ContractDecorator

    fun updateInterfaces(
        contractId: ContractId,
        projectId: ProjectId,
        interfaces: List<InterfaceId>,
        manifest: ManifestJson
    ): Boolean

    fun getByContractIdAndProjectId(contractId: ContractId, projectId: ProjectId): ContractDecorator?
    fun getManifestJsonByContractIdAndProjectId(contractId: ContractId, projectId: ProjectId): ManifestJson?
    fun getArtifactJsonByContractIdAndProjectId(contractId: ContractId, projectId: ProjectId): ArtifactJson?
    fun getInfoMarkdownByContractIdAndProjectId(contractId: ContractId, projectId: ProjectId): String?
    fun getAll(projectId: ProjectId, filters: ContractDecoratorFilters): List<ContractDecorator>
    fun getAllManifestJsonFiles(projectId: ProjectId, filters: ContractDecoratorFilters): List<ManifestJson>
    fun getAllArtifactJsonFiles(projectId: ProjectId, filters: ContractDecoratorFilters): List<ArtifactJson>
    fun getAllInfoMarkdownFiles(projectId: ProjectId, filters: ContractDecoratorFilters): List<String>
}
