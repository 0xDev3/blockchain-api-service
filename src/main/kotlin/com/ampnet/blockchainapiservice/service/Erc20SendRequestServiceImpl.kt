package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.blockchain.BlockchainService
import com.ampnet.blockchainapiservice.blockchain.properties.ChainSpec
import com.ampnet.blockchainapiservice.blockchain.properties.RpcUrlSpec
import com.ampnet.blockchainapiservice.exception.CannotAttachTxHashException
import com.ampnet.blockchainapiservice.exception.NonExistentClientIdException
import com.ampnet.blockchainapiservice.exception.ResourceNotFoundException
import com.ampnet.blockchainapiservice.model.params.CreateErc20SendRequestParams
import com.ampnet.blockchainapiservice.model.params.StoreErc20SendRequestParams
import com.ampnet.blockchainapiservice.model.result.BlockchainTransactionInfo
import com.ampnet.blockchainapiservice.model.result.ClientInfo
import com.ampnet.blockchainapiservice.model.result.Erc20SendRequest
import com.ampnet.blockchainapiservice.model.result.FullErc20SendRequest
import com.ampnet.blockchainapiservice.repository.ClientInfoRepository
import com.ampnet.blockchainapiservice.repository.Erc20SendRequestRepository
import com.ampnet.blockchainapiservice.util.Balance
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
class Erc20SendRequestServiceImpl(
    private val uuidProvider: UuidProvider,
    private val functionEncoderService: FunctionEncoderService,
    private val blockchainService: BlockchainService,
    private val clientInfoRepository: ClientInfoRepository,
    private val erc20SendRequestRepository: Erc20SendRequestRepository
) : Erc20SendRequestService {

    companion object : KLogging()

    override fun createErc20SendRequest(params: CreateErc20SendRequestParams): WithFunctionData<Erc20SendRequest> {
        logger.info { "Creating ERC20 send request, params: $params" }

        val clientInfo = params.getClientInfo()
        val id = uuidProvider.getUuid()
        val databaseParams = StoreErc20SendRequestParams.fromCreateParams(id, params, clientInfo)

        val data = encodeFunctionData(params.tokenRecipientAddress, params.tokenAmount, id)
        val erc20SendRequest = erc20SendRequestRepository.store(databaseParams)

        return WithFunctionData(erc20SendRequest, data)
    }

    override fun getErc20SendRequest(id: UUID, rpcSpec: RpcUrlSpec): FullErc20SendRequest {
        logger.debug { "Fetching ERC20 send request, id: $id, rpcSpec: $rpcSpec" }

        val erc20SendRequest = erc20SendRequestRepository.getById(id)
            ?: throw ResourceNotFoundException("ERC20 send request not found for ID: $id")

        val transactionInfo = erc20SendRequest.txHash?.let {
            blockchainService.fetchTransactionInfo(
                chainSpec = ChainSpec(
                    chainId = erc20SendRequest.chainId,
                    rpcSpec = rpcSpec
                ),
                txHash = erc20SendRequest.txHash
            )
        }
        val data = encodeFunctionData(erc20SendRequest.tokenRecipientAddress, erc20SendRequest.tokenAmount, id)
        val status = erc20SendRequest.determineStatus(transactionInfo, data)

        return FullErc20SendRequest.fromErc20SendRequest(
            request = erc20SendRequest,
            status = status,
            data = data,
            transactionInfo = transactionInfo
        )
    }

    override fun attachTxHash(id: UUID, txHash: TransactionHash) {
        logger.info { "Attach txHash to ERC20 send request, id: $id, txHash: $txHash" }

        val txHashAttached = erc20SendRequestRepository.setTxHash(id, txHash)

        if (txHashAttached.not()) {
            throw CannotAttachTxHashException("Unable to attach transaction hash to ERC20 send request with ID: $id")
        }
    }

    private fun CreateErc20SendRequestParams.getClientInfo(): ClientInfo =
        if (this.clientId != null) {
            logger.debug { "Fetching info for clientId: $clientId" }
            clientInfoRepository.getById(this.clientId) ?: throw NonExistentClientIdException(this.clientId)
        } else {
            ClientInfo.EMPTY
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

    private fun Erc20SendRequest.determineStatus(
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

    private fun Erc20SendRequest.isSuccess(
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
