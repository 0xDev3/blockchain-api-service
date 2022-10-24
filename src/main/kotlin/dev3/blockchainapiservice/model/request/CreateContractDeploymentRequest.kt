package com.ampnet.blockchainapiservice.model.request

import com.ampnet.blockchainapiservice.config.validation.MaxArgsSize
import com.ampnet.blockchainapiservice.config.validation.MaxJsonNodeChars
import com.ampnet.blockchainapiservice.config.validation.MaxStringSize
import com.ampnet.blockchainapiservice.config.validation.ValidAlias
import com.ampnet.blockchainapiservice.config.validation.ValidEthAddress
import com.ampnet.blockchainapiservice.config.validation.ValidUint256
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.util.FunctionArgument
import com.ampnet.blockchainapiservice.util.FunctionArgumentSchema
import com.ampnet.blockchainapiservice.util.annotation.SchemaIgnore
import com.ampnet.blockchainapiservice.util.annotation.SchemaName
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import java.math.BigInteger
import javax.validation.Valid
import javax.validation.constraints.NotNull

data class CreateContractDeploymentRequest(
    @field:NotNull
    @field:ValidAlias
    val alias: String,
    @field:NotNull
    @field:MaxStringSize
    val contractId: String,
    @field:Valid
    @field:NotNull
    @field:MaxArgsSize
    @field:SchemaIgnore
    val constructorParams: List<FunctionArgument>,
    @field:ValidEthAddress
    val deployerAddress: String?,
    @field:NotNull
    @field:ValidUint256
    val initialEthAmount: BigInteger,
    @field:MaxStringSize
    val redirectUrl: String?,
    @field:MaxJsonNodeChars
    val arbitraryData: JsonNode?,
    @field:Valid
    val screenConfig: ScreenConfig?
) {
    @Suppress("unused") // used for JSON schema generation
    @JsonIgnore
    @SchemaName("constructor_params")
    private val schemaConstructorParams: List<FunctionArgumentSchema> = emptyList()
}
