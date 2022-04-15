package com.ampnet.blockchainapiservice.model.request

import com.ampnet.blockchainapiservice.model.SendScreenConfig
import com.fasterxml.jackson.databind.JsonNode
import java.math.BigInteger

data class CreateSendErc20Request(
    val clientId: String?,
    val chainId: Long?,
    val redirectUrl: String?,
    val tokenAddress: String,
    val amount: BigInteger,
    val fromAddress: String?,
    val toAddress: String,
    val arbitraryData: JsonNode?,
    val screenConfig: SendScreenConfig?
)
