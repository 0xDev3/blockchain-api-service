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
import dev3.blockchainapiservice.util.WithTransactionData
import java.util.UUID

data class AssetSendRequest(
    val id: UUID,
    val projectId: UUID,
    val chainId: ChainId,
    val redirectUrl: String,
    val tokenAddress: ContractAddress?,
    val assetAmount: Balance,
    val assetSenderAddress: WalletAddress?,
    val assetRecipientAddress: WalletAddress,
    val txHash: TransactionHash?,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig,
    val createdAt: UtcDateTime
) {
    fun withTransactionData(
        status: Status,
        data: FunctionData?,
        value: Balance?,
        transactionInfo: BlockchainTransactionInfo?
    ): WithTransactionData<AssetSendRequest> =
        WithTransactionData(
            value = this,
            status = status,
            transactionData = TransactionData(
                txHash = this.txHash,
                transactionInfo = transactionInfo,
                fromAddress = this.assetSenderAddress,
                toAddress = this.tokenAddress ?: this.assetRecipientAddress,
                data = data,
                value = value
            )
        )
}