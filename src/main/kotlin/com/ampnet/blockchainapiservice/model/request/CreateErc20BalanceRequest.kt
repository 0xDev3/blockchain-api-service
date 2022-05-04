package com.ampnet.blockchainapiservice.model.request

import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.fasterxml.jackson.databind.JsonNode
import java.math.BigInteger

data class CreateErc20BalanceRequest(
    val clientId: String?,
    val chainId: Long?,
    val redirectUrl: String?,
    val tokenAddress: String?,
    val blockNumber: BigInteger?,
    val walletAddress: String?,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig?
)
