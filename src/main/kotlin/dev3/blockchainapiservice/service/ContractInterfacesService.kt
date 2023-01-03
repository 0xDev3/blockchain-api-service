package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.generated.jooq.id.ContractDeploymentRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.model.result.ContractDecorator
import dev3.blockchainapiservice.model.result.MatchingContractInterfaces
import dev3.blockchainapiservice.util.InterfaceId

interface ContractInterfacesService {
    fun attachMatchingInterfacesToDecorator(contractDecorator: ContractDecorator): ContractDecorator
    fun getSuggestedInterfacesForImportedSmartContract(id: ContractDeploymentRequestId): MatchingContractInterfaces
    fun addInterfacesToImportedContract(
        importedContractId: ContractDeploymentRequestId,
        projectId: ProjectId,
        interfaces: List<InterfaceId>
    )

    fun removeInterfacesFromImportedContract(
        importedContractId: ContractDeploymentRequestId,
        projectId: ProjectId,
        interfaces: List<InterfaceId>
    )

    fun setImportedContractInterfaces(
        importedContractId: ContractDeploymentRequestId,
        projectId: ProjectId,
        interfaces: List<InterfaceId>
    )
}
