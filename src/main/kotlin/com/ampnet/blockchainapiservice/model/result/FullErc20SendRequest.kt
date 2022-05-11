package com.ampnet.blockchainapiservice.model.result

import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.FunctionData
import com.ampnet.blockchainapiservice.util.Status
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.fasterxml.jackson.databind.JsonNode
import java.math.BigInteger
import java.util.UUID

data class FullErc20SendRequest(
    val id: UUID,
    val status: Status,
    val chainId: ChainId,
    val redirectUrl: String,
    val tokenAddress: ContractAddress,
    val tokenAmount: Balance,
    val tokenSenderAddress: WalletAddress?,
    val tokenRecipientAddress: WalletAddress,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig,
    val transactionData: FullTransactionData
) {
    companion object {
        fun fromErc20SendRequest(
            request: Erc20SendRequest,
            status: Status,
            data: FunctionData,
            transactionInfo: BlockchainTransactionInfo?
        ) = FullErc20SendRequest(
            id = request.id,
            status = status,
            chainId = request.chainId,
            redirectUrl = request.redirectUrl,
            tokenAddress = request.tokenAddress,
            tokenAmount = request.tokenAmount,
            tokenSenderAddress = request.tokenSenderAddress,
            tokenRecipientAddress = request.tokenRecipientAddress,
            arbitraryData = request.arbitraryData,
            screenConfig = request.screenConfig,
            transactionData = FullTransactionData(
                txHash = request.txHash,
                fromAddress = transactionInfo?.from,
                toAddress = transactionInfo?.to?.toContractAddress() ?: request.tokenAddress,
                data = data,
                blockConfirmations = transactionInfo?.blockConfirmations
            )
        )
    }
}

data class FullTransactionData(
    val txHash: TransactionHash?,
    val fromAddress: WalletAddress?,
    val toAddress: ContractAddress,
    val data: FunctionData,
    val blockConfirmations: BigInteger?
)
