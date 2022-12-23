package dev3.blockchainapiservice.features.contract.arbitrarycall.service

import dev3.blockchainapiservice.features.contract.arbitrarycall.model.filters.ContractArbitraryCallRequestFilters
import dev3.blockchainapiservice.features.contract.arbitrarycall.model.params.CreateContractArbitraryCallRequestParams
import dev3.blockchainapiservice.features.contract.arbitrarycall.model.result.ContractArbitraryCallRequest
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress
import dev3.blockchainapiservice.util.WithTransactionData
import java.util.UUID

interface ContractArbitraryCallRequestService {
    fun createContractArbitraryCallRequest(
        params: CreateContractArbitraryCallRequestParams,
        project: Project
    ): ContractArbitraryCallRequest

    fun getContractArbitraryCallRequest(id: UUID): WithTransactionData<ContractArbitraryCallRequest>
    fun getContractArbitraryCallRequestsByProjectIdAndFilters(
        projectId: UUID,
        filters: ContractArbitraryCallRequestFilters
    ): List<WithTransactionData<ContractArbitraryCallRequest>>

    fun attachTxInfo(id: UUID, txHash: TransactionHash, caller: WalletAddress)
}
