package dev3.blockchainapiservice.model.params

import dev3.blockchainapiservice.generated.jooq.id.ContractDeploymentRequestId
import dev3.blockchainapiservice.util.ContractAddress

data class PreStoreContractFunctionCallRequestParams(
    val createParams: CreateContractFunctionCallRequestParams,
    val deployedContractId: ContractDeploymentRequestId?,
    val contractAddress: ContractAddress
)
