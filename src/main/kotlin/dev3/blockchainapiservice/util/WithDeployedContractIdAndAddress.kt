package dev3.blockchainapiservice.util

import dev3.blockchainapiservice.generated.jooq.id.ContractDeploymentRequestId

data class WithDeployedContractIdAndAddress<T>(
    val value: T,
    val deployedContractId: ContractDeploymentRequestId?,
    val contractAddress: ContractAddress
)
