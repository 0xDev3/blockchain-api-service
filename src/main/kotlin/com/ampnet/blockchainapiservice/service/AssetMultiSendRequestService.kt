package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.model.params.CreateAssetMultiSendRequestParams
import com.ampnet.blockchainapiservice.model.result.AssetMultiSendRequest
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.ampnet.blockchainapiservice.util.WithFunctionDataOrEthValue
import com.ampnet.blockchainapiservice.util.WithMultiTransactionData
import java.util.UUID

interface AssetMultiSendRequestService {
    fun createAssetMultiSendRequest(
        params: CreateAssetMultiSendRequestParams,
        project: Project
    ): WithFunctionDataOrEthValue<AssetMultiSendRequest>

    fun getAssetMultiSendRequest(id: UUID): WithMultiTransactionData<AssetMultiSendRequest>
    fun getAssetMultiSendRequestsByProjectId(projectId: UUID): List<WithMultiTransactionData<AssetMultiSendRequest>>
    fun getAssetMultiSendRequestsBySender(sender: WalletAddress): List<WithMultiTransactionData<AssetMultiSendRequest>>
    fun attachApproveTxInfo(id: UUID, txHash: TransactionHash, caller: WalletAddress)
    fun attachDisperseTxInfo(id: UUID, txHash: TransactionHash, caller: WalletAddress)
}
