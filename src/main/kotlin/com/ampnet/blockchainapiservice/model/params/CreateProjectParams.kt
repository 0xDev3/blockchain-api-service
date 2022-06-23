package com.ampnet.blockchainapiservice.model.params

import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress

data class CreateProjectParams(
    val issuerContractAddress: ContractAddress,
    val redirectUrl: String,
    val chainId: ChainId,
    val customRpcUrl: String?
)
