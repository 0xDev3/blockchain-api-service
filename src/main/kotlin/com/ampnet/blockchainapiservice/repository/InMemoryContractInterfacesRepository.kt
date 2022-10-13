package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.model.json.InterfaceManifestJson
import com.ampnet.blockchainapiservice.util.ContractId
import mu.KLogging
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

@Repository
class InMemoryContractInterfacesRepository : ContractInterfacesRepository {

    companion object : KLogging()

    private val storage = ConcurrentHashMap<ContractId, InterfaceManifestJson>()
    private val infoMarkdownStorage = ConcurrentHashMap<ContractId, String>()

    override fun store(id: ContractId, interfaceManifestJson: InterfaceManifestJson): InterfaceManifestJson {
        logger.info { "Storing contract interface with ID: $id" }
        storage[id] = interfaceManifestJson
        return interfaceManifestJson
    }

    override fun store(id: ContractId, infoMd: String): String {
        logger.info { "Storing contract interface info.md with ID: $id" }
        infoMarkdownStorage[id] = infoMd
        return infoMd
    }

    override fun delete(id: ContractId): Boolean {
        logger.info { "Deleting contract interface with ID: $id" }
        infoMarkdownStorage.remove(id)
        return storage.remove(id) != null
    }

    override fun getById(id: ContractId): InterfaceManifestJson? {
        logger.debug { "Get contract interface by ID: $id" }
        return storage[id]
    }

    override fun getInfoMarkdownById(id: ContractId): String? {
        logger.debug { "Get contract interface info.md by ID: $id" }
        return infoMarkdownStorage[id]
    }

    override fun getAll(): List<InterfaceManifestJson> {
        logger.debug { "Get all contract interfaces" }
        return storage.values.toList()
    }
}
