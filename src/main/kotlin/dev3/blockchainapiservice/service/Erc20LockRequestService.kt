package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.model.params.CreateErc20LockRequestParams
import dev3.blockchainapiservice.model.result.Erc20LockRequest
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress
import dev3.blockchainapiservice.util.WithFunctionData
import dev3.blockchainapiservice.util.WithTransactionData
import java.util.UUID

interface Erc20LockRequestService {
    fun createErc20LockRequest(
        params: CreateErc20LockRequestParams,
        project: Project
    ): WithFunctionData<Erc20LockRequest>

    fun getErc20LockRequest(id: UUID): WithTransactionData<Erc20LockRequest>
    fun getErc20LockRequestsByProjectId(projectId: UUID): List<WithTransactionData<Erc20LockRequest>>
    fun attachTxInfo(id: UUID, txHash: TransactionHash, caller: WalletAddress)
}
