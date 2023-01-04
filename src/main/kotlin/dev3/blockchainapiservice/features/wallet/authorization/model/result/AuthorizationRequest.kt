package dev3.blockchainapiservice.features.wallet.authorization.model.result

import com.fasterxml.jackson.databind.JsonNode
import dev3.blockchainapiservice.generated.jooq.id.AuthorizationRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.util.SignedMessage
import dev3.blockchainapiservice.util.UtcDateTime
import dev3.blockchainapiservice.util.WalletAddress

data class AuthorizationRequest(
    val id: AuthorizationRequestId,
    val projectId: ProjectId,
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
        get() = messageToSignOverride ?: "Authorization message ID to sign: ${id.value}"
}
