package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.model.params.CreateReadonlyFunctionCallParams
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.model.result.ReadonlyFunctionCallResult
import dev3.blockchainapiservice.util.WithDeployedContractIdAndAddress

interface ContractReadonlyFunctionCallService {
    fun callReadonlyContractFunction(
        params: CreateReadonlyFunctionCallParams,
        project: Project
    ): WithDeployedContractIdAndAddress<ReadonlyFunctionCallResult>
}
