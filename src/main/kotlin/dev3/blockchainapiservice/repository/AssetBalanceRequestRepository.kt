package dev3.blockchainapiservice.repository

import dev3.blockchainapiservice.model.params.StoreAssetBalanceRequestParams
import dev3.blockchainapiservice.model.result.AssetBalanceRequest
import dev3.blockchainapiservice.util.SignedMessage
import dev3.blockchainapiservice.util.WalletAddress
import java.util.UUID

interface AssetBalanceRequestRepository {
    fun store(params: StoreAssetBalanceRequestParams): AssetBalanceRequest
    fun getById(id: UUID): AssetBalanceRequest?
    fun getAllByProjectId(projectId: UUID): List<AssetBalanceRequest>
    fun setSignedMessage(id: UUID, walletAddress: WalletAddress, signedMessage: SignedMessage): Boolean
}
