package dev3.blockchainapiservice.model.params

import dev3.blockchainapiservice.model.result.ContractDecorator
import dev3.blockchainapiservice.util.FunctionData

data class PreStoreContractDeploymentRequestParams(
    val createParams: CreateContractDeploymentRequestParams,
    val contractDecorator: ContractDecorator,
    val encodedConstructor: FunctionData
)
