package dev3.blockchainapiservice.model.result

import dev3.blockchainapiservice.exception.ContractDecoratorException
import dev3.blockchainapiservice.exception.ContractInterfaceNotFoundException
import dev3.blockchainapiservice.model.json.AbiInputOutput
import dev3.blockchainapiservice.model.json.AbiObject
import dev3.blockchainapiservice.model.json.ArtifactJson
import dev3.blockchainapiservice.model.json.EventDecorator
import dev3.blockchainapiservice.model.json.FunctionDecorator
import dev3.blockchainapiservice.model.json.InterfaceManifestJson
import dev3.blockchainapiservice.model.json.ManifestJson
import dev3.blockchainapiservice.model.json.OverridableDecorator
import dev3.blockchainapiservice.model.json.TypeDecorator
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
    val events: List<ContractEvent>
) {
    companion object {
        operator fun invoke(
            id: ContractId,
            artifact: ArtifactJson,
            manifest: ManifestJson,
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
                functions = decorateFunctions(artifact, manifest, interfaceFunctions),
                events = decorateEvents(artifact, manifest, interfaceEvents)
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
            interfaceFunctions: List<FunctionDecorator>
        ): List<ContractFunction> {
            val functions = artifact.abi.filter { it.type == "function" }
                .associateBy { "${it.name}(${it.inputs.orEmpty().toTypeList()})" }
            val decorators = (manifest.functionDecorators + interfaceFunctions).resolveOverrides()

            return decorators.map {
                val artifactFunction = functions.getAbiObjectBySignature(it.signature)

                ContractFunction(
                    name = it.name,
                    description = it.description,
                    solidityName = artifactFunction.name ?: throw ContractDecoratorException(
                        "Function ${it.signature} is missing function name in artifact.json"
                    ),
                    inputs = it.parameterDecorators.toContractParameters(artifactFunction.inputs.orEmpty()),
                    outputs = it.returnDecorators.toContractParameters(
                        artifactFunction.outputs ?: throw ContractDecoratorException(
                            "Function ${it.signature} is missing is missing outputs in artifact.json"
                        )
                    ),
                    emittableEvents = it.emittableEvents,
                    readOnly = artifactFunction.stateMutability == "view" || artifactFunction.stateMutability == "pure"
                )
            }
        }

        private fun decorateEvents(
            artifact: ArtifactJson,
            manifest: ManifestJson,
            interfaceEvents: List<EventDecorator>
        ): List<ContractEvent> {
            val events = artifact.abi.filter { it.type == "event" }
                .associateBy { "${it.name}(${it.inputs.orEmpty().toTypeList()})" }
            val decorators = (manifest.eventDecorators + interfaceEvents).resolveOverrides()

            return decorators.map {
                val artifactEvent = events.getAbiObjectBySignature(it.signature)

                ContractEvent(
                    name = it.name,
                    description = it.description,
                    solidityName = artifactEvent.name ?: throw ContractDecoratorException(
                        "Event ${it.signature} is missing event name in artifact.json"
                    ),
                    inputs = it.parameterDecorators.toContractParameters(artifactEvent.inputs.orEmpty())
                )
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
                    parameters = it.first.parameters?.toContractParameters(it.second.components ?: emptyList())
                )
            }

        private fun <T : OverridableDecorator> List<T>.resolveOverrides(): List<T> =
            distinctBy { it.signature }
    }
}

data class ContractParameter(
    val name: String,
    val description: String,
    val solidityName: String,
    val solidityType: String,
    val recommendedTypes: List<String>,
    val parameters: List<ContractParameter>?
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
    val inputs: List<ContractParameter>,
    val outputs: List<ContractParameter>,
    val emittableEvents: List<String>,
    val readOnly: Boolean
)

data class ContractEvent(
    val name: String,
    val description: String,
    val solidityName: String,
    val inputs: List<ContractParameter>
)
