package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.JsonSchemaDocumentation
import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.blockchain.ExampleContract
import com.ampnet.blockchainapiservice.exception.ResourceNotFoundException
import com.ampnet.blockchainapiservice.model.filters.AndList
import com.ampnet.blockchainapiservice.model.filters.ContractDecoratorFilters
import com.ampnet.blockchainapiservice.model.filters.OrList
import com.ampnet.blockchainapiservice.model.json.ArtifactJson
import com.ampnet.blockchainapiservice.model.json.ManifestJson
import com.ampnet.blockchainapiservice.model.response.ArtifactJsonsResponse
import com.ampnet.blockchainapiservice.model.response.ContractDecoratorResponse
import com.ampnet.blockchainapiservice.model.response.ContractDecoratorsResponse
import com.ampnet.blockchainapiservice.model.response.InfoMarkdownsResponse
import com.ampnet.blockchainapiservice.model.response.ManifestJsonsResponse
import com.ampnet.blockchainapiservice.model.result.ContractConstructor
import com.ampnet.blockchainapiservice.model.result.ContractDecorator
import com.ampnet.blockchainapiservice.model.result.ContractFunction
import com.ampnet.blockchainapiservice.model.result.ContractParameter
import com.ampnet.blockchainapiservice.repository.ContractDecoratorRepository
import com.ampnet.blockchainapiservice.util.ContractBinaryData
import com.ampnet.blockchainapiservice.util.ContractId
import com.ampnet.blockchainapiservice.util.ContractTag
import com.ampnet.blockchainapiservice.util.ContractTrait
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.springframework.http.ResponseEntity

class ContractDecoratorControllerTest : TestBase() {

    @Test
    fun mustCorrectlyFetchContractDecoratorsWithFilters() {
        val repository = mock<ContractDecoratorRepository>()
        val result = ContractDecorator(
            id = ContractId("examples.exampleContract"),
            name = "name",
            description = "description",
            binary = ContractBinaryData(ExampleContract.BINARY),
            tags = listOf(ContractTag("example"), ContractTag("simple")),
            implements = listOf(ContractTrait("traits.example"), ContractTrait("traits.exampleOwnable")),
            constructors = listOf(
                ContractConstructor(
                    inputs = listOf(
                        ContractParameter(
                            name = "Owner address",
                            description = "Contract owner address",
                            solidityName = "owner",
                            solidityType = "address",
                            recommendedTypes = listOf(),
                            parameters = null
                        )
                    ),
                    description = "Main constructor",
                    payable = true
                )
            ),
            functions = listOf(
                ContractFunction(
                    name = "Get contract owner",
                    description = "Fetches contract owner",
                    solidityName = "getOWner",
                    inputs = listOf(),
                    outputs = listOf(
                        ContractParameter(
                            name = "Owner address",
                            description = "Contract owner address",
                            solidityName = "",
                            solidityType = "address",
                            recommendedTypes = listOf(),
                            parameters = null
                        )
                    ),
                    emittableEvents = emptyList(),
                    readOnly = true
                )
            ),
            events = listOf()
        )

        val filters = ContractDecoratorFilters(
            contractTags = OrList(AndList(ContractTag("tag-1"), ContractTag("tag-2"))),
            contractImplements = OrList(AndList(ContractTrait("trait-1"), ContractTrait("trait-2")))
        )

        suppose("some contract decorators will be fetched with filters") {
            given(repository.getAll(filters))
                .willReturn(listOf(result))
        }

        val controller = ContractDecoratorController(repository)

        verify("controller returns correct response") {
            val response = controller.getContractDecorators(
                contractTags = listOf("tag-1 AND tag-2"),
                contractImplements = listOf("trait-1 AND trait-2")
            )

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(
                        ContractDecoratorsResponse(
                            listOf(
                                ContractDecoratorResponse(
                                    id = result.id.value,
                                    name = result.name,
                                    description = result.description,
                                    binary = result.binary.value,
                                    tags = result.tags.map { it.value },
                                    implements = result.implements.map { it.value },
                                    constructors = result.constructors,
                                    functions = result.functions,
                                    events = result.events
                                )
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchContractManifestJsonsWithFilters() {
        val repository = mock<ContractDecoratorRepository>()
        val result = ManifestJson(
            name = "name",
            description = "description",
            tags = emptyList(),
            implements = emptyList(),
            eventDecorators = emptyList(),
            constructorDecorators = emptyList(),
            functionDecorators = emptyList()
        )

        val filters = ContractDecoratorFilters(
            contractTags = OrList(AndList(ContractTag("tag-1"), ContractTag("tag-2"))),
            contractImplements = OrList(AndList(ContractTrait("trait-1"), ContractTrait("trait-2")))
        )

        suppose("some contract manifest.json files will be fetched with filters") {
            given(repository.getAllManifestJsonFiles(filters))
                .willReturn(listOf(result))
        }

        val controller = ContractDecoratorController(repository)

        verify("controller returns correct response") {
            val response = controller.getContractManifestJsonFiles(
                contractTags = listOf("tag-1 AND tag-2"),
                contractImplements = listOf("trait-1 AND trait-2")
            )

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(ResponseEntity.ok(ManifestJsonsResponse(listOf(result))))
        }
    }

    @Test
    fun mustCorrectlyFetchContractArtifactJsonsWithFilters() {
        val repository = mock<ContractDecoratorRepository>()
        val result = ArtifactJson(
            contractName = "example",
            sourceName = "Example",
            abi = emptyList(),
            bytecode = "0x0",
            deployedBytecode = "0x0",
            linkReferences = null,
            deployedLinkReferences = null
        )

        val filters = ContractDecoratorFilters(
            contractTags = OrList(AndList(ContractTag("tag-1"), ContractTag("tag-2"))),
            contractImplements = OrList(AndList(ContractTrait("trait-1"), ContractTrait("trait-2")))
        )

        suppose("some contract artifact.json files will be fetched with filters") {
            given(repository.getAllArtifactJsonFiles(filters))
                .willReturn(listOf(result))
        }

        val controller = ContractDecoratorController(repository)

        verify("controller returns correct response") {
            val response = controller.getContractArtifactJsonFiles(
                contractTags = listOf("tag-1 AND tag-2"),
                contractImplements = listOf("trait-1 AND trait-2")
            )

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(ResponseEntity.ok(ArtifactJsonsResponse(listOf(result))))
        }
    }

    @Test
    fun mustCorrectlyFetchContractInfoMarkdownsWithFilters() {
        val repository = mock<ContractDecoratorRepository>()
        val result = "info-md"

        val filters = ContractDecoratorFilters(
            contractTags = OrList(AndList(ContractTag("tag-1"), ContractTag("tag-2"))),
            contractImplements = OrList(AndList(ContractTrait("trait-1"), ContractTrait("trait-2")))
        )

        suppose("some contract info.md files will be fetched with filters") {
            given(repository.getAllInfoMarkdownFiles(filters))
                .willReturn(listOf(result))
        }

        val controller = ContractDecoratorController(repository)

        verify("controller returns correct response") {
            val response = controller.getContractInfoMarkdownFiles(
                contractTags = listOf("tag-1 AND tag-2"),
                contractImplements = listOf("trait-1 AND trait-2")
            )

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(ResponseEntity.ok(InfoMarkdownsResponse(listOf(result))))
        }
    }

    @Test
    fun mustCorrectlyFetchContractDecorator() {
        val id = ContractId("example")
        val repository = mock<ContractDecoratorRepository>()
        val result = ContractDecorator(
            id = ContractId("examples.exampleContract"),
            name = "name",
            description = "description",
            binary = ContractBinaryData(ExampleContract.BINARY),
            tags = listOf(ContractTag("example"), ContractTag("simple")),
            implements = listOf(ContractTrait("traits.example"), ContractTrait("traits.exampleOwnable")),
            constructors = listOf(
                ContractConstructor(
                    inputs = listOf(
                        ContractParameter(
                            name = "Owner address",
                            description = "Contract owner address",
                            solidityName = "owner",
                            solidityType = "address",
                            recommendedTypes = listOf(),
                            parameters = null
                        )
                    ),
                    description = "Main constructor",
                    payable = true
                )
            ),
            functions = listOf(
                ContractFunction(
                    name = "Get contract owner",
                    description = "Fetches contract owner",
                    solidityName = "getOWner",
                    inputs = listOf(),
                    outputs = listOf(
                        ContractParameter(
                            name = "Owner address",
                            description = "Contract owner address",
                            solidityName = "",
                            solidityType = "address",
                            recommendedTypes = listOf(),
                            parameters = null
                        )
                    ),
                    emittableEvents = emptyList(),
                    readOnly = true
                )
            ),
            events = listOf()
        )

        suppose("some contract decorator will be fetched") {
            given(repository.getById(id))
                .willReturn(result)
        }

        val controller = ContractDecoratorController(repository)

        verify("controller returns correct response") {
            val response = controller.getContractDecorator(id.value)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(
                        ContractDecoratorResponse(
                            id = result.id.value,
                            name = result.name,
                            description = result.description,
                            binary = result.binary.value,
                            tags = result.tags.map { it.value },
                            implements = result.implements.map { it.value },
                            constructors = result.constructors,
                            functions = result.functions,
                            events = result.events
                        )
                    )
                )
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenContractDecoratorIsNotFound() {
        val repository = mock<ContractDecoratorRepository>()
        val id = ContractId("example")

        suppose("null will be returned from the repository") {
            given(repository.getById(id))
                .willReturn(null)
        }

        val controller = ContractDecoratorController(repository)

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                controller.getContractDecorator(id.value)
            }
        }
    }

    @Test
    fun mustCorrectlyFetchContractManifestJson() {
        val id = ContractId("example")
        val repository = mock<ContractDecoratorRepository>()
        val result = ManifestJson(
            name = "name",
            description = "description",
            tags = emptyList(),
            implements = emptyList(),
            eventDecorators = emptyList(),
            constructorDecorators = emptyList(),
            functionDecorators = emptyList()
        )

        suppose("some contract manifest.json will be fetched") {
            given(repository.getManifestJsonById(id))
                .willReturn(result)
        }

        val controller = ContractDecoratorController(repository)

        verify("controller returns correct response") {
            val response = controller.getContractManifestJson(id.value)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(result)
                )
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenContractManifestJsonIsNotFound() {
        val repository = mock<ContractDecoratorRepository>()
        val id = ContractId("example")

        suppose("null will be returned from the repository") {
            given(repository.getManifestJsonById(id))
                .willReturn(null)
        }

        val controller = ContractDecoratorController(repository)

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                controller.getContractManifestJson(id.value)
            }
        }
    }

    @Test
    fun mustCorrectlyFetchContractArtifactJson() {
        val id = ContractId("example")
        val repository = mock<ContractDecoratorRepository>()
        val result = ArtifactJson(
            contractName = "example",
            sourceName = "Example",
            abi = emptyList(),
            bytecode = "0x0",
            deployedBytecode = "0x0",
            linkReferences = null,
            deployedLinkReferences = null
        )

        suppose("some contract artifact.json will be fetched") {
            given(repository.getArtifactJsonById(id))
                .willReturn(result)
        }

        val controller = ContractDecoratorController(repository)

        verify("controller returns correct response") {
            val response = controller.getContractArtifactJson(id.value)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(result)
                )
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenContractArtifactJsonIsNotFound() {
        val repository = mock<ContractDecoratorRepository>()
        val id = ContractId("example")

        suppose("null will be returned from the repository") {
            given(repository.getArtifactJsonById(id))
                .willReturn(null)
        }

        val controller = ContractDecoratorController(repository)

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                controller.getContractArtifactJson(id.value)
            }
        }
    }

    @Test
    fun mustCorrectlyFetchContractInfoMarkdown() {
        val id = ContractId("example")
        val repository = mock<ContractDecoratorRepository>()
        val result = "info-md"

        suppose("some contract info.md will be fetched") {
            given(repository.getInfoMarkdownById(id))
                .willReturn(result)
        }

        val controller = ContractDecoratorController(repository)

        verify("controller returns correct response") {
            val response = controller.getContractInfoMarkdown(id.value)

            assertThat(response).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(result)
                )
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenContractInfoMarkdownIsNotFound() {
        val repository = mock<ContractDecoratorRepository>()
        val id = ContractId("example")

        suppose("null will be returned from the repository") {
            given(repository.getInfoMarkdownById(id))
                .willReturn(null)
        }

        val controller = ContractDecoratorController(repository)

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                controller.getContractInfoMarkdown(id.value)
            }
        }
    }
}
