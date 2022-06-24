package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.blockchain.properties.RpcUrlSpec
import com.ampnet.blockchainapiservice.exception.CannotAttachTxInfoException
import com.ampnet.blockchainapiservice.model.params.CreateErc20SendRequestParams
import com.ampnet.blockchainapiservice.model.params.StoreErc20SendRequestParams
import com.ampnet.blockchainapiservice.model.result.BlockchainTransactionInfo
import com.ampnet.blockchainapiservice.model.result.Erc20SendRequest
import com.ampnet.blockchainapiservice.repository.Erc20SendRequestRepository
import com.ampnet.blockchainapiservice.util.AbiType.AbiType
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.FunctionArgument
import com.ampnet.blockchainapiservice.util.FunctionData
import com.ampnet.blockchainapiservice.util.Status
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.ampnet.blockchainapiservice.util.WithFunctionData
import com.ampnet.blockchainapiservice.util.WithTransactionData
import mu.KLogging
import org.springframework.stereotype.Service
import org.web3j.abi.datatypes.Utf8String
import java.util.UUID

@Service
class Erc20SendRequestServiceImpl(
    private val functionEncoderService: FunctionEncoderService,
    private val erc20SendRequestRepository: Erc20SendRequestRepository,
    private val erc20CommonService: Erc20CommonService
) : Erc20SendRequestService {

    companion object : KLogging()

    override fun createErc20SendRequest(params: CreateErc20SendRequestParams): WithFunctionData<Erc20SendRequest> {
        logger.info { "Creating ERC20 send request, params: $params" }

        val databaseParams = erc20CommonService.createDatabaseParams(StoreErc20SendRequestParams, params)
        val data = encodeFunctionData(params.tokenRecipientAddress, params.tokenAmount, databaseParams.id)
        val erc20SendRequest = erc20SendRequestRepository.store(databaseParams)

        return WithFunctionData(erc20SendRequest, data)
    }

    override fun getErc20SendRequest(id: UUID, rpcSpec: RpcUrlSpec): WithTransactionData<Erc20SendRequest> {
        logger.debug { "Fetching ERC20 send request, id: $id, rpcSpec: $rpcSpec" }

        val erc20SendRequest = erc20CommonService.fetchResource(
            erc20SendRequestRepository.getById(id),
            "ERC20 send request not found for ID: $id"
        )

        return erc20SendRequest.appendTransactionData(rpcSpec)
    }

    override fun getErc20SendRequestsBySender(
        sender: WalletAddress,
        rpcSpec: RpcUrlSpec
    ): List<WithTransactionData<Erc20SendRequest>> {
        logger.debug { "Fetching ERC20 send requests for sender: $sender, rpcSpec: $rpcSpec" }
        return erc20SendRequestRepository.getBySender(sender).map { it.appendTransactionData(rpcSpec) }
    }

    override fun getErc20SendRequestsByRecipient(
        recipient: WalletAddress,
        rpcSpec: RpcUrlSpec
    ): List<WithTransactionData<Erc20SendRequest>> {
        logger.debug { "Fetching ERC20 send requests for recipient: $recipient, rpcSpec: $rpcSpec" }
        return erc20SendRequestRepository.getByRecipient(recipient).map { it.appendTransactionData(rpcSpec) }
    }

    override fun attachTxInfo(id: UUID, txHash: TransactionHash, caller: WalletAddress) {
        logger.info { "Attach tx info to ERC20 send request, id: $id, txHash: $txHash, caller: $caller" }

        val txInfoAttached = erc20SendRequestRepository.setTxInfo(id, txHash, caller)

        if (txInfoAttached.not()) {
            throw CannotAttachTxInfoException("Unable to attach transaction info to ERC20 send request with ID: $id")
        }
    }

    private fun encodeFunctionData(tokenRecipientAddress: WalletAddress, tokenAmount: Balance, id: UUID): FunctionData =
        functionEncoderService.encode(
            functionName = "transfer",
            arguments = listOf(
                FunctionArgument(abiType = AbiType.Address, value = tokenRecipientAddress),
                FunctionArgument(abiType = AbiType.Uint256, value = tokenAmount)
            ),
            abiOutputTypes = listOf(AbiType.Bool),
            additionalData = listOf(Utf8String(id.toString()))
        )

    private fun Erc20SendRequest.appendTransactionData(rpcSpec: RpcUrlSpec): WithTransactionData<Erc20SendRequest> {
        val transactionInfo = erc20CommonService.fetchTransactionInfo(
            txHash = txHash,
            chainId = chainId,
            rpcSpec = rpcSpec
        )
        val data = encodeFunctionData(tokenRecipientAddress, tokenAmount, id)
        val status = determineStatus(transactionInfo, data)

        return withTransactionData(
            status = status,
            data = data,
            transactionInfo = transactionInfo
        )
    }

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
        transactionInfo.success && tokenAddress == transactionInfo.to.toContractAddress() &&
            txHash == transactionInfo.hash &&
            senderAddressMatches(tokenSenderAddress, transactionInfo.from) &&
            transactionInfo.data == expectedData

    private fun senderAddressMatches(senderAddress: WalletAddress?, fromAddress: WalletAddress): Boolean =
        senderAddress == null || senderAddress == fromAddress
}
