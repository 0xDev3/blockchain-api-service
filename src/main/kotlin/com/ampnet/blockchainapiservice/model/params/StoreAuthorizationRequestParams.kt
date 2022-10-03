package com.ampnet.blockchainapiservice.model.params

import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.util.UtcDateTime
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.fasterxml.jackson.databind.JsonNode
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
