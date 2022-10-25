package dev3.blockchainapiservice.model.params

import dev3.blockchainapiservice.exception.InvalidRequestBodyException
import dev3.blockchainapiservice.util.ContractAddress
import java.util.UUID

interface DeployedContractIdentifierRequestBody {
    val deployedContractId: UUID?
    val deployedContractAlias: String?
    val contractAddress: String?
}

sealed interface DeployedContractIdentifier {
    companion object {
        operator fun invoke(requestBody: DeployedContractIdentifierRequestBody) =
            listOfNotNull(
                requestBody.deployedContractId?.let { DeployedContractIdIdentifier(it) },
                requestBody.deployedContractAlias?.let { DeployedContractAliasIdentifier(it) },
                requestBody.contractAddress?.let { DeployedContractAddressIdentifier(ContractAddress(it)) }
            )
                .takeIf { it.size == 1 }
                ?.firstOrNull()
                ?: throw InvalidRequestBodyException(
                    "Exactly one of the possible contract identifier values must be specified:" +
                        " [deployed_contract_id, deployed_contract_address, contract_address]"
                )
    }
}

data class DeployedContractIdIdentifier(val id: UUID) : DeployedContractIdentifier
data class DeployedContractAliasIdentifier(val alias: String) : DeployedContractIdentifier
data class DeployedContractAddressIdentifier(val contractAddress: ContractAddress) : DeployedContractIdentifier
