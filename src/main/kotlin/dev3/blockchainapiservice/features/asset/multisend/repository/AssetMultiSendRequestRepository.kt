package dev3.blockchainapiservice.features.asset.multisend.repository

import dev3.blockchainapiservice.features.asset.multisend.model.params.StoreAssetMultiSendRequestParams
import dev3.blockchainapiservice.features.asset.multisend.model.result.AssetMultiSendRequest
import dev3.blockchainapiservice.generated.jooq.id.AssetMultiSendRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress

interface AssetMultiSendRequestRepository {
    fun store(params: StoreAssetMultiSendRequestParams): AssetMultiSendRequest
    fun getById(id: AssetMultiSendRequestId): AssetMultiSendRequest?
    fun getAllByProjectId(projectId: ProjectId): List<AssetMultiSendRequest>
    fun getBySender(sender: WalletAddress): List<AssetMultiSendRequest>
    fun setApproveTxInfo(id: AssetMultiSendRequestId, txHash: TransactionHash, caller: WalletAddress): Boolean
    fun setDisperseTxInfo(id: AssetMultiSendRequestId, txHash: TransactionHash, caller: WalletAddress): Boolean
}
