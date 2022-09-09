package com.ampnet.blockchainapiservice.model.response

import com.ampnet.blockchainapiservice.model.params.OutputParameterSchema
import com.ampnet.blockchainapiservice.model.result.ReadonlyFunctionCallResult
import com.ampnet.blockchainapiservice.util.WithDeployedContractIdAndAddress
import com.ampnet.blockchainapiservice.util.annotation.SchemaAnyOf
import com.ampnet.blockchainapiservice.util.annotation.SchemaIgnore
import com.ampnet.blockchainapiservice.util.annotation.SchemaName
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
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
