package dev3.blockchainapiservice.model.response

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import dev3.blockchainapiservice.model.params.OutputParameterSchema
import dev3.blockchainapiservice.model.result.ReadonlyFunctionCallResult
import dev3.blockchainapiservice.util.WithDeployedContractIdAndAddress
import dev3.blockchainapiservice.util.annotation.SchemaAnyOf
import dev3.blockchainapiservice.util.annotation.SchemaIgnore
import dev3.blockchainapiservice.util.annotation.SchemaName
import java.math.BigInteger
import java.time.OffsetDateTime
import java.util.UUID

data class ReturnValueTypes(
    val types: RecursiveReturnValueTypes
)

@SchemaAnyOf
data class RecursiveReturnValueTypes(
    val type1: String,
    val type2: Boolean,
    val type3: List<ReturnValueTypes>
)

data class ReadonlyFunctionCallResponse(
    val deployedContractId: UUID?,
    val contractAddress: String,
    val blockNumber: BigInteger,
    val timestamp: OffsetDateTime,
    @SchemaIgnore
    val outputParams: JsonNode,
    @SchemaIgnore
    val returnValues: List<Any>,
    val rawReturnValue: String
) {
    constructor(result: WithDeployedContractIdAndAddress<ReadonlyFunctionCallResult>, outputParams: JsonNode) : this(
        deployedContractId = result.deployedContractId,
        contractAddress = result.contractAddress.rawValue,
        blockNumber = result.value.blockNumber.value,
        timestamp = result.value.timestamp.value,
        outputParams = outputParams,
        returnValues = result.value.returnValues,
        rawReturnValue = result.value.rawReturnValue
    )

    @Suppress("unused") // used for JSON schema generation
    @JsonIgnore
    @SchemaName("output_params")
    private val schemaOutputStructParams: List<OutputParameterSchema> = emptyList()

    @Suppress("unused") // used for JSON schema generation
    @JsonIgnore
    @SchemaName("return_values")
    private val schemaReturnValues: List<ReturnValueTypes> = emptyList()
}
