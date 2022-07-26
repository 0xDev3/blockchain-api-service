package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.JsonSchemaDocumentation
import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.blockchain.ExampleContract
import com.ampnet.blockchainapiservice.exception.ResourceNotFoundException
import com.ampnet.blockchainapiservice.model.filters.AndList
import com.ampnet.blockchainapiservice.model.filters.ContractDecoratorFilters
import com.ampnet.blockchainapiservice.model.filters.OrList
import com.ampnet.blockchainapiservice.model.response.ContractDecoratorResponse
import com.ampnet.blockchainapiservice.model.response.ContractDecoratorsResponse
import com.ampnet.blockchainapiservice.model.result.ContractConstructor
import com.ampnet.blockchainapiservice.model.result.ContractDecorator
import com.ampnet.blockchainapiservice.model.result.ContractFunction
import com.ampnet.blockchainapiservice.model.result.ContractParameter
import com.ampnet.blockchainapiservice.repository.ContractDecoratorRepository
import com.ampnet.blockchainapiservice.util.ContractBinaryData
import com.ampnet.blockchainapiservice.util.ContractId
import com.ampnet.blockchainapiservice.util.ContractTag
import com.ampnet.blockchainapiservice.util.ContractTrait
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.springframework.http.ResponseEntity

class ContractDecoratorControllerTest : TestBase() {

    @Test
    fun mustCorrectlyFetchContractDecorator() {
        val id = ContractId("example")
        val repository = mock<ContractDecoratorRepository>()
        val result = ContractDecorator(
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
                            recommendedTypes = listOf()
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
                            recommendedTypes = listOf()
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

            Assertions.assertThat(response).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(
                        ContractDecoratorResponse(
                            id = result.id.value,
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
    fun mustCorrectlyFetchContractDecoratorsWithFilters() {
        val repository = mock<ContractDecoratorRepository>()
        val result = ContractDecorator(
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
                            recommendedTypes = listOf()
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
                            recommendedTypes = listOf()
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

            Assertions.assertThat(response).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(
                        ContractDecoratorsResponse(
                            listOf(
                                ContractDecoratorResponse(
                                    id = result.id.value,
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
}