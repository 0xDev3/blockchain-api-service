package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.model.params.StoreErc20LockRequestParams
import com.ampnet.blockchainapiservice.model.result.Erc20LockRequest
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import java.util.UUID

interface Erc20LockRequestRepository {
    fun store(params: StoreErc20LockRequestParams): Erc20LockRequest
    fun getById(id: UUID): Erc20LockRequest?
    fun setTxInfo(id: UUID, txHash: TransactionHash, caller: WalletAddress): Boolean
    fun getAllByProjectId(projectId: UUID): List<Erc20LockRequest>
}
