package dev3.blockchainapiservice.repository

import dev3.blockchainapiservice.generated.jooq.id.Erc20LockRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.model.params.StoreErc20LockRequestParams
import dev3.blockchainapiservice.model.result.Erc20LockRequest
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress

interface Erc20LockRequestRepository {
    fun store(params: StoreErc20LockRequestParams): Erc20LockRequest
    fun getById(id: Erc20LockRequestId): Erc20LockRequest?
    fun getAllByProjectId(projectId: ProjectId): List<Erc20LockRequest>
    fun setTxInfo(id: Erc20LockRequestId, txHash: TransactionHash, caller: WalletAddress): Boolean
}
