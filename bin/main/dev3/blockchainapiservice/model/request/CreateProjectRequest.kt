package dev3.blockchainapiservice.model.request

import dev3.blockchainapiservice.config.validation.MaxStringSize
import dev3.blockchainapiservice.config.validation.ValidEthAddress
import javax.validation.constraints.NotNull

data class CreateProjectRequest(
    @field:NotNull
    @field:ValidEthAddress
    val issuerContractAddress: String,
    @field:NotNull
    @field:MaxStringSize
    val baseRedirectUrl: String,
    @field:NotNull
    val chainId: Long,
    @field:MaxStringSize
    val customRpcUrl: String?
)
