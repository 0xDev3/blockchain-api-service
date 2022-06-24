package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.blockchain.properties.RpcUrlSpec
import com.ampnet.blockchainapiservice.model.params.CreateErc20SendRequestParams
import com.ampnet.blockchainapiservice.model.result.Erc20SendRequest
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.ampnet.blockchainapiservice.util.WithFunctionData
import com.ampnet.blockchainapiservice.util.WithTransactionData
import java.util.UUID

interface Erc20SendRequestService {
    fun createErc20SendRequest(params: CreateErc20SendRequestParams): WithFunctionData<Erc20SendRequest>
    fun getErc20SendRequest(id: UUID, rpcSpec: RpcUrlSpec): WithTransactionData<Erc20SendRequest>
    fun getErc20SendRequestsBySender(
        sender: WalletAddress,
        rpcSpec: RpcUrlSpec
    ): List<WithTransactionData<Erc20SendRequest>>

    fun getErc20SendRequestsByRecipient(
        recipient: WalletAddress,
        rpcSpec: RpcUrlSpec
    ): List<WithTransactionData<Erc20SendRequest>>

    fun attachTxInfo(id: UUID, txHash: TransactionHash, caller: WalletAddress)
}
