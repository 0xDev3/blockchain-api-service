package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.model.params.CreateErc20LockRequestParams
import com.ampnet.blockchainapiservice.model.result.Erc20LockRequest
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.ampnet.blockchainapiservice.util.WithFunctionData
import com.ampnet.blockchainapiservice.util.WithTransactionData
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
