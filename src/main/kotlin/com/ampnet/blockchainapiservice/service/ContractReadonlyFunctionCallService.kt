package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.model.params.CreateReadonlyFunctionCallParams
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.model.result.ReadonlyFunctionCallResult

interface ContractReadonlyFunctionCallService {
    fun callReadonlyContractFunction(
        params: CreateReadonlyFunctionCallParams,
        project: Project
    ): ReadonlyFunctionCallResult
}
