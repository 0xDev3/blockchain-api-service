package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.model.filters.ContractDeploymentRequestFilters
import dev3.blockchainapiservice.model.params.CreateContractDeploymentRequestParams
import dev3.blockchainapiservice.model.result.ContractDeploymentRequest
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress
import dev3.blockchainapiservice.util.WithTransactionData
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
