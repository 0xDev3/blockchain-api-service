package com.ampnet.blockchainapiservice.model.response

import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.result.AssetMultiSendRequest
import com.ampnet.blockchainapiservice.util.AssetType
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.Status
import com.ampnet.blockchainapiservice.util.WithFunctionDataOrEthValue
import com.ampnet.blockchainapiservice.util.WithMultiTransactionData
import com.fasterxml.jackson.databind.JsonNode
import java.math.BigInteger
import java.time.OffsetDateTime
import java.util.UUID

data class AssetMultiSendRequestResponse(
    val id: UUID,
    val projectId: UUID,
    val approveStatus: Status?,
    val disperseStatus: Status,
    val chainId: Long,
    val tokenAddress: String?,
    val disperseContractAddress: String,
    val assetType: AssetType,
    val items: List<MultiSendItemResponse>,
    val senderAddress: String?,
    val arbitraryData: JsonNode?,
    val approveScreenConfig: ScreenConfig?,
    val disperseScreenConfig: ScreenConfig?,
    val redirectUrl: String,
    val approveTx: TransactionResponse?,
    val disperseTx: TransactionResponse?,
    val createdAt: OffsetDateTime
) {
    constructor(request: WithFunctionDataOrEthValue<AssetMultiSendRequest>) : this(
        id = request.value.id,
        projectId = request.value.projectId,
        approveStatus = if (request.value.tokenAddress == null) null else Status.PENDING,
        disperseStatus = Status.PENDING,
        chainId = request.value.chainId.value,
        tokenAddress = request.value.tokenAddress?.rawValue,
        disperseContractAddress = request.value.disperseContractAddress.rawValue,
        assetType = if (request.value.tokenAddress != null) AssetType.TOKEN else AssetType.NATIVE,
        items = request.value.assetAmounts
            .zip(request.value.assetRecipientAddresses)
            .zip(request.value.itemNames)
            .map {
                MultiSendItemResponse(
                    walletAddress = it.first.second.rawValue,
                    amount = it.first.first.rawValue,
                    itemName = it.second
                )
            },
        senderAddress = request.value.assetSenderAddress?.rawValue,
        arbitraryData = request.value.arbitraryData,
        approveScreenConfig = request.value.approveScreenConfig.orEmpty(),
        disperseScreenConfig = request.value.disperseScreenConfig.orEmpty(),
        redirectUrl = request.value.redirectUrl,
        approveTx = request.value.tokenAddress?.let {
            TransactionResponse.unmined(
                from = request.value.assetSenderAddress,
                to = it,
                data = request.data,
                value = Balance.ZERO
            )
        },
        disperseTx = if (request.value.tokenAddress == null) {
            TransactionResponse.unmined(
                from = request.value.assetSenderAddress,
                to = request.value.disperseContractAddress,
                data = request.data,
                value = request.ethValue ?: Balance.ZERO
            )
        } else null,
        createdAt = request.value.createdAt.value
    )

    constructor(request: WithMultiTransactionData<AssetMultiSendRequest>) : this(
        id = request.value.id,
        projectId = request.value.projectId,
        approveStatus = request.approveStatus,
        disperseStatus = request.disperseStatus ?: Status.PENDING,
        chainId = request.value.chainId.value,
        tokenAddress = request.value.tokenAddress?.rawValue,
        disperseContractAddress = request.value.disperseContractAddress.rawValue,
        assetType = if (request.value.tokenAddress != null) AssetType.TOKEN else AssetType.NATIVE,
        items = request.value.assetAmounts
            .zip(request.value.assetRecipientAddresses)
            .zip(request.value.itemNames)
            .map {
                MultiSendItemResponse(
                    walletAddress = it.first.second.rawValue,
                    amount = it.first.first.rawValue,
                    itemName = it.second
                )
            },
        senderAddress = request.value.assetSenderAddress?.rawValue,
        arbitraryData = request.value.arbitraryData,
        approveScreenConfig = request.value.approveScreenConfig.orEmpty(),
        disperseScreenConfig = request.value.disperseScreenConfig.orEmpty(),
        redirectUrl = request.value.redirectUrl,
        approveTx = request.approveTransactionData?.let { TransactionResponse(it) },
        disperseTx = request.disperseTransactionData?.let { TransactionResponse(it) },
        createdAt = request.value.createdAt.value
    )
}

data class MultiSendItemResponse(
    val walletAddress: String,
    val amount: BigInteger,
    val itemName: String?
)
