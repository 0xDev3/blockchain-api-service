package dev3.blockchainapiservice.controller

import dev3.blockchainapiservice.JsonSchemaDocumentation
import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.exception.ResourceNotFoundException
import dev3.blockchainapiservice.model.json.InterfaceManifestJson
import dev3.blockchainapiservice.model.json.InterfaceManifestJsonWithId
import dev3.blockchainapiservice.model.response.ContractInterfaceManifestResponse
import dev3.blockchainapiservice.model.response.ContractInterfaceManifestsResponse
import dev3.blockchainapiservice.model.response.InfoMarkdownsResponse
import dev3.blockchainapiservice.repository.ContractInterfacesRepository
import dev3.blockchainapiservice.util.InterfaceId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.given
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
            eventDecorators = emptyList(),
            functionDecorators = emptyList()
        )

        suppose("some contract interfaces will be fetched") {
            given(repository.getAll())
                .willReturn(listOf(result))
        }

        val controller = ContractInterfacesController(repository)

        verify("controller returns correct response") {
            val response = controller.getContractInterfaces()

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(
                        ContractInterfaceManifestsResponse(
                            listOf(
                                ContractInterfaceManifestResponse(
                                    id = result.id.value,
                                    name = result.name,
                                    description = result.description,
                                    eventDecorators = result.eventDecorators,
                                    functionDecorators = result.functionDecorators
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
            given(repository.getAllInfoMarkdownFiles())
                .willReturn(listOf(result))
        }

        val controller = ContractInterfacesController(repository)

        verify("controller returns correct response") {
            val response = controller.getContractInterfaceInfoMarkdownFiles()

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
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
            eventDecorators = emptyList(),
            functionDecorators = emptyList()
        )

        suppose("some contract interface will be fetched") {
            given(repository.getById(id))
                .willReturn(result)
        }

        val controller = ContractInterfacesController(repository)

        verify("controller returns correct response") {
            val response = controller.getContractInterface(id.value)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(
                        ContractInterfaceManifestResponse(
                            id = id.value,
                            name = result.name,
                            description = result.description,
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
            given(repository.getById(id))
                .willReturn(null)
        }

        val controller = ContractInterfacesController(repository)

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
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
            given(repository.getInfoMarkdownById(id))
                .willReturn(result)
        }

        val controller = ContractInterfacesController(repository)

        verify("controller returns correct response") {
            val response = controller.getContractInterfaceInfoMarkdown(id.value)

            assertThat(response).withMessage()
                .isEqualTo(ResponseEntity.ok(result))
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenContractInterfaceInfoMarkdownIsNotFound() {
        val repository = mock<ContractInterfacesRepository>()
        val id = InterfaceId("example")

        suppose("null will be returned from the repository") {
            given(repository.getInfoMarkdownById(id))
                .willReturn(null)
        }

        val controller = ContractInterfacesController(repository)

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                controller.getContractInterfaceInfoMarkdown(id.value)
            }
        }
    }
}
