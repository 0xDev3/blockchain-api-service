package com.ampnet.blockchainapiservice.util

data class Erc20Balance(
    val wallet: WalletAddress,
    val blockNumber: BlockNumber,
    val timestamp: UtcDateTime,
    val amount: Balance
)
