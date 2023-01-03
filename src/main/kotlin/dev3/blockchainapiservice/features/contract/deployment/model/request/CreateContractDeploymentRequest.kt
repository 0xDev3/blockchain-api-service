package dev3.blockchainapiservice.features.contract.deployment.model.request

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import dev3.blockchainapiservice.config.validation.MaxArgsSize
import dev3.blockchainapiservice.config.validation.MaxJsonNodeChars
import dev3.blockchainapiservice.config.validation.MaxStringSize
import dev3.blockchainapiservice.config.validation.ValidAlias
import dev3.blockchainapiservice.config.validation.ValidEthAddress
import dev3.blockchainapiservice.config.validation.ValidUint256
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.util.FunctionArgument
import dev3.blockchainapiservice.util.FunctionArgumentSchema
import dev3.blockchainapiservice.util.annotation.SchemaIgnore
import dev3.blockchainapiservice.util.annotation.SchemaName
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
