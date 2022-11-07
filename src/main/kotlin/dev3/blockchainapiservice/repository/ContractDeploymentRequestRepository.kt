package dev3.blockchainapiservice.repository

import dev3.blockchainapiservice.model.filters.ContractDeploymentRequestFilters
import dev3.blockchainapiservice.model.params.StoreContractDeploymentRequestParams
import dev3.blockchainapiservice.model.result.ContractDeploymentRequest
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress
import java.util.UUID

interface ContractDeploymentRequestRepository {
    fun store(params: StoreContractDeploymentRequestParams, metadataProjectId: UUID): ContractDeploymentRequest
    fun markAsDeleted(id: UUID): Boolean
    fun getById(id: UUID): ContractDeploymentRequest?
    fun getByAliasAndProjectId(alias: String, projectId: UUID): ContractDeploymentRequest?
    fun getByContractAddressAndChainId(contractAddress: ContractAddress, chainId: ChainId): ContractDeploymentRequest?
    fun getAllByProjectId(projectId: UUID, filters: ContractDeploymentRequestFilters): List<ContractDeploymentRequest>
    fun setTxInfo(id: UUID, txHash: TransactionHash, deployer: WalletAddress): Boolean
    fun setContractAddress(id: UUID, contractAddress: ContractAddress): Boolean
}
