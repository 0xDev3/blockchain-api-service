package dev3.blockchainapiservice.blockchain.properties

import dev3.blockchainapiservice.util.ChainId

data class ChainSpec(
    val chainId: ChainId,
    val customRpcUrl: String?
)
