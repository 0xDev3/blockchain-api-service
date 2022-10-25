package dev3.blockchainapiservice.controller

import dev3.blockchainapiservice.JsonSchemaDocumentation
import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.blockchain.ExampleContract
import dev3.blockchainapiservice.exception.ResourceNotFoundException
import dev3.blockchainapiservice.model.filters.AndList
import dev3.blockchainapiservice.model.filters.ContractDecoratorFilters
import dev3.blockchainapiservice.model.filters.OrList
import dev3.blockchainapiservice.model.json.ArtifactJson
import dev3.blockchainapiservice.model.json.ManifestJson
import dev3.blockchainapiservice.model.response.ArtifactJsonsResponse
import dev3.blockchainapiservice.model.response.ContractDecoratorResponse
import dev3.blockchainapiservice.model.response.ContractDecoratorsResponse
import dev3.blockchainapiservice.model.response.InfoMarkdownsResponse
import dev3.blockchainapiservice.model.response.ManifestJsonsResponse
import dev3.blockchainapiservice.model.result.ContractConstructor
import dev3.blockchainapiservice.model.result.ContractDecorator
import dev3.blockchainapiservice.model.result.ContractFunction
import dev3.blockchainapiservice.model.result.ContractParameter
import dev3.blockchainapiservice.repository.ContractDecoratorRepository
import dev3.blockchainapiservice.repository.ImportedContractDecoratorRepository
import dev3.blockchainapiservice.util.ContractBinaryData
import dev3.blockchainapiservice.util.ContractId
import dev3.blockchainapiservice.util.ContractTag
import dev3.blockchainapiservice.util.InterfaceId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.springframework.http.ResponseEntity
import java.util.UUID

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
            implements = listOf(InterfaceId("traits.example"), InterfaceId("traits.exampleOwnable")),
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
            contractImplements = OrList(AndList(InterfaceId("trait-1"), InterfaceId("trait-2")))
        )

        suppose("some contract decorators will be fetched with filters") {
            given(repository.getAll(filters))
                .willReturn(listOf(result))
        }

        val controller = ContractDecoratorController(repository, mock())

        verify("controller returns correct response") {
            val response = controller.getContractDecorators(
                contractTags = listOf("tag-1 AND tag-2"),
                contractImplements = listOf("trait-1 AND trait-2"),
                projectId = null
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
    fun mustCorrectlyFetchContractDecoratorsWithFiltersAndProjectId() {
        val repository = mock<ImportedContractDecoratorRepository>()
        val result = ContractDecorator(
            id = ContractId("examples.exampleContract"),
            name = "name",
            description = "description",
            binary = ContractBinaryData(ExampleContract.BINARY),
            tags = listOf(ContractTag("example"), ContractTag("simple")),
            implements = listOf(InterfaceId("traits.example"), InterfaceId("traits.exampleOwnable")),
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
            contractImplements = OrList(AndList(InterfaceId("trait-1"), InterfaceId("trait-2")))
        )
        val projectId = UUID.randomUUID()

        suppose("some contract decorators will be fetched with filters") {
            given(repository.getAll(projectId, filters))
                .willReturn(listOf(result))
        }

        val controller = ContractDecoratorController(emptyRepository(filters), repository)

        verify("controller returns correct response") {
            val response = controller.getContractDecorators(
                contractTags = listOf("tag-1 AND tag-2"),
                contractImplements = listOf("trait-1 AND trait-2"),
                projectId = projectId
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
            tags = emptySet(),
            implements = emptySet(),
            eventDecorators = emptyList(),
            constructorDecorators = emptyList(),
            functionDecorators = emptyList()
        )

        val filters = ContractDecoratorFilters(
            contractTags = OrList(AndList(ContractTag("tag-1"), ContractTag("tag-2"))),
            contractImplements = OrList(AndList(InterfaceId("trait-1"), InterfaceId("trait-2")))
        )

        suppose("some contract manifest.json files will be fetched with filters") {
            given(repository.getAllManifestJsonFiles(filters))
                .willReturn(listOf(result))
        }

        val controller = ContractDecoratorController(repository, mock())

        verify("controller returns correct response") {
            val response = controller.getContractManifestJsonFiles(
                contractTags = listOf("tag-1 AND tag-2"),
                contractImplements = listOf("trait-1 AND trait-2"),
                projectId = null
            )

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(ResponseEntity.ok(ManifestJsonsResponse(listOf(result))))
        }
    }

    @Test
    fun mustCorrectlyFetchContractManifestJsonsWithFiltersAndProjectId() {
        val repository = mock<ImportedContractDecoratorRepository>()
        val result = ManifestJson(
            name = "name",
            description = "description",
            tags = emptySet(),
            implements = emptySet(),
            eventDecorators = emptyList(),
            constructorDecorators = emptyList(),
            functionDecorators = emptyList()
        )

        val filters = ContractDecoratorFilters(
            contractTags = OrList(AndList(ContractTag("tag-1"), ContractTag("tag-2"))),
            contractImplements = OrList(AndList(InterfaceId("trait-1"), InterfaceId("trait-2")))
        )
        val projectId = UUID.randomUUID()

        suppose("some contract manifest.json files will be fetched with filters") {
            given(repository.getAllManifestJsonFiles(projectId, filters))
                .willReturn(listOf(result))
        }

        val controller = ContractDecoratorController(emptyRepository(filters), repository)

        verify("controller returns correct response") {
            val response = controller.getContractManifestJsonFiles(
                contractTags = listOf("tag-1 AND tag-2"),
                contractImplements = listOf("trait-1 AND trait-2"),
                projectId = projectId
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
            contractImplements = OrList(AndList(InterfaceId("trait-1"), InterfaceId("trait-2")))
        )

        suppose("some contract artifact.json files will be fetched with filters") {
            given(repository.getAllArtifactJsonFiles(filters))
                .willReturn(listOf(result))
        }

        val controller = ContractDecoratorController(repository, mock())

        verify("controller returns correct response") {
            val response = controller.getContractArtifactJsonFiles(
                contractTags = listOf("tag-1 AND tag-2"),
                contractImplements = listOf("trait-1 AND trait-2"),
                projectId = null
            )

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(ResponseEntity.ok(ArtifactJsonsResponse(listOf(result))))
        }
    }

    @Test
    fun mustCorrectlyFetchContractArtifactJsonsWithFiltersAndProjectId() {
        val repository = mock<ImportedContractDecoratorRepository>()
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
            contractImplements = OrList(AndList(InterfaceId("trait-1"), InterfaceId("trait-2")))
        )
        val projectId = UUID.randomUUID()

        suppose("some contract artifact.json files will be fetched with filters") {
            given(repository.getAllArtifactJsonFiles(projectId, filters))
                .willReturn(listOf(result))
        }

        val controller = ContractDecoratorController(emptyRepository(filters), repository)

        verify("controller returns correct response") {
            val response = controller.getContractArtifactJsonFiles(
                contractTags = listOf("tag-1 AND tag-2"),
                contractImplements = listOf("trait-1 AND trait-2"),
                projectId = projectId
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
            contractImplements = OrList(AndList(InterfaceId("trait-1"), InterfaceId("trait-2")))
        )

        suppose("some contract info.md files will be fetched with filters") {
            given(repository.getAllInfoMarkdownFiles(filters))
                .willReturn(listOf(result))
        }

        val controller = ContractDecoratorController(repository, mock())

        verify("controller returns correct response") {
            val response = controller.getContractInfoMarkdownFiles(
                contractTags = listOf("tag-1 AND tag-2"),
                contractImplements = listOf("trait-1 AND trait-2"),
                projectId = null
            )

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(ResponseEntity.ok(InfoMarkdownsResponse(listOf(result))))
        }
    }

    @Test
    fun mustCorrectlyFetchContractInfoMarkdownsWithFiltersAndProjectId() {
        val repository = mock<ImportedContractDecoratorRepository>()
        val result = "info-md"

        val filters = ContractDecoratorFilters(
            contractTags = OrList(AndList(ContractTag("tag-1"), ContractTag("tag-2"))),
            contractImplements = OrList(AndList(InterfaceId("trait-1"), InterfaceId("trait-2")))
        )
        val projectId = UUID.randomUUID()

        suppose("some contract info.md files will be fetched with filters") {
            given(repository.getAllInfoMarkdownFiles(projectId, filters))
                .willReturn(listOf(result))
        }

        val controller = ContractDecoratorController(emptyRepository(filters), repository)

        verify("controller returns correct response") {
            val response = controller.getContractInfoMarkdownFiles(
                contractTags = listOf("tag-1 AND tag-2"),
                contractImplements = listOf("trait-1 AND trait-2"),
                projectId = projectId
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
            implements = listOf(InterfaceId("traits.example"), InterfaceId("traits.exampleOwnable")),
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

        val controller = ContractDecoratorController(repository, mock())

        verify("controller returns correct response") {
            val response = controller.getContractDecorator(id.value, null)

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
    fun mustCorrectlyFetchContractDecoratorWithProjectId() {
        val id = ContractId("example")
        val repository = mock<ImportedContractDecoratorRepository>()
        val result = ContractDecorator(
            id = ContractId("examples.exampleContract"),
            name = "name",
            description = "description",
            binary = ContractBinaryData(ExampleContract.BINARY),
            tags = listOf(ContractTag("example"), ContractTag("simple")),
            implements = listOf(InterfaceId("traits.example"), InterfaceId("traits.exampleOwnable")),
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
        val projectId = UUID.randomUUID()

        suppose("some contract decorator will be fetched") {
            given(repository.getByContractIdAndProjectId(id, projectId))
                .willReturn(result)
        }

        val controller = ContractDecoratorController(mock(), repository)

        verify("controller returns correct response") {
            val response = controller.getContractDecorator(id.value, projectId)

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

        val controller = ContractDecoratorController(repository, mock())

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                controller.getContractDecorator(id.value, null)
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
            tags = emptySet(),
            implements = emptySet(),
            eventDecorators = emptyList(),
            constructorDecorators = emptyList(),
            functionDecorators = emptyList()
        )

        suppose("some contract manifest.json will be fetched") {
            given(repository.getManifestJsonById(id))
                .willReturn(result)
        }

        val controller = ContractDecoratorController(repository, mock())

        verify("controller returns correct response") {
            val response = controller.getContractManifestJson(id.value, null)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(result)
                )
        }
    }

    @Test
    fun mustCorrectlyFetchContractManifestJsonWithProjectId() {
        val id = ContractId("example")
        val repository = mock<ImportedContractDecoratorRepository>()
        val result = ManifestJson(
            name = "name",
            description = "description",
            tags = emptySet(),
            implements = emptySet(),
            eventDecorators = emptyList(),
            constructorDecorators = emptyList(),
            functionDecorators = emptyList()
        )
        val projectId = UUID.randomUUID()

        suppose("some contract manifest.json will be fetched") {
            given(repository.getManifestJsonByContractIdAndProjectId(id, projectId))
                .willReturn(result)
        }

        val controller = ContractDecoratorController(mock(), repository)

        verify("controller returns correct response") {
            val response = controller.getContractManifestJson(id.value, projectId)

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

        val controller = ContractDecoratorController(repository, mock())

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                controller.getContractManifestJson(id.value, null)
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

        val controller = ContractDecoratorController(repository, mock())

        verify("controller returns correct response") {
            val response = controller.getContractArtifactJson(id.value, null)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(result)
                )
        }
    }

    @Test
    fun mustCorrectlyFetchContractArtifactJsonWithProjectId() {
        val id = ContractId("example")
        val repository = mock<ImportedContractDecoratorRepository>()
        val result = ArtifactJson(
            contractName = "example",
            sourceName = "Example",
            abi = emptyList(),
            bytecode = "0x0",
            deployedBytecode = "0x0",
            linkReferences = null,
            deployedLinkReferences = null
        )
        val projectId = UUID.randomUUID()

        suppose("some contract artifact.json will be fetched") {
            given(repository.getArtifactJsonByContractIdAndProjectId(id, projectId))
                .willReturn(result)
        }

        val controller = ContractDecoratorController(mock(), repository)

        verify("controller returns correct response") {
            val response = controller.getContractArtifactJson(id.value, projectId)

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

        val controller = ContractDecoratorController(repository, mock())

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                controller.getContractArtifactJson(id.value, null)
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

        val controller = ContractDecoratorController(repository, mock())

        verify("controller returns correct response") {
            val response = controller.getContractInfoMarkdown(id.value, null)

            assertThat(response).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(result)
                )
        }
    }

    @Test
    fun mustCorrectlyFetchContractInfoMarkdownWithProjectId() {
        val id = ContractId("example")
        val repository = mock<ImportedContractDecoratorRepository>()
        val result = "info-md"
        val projectId = UUID.randomUUID()

        suppose("some contract info.md will be fetched") {
            given(repository.getInfoMarkdownByContractIdAndProjectId(id, projectId))
                .willReturn(result)
        }

        val controller = ContractDecoratorController(mock(), repository)

        verify("controller returns correct response") {
            val response = controller.getContractInfoMarkdown(id.value, projectId)

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

        val controller = ContractDecoratorController(repository, mock())

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                controller.getContractInfoMarkdown(id.value, null)
            }
        }
    }

    private fun emptyRepository(filters: ContractDecoratorFilters): ContractDecoratorRepository {
        val repository = mock<ContractDecoratorRepository>()

        given(repository.getAll(filters)).willReturn(emptyList())
        given(repository.getAllManifestJsonFiles(filters)).willReturn(emptyList())
        given(repository.getAllArtifactJsonFiles(filters)).willReturn(emptyList())
        given(repository.getAllInfoMarkdownFiles(filters)).willReturn(emptyList())

        return repository
    }
}
