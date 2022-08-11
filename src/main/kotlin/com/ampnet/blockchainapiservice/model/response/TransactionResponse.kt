package com.ampnet.blockchainapiservice.model.response

import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.EthereumAddress
import com.ampnet.blockchainapiservice.util.FunctionData
import com.ampnet.blockchainapiservice.util.TransactionData
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import java.math.BigInteger
import java.time.OffsetDateTime

data class TransactionResponse(
    val txHash: String?,
    val from: String?,
    val to: String,
    val data: String?,
    @JsonSerialize(using = ToStringSerializer::class)
    val value: BigInteger,
    @JsonSerialize(using = ToStringSerializer::class)
    val blockConfirmations: BigInteger?,
    val timestamp: OffsetDateTime?
) {
    companion object {
        fun unmined(from: WalletAddress?, to: EthereumAddress, data: FunctionData?, value: Balance) =
            TransactionResponse(
                txHash = null,
                from = from?.rawValue,
                to = to.rawValue,
                data = data?.nullIfEmpty()?.value,
                value = value.rawValue,
                blockConfirmations = null,
                timestamp = null
            )

        private fun FunctionData.nullIfEmpty(): FunctionData? =
            if (this == FunctionData.EMPTY) null else this
    }

    constructor(transactionData: TransactionData) : this(
        txHash = transactionData.txHash?.value,
        from = transactionData.fromAddress?.rawValue,
        to = transactionData.toAddress.rawValue,
        data = transactionData.data?.nullIfEmpty()?.value,
        value = transactionData.value.rawValue,
        blockConfirmations = transactionData.blockConfirmations,
        timestamp = transactionData.timestamp?.value
    )
}
