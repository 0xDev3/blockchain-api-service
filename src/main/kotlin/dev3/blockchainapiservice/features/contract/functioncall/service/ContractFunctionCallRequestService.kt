package dev3.blockchainapiservice.features.contract.functioncall.service

import dev3.blockchainapiservice.features.contract.functioncall.model.filters.ContractFunctionCallRequestFilters
import dev3.blockchainapiservice.features.contract.functioncall.model.params.CreateContractFunctionCallRequestParams
import dev3.blockchainapiservice.features.contract.functioncall.model.result.ContractFunctionCallRequest
import dev3.blockchainapiservice.generated.jooq.id.ContractFunctionCallRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress
import dev3.blockchainapiservice.util.WithFunctionData
import dev3.blockchainapiservice.util.WithTransactionAndFunctionData

interface ContractFunctionCallRequestService {
    fun createContractFunctionCallRequest(
        params: CreateContractFunctionCallRequestParams,
        project: Project
    ): WithFunctionData<ContractFunctionCallRequest>

    fun getContractFunctionCallRequest(
        id: ContractFunctionCallRequestId
    ): WithTransactionAndFunctionData<ContractFunctionCallRequest>

    fun getContractFunctionCallRequestsByProjectIdAndFilters(
        projectId: ProjectId,
        filters: ContractFunctionCallRequestFilters
    ): List<WithTransactionAndFunctionData<ContractFunctionCallRequest>>

    fun attachTxInfo(id: ContractFunctionCallRequestId, txHash: TransactionHash, caller: WalletAddress)
}
