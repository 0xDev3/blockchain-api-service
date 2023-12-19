package dev3.blockchainapiservice.features.gas.model.request

import dev3.blockchainapiservice.config.validation.MaxFunctionDataSize
import dev3.blockchainapiservice.config.validation.MaxStringSize
import dev3.blockchainapiservice.config.validation.ValidEthAddress
import dev3.blockchainapiservice.config.validation.ValidUint256
import dev3.blockchainapiservice.model.params.DeployedContractIdentifierRequestBody
import java.math.BigInteger
import java.util.UUID
import javax.validation.constraints.NotNull

data class EstimateGasCostForContractArbitraryCallRequest(
    override val deployedContractId: UUID?,
    @field:MaxStringSize
    override val deployedContractAlias: String?,
    @field:ValidEthAddress
    override val contractAddress: String?,
    @field:NotNull
    @field:MaxFunctionDataSize
    val functionData: String,
    @field:NotNull
    @field:ValidUint256
    val ethAmount: BigInteger,
    @field:ValidEthAddress
    val callerAddress: String?
) : DeployedContractIdentifierRequestBody
