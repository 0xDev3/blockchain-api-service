package dev3.blockchainapiservice.repository

import dev3.blockchainapiservice.model.params.StoreAssetSendRequestParams
import dev3.blockchainapiservice.model.result.AssetSendRequest
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress
import java.util.UUID

interface AssetSendRequestRepository {
    fun store(params: StoreAssetSendRequestParams): AssetSendRequest
    fun getById(id: UUID): AssetSendRequest?
    fun getAllByProjectId(projectId: UUID): List<AssetSendRequest>
    fun getBySender(sender: WalletAddress): List<AssetSendRequest>
    fun getByRecipient(recipient: WalletAddress): List<AssetSendRequest>
    fun setTxInfo(id: UUID, txHash: TransactionHash, caller: WalletAddress): Boolean
}
