package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.blockchain.properties.RpcUrlSpec
import com.ampnet.blockchainapiservice.model.params.ParamsFactory
import com.ampnet.blockchainapiservice.model.result.BlockchainTransactionInfo
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.TransactionHash

interface Erc20CommonService {
    fun <P, R> createDatabaseParams(factory: ParamsFactory<P, R>, params: P, project: Project): R
    fun <R> fetchResource(resource: R?, message: String): R
    fun fetchTransactionInfo(
        txHash: TransactionHash?,
        chainId: ChainId,
        rpcSpec: RpcUrlSpec
    ): BlockchainTransactionInfo?
}
