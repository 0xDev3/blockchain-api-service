package com.ampnet.blockchainapiservice.model.params

import com.ampnet.blockchainapiservice.model.result.ContractDecorator
import com.ampnet.blockchainapiservice.util.FunctionData

data class PreStoreContractDeploymentRequestParams(
    val createParams: CreateContractDeploymentRequestParams,
    val contractDecorator: ContractDecorator,
    val encodedConstructor: FunctionData
)
