package com.ampnet.blockchainapiservice.blockchain.properties

import com.ampnet.blockchainapiservice.util.ChainId

data class ChainSpec(
    val chainId: ChainId,
    val customRpcUrl: String?
)
