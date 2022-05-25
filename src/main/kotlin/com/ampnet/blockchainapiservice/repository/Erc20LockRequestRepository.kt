package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.model.params.StoreErc20LockRequestParams
import com.ampnet.blockchainapiservice.model.result.Erc20LockRequest
import com.ampnet.blockchainapiservice.util.TransactionHash
import java.util.UUID

interface Erc20LockRequestRepository {
    fun store(params: StoreErc20LockRequestParams): Erc20LockRequest
    fun getById(id: UUID): Erc20LockRequest?
    fun setTxHash(id: UUID, txHash: TransactionHash): Boolean
}
