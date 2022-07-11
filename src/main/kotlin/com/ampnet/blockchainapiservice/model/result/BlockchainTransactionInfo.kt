package com.ampnet.blockchainapiservice.model.result

import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.EthereumAddress
import com.ampnet.blockchainapiservice.util.FunctionData
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.UtcDateTime
import com.ampnet.blockchainapiservice.util.WalletAddress
import java.math.BigInteger

data class BlockchainTransactionInfo(
    val hash: TransactionHash,
    val from: WalletAddress,
    val to: EthereumAddress,
    val deployedContractAddress: ContractAddress?,
    val data: FunctionData,
    val value: Balance,
    val blockConfirmations: BigInteger,
    val timestamp: UtcDateTime,
    val success: Boolean
)
