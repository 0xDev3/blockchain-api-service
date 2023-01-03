package dev3.blockchainapiservice.features.contract.arbitrarycall.service

import dev3.blockchainapiservice.features.contract.arbitrarycall.model.filters.ContractArbitraryCallRequestFilters
import dev3.blockchainapiservice.features.contract.arbitrarycall.model.params.CreateContractArbitraryCallRequestParams
import dev3.blockchainapiservice.features.contract.arbitrarycall.model.result.ContractArbitraryCallRequest
import dev3.blockchainapiservice.generated.jooq.id.ContractArbitraryCallRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress
import dev3.blockchainapiservice.util.WithTransactionData

interface ContractArbitraryCallRequestService {
    fun createContractArbitraryCallRequest(
        params: CreateContractArbitraryCallRequestParams,
        project: Project
    ): ContractArbitraryCallRequest

    fun getContractArbitraryCallRequest(
        id: ContractArbitraryCallRequestId
    ): WithTransactionData<ContractArbitraryCallRequest>

    fun getContractArbitraryCallRequestsByProjectIdAndFilters(
        projectId: ProjectId,
        filters: ContractArbitraryCallRequestFilters
    ): List<WithTransactionData<ContractArbitraryCallRequest>>

    fun attachTxInfo(id: ContractArbitraryCallRequestId, txHash: TransactionHash, caller: WalletAddress)
}
