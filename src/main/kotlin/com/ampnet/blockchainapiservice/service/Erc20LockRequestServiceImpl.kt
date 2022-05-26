package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.blockchain.properties.RpcUrlSpec
import com.ampnet.blockchainapiservice.exception.CannotAttachTxHashException
import com.ampnet.blockchainapiservice.model.params.CreateErc20LockRequestParams
import com.ampnet.blockchainapiservice.model.params.StoreErc20LockRequestParams
import com.ampnet.blockchainapiservice.model.result.BlockchainTransactionInfo
import com.ampnet.blockchainapiservice.model.result.Erc20LockRequest
import com.ampnet.blockchainapiservice.repository.Erc20LockRequestRepository
import com.ampnet.blockchainapiservice.util.AbiType.AbiType
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ContractAddress
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
class Erc20LockRequestServiceImpl(
    private val functionEncoderService: FunctionEncoderService,
    private val erc20LockRequestRepository: Erc20LockRequestRepository,
    private val erc20CommonService: Erc20CommonServiceImpl
) : Erc20LockRequestService {

    companion object : KLogging()

    override fun createErc20LockRequest(params: CreateErc20LockRequestParams): WithFunctionData<Erc20LockRequest> {
        logger.info { "Creating ERC20 lock request, params: $params" }

        val databaseParams = erc20CommonService.createDatabaseParams(StoreErc20LockRequestParams, params)
        val data = encodeFunctionData(databaseParams.tokenAddress, databaseParams.tokenAmount, databaseParams.id)
        val erc20LockRequest = erc20LockRequestRepository.store(databaseParams)

        return WithFunctionData(erc20LockRequest, data)
    }

    override fun getErc20LockRequest(id: UUID, rpcSpec: RpcUrlSpec): WithTransactionData<Erc20LockRequest> {
        logger.debug { "Fetching ERC20 lock request, id: $id, rpcSpec: $rpcSpec" }

        val erc20LockRequest = erc20CommonService.fetchResource(
            erc20LockRequestRepository.getById(id),
            "ERC20 lock request not found for ID: $id"
        )

        val transactionInfo = erc20CommonService.fetchTransactionInfo(
            txHash = erc20LockRequest.txHash,
            chainId = erc20LockRequest.chainId,
            rpcSpec = rpcSpec
        )
        val data = encodeFunctionData(erc20LockRequest.tokenAddress, erc20LockRequest.tokenAmount, id)
        val status = erc20LockRequest.determineStatus(transactionInfo, data)

        return erc20LockRequest.withTransactionData(
            status = status,
            data = data,
            transactionInfo = transactionInfo
        )
    }

    override fun attachTxHash(id: UUID, txHash: TransactionHash) {
        logger.info { "Attach txHash to ERC20 lock request, id: $id, txHash: $txHash" }

        val txHashAttached = erc20LockRequestRepository.setTxHash(id, txHash)

        if (txHashAttached.not()) {
            throw CannotAttachTxHashException("Unable to attach transaction hash to ERC20 lock request with ID: $id")
        }
    }

    private fun encodeFunctionData(tokenAddress: ContractAddress, tokenAmount: Balance, id: UUID): FunctionData =
        functionEncoderService.encode(
            functionName = "lock", // TODO what will be the actual lock function we will use?
            arguments = listOf(
                FunctionArgument(abiType = AbiType.Address, value = tokenAddress),
                FunctionArgument(abiType = AbiType.Uint256, value = tokenAmount)
            ),
            abiOutputTypes = listOf(AbiType.Bool),
            additionalData = listOf(Utf8String(id.toString()))
        )

    private fun Erc20LockRequest.determineStatus(
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

    private fun Erc20LockRequest.isSuccess(
        transactionInfo: BlockchainTransactionInfo,
        expectedData: FunctionData
    ): Boolean =
        lockContractAddress == transactionInfo.to.toContractAddress() &&
            txHash == transactionInfo.hash &&
            senderAddressMatches(tokenSenderAddress, transactionInfo.from) &&
            transactionInfo.data == expectedData

    private fun senderAddressMatches(senderAddress: WalletAddress?, fromAddress: WalletAddress): Boolean =
        senderAddress == null || senderAddress == fromAddress
}
