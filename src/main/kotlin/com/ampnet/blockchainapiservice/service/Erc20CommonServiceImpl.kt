package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.blockchain.BlockchainService
import com.ampnet.blockchainapiservice.blockchain.properties.ChainSpec
import com.ampnet.blockchainapiservice.blockchain.properties.RpcUrlSpec
import com.ampnet.blockchainapiservice.exception.ResourceNotFoundException
import com.ampnet.blockchainapiservice.model.params.ParamsFactory
import com.ampnet.blockchainapiservice.model.result.BlockchainTransactionInfo
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.TransactionHash
import mu.KLogging
import org.springframework.stereotype.Service

@Service
class Erc20CommonServiceImpl(
    private val uuidProvider: UuidProvider,
    private val utcDateTimeProvider: UtcDateTimeProvider,
    private val blockchainService: BlockchainService
) : Erc20CommonService {

    companion object : KLogging()

    override fun <P, R> createDatabaseParams(factory: ParamsFactory<P, R>, params: P, project: Project): R {
        // TODO use API key quota here in the future (out of MVP scope)
        return factory.fromCreateParams(uuidProvider.getUuid(), params, project, utcDateTimeProvider.getUtcDateTime())
    }

    override fun <R> fetchResource(resource: R?, message: String): R {
        return resource ?: throw ResourceNotFoundException(message)
    }

    override fun fetchTransactionInfo(
        txHash: TransactionHash?,
        chainId: ChainId,
        rpcSpec: RpcUrlSpec
    ): BlockchainTransactionInfo? = txHash?.let {
        blockchainService.fetchTransactionInfo(
            chainSpec = ChainSpec(
                chainId = chainId,
                rpcSpec = rpcSpec
            ),
            txHash = txHash
        )
    }
}
