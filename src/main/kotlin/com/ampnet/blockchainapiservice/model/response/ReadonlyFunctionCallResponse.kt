package com.ampnet.blockchainapiservice.model.response

import com.ampnet.blockchainapiservice.model.params.OutputParameterSchema
import com.ampnet.blockchainapiservice.model.result.ReadonlyFunctionCallResult
import com.ampnet.blockchainapiservice.util.WithDeployedContractIdAndAddress
import com.ampnet.blockchainapiservice.util.annotation.SchemaIgnore
import com.ampnet.blockchainapiservice.util.annotation.SchemaName
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import java.math.BigInteger
import java.time.OffsetDateTime
import java.util.UUID

data class ReadonlyFunctionCallResponse(
    val deployedContractId: UUID?,
    val contractAddress: String,
    @JsonSerialize(using = ToStringSerializer::class)
    val blockNumber: BigInteger,
    val timestamp: OffsetDateTime,
    @SchemaIgnore
    val outputParams: JsonNode,
    val returnValues: List<String>,
    val rawReturnValue: String
) {
    constructor(result: WithDeployedContractIdAndAddress<ReadonlyFunctionCallResult>, outputParams: JsonNode) : this(
        deployedContractId = result.deployedContractId,
        contractAddress = result.contractAddress.rawValue,
        blockNumber = result.value.blockNumber.value,
        timestamp = result.value.timestamp.value,
        outputParams = outputParams,
        returnValues = result.value.returnValues.map { it.toString() },
        rawReturnValue = result.value.rawReturnValue
    )

    @Suppress("unused") // used for JSON schema generation
    @JsonIgnore
    @SchemaName("output_params")
    private val schemaOutputStructParams: List<OutputParameterSchema> = emptyList()
}
