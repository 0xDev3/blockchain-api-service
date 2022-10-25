package dev3.blockchainapiservice.repository

import dev3.blockchainapiservice.model.json.InterfaceManifestJson
import dev3.blockchainapiservice.model.json.InterfaceManifestJsonWithId
import dev3.blockchainapiservice.util.InterfaceId

interface ContractInterfacesRepository {
    fun store(id: InterfaceId, interfaceManifestJson: InterfaceManifestJson): InterfaceManifestJson
    fun store(id: InterfaceId, infoMd: String): String
    fun delete(id: InterfaceId): Boolean
    fun getById(id: InterfaceId): InterfaceManifestJson?
    fun getInfoMarkdownById(id: InterfaceId): String?
    fun getAll(): List<InterfaceManifestJsonWithId>
    fun getAllInfoMarkdownFiles(): List<String>
    fun getAllWithPartiallyMatchingInterfaces(
        abiFunctionSignatures: Set<String>,
        abiEventSignatures: Set<String>
    ): List<InterfaceManifestJsonWithId>
}
