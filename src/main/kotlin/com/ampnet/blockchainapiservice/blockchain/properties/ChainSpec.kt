package com.ampnet.blockchainapiservice.blockchain.properties

import com.ampnet.blockchainapiservice.util.ChainId

data class ChainSpec(
    val chainId: ChainId,
    val rpcSpec: RpcUrlSpec
)

data class RpcUrlSpec(
    val url: String?,
    val urlOverride: String?
)
