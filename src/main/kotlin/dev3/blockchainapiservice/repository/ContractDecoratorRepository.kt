package dev3.blockchainapiservice.repository

import dev3.blockchainapiservice.model.filters.ContractDecoratorFilters
import dev3.blockchainapiservice.model.json.ArtifactJson
import dev3.blockchainapiservice.model.json.ManifestJson
import dev3.blockchainapiservice.model.result.ContractDecorator
import dev3.blockchainapiservice.util.ContractId

@Suppress("TooManyFunctions")
interface ContractDecoratorRepository {
    fun store(contractDecorator: ContractDecorator): ContractDecorator
    fun store(id: ContractId, manifestJson: ManifestJson): ManifestJson
    fun store(id: ContractId, artifactJson: ArtifactJson): ArtifactJson
    fun store(id: ContractId, infoMd: String): String
    fun delete(id: ContractId): Boolean
    fun getById(id: ContractId): ContractDecorator?
    fun getManifestJsonById(id: ContractId): ManifestJson?
    fun getArtifactJsonById(id: ContractId): ArtifactJson?
    fun getInfoMarkdownById(id: ContractId): String?
    fun getAll(filters: ContractDecoratorFilters): List<ContractDecorator>
    fun getAllManifestJsonFiles(filters: ContractDecoratorFilters): List<ManifestJson>
    fun getAllArtifactJsonFiles(filters: ContractDecoratorFilters): List<ArtifactJson>
    fun getAllInfoMarkdownFiles(filters: ContractDecoratorFilters): List<String>
}
