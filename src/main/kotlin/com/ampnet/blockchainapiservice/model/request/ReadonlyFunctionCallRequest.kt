package com.ampnet.blockchainapiservice.model.request

import com.ampnet.blockchainapiservice.config.validation.MaxArgsSize
import com.ampnet.blockchainapiservice.config.validation.MaxStringSize
import com.ampnet.blockchainapiservice.config.validation.ValidEthAddress
import com.ampnet.blockchainapiservice.config.validation.ValidUint256
import com.ampnet.blockchainapiservice.model.params.DeployedContractIdentifierRequestBody
import com.ampnet.blockchainapiservice.util.FunctionArgument
import java.math.BigInteger
import java.util.UUID
import javax.validation.Valid
import javax.validation.constraints.NotNull

data class ReadonlyFunctionCallRequest(
    override val deployedContractId: UUID?,
    @field:MaxStringSize
    override val deployedContractAlias: String?,
    @field:ValidEthAddress
    override val contractAddress: String?,
    @field:ValidUint256
    val blockNumber: BigInteger?,
    @field:NotNull
    @field:MaxStringSize
    val functionName: String,
    @field:Valid
    @field:NotNull
    @field:MaxArgsSize
    val functionParams: List<FunctionArgument>,
    @field:Valid
    @field:NotNull
    @field:MaxArgsSize
    val outputParams: List<@MaxStringSize String>, // TODO use more specific type
    @field:NotNull
    @field:ValidEthAddress
    val callerAddress: String
) : DeployedContractIdentifierRequestBody
