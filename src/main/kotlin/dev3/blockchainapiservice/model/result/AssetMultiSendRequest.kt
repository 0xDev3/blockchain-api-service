package dev3.blockchainapiservice.model.result

import com.fasterxml.jackson.databind.JsonNode
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.FunctionData
import dev3.blockchainapiservice.util.Status
import dev3.blockchainapiservice.util.TransactionData
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.UtcDateTime
import dev3.blockchainapiservice.util.WalletAddress
import dev3.blockchainapiservice.util.WithMultiTransactionData
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
