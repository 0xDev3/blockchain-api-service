package dev3.blockchainapiservice.features.contract.deployment.service

import dev3.blockchainapiservice.features.api.access.model.result.Project
import dev3.blockchainapiservice.features.contract.deployment.model.filters.ContractDeploymentRequestFilters
import dev3.blockchainapiservice.features.contract.deployment.model.params.CreateContractDeploymentRequestParams
import dev3.blockchainapiservice.features.contract.deployment.model.result.ContractDeploymentRequest
import dev3.blockchainapiservice.generated.jooq.id.ContractDeploymentRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress
import dev3.blockchainapiservice.util.WithTransactionData

interface ContractDeploymentRequestService {
    fun createContractDeploymentRequest(
        params: CreateContractDeploymentRequestParams,
        project: Project
    ): ContractDeploymentRequest

    fun markContractDeploymentRequestAsDeleted(id: ContractDeploymentRequestId, projectId: ProjectId)

    fun getContractDeploymentRequest(id: ContractDeploymentRequestId): WithTransactionData<ContractDeploymentRequest>
    fun getContractDeploymentRequestsByProjectIdAndFilters(
        projectId: ProjectId,
        filters: ContractDeploymentRequestFilters
    ): List<WithTransactionData<ContractDeploymentRequest>>

    fun getContractDeploymentRequestByProjectIdAndAlias(
        projectId: ProjectId,
        alias: String
    ): WithTransactionData<ContractDeploymentRequest>

    fun attachTxInfo(id: ContractDeploymentRequestId, txHash: TransactionHash, deployer: WalletAddress)
}
