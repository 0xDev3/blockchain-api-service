package com.ampnet.blockchainapiservice.model.result

import com.ampnet.blockchainapiservice.model.SendScreenConfig
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

data class FullSendErc20Request(
    val id: UUID,
    val status: Status,
    val chainId: ChainId,
    val redirectUrl: String,
    val tokenAddress: ContractAddress,
    val amount: Balance,
    val arbitraryData: JsonNode?,
    val sendScreenConfig: SendScreenConfig,
    val transactionData: FullTransactionData
)

data class FullTransactionData(
    val txHash: TransactionHash?,
    val fromAddress: WalletAddress?,
    val toAddress: WalletAddress,
    val data: FunctionData,
    val blockConfirmations: BigInteger?
)
