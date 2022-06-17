package com.ampnet.blockchainapiservice.model.request

data class CreateProjectRequest(
    val issuerContractAddress: String,
    val redirectUrl: String,
    val chainId: Long,
    val customRpcUrl: String?
)
