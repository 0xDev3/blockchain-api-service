package com.ampnet.blockchainapiservice.model.result

import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.ContractBinaryData
import com.ampnet.blockchainapiservice.util.FunctionData
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress

data class ContractDeploymentTransactionInfo(
    val hash: TransactionHash,
    val from: WalletAddress,
    val deployedContractAddress: ContractAddress,
    val data: FunctionData,
    val value: Balance,
    val binary: ContractBinaryData
)
