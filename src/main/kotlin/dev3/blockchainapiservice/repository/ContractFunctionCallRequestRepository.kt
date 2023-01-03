package dev3.blockchainapiservice.repository

import dev3.blockchainapiservice.generated.jooq.id.ContractFunctionCallRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.model.filters.ContractFunctionCallRequestFilters
import dev3.blockchainapiservice.model.params.StoreContractFunctionCallRequestParams
import dev3.blockchainapiservice.model.result.ContractFunctionCallRequest
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
