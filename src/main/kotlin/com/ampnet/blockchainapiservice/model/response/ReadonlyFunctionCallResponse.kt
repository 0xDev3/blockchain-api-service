package com.ampnet.blockchainapiservice.model.response

import com.ampnet.blockchainapiservice.model.result.ReadonlyFunctionCallResult
import com.ampnet.blockchainapiservice.util.WithDeployedContractIdAndAddress
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
    val returnValues: List<String>
) {
    constructor(result: WithDeployedContractIdAndAddress<ReadonlyFunctionCallResult>) : this(
        deployedContractId = result.deployedContractId,
        contractAddress = result.contractAddress.rawValue,
        blockNumber = result.value.blockNumber.value,
        timestamp = result.value.timestamp.value,
        returnValues = result.value.returnValues.map { it.toString() }
    )
}
