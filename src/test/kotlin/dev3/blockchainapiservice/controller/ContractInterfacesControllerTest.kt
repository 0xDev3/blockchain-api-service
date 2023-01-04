package dev3.blockchainapiservice.controller

import dev3.blockchainapiservice.JsonSchemaDocumentation
import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.exception.ResourceNotFoundException
import dev3.blockchainapiservice.features.contract.deployment.model.json.InterfaceManifestJson
import dev3.blockchainapiservice.features.contract.deployment.model.json.InterfaceManifestJsonWithId
import dev3.blockchainapiservice.features.contract.deployment.model.response.InfoMarkdownsResponse
import dev3.blockchainapiservice.features.contract.interfaces.controller.ContractInterfacesController
import dev3.blockchainapiservice.features.contract.interfaces.model.filters.ContractInterfaceFilters
import dev3.blockchainapiservice.features.contract.interfaces.model.response.ContractInterfaceManifestResponse
import dev3.blockchainapiservice.features.contract.interfaces.model.response.ContractInterfaceManifestsResponse
import dev3.blockchainapiservice.features.contract.interfaces.repository.ContractInterfacesRepository
import dev3.blockchainapiservice.model.filters.OrList
import dev3.blockchainapiservice.util.InterfaceId
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.http.ResponseEntity

class ContractInterfacesControllerTest : TestBase() {

    @Test
    fun mustCorrectlyFetchContractInterfaces() {
        val repository = mock<ContractInterfacesRepository>()
        val result = InterfaceManifestJsonWithId(
            id = InterfaceId("interface-id"),
            name = "name",
            description = "description",
            tags = emptySet(),
            matchingEventDecorators = emptyList(),
            matchingFunctionDecorators = emptyList()
        )

        suppose("some contract interfaces will be fetched") {
            call(repository.getAll(ContractInterfaceFilters(OrList(emptyList()))))
                .willReturn(listOf(result))
        }

        val controller = ContractInterfacesController(repository)

        verify("controller returns correct response") {
            val response = controller.getContractInterfaces(emptyList())

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        ContractInterfaceManifestsResponse(
                            listOf(
                                ContractInterfaceManifestResponse(
                                    id = result.id.value,
                                    name = result.name,
                                    description = result.description,
                                    tags = result.tags.toList(),
                                    eventDecorators = result.matchingEventDecorators,
                                    functionDecorators = result.matchingFunctionDecorators
                                )
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchContractInterfaceInfoMarkdowns() {
        val repository = mock<ContractInterfacesRepository>()
        val result = "info-md"

        suppose("some contract interface info.md files will be fetched") {
            call(repository.getAllInfoMarkdownFiles(ContractInterfaceFilters(OrList(emptyList()))))
                .willReturn(listOf(result))
        }

        val controller = ContractInterfacesController(repository)

        verify("controller returns correct response") {
            val response = controller.getContractInterfaceInfoMarkdownFiles(emptyList())

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(InfoMarkdownsResponse(listOf(result))))
        }
    }

    @Test
    fun mustCorrectlyFetchContractInterface() {
        val id = InterfaceId("example")
        val repository = mock<ContractInterfacesRepository>()
        val result = InterfaceManifestJson(
            name = "name",
            description = "description",
            tags = setOf("interface-tag"),
            eventDecorators = emptyList(),
            functionDecorators = emptyList()
        )

        suppose("some contract interface will be fetched") {
            call(repository.getById(id))
                .willReturn(result)
        }

        val controller = ContractInterfacesController(repository)

        verify("controller returns correct response") {
            val response = controller.getContractInterface(id.value)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        ContractInterfaceManifestResponse(
                            id = id.value,
                            name = result.name,
                            description = result.description,
                            tags = result.tags.toList(),
                            eventDecorators = result.eventDecorators,
                            functionDecorators = result.functionDecorators
                        )
                    )
                )
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenContractInterfaceIsNotFound() {
        val repository = mock<ContractInterfacesRepository>()
        val id = InterfaceId("example")

        suppose("null will be returned from the repository") {
            call(repository.getById(id))
                .willReturn(null)
        }

        val controller = ContractInterfacesController(repository)

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                controller.getContractInterface(id.value)
            }
        }
    }

    @Test
    fun mustCorrectlyFetchContractInterfaceInfoMarkdown() {
        val id = InterfaceId("example")
        val repository = mock<ContractInterfacesRepository>()
        val result = "info-md"

        suppose("some contract interface info.md will be fetched") {
            call(repository.getInfoMarkdownById(id))
                .willReturn(result)
        }

        val controller = ContractInterfacesController(repository)

        verify("controller returns correct response") {
            val response = controller.getContractInterfaceInfoMarkdown(id.value)

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(result))
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenContractInterfaceInfoMarkdownIsNotFound() {
        val repository = mock<ContractInterfacesRepository>()
        val id = InterfaceId("example")

        suppose("null will be returned from the repository") {
            call(repository.getInfoMarkdownById(id))
                .willReturn(null)
        }

        val controller = ContractInterfacesController(repository)

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                controller.getContractInterfaceInfoMarkdown(id.value)
            }
        }
    }
}
