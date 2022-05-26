package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.blockchain.BlockchainService
import com.ampnet.blockchainapiservice.blockchain.properties.ChainSpec
import com.ampnet.blockchainapiservice.blockchain.properties.RpcUrlSpec
import com.ampnet.blockchainapiservice.exception.NonExistentClientIdException
import com.ampnet.blockchainapiservice.exception.ResourceNotFoundException
import com.ampnet.blockchainapiservice.model.params.ClientIdParam
import com.ampnet.blockchainapiservice.model.params.ParamsFactory
import com.ampnet.blockchainapiservice.model.result.BlockchainTransactionInfo
import com.ampnet.blockchainapiservice.model.result.ClientInfo
import com.ampnet.blockchainapiservice.repository.ClientInfoRepository
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.TransactionHash
import mu.KLogging
import org.springframework.stereotype.Service

@Service
class Erc20CommonServiceImpl(
    private val uuidProvider: UuidProvider,
    private val clientInfoRepository: ClientInfoRepository,
    private val blockchainService: BlockchainService
) : Erc20CommonService {

    companion object : KLogging()

    override fun <P : ClientIdParam, R> createDatabaseParams(factory: ParamsFactory<P, R>, params: P): R {
        val clientInfo = params.getClientInfo()
        val id = uuidProvider.getUuid()
        return factory.fromCreateParams(id, params, clientInfo)
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

    private fun ClientIdParam.getClientInfo(): ClientInfo {
        val clientId = this.clientId

        return if (clientId != null) {
            logger.debug { "Fetching info for clientId: $clientId" }
            clientInfoRepository.getById(clientId) ?: throw NonExistentClientIdException(clientId)
        } else {
            ClientInfo.EMPTY
        }
    }
}
