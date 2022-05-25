package com.ampnet.blockchainapiservice.model.result

import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID

data class Erc20LockRequest(
    val id: UUID,
    val chainId: ChainId,
    val redirectUrl: String,
    val tokenAddress: ContractAddress,
    val tokenAmount: Balance,
    val lockContractAddress: ContractAddress,
    val tokenSenderAddress: WalletAddress?,
    val txHash: TransactionHash?,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig
)
