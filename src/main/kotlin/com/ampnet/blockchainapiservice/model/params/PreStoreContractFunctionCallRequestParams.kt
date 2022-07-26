package com.ampnet.blockchainapiservice.model.params

import com.ampnet.blockchainapiservice.util.ContractAddress
import java.util.UUID

data class PreStoreContractFunctionCallRequestParams(
    val createParams: CreateContractFunctionCallRequestParams,
    val deployedContractId: UUID?,
    val contractAddress: ContractAddress
)
