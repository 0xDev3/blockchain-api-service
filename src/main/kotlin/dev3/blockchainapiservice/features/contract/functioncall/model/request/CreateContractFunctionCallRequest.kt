package dev3.blockchainapiservice.features.contract.functioncall.model.request

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import dev3.blockchainapiservice.config.validation.MaxArgsSize
import dev3.blockchainapiservice.config.validation.MaxJsonNodeChars
import dev3.blockchainapiservice.config.validation.MaxStringSize
import dev3.blockchainapiservice.config.validation.ValidEthAddress
import dev3.blockchainapiservice.config.validation.ValidUint256
import dev3.blockchainapiservice.features.contract.deployment.model.params.DeployedContractIdentifierRequestBody
import dev3.blockchainapiservice.features.functions.encoding.model.FunctionArgument
import dev3.blockchainapiservice.features.functions.encoding.model.FunctionArgumentSchema
import dev3.blockchainapiservice.generated.jooq.id.ContractDeploymentRequestId
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.util.annotation.SchemaIgnore
import dev3.blockchainapiservice.util.annotation.SchemaName
import java.math.BigInteger
import javax.validation.Valid
import javax.validation.constraints.NotNull

data class CreateContractFunctionCallRequest(
    override val deployedContractId: ContractDeploymentRequestId?,
    @field:MaxStringSize
    override val deployedContractAlias: String?,
    @field:ValidEthAddress
    override val contractAddress: String?,
    @field:NotNull
    @field:MaxStringSize
    val functionName: String,
    @field:Valid
    @field:NotNull
    @field:MaxArgsSize
    @field:SchemaIgnore
    val functionParams: List<FunctionArgument>,
    @field:NotNull
    @field:ValidUint256
    val ethAmount: BigInteger,
    @field:MaxStringSize
    val redirectUrl: String?,
    @field:MaxJsonNodeChars
    val arbitraryData: JsonNode?,
    @field:Valid
    val screenConfig: ScreenConfig?,
    @field:ValidEthAddress
    val callerAddress: String?
) : DeployedContractIdentifierRequestBody {
    @Suppress("unused") // used for JSON schema generation
    @JsonIgnore
    @SchemaName("function_params")
    private val schemaFunctionParams: List<FunctionArgumentSchema> = emptyList()
}
