package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.features.api.access.model.result.Project
import dev3.blockchainapiservice.model.DeserializableEvent
import dev3.blockchainapiservice.model.params.ParamsFactory
import dev3.blockchainapiservice.model.result.BlockchainTransactionInfo
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.TransactionHash

interface EthCommonService {
    fun <P, R> createDatabaseParams(factory: ParamsFactory<P, R>, params: P, project: Project): R
    fun <R> fetchResource(resource: R?, message: String): R
    fun fetchTransactionInfo(
        txHash: TransactionHash?,
        chainId: ChainId,
        customRpcUrl: String?,
        events: List<DeserializableEvent>
    ): BlockchainTransactionInfo?
}
