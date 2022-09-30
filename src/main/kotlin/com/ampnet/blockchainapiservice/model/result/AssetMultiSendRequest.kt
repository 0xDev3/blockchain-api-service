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
import com.ampnet.blockchainapiservice.util.WithMultiTransactionData
import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID

data class AssetMultiSendRequest(
    val id: UUID,
    val projectId: UUID,
    val chainId: ChainId,
    val redirectUrl: String,
    val tokenAddress: ContractAddress?,
    val disperseContractAddress: ContractAddress,
    val assetAmounts: List<Balance>,
    val assetRecipientAddresses: List<WalletAddress>,
    val itemNames: List<String?>,
    val assetSenderAddress: WalletAddress?,
    val approveTxHash: TransactionHash?,
    val disperseTxHash: TransactionHash?,
    val arbitraryData: JsonNode?,
    val approveScreenConfig: ScreenConfig,
    val disperseScreenConfig: ScreenConfig,
    val createdAt: UtcDateTime
) {
    @Suppress("LongParameterList")
    fun withMultiTransactionData(
        approveStatus: Status?,
        approveData: FunctionData?,
        approveTransactionInfo: BlockchainTransactionInfo?,
        disperseStatus: Status?,
        disperseData: FunctionData?,
        disperseValue: Balance?,
        disperseTransactionInfo: BlockchainTransactionInfo?
    ): WithMultiTransactionData<AssetMultiSendRequest> =
        WithMultiTransactionData(
            value = this,
            approveStatus = approveStatus,
            approveTransactionData = tokenAddress?.let {
                TransactionData(
                    txHash = this.approveTxHash,
                    transactionInfo = approveTransactionInfo,
                    fromAddress = this.assetSenderAddress,
                    toAddress = it,
                    data = approveData,
                    value = Balance.ZERO
                )
            },
            disperseStatus = disperseStatus,
            disperseTransactionData = if (disperseStatus != null) {
                TransactionData(
                    txHash = this.disperseTxHash,
                    transactionInfo = disperseTransactionInfo,
                    fromAddress = this.assetSenderAddress,
                    toAddress = this.disperseContractAddress,
                    data = disperseData,
                    value = disperseValue
                )
            } else null
        )
}
