package dev3.blockchainapiservice.model.params

import dev3.blockchainapiservice.util.ContractAddress
import java.util.UUID

data class PreStoreContractFunctionCallRequestParams(
    val createParams: CreateContractFunctionCallRequestParams,
    val deployedContractId: UUID?,
    val contractAddress: ContractAddress
)
