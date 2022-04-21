package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.blockchain.BlockchainService
import com.ampnet.blockchainapiservice.blockchain.properties.ChainSpec
import com.ampnet.blockchainapiservice.blockchain.properties.RpcUrlSpec
import com.ampnet.blockchainapiservice.config.ApplicationProperties
import com.ampnet.blockchainapiservice.exception.CannotAttachTxHashException
import com.ampnet.blockchainapiservice.exception.IncompleteSendErc20RequestException
import com.ampnet.blockchainapiservice.exception.NonExistentClientIdException
import com.ampnet.blockchainapiservice.exception.ResourceNotFoundException
import com.ampnet.blockchainapiservice.model.params.CreateSendErc20RequestParams
import com.ampnet.blockchainapiservice.model.params.StoreSendErc20RequestParams
import com.ampnet.blockchainapiservice.model.result.BlockchainTransactionInfo
import com.ampnet.blockchainapiservice.model.result.FullSendErc20Request
import com.ampnet.blockchainapiservice.model.result.SendErc20Request
import com.ampnet.blockchainapiservice.repository.ClientInfoRepository
import com.ampnet.blockchainapiservice.repository.SendErc20RequestRepository
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.FunctionArgument
import com.ampnet.blockchainapiservice.util.FunctionData
import com.ampnet.blockchainapiservice.util.Status
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.ampnet.blockchainapiservice.util.WithFunctionData
import mu.KLogging
import org.springframework.stereotype.Service
import org.web3j.abi.datatypes.Utf8String
import java.util.UUID

@Service
class SendErc20RequestServiceImpl(
    private val uuidProvider: UuidProvider,
    private val functionEncoderService: FunctionEncoderService,
    private val blockchainService: BlockchainService,
    private val clientInfoRepository: ClientInfoRepository,
    private val sendErc20RequestRepository: SendErc20RequestRepository,
    private val applicationProperties: ApplicationProperties
) : SendErc20RequestService {

    companion object : KLogging()

    override fun createSendErc20Request(params: CreateSendErc20RequestParams): WithFunctionData<SendErc20Request> {
        logger.info { "Creating send ERC20 request, params: $params" }

        val (chainId, redirectUrl) = params.getChainIdAndRedirectUrl()

        val id = uuidProvider.getUuid()
        val data = encodeFunctionData(params.tokenRecipientAddress, params.tokenAmount, id)

        val databaseParams = StoreSendErc20RequestParams.fromCreateParams(params, id, chainId, redirectUrl)
        val sendErc20Request = sendErc20RequestRepository.store(databaseParams)

        return WithFunctionData(
            sendErc20Request.copy(
                redirectUrl = sendErc20Request.redirectUrl +
                    applicationProperties.sendRequest.redirectPath.replace("{id}", id.toString())
            ),
            data
        )
    }

    override fun getSendErc20Request(id: UUID, rpcSpec: RpcUrlSpec): FullSendErc20Request {
        logger.debug { "Fetching send ERC20 request, id: $id, rpcSpec: $rpcSpec" }

        val sendErc20Request = sendErc20RequestRepository.getById(id)
            ?: throw ResourceNotFoundException("Send request not found for ID: $id")

        val transactionInfo = sendErc20Request.txHash?.let {
            blockchainService.fetchTransactionInfo(
                chainSpec = ChainSpec(
                    chainId = sendErc20Request.chainId,
                    rpcSpec = rpcSpec
                ),
                txHash = sendErc20Request.txHash
            )
        }
        val data = encodeFunctionData(sendErc20Request.tokenRecipientAddress, sendErc20Request.tokenAmount, id)
        val status = sendErc20Request.determineStatus(transactionInfo, data)

        return FullSendErc20Request.fromSendErc20Request(
            request = sendErc20Request,
            status = status,
            redirectPath = applicationProperties.sendRequest.redirectPath.replace("{id}", id.toString()),
            data = data,
            transactionInfo = transactionInfo
        )
    }

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

    private fun encodeFunctionData(tokenRecipientAddress: WalletAddress, tokenAmount: Balance, id: UUID): FunctionData =
        functionEncoderService.encode(
            functionName = "transfer",
            arguments = listOf(
                FunctionArgument(abiType = "address", value = tokenRecipientAddress.rawValue),
                FunctionArgument(abiType = "uint256", value = tokenAmount.rawValue)
            ),
            abiOutputTypes = listOf("bool"),
            additionalData = listOf(Utf8String(id.toString()))
        )

    private fun SendErc20Request.determineStatus(
        transactionInfo: BlockchainTransactionInfo?,
        expectedData: FunctionData
    ): Status =
        if (transactionInfo == null) { // implies that either txHash is null or transaction is not yet mined
            Status.PENDING
        } else if (isSuccess(transactionInfo, expectedData)) {
            Status.SUCCESS
        } else {
            Status.FAILED
        }

    private fun SendErc20Request.isSuccess(
        transactionInfo: BlockchainTransactionInfo,
        expectedData: FunctionData
    ): Boolean =
        tokenAddress == transactionInfo.to.toContractAddress() &&
            txHash == transactionInfo.hash &&
            senderAddressMatches(tokenSenderAddress, transactionInfo.from) &&
            transactionInfo.data == expectedData

    private fun senderAddressMatches(senderAddress: WalletAddress?, fromAddress: WalletAddress): Boolean =
        senderAddress == null || senderAddress == fromAddress
}
