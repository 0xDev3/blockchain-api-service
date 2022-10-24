package com.ampnet.blockchainapiservice.model.params

import com.ampnet.blockchainapiservice.util.BaseUrl
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress

data class CreateProjectParams(
    val issuerContractAddress: ContractAddress,
    val baseRedirectUrl: BaseUrl,
    val chainId: ChainId,
    val customRpcUrl: String?
)
