package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.blockchain.BlockchainService
import dev3.blockchainapiservice.blockchain.properties.ChainSpec
import dev3.blockchainapiservice.exception.ResourceNotFoundException
import dev3.blockchainapiservice.model.params.ParamsFactory
import dev3.blockchainapiservice.model.result.BlockchainTransactionInfo
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.TransactionHash
import mu.KLogging
import org.springframework.stereotype.Service

@Service
class EthCommonServiceImpl(
    private val uuidProvider: UuidProvider,
    private val utcDateTimeProvider: UtcDateTimeProvider,
    private val blockchainService: BlockchainService
) : EthCommonService {

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
        customRpcUrl: String?
    ): BlockchainTransactionInfo? = txHash?.let {
        blockchainService.fetchTransactionInfo(
            chainSpec = ChainSpec(
                chainId = chainId,
                customRpcUrl = customRpcUrl
            ),
            txHash = txHash
        )
    }
}
