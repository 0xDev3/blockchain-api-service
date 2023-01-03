package dev3.blockchainapiservice.features.asset.balance.service

import dev3.blockchainapiservice.features.asset.balance.model.params.CreateAssetBalanceRequestParams
import dev3.blockchainapiservice.features.asset.balance.model.result.AssetBalanceRequest
import dev3.blockchainapiservice.features.asset.balance.model.result.FullAssetBalanceRequest
import dev3.blockchainapiservice.generated.jooq.id.AssetBalanceRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.util.SignedMessage
import dev3.blockchainapiservice.util.WalletAddress

interface AssetBalanceRequestService {
    fun createAssetBalanceRequest(params: CreateAssetBalanceRequestParams, project: Project): AssetBalanceRequest
    fun getAssetBalanceRequest(id: AssetBalanceRequestId): FullAssetBalanceRequest
    fun getAssetBalanceRequestsByProjectId(projectId: ProjectId): List<FullAssetBalanceRequest>
    fun attachWalletAddressAndSignedMessage(
        id: AssetBalanceRequestId,
        walletAddress: WalletAddress,
        signedMessage: SignedMessage
    )
}
