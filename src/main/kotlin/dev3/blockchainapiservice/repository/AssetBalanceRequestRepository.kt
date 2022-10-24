package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.model.params.StoreAssetBalanceRequestParams
import com.ampnet.blockchainapiservice.model.result.AssetBalanceRequest
import com.ampnet.blockchainapiservice.util.SignedMessage
import com.ampnet.blockchainapiservice.util.WalletAddress
import java.util.UUID

interface AssetBalanceRequestRepository {
    fun store(params: StoreAssetBalanceRequestParams): AssetBalanceRequest
    fun getById(id: UUID): AssetBalanceRequest?
    fun getAllByProjectId(projectId: UUID): List<AssetBalanceRequest>
    fun setSignedMessage(id: UUID, walletAddress: WalletAddress, signedMessage: SignedMessage): Boolean
}
