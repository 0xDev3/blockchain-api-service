package com.ampnet.blockchainapiservice.model.result

import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress

data class ClientInfo(
    val clientId: String,
    val chainId: ChainId?,
    val sendRedirectUrl: String?,
    val balanceRedirectUrl: String?,
    val tokenAddress: ContractAddress?
)
