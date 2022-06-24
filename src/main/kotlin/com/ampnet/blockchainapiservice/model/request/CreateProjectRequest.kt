package com.ampnet.blockchainapiservice.model.request

data class CreateProjectRequest(
    val issuerContractAddress: String,
    val baseRedirectUrl: String,
    val chainId: Long,
    val customRpcUrl: String?
)
