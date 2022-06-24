package com.ampnet.blockchainapiservice.model.request

import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.fasterxml.jackson.databind.JsonNode
import java.math.BigInteger

data class CreateErc20SendRequest(
    val redirectUrl: String?,
    val tokenAddress: String,
    val amount: BigInteger,
    val senderAddress: String?,
    val recipientAddress: String,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig?
)
