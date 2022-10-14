package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.model.json.EventDecorator
import com.ampnet.blockchainapiservice.model.json.FunctionDecorator
import com.ampnet.blockchainapiservice.model.json.InterfaceManifestJson
import com.ampnet.blockchainapiservice.model.json.PartiallyMatchingInterfaceManifest
import com.ampnet.blockchainapiservice.util.ContractId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class InMemoryContractInterfacesRepositoryTest : TestBase() {

    @Test
    fun mustCorrectlyStoreAndThenGetContractInterfaceById() {
        val repository = InMemoryContractInterfacesRepository()
        val id = ContractId("id")
        val interfaceManifest = InterfaceManifestJson(
            name = "name",
            description = "description",
            eventDecorators = emptyList(),
            functionDecorators = emptyList()
        )

        val storedInterface = suppose("some contract interface is stored") {
            repository.store(id, interfaceManifest)
        }

        verify("correct contract decorator is returned") {
            assertThat(storedInterface).withMessage()
                .isEqualTo(interfaceManifest)
            assertThat(repository.getById(id)).withMessage()
                .isEqualTo(interfaceManifest)
        }
    }

    @Test
    fun mustCorrectlyStoreAndThenGetContractInterfaceInfoMarkdownById() {
        val repository = InMemoryContractInterfacesRepository()
        val infoMd = "info-md"
        val id = ContractId("id")

        val storedInfoMd = suppose("some contract interface info.md is stored") {
            repository.store(id, infoMd)
        }

        verify("correct contract interface info.md is returned") {
            assertThat(storedInfoMd).withMessage()
                .isEqualTo(infoMd)
            assertThat(repository.getInfoMarkdownById(id)).withMessage()
                .isEqualTo(infoMd)
        }
    }

    @Test
    fun mustCorrectlyDeleteContractInterfaceAndThenReturnNullWhenGettingById() {
        val repository = InMemoryContractInterfacesRepository()
        val id = ContractId("id")
        val interfaceManifest = InterfaceManifestJson(
            name = "name",
            description = "description",
            eventDecorators = emptyList(),
            functionDecorators = emptyList()
        )

        suppose("some contract interface is stored") {
            repository.store(id, interfaceManifest)
        }

        verify("correct contract interface is returned") {
            assertThat(repository.getById(id)).withMessage()
                .isEqualTo(interfaceManifest)
        }

        val infoMd = "info-md"

        suppose("some contract info.md is stored") {
            repository.store(id, infoMd)
        }

        verify("correct contract interface info.md is returned") {
            assertThat(repository.getInfoMarkdownById(id)).withMessage()
                .isEqualTo(infoMd)
        }

        suppose("contract interface is deleted") {
            repository.delete(id)
        }

        verify("null is returned") {
            assertThat(repository.getById(id)).withMessage()
                .isNull()
            assertThat(repository.getInfoMarkdownById(id)).withMessage()
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyGetAllContractInterfaces() {
        val repository = InMemoryContractInterfacesRepository()

        val id1 = ContractId("id-1")
        val interfaceManifest1 = InterfaceManifestJson(
            name = "name-1",
            description = "description-1",
            eventDecorators = emptyList(),
            functionDecorators = emptyList()
        )

        val id2 = ContractId("id-2")
        val interfaceManifest2 = InterfaceManifestJson(
            name = "name-2",
            description = "description-2",
            eventDecorators = emptyList(),
            functionDecorators = emptyList()
        )

        suppose("some contract interfaces are stored") {
            repository.store(id1, interfaceManifest1)
            repository.store(id2, interfaceManifest2)
        }

        verify("correct contract interfaces are returned") {
            assertThat(repository.getAll()).withMessage()
                .containsExactlyInAnyOrderElementsOf(listOf(interfaceManifest1, interfaceManifest2))
        }
    }

    @Test
    fun mustCorrectlyGetAllPartiallyContractInterfacesWhenThereIsAFullMatch() {
        val repository = InMemoryContractInterfacesRepository()

        val id1 = ContractId("id-1")
        val interfaceManifest1 = InterfaceManifestJson(
            name = "name-1",
            description = "description-1",
            eventDecorators = listOf(
                simpleEventDecorator("Event(string)")
            ),
            functionDecorators = listOf(
                simpleFunctionDecorator("function(string)")
            )
        )

        val id2 = ContractId("id-2")
        val interfaceManifest2 = InterfaceManifestJson(
            name = "name-2",
            description = "description-2",
            eventDecorators = listOf(
                simpleEventDecorator("NonMatchingEvent(string)")
            ),
            functionDecorators = listOf(
                simpleFunctionDecorator("nonMatchingFunction(string)")
            )
        )

        suppose("some contract interfaces are stored") {
            repository.store(id1, interfaceManifest1)
            repository.store(id2, interfaceManifest2)
        }

        verify("correct contract interfaces are returned") {
            assertThat(
                repository.getAllWithPartiallyMatchingInterfaces(
                    abiFunctionSignatures = setOf("function(string)"),
                    abiEventSignatures = setOf("Event(string)")
                )
            ).withMessage()
                .containsExactlyInAnyOrderElementsOf(
                    listOf(
                        PartiallyMatchingInterfaceManifest(
                            id = id1,
                            name = "name-1",
                            description = "description-1",
                            eventDecorators = interfaceManifest1.eventDecorators,
                            functionDecorators = interfaceManifest1.functionDecorators
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyGetAllPartiallyContractInterfacesWhenThereIsAPartialMatch() {
        val repository = InMemoryContractInterfacesRepository()

        val id1 = ContractId("id-1")
        val interfaceManifest1 = InterfaceManifestJson(
            name = "name-1",
            description = "description-1",
            eventDecorators = listOf(
                simpleEventDecorator("Event(string)")
            ),
            functionDecorators = listOf(
                simpleFunctionDecorator("function(string)")
            )
        )

        val id2 = ContractId("id-2")
        val interfaceManifest2 = InterfaceManifestJson(
            name = "name-2",
            description = "description-2",
            eventDecorators = listOf(
                simpleEventDecorator("NonMatchingEvent(string)")
            ),
            functionDecorators = listOf(
                simpleFunctionDecorator("nonMatchingFunction(string)")
            )
        )

        suppose("some contract interfaces are stored") {
            repository.store(id1, interfaceManifest1)
            repository.store(id2, interfaceManifest2)
        }

        verify("correct contract interfaces are returned") {
            assertThat(
                repository.getAllWithPartiallyMatchingInterfaces(
                    abiFunctionSignatures = setOf("function(string)", "anotherFunction(string)"),
                    abiEventSignatures = setOf("Event(string)", "AnotherEvent(string)")
                )
            ).withMessage()
                .containsExactlyInAnyOrderElementsOf(
                    listOf(
                        PartiallyMatchingInterfaceManifest(
                            id = id1,
                            name = "name-1",
                            description = "description-1",
                            eventDecorators = interfaceManifest1.eventDecorators,
                            functionDecorators = interfaceManifest1.functionDecorators
                        )
                    )
                )
        }
    }

    @Test
    fun mustNotGetPartiallyContractInterfacesWhenInterfaceHasSignaturesMoreThanAbi() {
        val repository = InMemoryContractInterfacesRepository()

        val id1 = ContractId("id-1")
        val interfaceManifest1 = InterfaceManifestJson(
            name = "name-1",
            description = "description-1",
            eventDecorators = listOf(
                simpleEventDecorator("Event(string)")
            ),
            functionDecorators = listOf(
                simpleFunctionDecorator("function(string)")
            )
        )

        val id2 = ContractId("id-2")
        val interfaceManifest2 = InterfaceManifestJson(
            name = "name-2",
            description = "description-2",
            eventDecorators = listOf(
                simpleEventDecorator("NonMatchingEvent(string)")
            ),
            functionDecorators = listOf(
                simpleFunctionDecorator("nonMatchingFunction(string)")
            )
        )

        suppose("some contract interfaces are stored") {
            repository.store(id1, interfaceManifest1)
            repository.store(id2, interfaceManifest2)
        }

        verify("correct contract interfaces are returned") {
            assertThat(
                repository.getAllWithPartiallyMatchingInterfaces(
                    abiFunctionSignatures = emptySet(),
                    abiEventSignatures = emptySet()
                )
            ).withMessage()
                .isEmpty()
        }
    }

    private fun simpleEventDecorator(signature: String) =
        EventDecorator(
            signature = signature,
            name = "",
            description = "",
            parameterDecorators = emptyList()
        )

    private fun simpleFunctionDecorator(signature: String) =
        FunctionDecorator(
            signature = signature,
            name = "",
            description = "",
            parameterDecorators = emptyList(),
            returnDecorators = emptyList(),
            emittableEvents = emptyList()
        )
}
