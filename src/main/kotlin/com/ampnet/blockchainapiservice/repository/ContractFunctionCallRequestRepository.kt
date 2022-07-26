package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.model.filters.ContractFunctionCallRequestFilters
import com.ampnet.blockchainapiservice.model.params.StoreContractFunctionCallRequestParams
import com.ampnet.blockchainapiservice.model.result.ContractFunctionCallRequest
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
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
