package com.ampnet.blockchainapiservice.model.result

import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.FunctionData
import com.ampnet.blockchainapiservice.util.Status
import com.ampnet.blockchainapiservice.util.TransactionData
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.UtcDateTime
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.ampnet.blockchainapiservice.util.WithTransactionData
import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID

data class ContractFunctionCallRequest(
    val id: UUID,
    val deployedContractId: UUID?,
    val contractAddress: ContractAddress,
    val functionName: String,
    val functionParams: JsonNode,
    val ethAmount: Balance,
    val chainId: ChainId,
    val redirectUrl: String,
    val projectId: UUID,
    val createdAt: UtcDateTime,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig,
    val callerAddress: WalletAddress?,
    val txHash: TransactionHash?
) {
    fun withTransactionData(
        status: Status,
        data: FunctionData,
        transactionInfo: BlockchainTransactionInfo?
    ): WithTransactionData<ContractFunctionCallRequest> =
        WithTransactionData(
            value = this,
            status = status,
            transactionData = TransactionData(
                txHash = this.txHash,
                fromAddress = transactionInfo?.from ?: this.callerAddress,
                toAddress = transactionInfo?.to ?: this.contractAddress,
                data = data,
                value = transactionInfo?.value ?: ethAmount,
                blockConfirmations = transactionInfo?.blockConfirmations,
                timestamp = transactionInfo?.timestamp
            )
        )
}
