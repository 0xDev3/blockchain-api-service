package dev3.blockchainapiservice.features.asset.balance.repository

import dev3.blockchainapiservice.features.asset.balance.model.params.StoreAssetBalanceRequestParams
import dev3.blockchainapiservice.features.asset.balance.model.result.AssetBalanceRequest
import dev3.blockchainapiservice.generated.jooq.id.AssetBalanceRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.util.SignedMessage
import dev3.blockchainapiservice.util.WalletAddress

interface AssetBalanceRequestRepository {
    fun store(params: StoreAssetBalanceRequestParams): AssetBalanceRequest
    fun getById(id: AssetBalanceRequestId): AssetBalanceRequest?
    fun getAllByProjectId(projectId: ProjectId): List<AssetBalanceRequest>
    fun setSignedMessage(id: AssetBalanceRequestId, walletAddress: WalletAddress, signedMessage: SignedMessage): Boolean
}
