package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.model.params.StoreSendErc20RequestParams
import com.ampnet.blockchainapiservice.model.result.SendErc20Request
import com.ampnet.blockchainapiservice.util.TransactionHash
import java.util.UUID

interface SendErc20RequestRepository {
    fun store(params: StoreSendErc20RequestParams): SendErc20Request
    fun getById(id: UUID): SendErc20Request?
    fun setTxHash(id: UUID, txHash: TransactionHash): Boolean
}
