package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.model.filters.ContractDeploymentRequestFilters
import com.ampnet.blockchainapiservice.model.params.CreateContractDeploymentRequestParams
import com.ampnet.blockchainapiservice.model.result.ContractDeploymentRequest
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.ampnet.blockchainapiservice.util.WithTransactionData
import java.util.UUID

interface ContractDeploymentRequestService {
    fun createContractDeploymentRequest(
        params: CreateContractDeploymentRequestParams,
        project: Project
    ): ContractDeploymentRequest

    fun markContractDeploymentRequestAsDeleted(id: UUID, projectId: UUID)

    fun getContractDeploymentRequest(id: UUID): WithTransactionData<ContractDeploymentRequest>
    fun getContractDeploymentRequestsByProjectIdAndFilters(
        projectId: UUID,
        filters: ContractDeploymentRequestFilters
    ): List<WithTransactionData<ContractDeploymentRequest>>

    fun getContractDeploymentRequestByProjectIdAndAlias(
        projectId: UUID,
        alias: String
    ): WithTransactionData<ContractDeploymentRequest>

    fun attachTxInfo(id: UUID, txHash: TransactionHash, deployer: WalletAddress)
}
