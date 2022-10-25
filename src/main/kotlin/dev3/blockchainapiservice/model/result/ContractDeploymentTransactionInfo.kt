package dev3.blockchainapiservice.model.result

import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.ContractBinaryData
import dev3.blockchainapiservice.util.FunctionData
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress

data class ContractDeploymentTransactionInfo(
    val hash: TransactionHash,
    val from: WalletAddress,
    val deployedContractAddress: ContractAddress,
    val data: FunctionData,
    val value: Balance,
    val binary: ContractBinaryData
)
