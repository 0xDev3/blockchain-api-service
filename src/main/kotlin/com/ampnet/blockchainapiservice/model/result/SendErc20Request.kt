package com.ampnet.blockchainapiservice.model.result

import com.ampnet.blockchainapiservice.model.SendScreenConfig
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID

data class SendErc20Request(
    val id: UUID,
    val chainId: ChainId,
    val redirectUrl: String,
    val tokenAddress: ContractAddress,
    val amount: Balance,
    val arbitraryData: JsonNode?,
    val sendScreenConfig: SendScreenConfig,
    val transactionData: TransactionData
)

data class TransactionData(
    val txHash: TransactionHash?,
    val fromAddress: WalletAddress?,
    val toAddress: WalletAddress
)
