package com.ampnet.blockchainapiservice.model.response

import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.result.Erc20SendRequest
import com.ampnet.blockchainapiservice.util.Status
import com.ampnet.blockchainapiservice.util.WithFunctionData
import com.ampnet.blockchainapiservice.util.WithTransactionData
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import java.math.BigInteger
import java.util.UUID

data class Erc20SendRequestResponse(
    val id: UUID,
    val status: Status,
    val chainId: Long,
    val tokenAddress: String,
    @JsonSerialize(using = ToStringSerializer::class)
    val amount: BigInteger,
    val senderAddress: String?,
    val recipientAddress: String,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig?,
    val redirectUrl: String,
    val sendTx: TransactionResponse
) {
    constructor(sendRequest: WithFunctionData<Erc20SendRequest>) : this(
        id = sendRequest.value.id,
        status = Status.PENDING,
        chainId = sendRequest.value.chainId.value,
        tokenAddress = sendRequest.value.tokenAddress.rawValue,
        amount = sendRequest.value.tokenAmount.rawValue,
        senderAddress = sendRequest.value.tokenSenderAddress?.rawValue,
        recipientAddress = sendRequest.value.tokenRecipientAddress.rawValue,
        arbitraryData = sendRequest.value.arbitraryData,
        screenConfig = sendRequest.value.screenConfig.orEmpty(),
        redirectUrl = sendRequest.value.redirectUrl,
        sendTx = TransactionResponse(
            txHash = null,
            from = sendRequest.value.tokenSenderAddress?.rawValue,
            to = sendRequest.value.tokenAddress.rawValue,
            data = sendRequest.data.value,
            blockConfirmations = null,
            timestamp = null
        )
    )

    constructor(sendRequest: WithTransactionData<Erc20SendRequest>) : this(
        id = sendRequest.value.id,
        status = sendRequest.status,
        chainId = sendRequest.value.chainId.value,
        tokenAddress = sendRequest.value.tokenAddress.rawValue,
        amount = sendRequest.value.tokenAmount.rawValue,
        senderAddress = sendRequest.value.tokenSenderAddress?.rawValue,
        recipientAddress = sendRequest.value.tokenRecipientAddress.rawValue,
        arbitraryData = sendRequest.value.arbitraryData,
        screenConfig = sendRequest.value.screenConfig.orEmpty(),
        redirectUrl = sendRequest.value.redirectUrl,
        sendTx = TransactionResponse(
            txHash = sendRequest.transactionData.txHash?.value,
            from = sendRequest.transactionData.fromAddress?.rawValue,
            to = sendRequest.transactionData.toAddress.rawValue,
            data = sendRequest.transactionData.data.value,
            blockConfirmations = sendRequest.transactionData.blockConfirmations,
            timestamp = sendRequest.transactionData.timestamp?.value
        )
    )
}
