package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.model.params.CreateAssetBalanceRequestParams
import com.ampnet.blockchainapiservice.model.result.AssetBalanceRequest
import com.ampnet.blockchainapiservice.model.result.FullAssetBalanceRequest
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.util.SignedMessage
import com.ampnet.blockchainapiservice.util.WalletAddress
import java.util.UUID

interface AssetBalanceRequestService {
    fun createAssetBalanceRequest(params: CreateAssetBalanceRequestParams, project: Project): AssetBalanceRequest
    fun getAssetBalanceRequest(id: UUID): FullAssetBalanceRequest
    fun getAssetBalanceRequestsByProjectId(projectId: UUID): List<FullAssetBalanceRequest>
    fun attachWalletAddressAndSignedMessage(id: UUID, walletAddress: WalletAddress, signedMessage: SignedMessage)
}
