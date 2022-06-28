package com.ampnet.blockchainapiservice.model.response

import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.result.Erc20SendRequest
import com.ampnet.blockchainapiservice.util.AssetType
import com.ampnet.blockchainapiservice.util.Status
import com.ampnet.blockchainapiservice.util.WithFunctionDataOrEthValue
import com.ampnet.blockchainapiservice.util.WithTransactionData
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import java.math.BigInteger
import java.time.OffsetDateTime
import java.util.UUID

data class Erc20SendRequestResponse(
    val id: UUID,
    val projectId: UUID,
    val status: Status,
    val chainId: Long,
    val tokenAddress: String?,
    val assetType: AssetType,
    @JsonSerialize(using = ToStringSerializer::class)
    val amount: BigInteger,
    val senderAddress: String?,
    val recipientAddress: String,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig?,
    val redirectUrl: String,
    val sendTx: TransactionResponse,
    val createdAt: OffsetDateTime
) {
    constructor(sendRequest: WithFunctionDataOrEthValue<Erc20SendRequest>) : this(
        id = sendRequest.value.id,
        projectId = sendRequest.value.projectId,
        status = Status.PENDING,
        chainId = sendRequest.value.chainId.value,
        tokenAddress = sendRequest.value.tokenAddress?.rawValue,
        assetType = if (sendRequest.value.tokenAddress != null) AssetType.TOKEN else AssetType.NATIVE,
        amount = sendRequest.value.tokenAmount.rawValue,
        senderAddress = sendRequest.value.tokenSenderAddress?.rawValue,
        recipientAddress = sendRequest.value.tokenRecipientAddress.rawValue,
        arbitraryData = sendRequest.value.arbitraryData,
        screenConfig = sendRequest.value.screenConfig.orEmpty(),
        redirectUrl = sendRequest.value.redirectUrl,
        sendTx = TransactionResponse(
            txHash = null,
            from = sendRequest.value.tokenSenderAddress?.rawValue,
            to = sendRequest.value.tokenAddress?.rawValue ?: sendRequest.value.tokenRecipientAddress.rawValue,
            data = sendRequest.data?.value,
            value = sendRequest.ethValue?.rawValue,
            blockConfirmations = null,
            timestamp = null
        ),
        createdAt = sendRequest.value.createdAt.value
    )

    constructor(sendRequest: WithTransactionData<Erc20SendRequest>) : this(
        id = sendRequest.value.id,
        projectId = sendRequest.value.projectId,
        status = sendRequest.status,
        chainId = sendRequest.value.chainId.value,
        tokenAddress = sendRequest.value.tokenAddress?.rawValue,
        assetType = if (sendRequest.value.tokenAddress != null) AssetType.TOKEN else AssetType.NATIVE,
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
            data = sendRequest.transactionData.data?.value,
            value = sendRequest.transactionData.value?.rawValue,
            blockConfirmations = sendRequest.transactionData.blockConfirmations,
            timestamp = sendRequest.transactionData.timestamp?.value
        ),
        createdAt = sendRequest.value.createdAt.value
    )
}
