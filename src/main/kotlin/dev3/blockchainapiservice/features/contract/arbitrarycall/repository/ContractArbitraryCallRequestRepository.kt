package dev3.blockchainapiservice.features.contract.arbitrarycall.repository

import dev3.blockchainapiservice.features.contract.arbitrarycall.model.filters.ContractArbitraryCallRequestFilters
import dev3.blockchainapiservice.features.contract.arbitrarycall.model.params.StoreContractArbitraryCallRequestParams
import dev3.blockchainapiservice.features.contract.arbitrarycall.model.result.ContractArbitraryCallRequest
import dev3.blockchainapiservice.generated.jooq.id.ContractArbitraryCallRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress

interface ContractArbitraryCallRequestRepository {
    fun store(params: StoreContractArbitraryCallRequestParams): ContractArbitraryCallRequest
    fun getById(id: ContractArbitraryCallRequestId): ContractArbitraryCallRequest?
    fun getAllByProjectId(
        projectId: ProjectId,
        filters: ContractArbitraryCallRequestFilters
    ): List<ContractArbitraryCallRequest>

    fun setTxInfo(id: ContractArbitraryCallRequestId, txHash: TransactionHash, caller: WalletAddress): Boolean
}
