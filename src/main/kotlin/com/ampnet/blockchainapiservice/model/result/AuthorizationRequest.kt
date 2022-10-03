package com.ampnet.blockchainapiservice.model.result

import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.util.SignedMessage
import com.ampnet.blockchainapiservice.util.UtcDateTime
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID

data class AuthorizationRequest(
    val id: UUID,
    val projectId: UUID,
    val redirectUrl: String,
    val messageToSignOverride: String?,
    val storeIndefinitely: Boolean,
    val requestedWalletAddress: WalletAddress?,
    val actualWalletAddress: WalletAddress?,
    val signedMessage: SignedMessage?,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig,
    val createdAt: UtcDateTime
) {
    val messageToSign: String
        get() = messageToSignOverride ?: "Authorization message ID to sign: $id"
}
