package dev3.blockchainapiservice.model.response

import com.fasterxml.jackson.databind.JsonNode
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.model.result.AssetSendRequest
import dev3.blockchainapiservice.util.AssetType
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.Status
import dev3.blockchainapiservice.util.WithFunctionDataOrEthValue
import dev3.blockchainapiservice.util.WithTransactionData
import java.math.BigInteger
import java.time.OffsetDateTime
import java.util.UUID

data class AssetSendRequestResponse(
    val id: UUID,
    val projectId: UUID,
    val status: Status,
    val chainId: Long,
    val tokenAddress: String?,
    val assetType: AssetType,
    val amount: BigInteger,
    val senderAddress: String?,
    val recipientAddress: String,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig?,
    val redirectUrl: String,
    val sendTx: TransactionResponse,
    val createdAt: OffsetDateTime
) {
    constructor(sendRequest: WithFunctionDataOrEthValue<AssetSendRequest>) : this(
        id = sendRequest.value.id,
        projectId = sendRequest.value.projectId,
        status = Status.PENDING,
        chainId = sendRequest.value.chainId.value,
        tokenAddress = sendRequest.value.tokenAddress?.rawValue,
        assetType = if (sendRequest.value.tokenAddress != null) AssetType.TOKEN else AssetType.NATIVE,
        amount = sendRequest.value.assetAmount.rawValue,
        senderAddress = sendRequest.value.assetSenderAddress?.rawValue,
        recipientAddress = sendRequest.value.assetRecipientAddress.rawValue,
        arbitraryData = sendRequest.value.arbitraryData,
        screenConfig = sendRequest.value.screenConfig.orEmpty(),
        redirectUrl = sendRequest.value.redirectUrl,
        sendTx = TransactionResponse.unmined(
            from = sendRequest.value.assetSenderAddress,
            to = sendRequest.value.tokenAddress ?: sendRequest.value.assetRecipientAddress,
            data = sendRequest.data,
            value = sendRequest.ethValue ?: Balance.ZERO
        ),
        createdAt = sendRequest.value.createdAt.value
    )

    constructor(sendRequest: WithTransactionData<AssetSendRequest>) : this(
        id = sendRequest.value.id,
        projectId = sendRequest.value.projectId,
        status = sendRequest.status,
        chainId = sendRequest.value.chainId.value,
        tokenAddress = sendRequest.value.tokenAddress?.rawValue,
        assetType = if (sendRequest.value.tokenAddress != null) AssetType.TOKEN else AssetType.NATIVE,
        amount = sendRequest.value.assetAmount.rawValue,
        senderAddress = sendRequest.value.assetSenderAddress?.rawValue,
        recipientAddress = sendRequest.value.assetRecipientAddress.rawValue,
        arbitraryData = sendRequest.value.arbitraryData,
        screenConfig = sendRequest.value.screenConfig.orEmpty(),
        redirectUrl = sendRequest.value.redirectUrl,
        sendTx = TransactionResponse(sendRequest.transactionData),
        createdAt = sendRequest.value.createdAt.value
    )
}
