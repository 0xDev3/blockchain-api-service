package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.model.json.InterfaceManifestJson
import com.ampnet.blockchainapiservice.model.json.PartiallyMatchingInterfaceManifest
import com.ampnet.blockchainapiservice.util.ContractId

interface ContractInterfacesRepository {
    fun store(id: ContractId, interfaceManifestJson: InterfaceManifestJson): InterfaceManifestJson
    fun store(id: ContractId, infoMd: String): String
    fun delete(id: ContractId): Boolean
    fun getById(id: ContractId): InterfaceManifestJson?
    fun getInfoMarkdownById(id: ContractId): String?
    fun getAll(): List<InterfaceManifestJson>
    fun getAllWithPartiallyMatchingInterfaces(
        abiFunctionSignatures: Set<String>,
        abiEventSignatures: Set<String>
    ): List<PartiallyMatchingInterfaceManifest>
}
