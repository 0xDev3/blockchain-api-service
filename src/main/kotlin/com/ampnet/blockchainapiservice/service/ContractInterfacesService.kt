package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.model.json.InterfaceManifestJsonWithId
import com.ampnet.blockchainapiservice.util.InterfaceId
import java.util.UUID

interface ContractInterfacesService {
    fun getSuggestedInterfacesForImportedSmartContract(id: UUID): List<InterfaceManifestJsonWithId>
    fun addInterfacesToImportedContract(importedContractId: UUID, projectId: UUID, interfaces: List<InterfaceId>)
    fun removeInterfacesFromImportedContract(importedContractId: UUID, projectId: UUID, interfaces: List<InterfaceId>)
    fun setImportedContractInterfaces(importedContractId: UUID, projectId: UUID, interfaces: List<InterfaceId>)
}
