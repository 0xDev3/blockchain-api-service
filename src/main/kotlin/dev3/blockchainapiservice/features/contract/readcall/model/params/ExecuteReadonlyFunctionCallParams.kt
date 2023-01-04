package dev3.blockchainapiservice.features.contract.readcall.model.params

import com.fasterxml.jackson.databind.JsonNode
import dev3.blockchainapiservice.config.validation.MaxJsonNodeChars
import dev3.blockchainapiservice.config.validation.ValidationConstants
import dev3.blockchainapiservice.features.contract.abi.model.AbiType
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.FunctionData
import dev3.blockchainapiservice.util.WalletAddress
import dev3.blockchainapiservice.util.annotation.SchemaAnyOf

data class ExecuteReadonlyFunctionCallParams(
    val contractAddress: ContractAddress,
    val callerAddress: WalletAddress,
    val functionName: String,
    val functionData: FunctionData,
    val outputParams: List<OutputParameter>
)

data class OutputParameter(
    val deserializedType: AbiType,
    @field:MaxJsonNodeChars(ValidationConstants.FUNCTION_ARGUMENT_MAX_JSON_CHARS)
    val rawJson: JsonNode? = null // needed to hold pre-deserialization value
)

data class OutputParameterSchema(
    val type: String,
    val elems: List<OutputParameterTypes>
)

@SchemaAnyOf
data class OutputParameterTypes(
    val type1: OutputParameterSchema,
    val type2: String
)
