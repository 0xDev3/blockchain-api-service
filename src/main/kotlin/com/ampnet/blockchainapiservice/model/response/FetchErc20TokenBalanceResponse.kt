package com.ampnet.blockchainapiservice.model.response

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import java.math.BigInteger

data class FetchErc20TokenBalanceResponse(
    val walletAddress: String,
    @JsonSerialize(using = ToStringSerializer::class)
    val tokenBalance: BigInteger,
    val tokenAddress: String
)
