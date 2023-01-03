package dev3.blockchainapiservice.repository

import dev3.blockchainapiservice.generated.jooq.id.AssetSendRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.model.params.StoreAssetSendRequestParams
import dev3.blockchainapiservice.model.result.AssetSendRequest
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress

interface AssetSendRequestRepository {
    fun store(params: StoreAssetSendRequestParams): AssetSendRequest
    fun getById(id: AssetSendRequestId): AssetSendRequest?
    fun getAllByProjectId(projectId: ProjectId): List<AssetSendRequest>
    fun getBySender(sender: WalletAddress): List<AssetSendRequest>
    fun getByRecipient(recipient: WalletAddress): List<AssetSendRequest>
    fun setTxInfo(id: AssetSendRequestId, txHash: TransactionHash, caller: WalletAddress): Boolean
}
