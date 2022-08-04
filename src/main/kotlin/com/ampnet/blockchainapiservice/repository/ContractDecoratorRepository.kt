package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.model.filters.ContractDecoratorFilters
import com.ampnet.blockchainapiservice.model.json.ArtifactJson
import com.ampnet.blockchainapiservice.model.json.ManifestJson
import com.ampnet.blockchainapiservice.model.result.ContractDecorator
import com.ampnet.blockchainapiservice.util.ContractId

interface ContractDecoratorRepository {
    fun store(contractDecorator: ContractDecorator): ContractDecorator
    fun store(id: ContractId, manifestJson: ManifestJson): ManifestJson
    fun store(id: ContractId, artifactJson: ArtifactJson): ArtifactJson
    fun delete(id: ContractId): Boolean
    fun getById(id: ContractId): ContractDecorator?
    fun getManifestJsonById(id: ContractId): ManifestJson?
    fun getArtifactJsonById(id: ContractId): ArtifactJson?
    fun getAll(filters: ContractDecoratorFilters): List<ContractDecorator>
    fun getAllManifestJsonFiles(filters: ContractDecoratorFilters): List<ManifestJson>
    fun getAllArtifactJsonFiles(filters: ContractDecoratorFilters): List<ArtifactJson>
}
