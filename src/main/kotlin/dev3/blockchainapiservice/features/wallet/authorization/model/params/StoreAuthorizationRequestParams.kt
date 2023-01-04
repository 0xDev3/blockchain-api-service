package dev3.blockchainapiservice.features.wallet.authorization.model.params

import com.fasterxml.jackson.databind.JsonNode
import dev3.blockchainapiservice.features.api.access.model.result.Project
import dev3.blockchainapiservice.generated.jooq.id.AuthorizationRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.model.params.ParamsFactory
import dev3.blockchainapiservice.util.UtcDateTime
import dev3.blockchainapiservice.util.WalletAddress
import java.util.UUID

data class StoreAuthorizationRequestParams(
    val id: AuthorizationRequestId,
    val projectId: ProjectId,
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
            id = AuthorizationRequestId(id),
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
