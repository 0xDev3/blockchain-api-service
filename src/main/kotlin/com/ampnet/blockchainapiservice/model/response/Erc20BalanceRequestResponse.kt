package com.ampnet.blockchainapiservice.model.response

import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.util.Status
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import java.math.BigInteger
import java.time.OffsetDateTime
import java.util.UUID

data class Erc20BalanceRequestResponse(
    val id: UUID,
    val status: Status,
    val chainId: Long,
    val redirectUrl: String,
    val tokenAddress: String,
    @JsonSerialize(using = ToStringSerializer::class)
    val blockNumber: BigInteger?,
    val walletAddress: String?,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig?,
    val balance: BalanceResponse?,
    val messageToSign: String,
    val signedMessage: String?
)

data class BalanceResponse(
    val wallet: String,
    @JsonSerialize(using = ToStringSerializer::class)
    val blockNumber: BigInteger,
    val timestamp: OffsetDateTime,
    @JsonSerialize(using = ToStringSerializer::class)
    val amount: BigInteger
)
