package com.ampnet.blockchainapiservice.model.response

import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.result.AssetBalanceRequest
import com.ampnet.blockchainapiservice.model.result.FullAssetBalanceRequest
import com.ampnet.blockchainapiservice.util.AssetType
import com.ampnet.blockchainapiservice.util.Status
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import java.math.BigInteger
import java.time.OffsetDateTime
import java.util.UUID

data class AssetBalanceRequestResponse(
    val id: UUID,
    val projectId: UUID,
    val status: Status,
    val chainId: Long,
    val redirectUrl: String,
    val tokenAddress: String?,
    val assetType: AssetType,
    @JsonSerialize(using = ToStringSerializer::class)
    val blockNumber: BigInteger?,
    val walletAddress: String?,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig?,
    val balance: BalanceResponse?,
    val messageToSign: String,
    val signedMessage: String?,
    val createdAt: OffsetDateTime
) {
    constructor(balanceRequest: AssetBalanceRequest) : this(
        id = balanceRequest.id,
        projectId = balanceRequest.projectId,
        status = Status.PENDING,
        chainId = balanceRequest.chainId.value,
        redirectUrl = balanceRequest.redirectUrl,
        tokenAddress = balanceRequest.tokenAddress?.rawValue,
        assetType = if (balanceRequest.tokenAddress != null) AssetType.TOKEN else AssetType.NATIVE,
        blockNumber = balanceRequest.blockNumber?.value,
        walletAddress = balanceRequest.requestedWalletAddress?.rawValue,
        arbitraryData = balanceRequest.arbitraryData,
        screenConfig = balanceRequest.screenConfig.orEmpty(),
        balance = null,
        messageToSign = balanceRequest.messageToSign,
        signedMessage = balanceRequest.signedMessage?.value,
        createdAt = balanceRequest.createdAt.value
    )

    constructor(balanceRequest: FullAssetBalanceRequest) : this(
        id = balanceRequest.id,
        projectId = balanceRequest.projectId,
        status = balanceRequest.status,
        chainId = balanceRequest.chainId.value,
        redirectUrl = balanceRequest.redirectUrl,
        tokenAddress = balanceRequest.tokenAddress?.rawValue,
        assetType = if (balanceRequest.tokenAddress != null) AssetType.TOKEN else AssetType.NATIVE,
        blockNumber = balanceRequest.blockNumber?.value,
        walletAddress = balanceRequest.requestedWalletAddress?.rawValue,
        arbitraryData = balanceRequest.arbitraryData,
        screenConfig = balanceRequest.screenConfig.orEmpty(),
        balance = balanceRequest.balance?.let {
            BalanceResponse(
                wallet = it.wallet.rawValue,
                blockNumber = it.blockNumber.value,
                timestamp = it.timestamp.value,
                amount = it.amount.rawValue
            )
        },
        messageToSign = balanceRequest.messageToSign,
        signedMessage = balanceRequest.signedMessage?.value,
        createdAt = balanceRequest.createdAt.value
    )
}

data class BalanceResponse(
    val wallet: String,
    @JsonSerialize(using = ToStringSerializer::class)
    val blockNumber: BigInteger,
    val timestamp: OffsetDateTime,
    @JsonSerialize(using = ToStringSerializer::class)
    val amount: BigInteger
)
