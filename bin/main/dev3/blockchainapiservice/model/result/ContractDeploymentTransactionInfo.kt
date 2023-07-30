package dev3.blockchainapiservice.model.result

import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.BlockNumber
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.ContractBinaryData
import dev3.blockchainapiservice.util.FunctionData
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress

sealed interface ContractDeploymentTransactionInfo {
    val deployedContractAddress: ContractAddress
    val binary: ContractBinaryData
}

data class FullContractDeploymentTransactionInfo(
    val hash: TransactionHash,
    val from: WalletAddress,
    override val deployedContractAddress: ContractAddress,
    val data: FunctionData,
    val value: Balance,
    override val binary: ContractBinaryData,
    val blockNumber: BlockNumber
) : ContractDeploymentTransactionInfo

data class ContractBinaryInfo(
    override val deployedContractAddress: ContractAddress,
    override val binary: ContractBinaryData,
) : ContractDeploymentTransactionInfo
