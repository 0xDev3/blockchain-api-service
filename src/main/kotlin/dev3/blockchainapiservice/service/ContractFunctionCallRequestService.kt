package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.model.filters.ContractFunctionCallRequestFilters
import dev3.blockchainapiservice.model.params.CreateContractFunctionCallRequestParams
import dev3.blockchainapiservice.model.result.ContractFunctionCallRequest
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress
import dev3.blockchainapiservice.util.WithFunctionData
import dev3.blockchainapiservice.util.WithTransactionAndFunctionData
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
