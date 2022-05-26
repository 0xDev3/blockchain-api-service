package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.blockchain.properties.RpcUrlSpec
import com.ampnet.blockchainapiservice.model.params.CreateErc20LockRequestParams
import com.ampnet.blockchainapiservice.model.result.Erc20LockRequest
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WithFunctionData
import com.ampnet.blockchainapiservice.util.WithTransactionData
import java.util.UUID

interface Erc20LockRequestService {
    fun createErc20LockRequest(params: CreateErc20LockRequestParams): WithFunctionData<Erc20LockRequest>
    fun getErc20LockRequest(id: UUID, rpcSpec: RpcUrlSpec): WithTransactionData<Erc20LockRequest>
    fun attachTxHash(id: UUID, txHash: TransactionHash)
}
