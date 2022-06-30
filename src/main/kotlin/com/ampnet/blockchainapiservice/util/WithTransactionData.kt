package com.ampnet.blockchainapiservice.util

import java.math.BigInteger

data class WithTransactionData<T>(val value: T, val status: Status, val transactionData: TransactionData)

data class TransactionData(
    val txHash: TransactionHash?,
    val fromAddress: WalletAddress?,
    val toAddress: EthereumAddress,
    val data: FunctionData?,
    val value: Balance?,
    val blockConfirmations: BigInteger?,
    val timestamp: UtcDateTime?
)
