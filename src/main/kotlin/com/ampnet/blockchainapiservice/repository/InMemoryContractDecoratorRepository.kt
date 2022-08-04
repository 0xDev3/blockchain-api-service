package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.model.filters.AndList
import com.ampnet.blockchainapiservice.model.filters.ContractDecoratorFilters
import com.ampnet.blockchainapiservice.model.filters.OrList
import com.ampnet.blockchainapiservice.model.json.ArtifactJson
import com.ampnet.blockchainapiservice.model.json.ManifestJson
import com.ampnet.blockchainapiservice.model.result.ContractDecorator
import com.ampnet.blockchainapiservice.util.ContractId
import mu.KLogging
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

@Repository
@Suppress("TooManyFunctions")
class InMemoryContractDecoratorRepository : ContractDecoratorRepository {

    companion object : KLogging()

    private val storage = ConcurrentHashMap<ContractId, ContractDecorator>()
    private val manifestJsonStorage = ConcurrentHashMap<ContractId, ManifestJson>()
    private val artifactJsonStorage = ConcurrentHashMap<ContractId, ArtifactJson>()

    override fun store(contractDecorator: ContractDecorator): ContractDecorator {
        logger.info { "Storing contract decorator with ID: ${contractDecorator.id}" }
        storage[contractDecorator.id] = contractDecorator
        return contractDecorator
    }

    override fun store(id: ContractId, manifestJson: ManifestJson): ManifestJson {
        logger.info { "Storing contract manifest.json with ID: $id" }
        manifestJsonStorage[id] = manifestJson
        return manifestJson
    }

    override fun store(id: ContractId, artifactJson: ArtifactJson): ArtifactJson {
        logger.info { "Storing contract artifact.json with ID: $id" }
        artifactJsonStorage[id] = artifactJson
        return artifactJson
    }

    override fun delete(id: ContractId): Boolean {
        logger.info { "Deleting contract decorator with ID: $id" }
        manifestJsonStorage.remove(id)
        artifactJsonStorage.remove(id)
        return storage.remove(id) != null
    }

    override fun getById(id: ContractId): ContractDecorator? {
        logger.debug { "Get contract decorator by ID: $id" }
        return storage[id]
    }

    override fun getManifestJsonById(id: ContractId): ManifestJson? {
        logger.debug { "Get contract manifest.json by ID: $id" }
        return manifestJsonStorage[id]
    }

    override fun getArtifactJsonById(id: ContractId): ArtifactJson? {
        logger.debug { "Get contract artifact.json by ID: $id" }
        return artifactJsonStorage[id]
    }

    override fun getAll(filters: ContractDecoratorFilters): List<ContractDecorator> {
        logger.debug { "Get all contract decorators, filters: $filters" }
        return storage.values
            .filterBy(filters.contractTags) { it.tags }
            .filterBy(filters.contractImplements) { it.implements }
            .toList()
    }

    override fun getAllManifestJsonFiles(filters: ContractDecoratorFilters): List<ManifestJson> {
        logger.debug { "Get all contract manifest.json files, filters: $filters" }
        return getAll(filters).mapNotNull { manifestJsonStorage[it.id] }
    }

    override fun getAllArtifactJsonFiles(filters: ContractDecoratorFilters): List<ArtifactJson> {
        logger.debug { "Get all contract artifact.json files, filters: $filters" }
        return getAll(filters).mapNotNull { artifactJsonStorage[it.id] }
    }

    private fun <T> Collection<ContractDecorator>.filterBy(
        orList: OrList<AndList<T>>,
        values: (ContractDecorator) -> List<T>
    ): Collection<ContractDecorator> {
        val conditions = orList.list.map { it.list }

        return if (conditions.isEmpty()) {
            this
        } else {
            filter { decorator ->
                val decoratorValues = values(decorator)
                conditions.map { condition -> decoratorValues.containsAll(condition) }.contains(true)
            }
        }
    }
}
