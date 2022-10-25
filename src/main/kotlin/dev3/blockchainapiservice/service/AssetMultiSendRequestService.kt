package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.model.params.CreateAssetMultiSendRequestParams
import dev3.blockchainapiservice.model.result.AssetMultiSendRequest
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress
import dev3.blockchainapiservice.util.WithFunctionDataOrEthValue
import dev3.blockchainapiservice.util.WithMultiTransactionData
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
