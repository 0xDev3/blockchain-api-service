package dev3.blockchainapiservice.repository

import dev3.blockchainapiservice.model.filters.ContractFunctionCallRequestFilters
import dev3.blockchainapiservice.model.params.StoreContractFunctionCallRequestParams
import dev3.blockchainapiservice.model.result.ContractFunctionCallRequest
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress
import java.util.UUID

interface ContractFunctionCallRequestRepository {
    fun store(params: StoreContractFunctionCallRequestParams): ContractFunctionCallRequest
    fun getById(id: UUID): ContractFunctionCallRequest?
    fun getAllByProjectId(
        projectId: UUID,
        filters: ContractFunctionCallRequestFilters
    ): List<ContractFunctionCallRequest>

    fun setTxInfo(id: UUID, txHash: TransactionHash, caller: WalletAddress): Boolean
}
