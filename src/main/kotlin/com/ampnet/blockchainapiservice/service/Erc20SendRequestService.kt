package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.blockchain.properties.RpcUrlSpec
import com.ampnet.blockchainapiservice.model.params.CreateErc20SendRequestParams
import com.ampnet.blockchainapiservice.model.result.Erc20SendRequest
import com.ampnet.blockchainapiservice.model.result.FullErc20SendRequest
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WithFunctionData
import java.util.UUID

interface Erc20SendRequestService {
    fun createErc20SendRequest(params: CreateErc20SendRequestParams): WithFunctionData<Erc20SendRequest>
    fun getErc20SendRequest(id: UUID, rpcSpec: RpcUrlSpec): FullErc20SendRequest
    fun attachTxHash(id: UUID, txHash: TransactionHash)
}