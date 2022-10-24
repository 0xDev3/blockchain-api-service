package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.model.filters.ContractFunctionCallRequestFilters
import com.ampnet.blockchainapiservice.model.params.CreateContractFunctionCallRequestParams
import com.ampnet.blockchainapiservice.model.result.ContractFunctionCallRequest
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.ampnet.blockchainapiservice.util.WithFunctionData
import com.ampnet.blockchainapiservice.util.WithTransactionAndFunctionData
import java.util.UUID

interface ContractFunctionCallRequestService {
    fun createContractFunctionCallRequest(
        params: CreateContractFunctionCallRequestParams,
        project: Project
    ): WithFunctionData<ContractFunctionCallRequest>

    fun getContractFunctionCallRequest(id: UUID): WithTransactionAndFunctionData<ContractFunctionCallRequest>
    fun getContractFunctionCallRequestsByProjectIdAndFilters(
        projectId: UUID,
        filters: ContractFunctionCallRequestFilters
    ): List<WithTransactionAndFunctionData<ContractFunctionCallRequest>>

    fun attachTxInfo(id: UUID, txHash: TransactionHash, caller: WalletAddress)
}
