package dev3.blockchainapiservice.model.params

import com.fasterxml.jackson.databind.JsonNode
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.util.UtcDateTime
import dev3.blockchainapiservice.util.WalletAddress
import java.util.UUID

data class StoreAuthorizationRequestParams(
    val id: UUID,
    val projectId: UUID,
    val redirectUrl: String,
    val messageToSignOverride: String?,
    val storeIndefinitely: Boolean,
    val requestedWalletAddress: WalletAddress?,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig,
    val createdAt: UtcDateTime
) {
    companion object : ParamsFactory<CreateAuthorizationRequestParams, StoreAuthorizationRequestParams> {
        private const val PATH = "/request-authorization/\${id}/action"

        override fun fromCreateParams(
            id: UUID,
            params: CreateAuthorizationRequestParams,
            project: Project,
            createdAt: UtcDateTime
        ) = StoreAuthorizationRequestParams(
            id = id,
            projectId = project.id,
            redirectUrl = project.createRedirectUrl(params.redirectUrl, id, PATH),
            messageToSignOverride = params.messageToSign,
            storeIndefinitely = params.storeIndefinitely,
            requestedWalletAddress = params.requestedWalletAddress,
            arbitraryData = params.arbitraryData,
            screenConfig = params.screenConfig,
            createdAt = createdAt
        )
    }
}
