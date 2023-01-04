package dev3.blockchainapiservice.features.contract.readcall.service

import dev3.blockchainapiservice.features.api.access.model.result.Project
import dev3.blockchainapiservice.features.contract.readcall.model.params.CreateReadonlyFunctionCallParams
import dev3.blockchainapiservice.features.contract.readcall.model.result.ReadonlyFunctionCallResult
import dev3.blockchainapiservice.util.WithDeployedContractIdAndAddress

interface ContractReadonlyFunctionCallService {
    fun callReadonlyContractFunction(
        params: CreateReadonlyFunctionCallParams,
        project: Project
    ): WithDeployedContractIdAndAddress<ReadonlyFunctionCallResult>
}
