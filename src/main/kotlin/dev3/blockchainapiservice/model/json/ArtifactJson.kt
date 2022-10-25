package dev3.blockchainapiservice.model.json

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
data class ArtifactJson(
    val contractName: String,
    val sourceName: String,
    val abi: List<AbiObject>,
    val bytecode: String,
    val deployedBytecode: String,
    val linkReferences: JsonNode?,
    val deployedLinkReferences: JsonNode?
)

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
data class AbiObject(
    val anonymous: Boolean?,
    val inputs: List<AbiInputOutput>?,
    val outputs: List<AbiInputOutput>?,
    val stateMutability: String?,
    val name: String?,
    val type: String
)

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
data class AbiInputOutput(
    val components: List<AbiInputOutput>?,
    val internalType: String,
    val name: String,
    val type: String,
    val indexed: Boolean?
)
