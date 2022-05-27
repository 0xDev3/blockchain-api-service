package com.ampnet.blockchainapiservice.util

import java.math.BigInteger

data class WithTransactionData<T>(val value: T, val status: Status, val transactionData: TransactionData)

data class TransactionData(
    val txHash: TransactionHash?,
    val fromAddress: WalletAddress?,
    val toAddress: ContractAddress,
    val data: FunctionData,
    val blockConfirmations: BigInteger?
)
