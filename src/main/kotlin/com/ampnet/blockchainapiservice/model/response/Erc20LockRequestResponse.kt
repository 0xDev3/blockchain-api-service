package com.ampnet.blockchainapiservice.model.response

import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.util.Status
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import java.math.BigInteger
import java.util.UUID

data class Erc20LockRequestResponse(
    val id: UUID,
    val status: Status,
    val chainId: Long,
    val tokenAddress: String,
    @JsonSerialize(using = ToStringSerializer::class)
    val amount: BigInteger,
    val lockContractAddress: String,
    val senderAddress: String?,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig?,
    val redirectUrl: String,
    val lockTx: TransactionResponse
)
