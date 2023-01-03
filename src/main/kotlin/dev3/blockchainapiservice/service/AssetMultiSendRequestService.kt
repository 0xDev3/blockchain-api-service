package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.generated.jooq.id.AssetMultiSendRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.model.params.CreateAssetMultiSendRequestParams
import dev3.blockchainapiservice.model.result.AssetMultiSendRequest
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress
import dev3.blockchainapiservice.util.WithFunctionDataOrEthValue
import dev3.blockchainapiservice.util.WithMultiTransactionData

interface AssetMultiSendRequestService {
    fun createAssetMultiSendRequest(
        params: CreateAssetMultiSendRequestParams,
        project: Project
    ): WithFunctionDataOrEthValue<AssetMultiSendRequest>

    fun getAssetMultiSendRequest(id: AssetMultiSendRequestId): WithMultiTransactionData<AssetMultiSendRequest>
    fun getAssetMultiSendRequestsByProjectId(
        projectId: ProjectId
    ): List<WithMultiTransactionData<AssetMultiSendRequest>>

    fun getAssetMultiSendRequestsBySender(sender: WalletAddress): List<WithMultiTransactionData<AssetMultiSendRequest>>
    fun attachApproveTxInfo(id: AssetMultiSendRequestId, txHash: TransactionHash, caller: WalletAddress)
    fun attachDisperseTxInfo(id: AssetMultiSendRequestId, txHash: TransactionHash, caller: WalletAddress)
}
