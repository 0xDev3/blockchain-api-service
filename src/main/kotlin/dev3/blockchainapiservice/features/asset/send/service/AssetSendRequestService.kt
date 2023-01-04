package dev3.blockchainapiservice.features.asset.send.service

import dev3.blockchainapiservice.features.api.access.model.result.Project
import dev3.blockchainapiservice.features.asset.send.model.params.CreateAssetSendRequestParams
import dev3.blockchainapiservice.features.asset.send.model.result.AssetSendRequest
import dev3.blockchainapiservice.generated.jooq.id.AssetSendRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress
import dev3.blockchainapiservice.util.WithFunctionDataOrEthValue
import dev3.blockchainapiservice.util.WithTransactionData

interface AssetSendRequestService {
    fun createAssetSendRequest(
        params: CreateAssetSendRequestParams,
        project: Project
    ): WithFunctionDataOrEthValue<AssetSendRequest>

    fun getAssetSendRequest(id: AssetSendRequestId): WithTransactionData<AssetSendRequest>
    fun getAssetSendRequestsByProjectId(projectId: ProjectId): List<WithTransactionData<AssetSendRequest>>
    fun getAssetSendRequestsBySender(sender: WalletAddress): List<WithTransactionData<AssetSendRequest>>
    fun getAssetSendRequestsByRecipient(recipient: WalletAddress): List<WithTransactionData<AssetSendRequest>>
    fun attachTxInfo(id: AssetSendRequestId, txHash: TransactionHash, caller: WalletAddress)
}
