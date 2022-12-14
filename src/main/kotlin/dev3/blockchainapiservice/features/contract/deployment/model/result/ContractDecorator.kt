package dev3.blockchainapiservice.features.contract.deployment.model.result

import com.fasterxml.jackson.databind.ObjectMapper
import dev3.blockchainapiservice.exception.ContractDecoratorException
import dev3.blockchainapiservice.exception.ContractInterfaceNotFoundException
import dev3.blockchainapiservice.features.contract.deployment.model.json.AbiInputOutput
import dev3.blockchainapiservice.features.contract.deployment.model.json.AbiObject
import dev3.blockchainapiservice.features.contract.deployment.model.json.ArtifactJson
import dev3.blockchainapiservice.features.contract.deployment.model.json.EventDecorator
import dev3.blockchainapiservice.features.contract.deployment.model.json.EventTypeDecorator
import dev3.blockchainapiservice.features.contract.deployment.model.json.FunctionDecorator
import dev3.blockchainapiservice.features.contract.deployment.model.json.InterfaceManifestJson
import dev3.blockchainapiservice.features.contract.deployment.model.json.ManifestJson
import dev3.blockchainapiservice.features.contract.deployment.model.json.OverridableDecorator
import dev3.blockchainapiservice.features.contract.deployment.model.json.ReturnTypeDecorator
import dev3.blockchainapiservice.features.contract.deployment.model.json.TypeDecorator
import dev3.blockchainapiservice.features.contract.readcall.model.params.OutputParameter
import dev3.blockchainapiservice.model.DeserializableEvent
import dev3.blockchainapiservice.model.DeserializableEventInput
import dev3.blockchainapiservice.util.ContractBinaryData
import dev3.blockchainapiservice.util.ContractId
import dev3.blockchainapiservice.util.ContractTag
import dev3.blockchainapiservice.util.InterfaceId

data class ContractDecorator(
    val id: ContractId,
    val name: String?,
    val description: String?,
    val binary: ContractBinaryData,
    val tags: List<ContractTag>,
    val implements: List<InterfaceId>,
    val constructors: List<ContractConstructor>,
    val functions: List<ContractFunction>,
    val events: List<ContractEvent>,
    val manifest: ManifestJson,
    val artifact: ArtifactJson
) {
    @Suppress("TooManyFunctions")
    companion object {
        operator fun invoke(
            id: ContractId,
            artifact: ArtifactJson,
            manifest: ManifestJson,
            imported: Boolean,
            interfacesProvider: ((InterfaceId) -> InterfaceManifestJson?)?
        ): ContractDecorator {
            val manifestInterfaces = interfacesProvider?.let { provider ->
                manifest.implements.map(InterfaceId::invoke).map { id ->
                    provider(id) ?: throw ContractInterfaceNotFoundException(id)
                }
            }.orEmpty()

            val interfaceFunctions = manifestInterfaces.flatMap { it.functionDecorators }.resolveOverrides()
            val interfaceEvents = manifestInterfaces.flatMap { it.eventDecorators }.resolveOverrides()
            val decoratorTags = manifest.tags + manifestInterfaces.flatMap { it.tags }

            return ContractDecorator(
                id = id,
                name = manifest.name,
                description = manifest.description,
                binary = ContractBinaryData(artifact.bytecode),
                tags = decoratorTags.map { ContractTag(it) },
                implements = manifest.implements.map { InterfaceId(it) },
                constructors = decorateConstructors(artifact, manifest),
                functions = decorateFunctions(artifact, manifest, interfaceFunctions, imported),
                events = decorateEvents(artifact, manifest, interfaceEvents, imported),
                manifest = manifest,
                artifact = artifact
            )
        }

        private fun decorateConstructors(artifact: ArtifactJson, manifest: ManifestJson): List<ContractConstructor> {
            val constructors = artifact.abi.filter { it.type == "constructor" }
                .associateBy { "constructor(${it.inputs.orEmpty().toTypeList()})" }

            return manifest.constructorDecorators.map {
                val artifactConstructor = constructors.getAbiObjectBySignature(it.signature)

                ContractConstructor(
                    inputs = it.parameterDecorators.toContractParameters(artifactConstructor.inputs.orEmpty()),
                    description = it.description,
                    payable = artifactConstructor.stateMutability == "payable"
                )
            }
        }

        private fun decorateFunctions(
            artifact: ArtifactJson,
            manifest: ManifestJson,
            interfaceFunctions: List<FunctionDecorator>,
            imported: Boolean
        ): List<ContractFunction> {
            val functions = artifact.abi.filter { it.type == "function" }
                .associateBy { "${it.name}(${it.inputs.orEmpty().toTypeList()})" }
            val decorators = concatByPriority(
                manifestItems = manifest.functionDecorators,
                interfaceItems = interfaceFunctions,
                imported = imported
            ).resolveOverrides()

            return decorators.map {
                val artifactFunction = functions.getAbiObjectBySignature(it.signature)

                ContractFunction(
                    name = it.name,
                    description = it.description,
                    solidityName = artifactFunction.name ?: throw ContractDecoratorException(
                        "Function ${it.signature} is missing function name in artifact.json"
                    ),
                    signature = it.signature,
                    inputs = it.parameterDecorators.toContractParameters(artifactFunction.inputs.orEmpty()),
                    outputs = it.returnDecorators.returnTypeToContractParameters(
                        artifactFunction.outputs ?: throw ContractDecoratorException(
                            "Function ${it.signature} is missing is missing outputs in artifact.json"
                        )
                    ),
                    emittableEvents = it.emittableEvents,
                    readOnly = it.readOnly || artifactFunction.stateMutability == "view" ||
                        artifactFunction.stateMutability == "pure"
                )
            }
        }

        private fun decorateEvents(
            artifact: ArtifactJson,
            manifest: ManifestJson,
            interfaceEvents: List<EventDecorator>,
            imported: Boolean
        ): List<ContractEvent> {
            val events = artifact.abi.filter { it.type == "event" }
                .associateBy { "${it.name}(${it.inputs.orEmpty().toTypeList()})" }
            val decorators = concatByPriority(
                manifestItems = manifest.eventDecorators,
                interfaceItems = interfaceEvents,
                imported = imported
            ).resolveOverrides()

            return decorators.mapNotNull { decorator ->
                val artifactEvent = events[decorator.signature]

                artifactEvent?.let {
                    ContractEvent(
                        name = decorator.name,
                        description = decorator.description,
                        solidityName = artifactEvent.name ?: throw ContractDecoratorException(
                            "Event ${decorator.signature} is missing event name in artifact.json"
                        ),
                        signature = decorator.signature,
                        inputs = decorator.parameterDecorators.eventTypeToContractParameters(
                            artifactEvent.inputs.orEmpty()
                        )
                    )
                }
            }
        }

        private fun Map<String, AbiObject>.getAbiObjectBySignature(signature: String): AbiObject =
            this[signature] ?: throw ContractDecoratorException(
                "Decorator signature $signature not found in artifact.json"
            )

        private fun List<AbiInputOutput>.toTypeList(): String =
            joinToString(separator = ",") {
                if (it.type.startsWith("tuple")) it.buildTupleType(it.type.removePrefix("tuple")) else it.type
            }

        private fun AbiInputOutput.buildTupleType(arraySuffix: String): String =
            "tuple(${components.orEmpty().toTypeList()})$arraySuffix"

        private fun List<TypeDecorator>.toContractParameters(abi: List<AbiInputOutput>): List<ContractParameter> =
            zip(abi).map {
                ContractParameter(
                    name = it.first.name,
                    description = it.first.description,
                    solidityName = it.second.name,
                    solidityType = it.second.type,
                    recommendedTypes = it.first.recommendedTypes,
                    parameters = it.first.parameters?.toContractParameters(it.second.components ?: emptyList()),
                    hints = it.first.hints
                )
            }

        private fun List<EventTypeDecorator>.eventTypeToContractParameters(
            abi: List<AbiInputOutput>
        ): List<EventParameter> =
            zip(abi).map {
                EventParameter(
                    name = it.first.name,
                    description = it.first.description,
                    indexed = it.first.indexed,
                    solidityName = it.second.name,
                    solidityType = it.second.type,
                    recommendedTypes = it.first.recommendedTypes,
                    parameters = it.first.parameters?.toContractParameters(it.second.components ?: emptyList()),
                    hints = it.first.hints
                )
            }

        private fun List<ReturnTypeDecorator>.returnTypeToContractParameters(
            abi: List<AbiInputOutput>
        ): List<ContractParameter> =
            asSequence().zip(abi.asSequence() + sequence { yield(AbiInputOutput.EMPTY) }).map {
                ContractParameter(
                    name = it.first.name,
                    description = it.first.description,
                    solidityName = it.second.name,
                    solidityType = it.first.solidityType,
                    recommendedTypes = it.first.recommendedTypes,
                    parameters = it.first.parameters?.returnTypeToContractParameters(it.second.components.orEmpty()),
                    hints = it.first.hints
                )
            }.toList()

        private fun <T : OverridableDecorator> List<T>.resolveOverrides(): List<T> =
            distinctBy { it.signature }

        private fun <T> concatByPriority(manifestItems: List<T>, interfaceItems: List<T>, imported: Boolean): List<T> =
            if (imported) interfaceItems + manifestItems else manifestItems + interfaceItems
    }

    fun getDeserializableEvents(objectMapper: ObjectMapper): List<DeserializableEvent> =
        events.map { event ->
            val (indexedInputs, regularInputs) = event.inputs.partition { it.indexed }

            DeserializableEvent(
                signature = event.signature,
                inputsOrder = event.inputs.map { it.solidityName },
                indexedInputs = indexedInputs.map { it.toDeserializableEventInput(objectMapper) },
                regularInputs = regularInputs.map { it.toDeserializableEventInput(objectMapper) }
            )
        }

    private fun EventParameter.toDeserializableEventInput(objectMapper: ObjectMapper): DeserializableEventInput {
        val abiType = objectMapper.readValue(solidityType.toOutputTypeJson(parameters), OutputParameter::class.java)
        return DeserializableEventInput(
            name = solidityName,
            abiType = abiType.deserializedType
        )
    }

    private fun String.toOutputTypeJson(parameters: List<ContractParameter>?): String =
        if (startsWith("tuple")) {
            val elems = parameters?.map { it.solidityType.toOutputTypeJson(it.parameters) }
                .orEmpty().joinToString(separator = ",", prefix = "[", postfix = "]")
            "{\"type\":\"$this\",\"elems\":$elems}"
        } else "\"$this\""
}

data class ContractParameter(
    val name: String,
    val description: String,
    val solidityName: String,
    val solidityType: String,
    val recommendedTypes: List<String>,
    val parameters: List<ContractParameter>?,
    val hints: List<Any>?
)

data class EventParameter(
    val name: String,
    val description: String,
    val indexed: Boolean,
    val solidityName: String,
    val solidityType: String,
    val recommendedTypes: List<String>,
    val parameters: List<ContractParameter>?,
    val hints: List<Any>?
)

data class ContractConstructor(
    val inputs: List<ContractParameter>,
    val description: String,
    val payable: Boolean
)

data class ContractFunction(
    val name: String,
    val description: String,
    val solidityName: String,
    val signature: String,
    val inputs: List<ContractParameter>,
    val outputs: List<ContractParameter>,
    val emittableEvents: List<String>,
    val readOnly: Boolean
)

data class ContractEvent(
    val name: String,
    val description: String,
    val solidityName: String,
    val signature: String,
    val inputs: List<EventParameter>
)
