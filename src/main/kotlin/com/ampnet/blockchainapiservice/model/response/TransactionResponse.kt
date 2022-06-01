package com.ampnet.blockchainapiservice.model.response

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import java.math.BigInteger

data class TransactionResponse(
    val txHash: String?,
    val from: String?,
    val to: String,
    val data: String,
    @JsonSerialize(using = ToStringSerializer::class)
    val blockConfirmations: BigInteger?
)
