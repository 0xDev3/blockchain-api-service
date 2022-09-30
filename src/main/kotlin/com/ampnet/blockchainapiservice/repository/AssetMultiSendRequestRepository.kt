package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.model.params.StoreAssetMultiSendRequestParams
import com.ampnet.blockchainapiservice.model.result.AssetMultiSendRequest
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import java.util.UUID

interface AssetMultiSendRequestRepository {
    fun store(params: StoreAssetMultiSendRequestParams): AssetMultiSendRequest
    fun getById(id: UUID): AssetMultiSendRequest?
    fun getAllByProjectId(projectId: UUID): List<AssetMultiSendRequest>
    fun getBySender(sender: WalletAddress): List<AssetMultiSendRequest>
    fun setApproveTxInfo(id: UUID, txHash: TransactionHash, caller: WalletAddress): Boolean
    fun setDisperseTxInfo(id: UUID, txHash: TransactionHash, caller: WalletAddress): Boolean
}
