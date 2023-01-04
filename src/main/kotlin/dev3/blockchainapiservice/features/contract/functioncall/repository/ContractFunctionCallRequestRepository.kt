package dev3.blockchainapiservice.features.contract.functioncall.repository

import dev3.blockchainapiservice.features.contract.functioncall.model.filters.ContractFunctionCallRequestFilters
import dev3.blockchainapiservice.features.contract.functioncall.model.params.StoreContractFunctionCallRequestParams
import dev3.blockchainapiservice.features.contract.functioncall.model.result.ContractFunctionCallRequest
import dev3.blockchainapiservice.generated.jooq.id.ContractFunctionCallRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress

interface ContractFunctionCallRequestRepository {
    fun store(params: StoreContractFunctionCallRequestParams): ContractFunctionCallRequest
    fun getById(id: ContractFunctionCallRequestId): ContractFunctionCallRequest?
    fun getAllByProjectId(
        projectId: ProjectId,
        filters: ContractFunctionCallRequestFilters
    ): List<ContractFunctionCallRequest>

    fun setTxInfo(id: ContractFunctionCallRequestId, txHash: TransactionHash, caller: WalletAddress): Boolean
}
