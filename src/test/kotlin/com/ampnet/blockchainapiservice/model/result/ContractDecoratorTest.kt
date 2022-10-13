package com.ampnet.blockchainapiservice.model.result

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.model.json.AbiInputOutput
import com.ampnet.blockchainapiservice.model.json.AbiObject
import com.ampnet.blockchainapiservice.model.json.ArtifactJson
import com.ampnet.blockchainapiservice.model.json.ConstructorDecorator
import com.ampnet.blockchainapiservice.model.json.EventDecorator
import com.ampnet.blockchainapiservice.model.json.FunctionDecorator
import com.ampnet.blockchainapiservice.model.json.InterfaceManifestJson
import com.ampnet.blockchainapiservice.model.json.ManifestJson
import com.ampnet.blockchainapiservice.model.json.TypeDecorator
import com.ampnet.blockchainapiservice.util.ContractBinaryData
import com.ampnet.blockchainapiservice.util.ContractId
import com.ampnet.blockchainapiservice.util.ContractTag
import com.ampnet.blockchainapiservice.util.ContractTrait
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ContractDecoratorTest : TestBase() {

    companion object {
        private val ARTIFACT_JSON = ArtifactJson(
            contractName = "Test",
            sourceName = "Test.sol",
            abi = listOf(
                constructorAbi("string"),
                constructorAbi("uint"),
                constructorAbi("int"),
                constructorAbi("bool"),
                functionAbi("fromDecorator", "string"),
                functionAbi("fromOverride1", "uint"),
                functionAbi("fromOverride2", "int"),
                functionAbi("extraFunction", "bool"),
                eventAbi("FromDecorator", "string"),
                eventAbi("FromOverride1", "uint"),
                eventAbi("FromOverride2", "int"),
                eventAbi("ExtraEvent", "bool")
            ),
            bytecode = "0",
            deployedBytecode = "0",
            linkReferences = null,
            deployedLinkReferences = null
        )
        private val MANIFEST_JSON = ManifestJson(
            name = "name",
            description = "description",
            tags = emptyList(),
            implements = listOf("override-1", "override-2", "extra"),
            eventDecorators = listOf(
                eventDecorator("FromDecorator", "string", "not-overridden-1")
            ),
            constructorDecorators = listOf(
                constructorDecorator("string", "not-overridden-1")
            ),
            functionDecorators = listOf(
                functionDecorator("fromDecorator", "string", "not-overridden-1")
            )
        )
        private val INTERFACE_MANIFEST_OVERRIDE_1 = InterfaceManifestJson(
            eventDecorators = listOf(
                eventDecorator("FromDecorator", "string", "overridden-1-1"),
                eventDecorator("FromOverride1", "uint", "overridden-1-2")
            ),
            constructorDecorators = listOf(
                constructorDecorator("string", "overridden-1-1"),
                constructorDecorator("uint", "overridden-1-2")
            ),
            functionDecorators = listOf(
                functionDecorator("fromDecorator", "string", "overridden-1-1"),
                functionDecorator("fromOverride1", "uint", "overridden-1-2")
            )
        )
        private val INTERFACE_MANIFEST_OVERRIDE_2 = InterfaceManifestJson(
            eventDecorators = listOf(
                eventDecorator("FromDecorator", "string", "overridden-2-1"),
                eventDecorator("FromOverride1", "uint", "overridden-2-2"),
                eventDecorator("FromOverride2", "int", "overridden-2-3")
            ),
            constructorDecorators = listOf(
                constructorDecorator("string", "overridden-2-1"),
                constructorDecorator("uint", "overridden-2-2"),
                constructorDecorator("int", "overridden-2-3")
            ),
            functionDecorators = listOf(
                functionDecorator("fromDecorator", "string", "overridden-2-1"),
                functionDecorator("fromOverride1", "uint", "overridden-2-2"),
                functionDecorator("fromOverride2", "int", "overridden-2-3")
            )
        )
        private val INTERFACE_MANIFEST_EXTRA = InterfaceManifestJson(
            eventDecorators = listOf(
                eventDecorator("FromDecorator", "string", "extra-1"),
                eventDecorator("FromOverride1", "uint", "extra-2"),
                eventDecorator("FromOverride2", "int", "extra-3"),
                eventDecorator("ExtraEvent", "bool", "extra-4")
            ),
            constructorDecorators = listOf(
                constructorDecorator("string", "extra-1"),
                constructorDecorator("uint", "extra-2"),
                constructorDecorator("int", "extra-3"),
                constructorDecorator("bool", "extra-4")
            ),
            functionDecorators = listOf(
                functionDecorator("fromDecorator", "string", "extra-1"),
                functionDecorator("fromOverride1", "uint", "extra-2"),
                functionDecorator("fromOverride2", "int", "extra-3"),
                functionDecorator("extraFunction", "bool", "extra-4")
            )
        )

        private fun constructorAbi(argType: String) =
            AbiObject(
                anonymous = null,
                inputs = listOf(
                    AbiInputOutput(
                        components = null,
                        internalType = argType,
                        name = "arg1",
                        type = argType,
                        indexed = null
                    )
                ),
                outputs = null,
                stateMutability = null,
                name = "",
                type = "constructor"
            )

        private fun functionAbi(name: String, argType: String) =
            AbiObject(
                anonymous = null,
                inputs = listOf(
                    AbiInputOutput(
                        components = null,
                        internalType = argType,
                        name = "arg1",
                        type = argType,
                        indexed = null
                    )
                ),
                outputs = emptyList(),
                stateMutability = null,
                name = name,
                type = "function"
            )

        private fun eventAbi(name: String, argType: String) =
            AbiObject(
                anonymous = null,
                inputs = listOf(
                    AbiInputOutput(
                        components = null,
                        internalType = argType,
                        name = "arg1",
                        type = argType,
                        indexed = null
                    )
                ),
                outputs = emptyList(),
                stateMutability = null,
                name = name,
                type = "event"
            )

        private fun constructorDecorator(argType: String, description: String) =
            ConstructorDecorator(
                signature = "constructor($argType)",
                description = description,
                parameterDecorators = listOf(
                    TypeDecorator(
                        name = "Arg1",
                        description = "Arg1",
                        recommendedTypes = emptyList(),
                        parameters = null
                    )
                )
            )

        private fun functionDecorator(name: String, argType: String, description: String) =
            FunctionDecorator(
                signature = "$name($argType)",
                name = description,
                description = description,
                parameterDecorators = listOf(
                    TypeDecorator(
                        name = "Arg1",
                        description = "Arg1",
                        recommendedTypes = emptyList(),
                        parameters = null
                    )
                ),
                returnDecorators = emptyList(),
                emittableEvents = emptyList()
            )

        private fun eventDecorator(name: String, argType: String, description: String) =
            EventDecorator(
                signature = "$name($argType)",
                name = description,
                description = description,
                parameterDecorators = listOf(
                    TypeDecorator(
                        name = "Arg1",
                        description = "Arg1",
                        recommendedTypes = emptyList(),
                        parameters = null
                    )
                )
            )
    }

    @Test
    fun mustCorrectlyOverrideConstructorsFunctionsAndEvents() {
        val map = mapOf(
            ContractId("override-1") to INTERFACE_MANIFEST_OVERRIDE_1,
            ContractId("override-2") to INTERFACE_MANIFEST_OVERRIDE_2,
            ContractId("extra") to INTERFACE_MANIFEST_EXTRA
        )
        val interfacesProvider: (ContractId) -> InterfaceManifestJson? = { id -> map[id] }

        verify("constructors, functions and events are correctly overridden") {
            val id = ContractId("contract-id")
            val result = ContractDecorator(
                id = id,
                artifact = ARTIFACT_JSON,
                manifest = MANIFEST_JSON,
                interfacesProvider = interfacesProvider
            )

            assertThat(result).withMessage()
                .isEqualTo(
                    ContractDecorator(
                        id = id,
                        name = MANIFEST_JSON.name,
                        description = MANIFEST_JSON.description,
                        binary = ContractBinaryData(ARTIFACT_JSON.bytecode),
                        tags = MANIFEST_JSON.tags.map { ContractTag(it) },
                        implements = MANIFEST_JSON.implements.map { ContractTrait(it) },
                        constructors = listOf(
                            ContractConstructor(
                                inputs = listOf(
                                    ContractParameter(
                                        name = "Arg1",
                                        description = "Arg1",
                                        solidityType = "string",
                                        solidityName = "arg1",
                                        recommendedTypes = emptyList(),
                                        parameters = null
                                    )
                                ),
                                description = "not-overridden-1",
                                payable = false
                            ),
                            ContractConstructor(
                                inputs = listOf(
                                    ContractParameter(
                                        name = "Arg1",
                                        description = "Arg1",
                                        solidityType = "uint",
                                        solidityName = "arg1",
                                        recommendedTypes = emptyList(),
                                        parameters = null
                                    )
                                ),
                                description = "overridden-1-2",
                                payable = false
                            ),
                            ContractConstructor(
                                inputs = listOf(
                                    ContractParameter(
                                        name = "Arg1",
                                        description = "Arg1",
                                        solidityType = "int",
                                        solidityName = "arg1",
                                        recommendedTypes = emptyList(),
                                        parameters = null
                                    )
                                ),
                                description = "overridden-2-3",
                                payable = false
                            ),
                            ContractConstructor(
                                inputs = listOf(
                                    ContractParameter(
                                        name = "Arg1",
                                        description = "Arg1",
                                        solidityType = "bool",
                                        solidityName = "arg1",
                                        recommendedTypes = emptyList(),
                                        parameters = null
                                    )
                                ),
                                description = "extra-4",
                                payable = false
                            )
                        ),
                        functions = listOf(
                            ContractFunction(
                                name = "not-overridden-1",
                                description = "not-overridden-1",
                                solidityName = "fromDecorator",
                                inputs = listOf(
                                    ContractParameter(
                                        name = "Arg1",
                                        description = "Arg1",
                                        solidityType = "string",
                                        solidityName = "arg1",
                                        recommendedTypes = emptyList(),
                                        parameters = null
                                    )
                                ),
                                outputs = emptyList(),
                                emittableEvents = emptyList(),
                                readOnly = false
                            ),
                            ContractFunction(
                                name = "overridden-1-2",
                                description = "overridden-1-2",
                                solidityName = "fromOverride1",
                                inputs = listOf(
                                    ContractParameter(
                                        name = "Arg1",
                                        description = "Arg1",
                                        solidityType = "uint",
                                        solidityName = "arg1",
                                        recommendedTypes = emptyList(),
                                        parameters = null
                                    )
                                ),
                                outputs = emptyList(),
                                emittableEvents = emptyList(),
                                readOnly = false
                            ),
                            ContractFunction(
                                name = "overridden-2-3",
                                description = "overridden-2-3",
                                solidityName = "fromOverride2",
                                inputs = listOf(
                                    ContractParameter(
                                        name = "Arg1",
                                        description = "Arg1",
                                        solidityType = "int",
                                        solidityName = "arg1",
                                        recommendedTypes = emptyList(),
                                        parameters = null
                                    )
                                ),
                                outputs = emptyList(),
                                emittableEvents = emptyList(),
                                readOnly = false
                            ),
                            ContractFunction(
                                name = "extra-4",
                                description = "extra-4",
                                solidityName = "extraFunction",
                                inputs = listOf(
                                    ContractParameter(
                                        name = "Arg1",
                                        description = "Arg1",
                                        solidityType = "bool",
                                        solidityName = "arg1",
                                        recommendedTypes = emptyList(),
                                        parameters = null
                                    )
                                ),
                                outputs = emptyList(),
                                emittableEvents = emptyList(),
                                readOnly = false
                            )
                        ),
                        events = listOf(
                            ContractEvent(
                                name = "not-overridden-1",
                                description = "not-overridden-1",
                                solidityName = "FromDecorator",
                                inputs = listOf(
                                    ContractParameter(
                                        name = "Arg1",
                                        description = "Arg1",
                                        solidityType = "string",
                                        solidityName = "arg1",
                                        recommendedTypes = emptyList(),
                                        parameters = null
                                    )
                                )
                            ),
                            ContractEvent(
                                name = "overridden-1-2",
                                description = "overridden-1-2",
                                solidityName = "FromOverride1",
                                inputs = listOf(
                                    ContractParameter(
                                        name = "Arg1",
                                        description = "Arg1",
                                        solidityType = "uint",
                                        solidityName = "arg1",
                                        recommendedTypes = emptyList(),
                                        parameters = null
                                    )
                                )
                            ),
                            ContractEvent(
                                name = "overridden-2-3",
                                description = "overridden-2-3",
                                solidityName = "FromOverride2",
                                inputs = listOf(
                                    ContractParameter(
                                        name = "Arg1",
                                        description = "Arg1",
                                        solidityType = "int",
                                        solidityName = "arg1",
                                        recommendedTypes = emptyList(),
                                        parameters = null
                                    )
                                )
                            ),
                            ContractEvent(
                                name = "extra-4",
                                description = "extra-4",
                                solidityName = "ExtraEvent",
                                inputs = listOf(
                                    ContractParameter(
                                        name = "Arg1",
                                        description = "Arg1",
                                        solidityType = "bool",
                                        solidityName = "arg1",
                                        recommendedTypes = emptyList(),
                                        parameters = null
                                    )
                                )
                            )
                        )
                    )
                )
        }
    }
}
