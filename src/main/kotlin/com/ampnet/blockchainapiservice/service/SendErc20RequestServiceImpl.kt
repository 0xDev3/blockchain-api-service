package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.exception.CannotAttachTxHashException
import com.ampnet.blockchainapiservice.exception.IncompleteSendErc20RequestException
import com.ampnet.blockchainapiservice.exception.NonExistentClientIdException
import com.ampnet.blockchainapiservice.model.params.CreateSendErc20RequestParams
import com.ampnet.blockchainapiservice.model.params.StoreSendErc20RequestParams
import com.ampnet.blockchainapiservice.model.result.FullSendErc20Request
import com.ampnet.blockchainapiservice.model.result.SendErc20Request
import com.ampnet.blockchainapiservice.repository.ClientInfoRepository
import com.ampnet.blockchainapiservice.repository.SendErc20RequestRepository
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.FunctionArgument
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WithFunctionData
import mu.KLogging
import org.springframework.stereotype.Service
import org.web3j.abi.datatypes.Utf8String
import java.util.UUID

@Service
class SendErc20RequestServiceImpl(
    private val uuidProvider: UuidProvider,
    private val functionEncoderService: FunctionEncoderService,
    private val clientInfoRepository: ClientInfoRepository,
    private val sendErc20RequestRepository: SendErc20RequestRepository
) : SendErc20RequestService {

    companion object : KLogging()

    override fun createSendErc20Request(params: CreateSendErc20RequestParams): WithFunctionData<SendErc20Request> {
        logger.info { "Creating send ERC20 request, params: $params" }

        val (chainId, redirectUrl) = params.getChainIdAndRedirectUrl()

        val id = uuidProvider.getUuid()
        val data = functionEncoderService.encode(
            functionName = "transfer",
            arguments = listOf(
                FunctionArgument(abiType = "address", value = params.toAddress),
                FunctionArgument(abiType = "uint256", value = params.amount.rawValue)
            ),
            abiOutputTypes = listOf("bool"),
            additionalData = listOf(Utf8String(id.toString()))
        )

        val databaseParams = StoreSendErc20RequestParams.fromCreateParams(params, id, chainId, redirectUrl)
        val sendErc20Request = sendErc20RequestRepository.store(databaseParams)

        return WithFunctionData(sendErc20Request, data)
    }

    override fun getSendErc20Request(id: UUID, rpcUrl: String?): FullSendErc20Request = TODO()

    override fun attachTxHash(id: UUID, txHash: TransactionHash) {
        logger.info { "Attach txHash to send ERC20 request, id: $id, txHash: $txHash" }

        val txHashAttached = sendErc20RequestRepository.setTxHash(id, txHash)

        if (txHashAttached.not()) {
            throw CannotAttachTxHashException("Unable to attach transaction hash to send request with ID: $id")
        }
    }

    private fun CreateSendErc20RequestParams.getChainIdAndRedirectUrl(): Pair<ChainId, String> =
        if (this.clientId != null) {
            logger.debug { "Fetching info for clientId: $clientId" }
            val clientInfo = clientInfoRepository.getById(this.clientId)
                ?: throw NonExistentClientIdException(this.clientId)
            Pair(clientInfo.chainId, clientInfo.redirectUrl)
        } else {
            logger.debug { "No clientId provided, using specified chainId and redirectUrl" }
            Pair(
                this.chainId ?: throw IncompleteSendErc20RequestException("Missing chainId"),
                this.redirectUrl ?: throw IncompleteSendErc20RequestException("Missing redirectUrl")
            )
        }
}
