package dev3.blockchainapiservice.features.contract.deployment.repository

import dev3.blockchainapiservice.features.contract.deployment.model.filters.ContractDeploymentRequestFilters
import dev3.blockchainapiservice.features.contract.deployment.model.params.StoreContractDeploymentRequestParams
import dev3.blockchainapiservice.features.contract.deployment.model.result.ContractDeploymentRequest
import dev3.blockchainapiservice.generated.jooq.id.ContractDeploymentRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress

interface ContractDeploymentRequestRepository {
    fun store(params: StoreContractDeploymentRequestParams, metadataProjectId: ProjectId): ContractDeploymentRequest
    fun markAsDeleted(id: ContractDeploymentRequestId): Boolean
    fun getById(id: ContractDeploymentRequestId): ContractDeploymentRequest?
    fun getByAliasAndProjectId(alias: String, projectId: ProjectId): ContractDeploymentRequest?
    fun getByContractAddressAndChainId(contractAddress: ContractAddress, chainId: ChainId): ContractDeploymentRequest?
    fun getByContractAddressChainIdAndProjectId(
        contractAddress: ContractAddress,
        chainId: ChainId,
        projectId: ProjectId
    ): ContractDeploymentRequest?

    fun getAllByProjectId(
        projectId: ProjectId,
        filters: ContractDeploymentRequestFilters
    ): List<ContractDeploymentRequest>

    fun setTxInfo(id: ContractDeploymentRequestId, txHash: TransactionHash, deployer: WalletAddress): Boolean
    fun setContractAddress(id: ContractDeploymentRequestId, contractAddress: ContractAddress): Boolean
}
