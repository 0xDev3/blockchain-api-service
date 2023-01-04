package dev3.blockchainapiservice.features.api.access.model.params

import dev3.blockchainapiservice.util.BaseUrl
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress

data class CreateProjectParams(
    val issuerContractAddress: ContractAddress,
    val baseRedirectUrl: BaseUrl,
    val chainId: ChainId,
    val customRpcUrl: String?
)
