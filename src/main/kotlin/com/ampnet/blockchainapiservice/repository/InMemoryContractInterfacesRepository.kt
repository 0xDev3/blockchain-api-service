package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.model.json.InterfaceManifestJson
import com.ampnet.blockchainapiservice.model.json.InterfaceManifestJsonWithId
import com.ampnet.blockchainapiservice.model.json.OverridableDecorator
import com.ampnet.blockchainapiservice.util.InterfaceId
import mu.KLogging
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

@Repository
class InMemoryContractInterfacesRepository : ContractInterfacesRepository {

    companion object : KLogging()

    private val storage = ConcurrentHashMap<InterfaceId, InterfaceManifestJson>()
    private val infoMarkdownStorage = ConcurrentHashMap<InterfaceId, String>()

    override fun store(id: InterfaceId, interfaceManifestJson: InterfaceManifestJson): InterfaceManifestJson {
        logger.info { "Storing contract interface with ID: $id" }
        storage[id] = interfaceManifestJson
        return interfaceManifestJson
    }

    override fun store(id: InterfaceId, infoMd: String): String {
        logger.info { "Storing contract interface info.md with ID: $id" }
        infoMarkdownStorage[id] = infoMd
        return infoMd
    }

    override fun delete(id: InterfaceId): Boolean {
        logger.info { "Deleting contract interface with ID: $id" }
        infoMarkdownStorage.remove(id)
        return storage.remove(id) != null
    }

    override fun getById(id: InterfaceId): InterfaceManifestJson? {
        logger.debug { "Get contract interface by ID: $id" }
        return storage[id]
    }

    override fun getInfoMarkdownById(id: InterfaceId): String? {
        logger.debug { "Get contract interface info.md by ID: $id" }
        return infoMarkdownStorage[id]
    }

    override fun getAll(): List<InterfaceManifestJsonWithId> {
        logger.debug { "Get all contract interfaces" }
        return storage.entries.map {
            InterfaceManifestJsonWithId(
                id = it.key,
                name = it.value.name,
                description = it.value.description,
                eventDecorators = it.value.eventDecorators,
                functionDecorators = it.value.functionDecorators
            )
        }
    }

    override fun getAllInfoMarkdownFiles(): List<String> {
        logger.debug { "Get all contract interface info.md files" }
        return infoMarkdownStorage.values.toList()
    }

    override fun getAllWithPartiallyMatchingInterfaces(
        abiFunctionSignatures: Set<String>,
        abiEventSignatures: Set<String>
    ): List<InterfaceManifestJsonWithId> {
        logger.debug { "Get all partially matching contract interfaces" }
        return storage.entries.mapNotNull {
            val id = it.key
            val interfaceDecorator = it.value
            val matchingFunctions = findMatches(interfaceDecorator.functionDecorators, abiFunctionSignatures)
            val matchingEvents = findMatches(interfaceDecorator.eventDecorators, abiEventSignatures)

            matchingFunctions?.let {
                matchingEvents?.let {
                    InterfaceManifestJsonWithId(
                        id = id,
                        name = interfaceDecorator.name,
                        description = interfaceDecorator.description,
                        eventDecorators = matchingEvents,
                        functionDecorators = matchingFunctions
                    )
                }
            }
        }
    }

    private fun <T : OverridableDecorator> findMatches(
        interfaceDecorators: List<T>,
        abiSignatures: Set<String>
    ): List<T>? {
        val interfaceDecoratorSignatures = interfaceDecorators.map { it.signature }.toSet()
        return interfaceDecorators.takeIf { abiSignatures.containsAll(interfaceDecoratorSignatures) }
    }
}
