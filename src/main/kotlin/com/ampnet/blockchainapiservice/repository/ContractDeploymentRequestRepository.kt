package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.model.filters.ContractDeploymentRequestFilters
import com.ampnet.blockchainapiservice.model.params.StoreContractDeploymentRequestParams
import com.ampnet.blockchainapiservice.model.result.ContractDeploymentRequest
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import java.util.UUID

interface ContractDeploymentRequestRepository {
    fun store(params: StoreContractDeploymentRequestParams): ContractDeploymentRequest
    fun getById(id: UUID): ContractDeploymentRequest?
    fun getByAliasAndProjectId(alias: String, projectId: UUID): ContractDeploymentRequest?
    fun getAllByProjectId(projectId: UUID, filters: ContractDeploymentRequestFilters): List<ContractDeploymentRequest>
    fun setTxInfo(id: UUID, txHash: TransactionHash, deployer: WalletAddress): Boolean
    fun setContractAddress(id: UUID, contractAddress: ContractAddress): Boolean
}
