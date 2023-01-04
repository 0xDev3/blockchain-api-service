package dev3.blockchainapiservice.features.asset.lock.service

import dev3.blockchainapiservice.features.api.access.model.result.Project
import dev3.blockchainapiservice.features.asset.lock.model.params.CreateErc20LockRequestParams
import dev3.blockchainapiservice.features.asset.lock.model.result.Erc20LockRequest
import dev3.blockchainapiservice.generated.jooq.id.Erc20LockRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress
import dev3.blockchainapiservice.util.WithFunctionData
import dev3.blockchainapiservice.util.WithTransactionData

interface Erc20LockRequestService {
    fun createErc20LockRequest(
        params: CreateErc20LockRequestParams,
        project: Project
    ): WithFunctionData<Erc20LockRequest>

    fun getErc20LockRequest(id: Erc20LockRequestId): WithTransactionData<Erc20LockRequest>
    fun getErc20LockRequestsByProjectId(projectId: ProjectId): List<WithTransactionData<Erc20LockRequest>>
    fun attachTxInfo(id: Erc20LockRequestId, txHash: TransactionHash, caller: WalletAddress)
}
