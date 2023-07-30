package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.model.json.InterfaceManifestJsonWithId
import dev3.blockchainapiservice.model.result.ContractDecorator
import dev3.blockchainapiservice.util.InterfaceId
import java.util.UUID

interface ContractInterfacesService {
    fun attachMatchingInterfacesToDecorator(contractDecorator: ContractDecorator): ContractDecorator
    fun getSuggestedInterfacesForImportedSmartContract(id: UUID): List<InterfaceManifestJsonWithId>
    fun addInterfacesToImportedContract(importedContractId: UUID, projectId: UUID, interfaces: List<InterfaceId>)
    fun removeInterfacesFromImportedContract(importedContractId: UUID, projectId: UUID, interfaces: List<InterfaceId>)
    fun setImportedContractInterfaces(importedContractId: UUID, projectId: UUID, interfaces: List<InterfaceId>)
}
