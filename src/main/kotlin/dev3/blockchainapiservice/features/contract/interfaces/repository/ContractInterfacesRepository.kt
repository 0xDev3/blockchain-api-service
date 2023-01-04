package dev3.blockchainapiservice.features.contract.interfaces.repository

import dev3.blockchainapiservice.features.contract.deployment.model.json.InterfaceManifestJson
import dev3.blockchainapiservice.features.contract.deployment.model.json.InterfaceManifestJsonWithId
import dev3.blockchainapiservice.features.contract.interfaces.model.filters.ContractInterfaceFilters
import dev3.blockchainapiservice.util.InterfaceId

interface ContractInterfacesRepository {
    fun store(id: InterfaceId, interfaceManifestJson: InterfaceManifestJson): InterfaceManifestJson
    fun store(id: InterfaceId, infoMd: String): String
    fun delete(id: InterfaceId): Boolean
    fun getById(id: InterfaceId): InterfaceManifestJson?
    fun getInfoMarkdownById(id: InterfaceId): String?
    fun getAll(filters: ContractInterfaceFilters): List<InterfaceManifestJsonWithId>
    fun getAllInfoMarkdownFiles(filters: ContractInterfaceFilters): List<String>
    fun getAllWithPartiallyMatchingInterfaces(
        abiFunctionSignatures: Set<String>,
        abiEventSignatures: Set<String>
    ): List<InterfaceManifestJsonWithId>
}
