package dev3.blockchainapiservice.repository

import dev3.blockchainapiservice.model.params.StoreAssetMultiSendRequestParams
import dev3.blockchainapiservice.model.result.AssetMultiSendRequest
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress
import java.util.UUID

interface AssetMultiSendRequestRepository {
    fun store(params: StoreAssetMultiSendRequestParams): AssetMultiSendRequest
    fun getById(id: UUID): AssetMultiSendRequest?
    fun getAllByProjectId(projectId: UUID): List<AssetMultiSendRequest>
    fun getBySender(sender: WalletAddress): List<AssetMultiSendRequest>
    fun setApproveTxInfo(id: UUID, txHash: TransactionHash, caller: WalletAddress): Boolean
    fun setDisperseTxInfo(id: UUID, txHash: TransactionHash, caller: WalletAddress): Boolean
}
