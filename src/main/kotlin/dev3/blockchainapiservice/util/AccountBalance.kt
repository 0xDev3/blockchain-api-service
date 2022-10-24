package com.ampnet.blockchainapiservice.util

data class AccountBalance(
    val wallet: WalletAddress,
    val blockNumber: BlockNumber,
    val timestamp: UtcDateTime,
    val amount: Balance
)
