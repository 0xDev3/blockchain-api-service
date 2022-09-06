package com.ampnet.blockchainapiservice.model.params

import com.ampnet.blockchainapiservice.config.validation.MaxJsonNodeChars
import com.ampnet.blockchainapiservice.config.validation.ValidationConstants
import com.ampnet.blockchainapiservice.util.AbiType
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.FunctionData
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.ampnet.blockchainapiservice.util.annotation.SchemaAnyOf
import com.fasterxml.jackson.databind.JsonNode

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
