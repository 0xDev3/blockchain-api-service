package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.model.params.CreateAssetSendRequestParams
import com.ampnet.blockchainapiservice.model.result.AssetSendRequest
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.ampnet.blockchainapiservice.util.WithFunctionDataOrEthValue
import com.ampnet.blockchainapiservice.util.WithTransactionData
import java.util.UUID

interface AssetSendRequestService {
    fun createAssetSendRequest(
        params: CreateAssetSendRequestParams,
        project: Project
    ): WithFunctionDataOrEthValue<AssetSendRequest>

    fun getAssetSendRequest(id: UUID): WithTransactionData<AssetSendRequest>
    fun getAssetSendRequestsByProjectId(projectId: UUID): List<WithTransactionData<AssetSendRequest>>
    fun getAssetSendRequestsBySender(sender: WalletAddress): List<WithTransactionData<AssetSendRequest>>
    fun getAssetSendRequestsByRecipient(recipient: WalletAddress): List<WithTransactionData<AssetSendRequest>>
    fun attachTxInfo(id: UUID, txHash: TransactionHash, caller: WalletAddress)
}
