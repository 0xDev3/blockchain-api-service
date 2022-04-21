package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.blockchain.properties.RpcUrlSpec
import com.ampnet.blockchainapiservice.model.params.CreateSendErc20RequestParams
import com.ampnet.blockchainapiservice.model.result.FullSendErc20Request
import com.ampnet.blockchainapiservice.model.result.SendErc20Request
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WithFunctionData
import java.util.UUID

interface SendErc20RequestService {
    fun createSendErc20Request(params: CreateSendErc20RequestParams): WithFunctionData<SendErc20Request>
    fun getSendErc20Request(id: UUID, rpcSpec: RpcUrlSpec): FullSendErc20Request
    fun attachTxHash(id: UUID, txHash: TransactionHash)
}
