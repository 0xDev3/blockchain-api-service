package dev3.blockchainapiservice.features.contract.arbitrarycall.repository

import dev3.blockchainapiservice.features.contract.arbitrarycall.model.filters.ContractArbitraryCallRequestFilters
import dev3.blockchainapiservice.features.contract.arbitrarycall.model.params.StoreContractArbitraryCallRequestParams
import dev3.blockchainapiservice.features.contract.arbitrarycall.model.result.ContractArbitraryCallRequest
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress
import java.util.UUID

interface ContractArbitraryCallRequestRepository {
    fun store(params: StoreContractArbitraryCallRequestParams): ContractArbitraryCallRequest
    fun getById(id: UUID): ContractArbitraryCallRequest?
    fun getAllByProjectId(
        projectId: UUID,
        filters: ContractArbitraryCallRequestFilters
    ): List<ContractArbitraryCallRequest>

    fun setTxInfo(id: UUID, txHash: TransactionHash, caller: WalletAddress): Boolean
}
