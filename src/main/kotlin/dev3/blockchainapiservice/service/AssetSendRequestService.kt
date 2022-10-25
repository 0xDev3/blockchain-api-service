package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.model.params.CreateAssetSendRequestParams
import dev3.blockchainapiservice.model.result.AssetSendRequest
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress
import dev3.blockchainapiservice.util.WithFunctionDataOrEthValue
import dev3.blockchainapiservice.util.WithTransactionData
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
