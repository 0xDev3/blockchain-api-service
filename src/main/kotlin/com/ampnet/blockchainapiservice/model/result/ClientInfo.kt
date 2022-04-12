package com.ampnet.blockchainapiservice.model.result

import com.ampnet.blockchainapiservice.util.ChainId

data class ClientInfo(
    val clientId: String,
    val chainId: ChainId,
    val redirectUrl: String
)
