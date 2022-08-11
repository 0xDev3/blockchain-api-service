package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.ControllerTestBase
import com.ampnet.blockchainapiservice.blockchain.ExampleContract
import com.ampnet.blockchainapiservice.exception.ErrorCode
import com.ampnet.blockchainapiservice.model.filters.ContractDecoratorFilters
import com.ampnet.blockchainapiservice.model.filters.OrList
import com.ampnet.blockchainapiservice.model.json.ArtifactJson
import com.ampnet.blockchainapiservice.model.json.ConstructorDecorator
import com.ampnet.blockchainapiservice.model.json.FunctionDecorator
import com.ampnet.blockchainapiservice.model.json.ManifestJson
import com.ampnet.blockchainapiservice.model.json.TypeDecorator
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.util.UUID

class ContractDecoratorControllerApiTest : ControllerTestBase() {

    companion object {
        private val CONTRACT_DECORATOR = ContractDecorator(
            id = ContractId("examples.exampleContract"),
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
                            recommendedTypes = emptyList()
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
                    solidityName = "getOwner",
                    inputs = emptyList(),
                    outputs = listOf(
                        ContractParameter(
                            name = "Owner address",
                            description = "Contract owner address",
                            solidityName = "",
                            solidityType = "address",
                            recommendedTypes = emptyList()
                        )
                    ),
                    emittableEvents = emptyList(),
                    readOnly = true
                )
            ),
            events = emptyList()
        )
        private val MANIFEST_JSON = ManifestJson(
            tags = listOf("example", "simple"),
            implements = listOf("traits.example", "traits.exampleOwnable"),
            eventDecorators = emptyList(),
            constructorDecorators = listOf(
                ConstructorDecorator(
                    signature = "constructor(address)",
                    description = "Main constructor",
                    parameterDecorators = listOf(
                        TypeDecorator(
                            name = "Owner address",
                            description = "Contract owner address",
                            recommendedTypes = emptyList()
                        )
                    )
                )
            ),
            functionDecorators = listOf(
                FunctionDecorator(
                    signature = "getOwner()",
                    name = "Get contract owner",
                    description = "Fetches contract owner",
                    parameterDecorators = emptyList(),
                    returnDecorators = listOf(
                        TypeDecorator(
                            name = "Owner address",
                            description = "Contract owner address",
                            recommendedTypes = emptyList()
                        )
                    ),
                    emittableEvents = emptyList()
                )
            )
        )
        private val ARTIFACT_JSON = ArtifactJson(
            contractName = "ExampleContract",
            sourceName = "ExampleContract.sol",
            abi = listOf(),
            bytecode = "0x0",
            deployedBytecode = "0x0",
            linkReferences = null,
            deployedLinkReferences = null
        )
        private val INFO_MD = "# info.md file contents"
    }

    @Autowired
    private lateinit var contractDecoratorRepository: ContractDecoratorRepository

    @BeforeEach
    fun beforeEach() {
        contractDecoratorRepository.getAll(ContractDecoratorFilters(OrList(), OrList())).forEach {
            contractDecoratorRepository.delete(it.id)
        }
    }

    @Test
    fun mustCorrectlyFetchContractDecoratorsWithFilters() {
        suppose("some contract decorator exists in the database") {
            contractDecoratorRepository.store(CONTRACT_DECORATOR)
        }

        val response = suppose("request to fetch contract decorators is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get(
                    "/v1/deployable-contracts/?tags=example AND simple,other" +
                        "&implements=traits/example AND traits/exampleOwnable,traits/other"
                )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ContractDecoratorsResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    ContractDecoratorsResponse(
                        listOf(
                            ContractDecoratorResponse(
                                id = CONTRACT_DECORATOR.id.value,
                                binary = CONTRACT_DECORATOR.binary.value,
                                tags = CONTRACT_DECORATOR.tags.map { it.value },
                                implements = CONTRACT_DECORATOR.implements.map { it.value },
                                constructors = CONTRACT_DECORATOR.constructors,
                                functions = CONTRACT_DECORATOR.functions,
                                events = CONTRACT_DECORATOR.events
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchContractManifestsWithFilters() {
        suppose("some contract manifest.json exists in the database") {
            contractDecoratorRepository.store(CONTRACT_DECORATOR)
            contractDecoratorRepository.store(CONTRACT_DECORATOR.id, MANIFEST_JSON)
        }

        val response = suppose("request to fetch contract manifest.json files is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get(
                    "/v1/deployable-contracts/manifest.json?tags=example AND simple,other" +
                        "&implements=traits/example AND traits/exampleOwnable,traits/other"
                )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ManifestJsonsResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(ManifestJsonsResponse(listOf(MANIFEST_JSON)))
        }
    }

    @Test
    fun mustCorrectlyFetchContractArtifactsWithFilters() {
        suppose("some contract artifact.json exists in the database") {
            contractDecoratorRepository.store(CONTRACT_DECORATOR)
            contractDecoratorRepository.store(CONTRACT_DECORATOR.id, ARTIFACT_JSON)
        }

        val response = suppose("request to fetch contract artifact.json files is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get(
                    "/v1/deployable-contracts/artifact.json?tags=example AND simple,other" +
                        "&implements=traits/example AND traits/exampleOwnable,traits/other"
                )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ArtifactJsonsResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(
                response.copy(
                    artifacts = response.artifacts.map {
                        it.copy(linkReferences = null, deployedLinkReferences = null)
                    }
                )
            ).withMessage()
                .isEqualTo(ArtifactJsonsResponse(listOf(ARTIFACT_JSON)))
        }
    }

    @Test
    fun mustCorrectlyFetchContractInfoMarkdownsWithFilters() {
        suppose("some contract info.md exists in the database") {
            contractDecoratorRepository.store(CONTRACT_DECORATOR)
            contractDecoratorRepository.store(CONTRACT_DECORATOR.id, INFO_MD)
        }

        val response = suppose("request to fetch contract info.md files is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get(
                    "/v1/deployable-contracts/info.md?tags=example AND simple,other" +
                        "&implements=traits/example AND traits/exampleOwnable,traits/other"
                )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, InfoMarkdownsResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(InfoMarkdownsResponse(listOf(INFO_MD)))
        }
    }

    @Test
    fun mustCorrectlyFetchContractDecorator() {
        suppose("some contract decorator exists in the database") {
            contractDecoratorRepository.store(CONTRACT_DECORATOR)
        }

        val response = suppose("request to fetch contract decorator is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/deployable-contracts/${CONTRACT_DECORATOR.id.value}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ContractDecoratorResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    ContractDecoratorResponse(
                        id = CONTRACT_DECORATOR.id.value,
                        binary = CONTRACT_DECORATOR.binary.value,
                        tags = CONTRACT_DECORATOR.tags.map { it.value },
                        implements = CONTRACT_DECORATOR.implements.map { it.value },
                        constructors = CONTRACT_DECORATOR.constructors,
                        functions = CONTRACT_DECORATOR.functions,
                        events = CONTRACT_DECORATOR.events
                    )
                )
        }
    }

    @Test
    fun mustReturn404NotFoundForNonExistentContractDecorator() {
        verify("404 is returned for non-existent contract decorator") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/deployable-contracts/${UUID.randomUUID()}")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    fun mustCorrectlyFetchContractManifestJson() {
        suppose("some contract manifest.json exists in the database") {
            contractDecoratorRepository.store(CONTRACT_DECORATOR)
            contractDecoratorRepository.store(CONTRACT_DECORATOR.id, MANIFEST_JSON)
        }

        val response = suppose("request to fetch contract manifest.json is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/deployable-contracts/${CONTRACT_DECORATOR.id.value}/manifest.json")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ManifestJson::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(MANIFEST_JSON)
        }
    }

    @Test
    fun mustReturn404NotFoundForNonExistentContractManifestJson() {
        verify("404 is returned for non-existent contract manifest.json") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/deployable-contracts/${UUID.randomUUID()}/manifest.json")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    fun mustCorrectlyFetchContractArtifactJson() {
        suppose("some contract artifact.json exists in the database") {
            contractDecoratorRepository.store(CONTRACT_DECORATOR)
            contractDecoratorRepository.store(CONTRACT_DECORATOR.id, ARTIFACT_JSON)
        }

        val response = suppose("request to fetch contract artifact.json is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/deployable-contracts/${CONTRACT_DECORATOR.id.value}/artifact.json")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ArtifactJson::class.java)
        }

        verify("correct response is returned") {
            assertThat(response.copy(linkReferences = null, deployedLinkReferences = null)).withMessage()
                .isEqualTo(ARTIFACT_JSON)
        }
    }

    @Test
    fun mustReturn404NotFoundForNonExistentContractArtifactJson() {
        verify("404 is returned for non-existent contract artifact.json") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/deployable-contracts/${UUID.randomUUID()}/artifact.json")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    fun mustCorrectlyFetchContractInfoMarkdown() {
        suppose("some contract info.md exists in the database") {
            contractDecoratorRepository.store(CONTRACT_DECORATOR)
            contractDecoratorRepository.store(CONTRACT_DECORATOR.id, INFO_MD)
        }

        val response = suppose("request to fetch contract info.md is made") {
            mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/deployable-contracts/${CONTRACT_DECORATOR.id.value}/info.md")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
                .response
                .contentAsString
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(INFO_MD)
        }
    }

    @Test
    fun mustReturn404NotFoundForNonExistentContractInfoMarkdown() {
        verify("404 is returned for non-existent contract info.md") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/deployable-contracts/${UUID.randomUUID()}/info.md")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }
}
