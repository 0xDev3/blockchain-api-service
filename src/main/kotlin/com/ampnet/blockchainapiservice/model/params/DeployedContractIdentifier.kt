package com.ampnet.blockchainapiservice.model.params

import com.ampnet.blockchainapiservice.util.ContractAddress
import java.util.UUID

sealed interface DeployedContractIdentifier

data class DeployedContractIdIdentifier(val id: UUID) : DeployedContractIdentifier
data class DeployedContractAliasIdentifier(val alias: String) : DeployedContractIdentifier
data class DeployedContractAddressIdentifier(val contractAddress: ContractAddress) : DeployedContractIdentifier
