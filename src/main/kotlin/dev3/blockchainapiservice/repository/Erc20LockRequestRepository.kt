package dev3.blockchainapiservice.repository

import dev3.blockchainapiservice.model.params.StoreErc20LockRequestParams
import dev3.blockchainapiservice.model.result.Erc20LockRequest
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress
import java.util.UUID

interface Erc20LockRequestRepository {
    fun store(params: StoreErc20LockRequestParams): Erc20LockRequest
    fun getById(id: UUID): Erc20LockRequest?
    fun getAllByProjectId(projectId: UUID): List<Erc20LockRequest>
    fun setTxInfo(id: UUID, txHash: TransactionHash, caller: WalletAddress): Boolean
}
