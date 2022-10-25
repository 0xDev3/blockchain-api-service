package dev3.blockchainapiservice.util

import java.util.UUID

data class WithDeployedContractIdAndAddress<T>(
    val value: T,
    val deployedContractId: UUID?,
    val contractAddress: ContractAddress
)
