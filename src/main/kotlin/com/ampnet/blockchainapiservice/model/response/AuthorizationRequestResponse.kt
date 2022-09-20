package com.ampnet.blockchainapiservice.model.response

import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.result.AuthorizationRequest
import com.ampnet.blockchainapiservice.util.Status
import com.ampnet.blockchainapiservice.util.WithStatus
import com.fasterxml.jackson.databind.JsonNode
import java.time.OffsetDateTime
import java.util.UUID

data class AuthorizationRequestResponse(
    val id: UUID,
    val projectId: UUID,
    val status: Status,
    val redirectUrl: String,
    val walletAddress: String?,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig?,
    val messageToSign: String,
    val signedMessage: String?,
    val createdAt: OffsetDateTime
) {
    constructor(balanceRequest: AuthorizationRequest) : this(
        id = balanceRequest.id,
        projectId = balanceRequest.projectId,
        status = Status.PENDING,
        redirectUrl = balanceRequest.redirectUrl,
        walletAddress = balanceRequest.requestedWalletAddress?.rawValue,
        arbitraryData = balanceRequest.arbitraryData,
        screenConfig = balanceRequest.screenConfig.orEmpty(),
        messageToSign = balanceRequest.messageToSign,
        signedMessage = balanceRequest.signedMessage?.value,
        createdAt = balanceRequest.createdAt.value
    )

    constructor(balanceRequest: WithStatus<AuthorizationRequest>) : this(
        id = balanceRequest.value.id,
        projectId = balanceRequest.value.projectId,
        status = balanceRequest.status,
        redirectUrl = balanceRequest.value.redirectUrl,
        walletAddress = balanceRequest.value.requestedWalletAddress?.rawValue,
        arbitraryData = balanceRequest.value.arbitraryData,
        screenConfig = balanceRequest.value.screenConfig.orEmpty(),
        messageToSign = balanceRequest.value.messageToSign,
        signedMessage = balanceRequest.value.signedMessage?.value,
        createdAt = balanceRequest.value.createdAt.value
    )
}
