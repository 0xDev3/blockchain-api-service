package dev3.blockchainapiservice.model.params

import com.fasterxml.jackson.databind.JsonNode
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.UtcDateTime
import dev3.blockchainapiservice.util.WalletAddress
import java.util.UUID

data class StoreAssetMultiSendRequestParams(
    val id: UUID,
    val projectId: UUID,
    val chainId: ChainId,
    val redirectUrl: String,
    val tokenAddress: ContractAddress?,
    val disperseContractAddress: ContractAddress,
    val assetAmounts: List<Balance>,
    val assetRecipientAddresses: List<WalletAddress>,
    val itemNames: List<String?>,
    val assetSenderAddress: WalletAddress?,
    val arbitraryData: JsonNode?,
    val approveScreenConfig: ScreenConfig,
    val disperseScreenConfig: ScreenConfig,
    val createdAt: UtcDateTime
) {
    companion object : ParamsFactory<CreateAssetMultiSendRequestParams, StoreAssetMultiSendRequestParams> {
        private const val PATH = "/request-multi-send/\${id}/action"

        override fun fromCreateParams(
            id: UUID,
            params: CreateAssetMultiSendRequestParams,
            project: Project,
            createdAt: UtcDateTime
        ) = StoreAssetMultiSendRequestParams(
            id = id,
            projectId = project.id,
            chainId = project.chainId,
            redirectUrl = project.createRedirectUrl(params.redirectUrl, id, PATH),
            tokenAddress = params.tokenAddress,
            disperseContractAddress = params.disperseContractAddress,
            assetAmounts = params.assetAmounts,
            assetRecipientAddresses = params.assetRecipientAddresses,
            itemNames = params.itemNames,
            assetSenderAddress = params.assetSenderAddress,
            arbitraryData = params.arbitraryData,
            disperseScreenConfig = params.disperseScreenConfig,
            approveScreenConfig = params.approveScreenConfig,
            createdAt = createdAt
        )
    }
}
