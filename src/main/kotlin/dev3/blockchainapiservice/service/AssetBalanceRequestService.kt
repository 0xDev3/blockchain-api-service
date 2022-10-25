package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.model.params.CreateAssetBalanceRequestParams
import dev3.blockchainapiservice.model.result.AssetBalanceRequest
import dev3.blockchainapiservice.model.result.FullAssetBalanceRequest
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.util.SignedMessage
import dev3.blockchainapiservice.util.WalletAddress
import java.util.UUID

interface AssetBalanceRequestService {
    fun createAssetBalanceRequest(params: CreateAssetBalanceRequestParams, project: Project): AssetBalanceRequest
    fun getAssetBalanceRequest(id: UUID): FullAssetBalanceRequest
    fun getAssetBalanceRequestsByProjectId(projectId: UUID): List<FullAssetBalanceRequest>
    fun attachWalletAddressAndSignedMessage(id: UUID, walletAddress: WalletAddress, signedMessage: SignedMessage)
}
