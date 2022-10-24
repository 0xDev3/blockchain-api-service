package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.model.params.StoreAssetSendRequestParams
import com.ampnet.blockchainapiservice.model.result.AssetSendRequest
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import java.util.UUID

interface AssetSendRequestRepository {
    fun store(params: StoreAssetSendRequestParams): AssetSendRequest
    fun getById(id: UUID): AssetSendRequest?
    fun getAllByProjectId(projectId: UUID): List<AssetSendRequest>
    fun getBySender(sender: WalletAddress): List<AssetSendRequest>
    fun getByRecipient(recipient: WalletAddress): List<AssetSendRequest>
    fun setTxInfo(id: UUID, txHash: TransactionHash, caller: WalletAddress): Boolean
}
