package com.ampnet.blockchainapiservice.model.result

import com.ampnet.blockchainapiservice.util.FunctionData
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.UtcDateTime
import com.ampnet.blockchainapiservice.util.WalletAddress
import java.math.BigInteger

data class BlockchainTransactionInfo(
    val hash: TransactionHash,
    val from: WalletAddress,
    val to: WalletAddress,
    val data: FunctionData,
    val blockConfirmations: BigInteger,
    val timestamp: UtcDateTime
)
