package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.model.filters.AndList
import com.ampnet.blockchainapiservice.model.filters.ContractDecoratorFilters
import com.ampnet.blockchainapiservice.model.filters.OrList
import com.ampnet.blockchainapiservice.model.result.ContractDecorator
import com.ampnet.blockchainapiservice.util.ContractBinaryData
import com.ampnet.blockchainapiservice.util.ContractId
import com.ampnet.blockchainapiservice.util.ContractTag
import com.ampnet.blockchainapiservice.util.ContractTrait
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class InMemoryContractDecoratorRepositoryTest : TestBase() {

    @Test
    fun mustCorrectlyStoreAndThenGetContractDecoratorById() {
        val repository = InMemoryContractDecoratorRepository()
        val decorator = ContractDecorator(
            id = ContractId("example"),
            binary = ContractBinaryData("0x0"),
            tags = emptyList(),
            implements = emptyList(),
            constructors = emptyList(),
            functions = emptyList(),
            events = emptyList()
        )

        val storedDecorator = suppose("some contract decorator is stored") {
            repository.store(decorator)
        }

        verify("correct contract decorator is returned") {
            assertThat(storedDecorator).withMessage()
                .isEqualTo(decorator)
            assertThat(repository.getById(decorator.id)).withMessage()
                .isEqualTo(decorator)
        }
    }

    @Test
    fun mustCorrectlyDeleteContractDecoratorAndThenReturnNullWhenGettingById() {
        val repository = InMemoryContractDecoratorRepository()
        val decorator = ContractDecorator(
            id = ContractId("example"),
            binary = ContractBinaryData("0x0"),
            tags = emptyList(),
            implements = emptyList(),
            constructors = emptyList(),
            functions = emptyList(),
            events = emptyList()
        )

        suppose("some contract decorator is stored") {
            repository.store(decorator)
        }

        verify("correct contract decorator is returned") {
            assertThat(repository.getById(decorator.id)).withMessage()
                .isEqualTo(decorator)
        }

        suppose("contract decorator is deleted") {
            repository.delete(decorator.id)
        }

        verify("null is returned") {
            assertThat(repository.getById(decorator.id)).withMessage()
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyGetAllContractDecoratorsWithSomeTagFilters() {
        val matching = listOf(
            decorator(tags = listOf(ContractTag("1"), ContractTag("2"))),
            decorator(tags = listOf(ContractTag("3"))),
            decorator(tags = listOf(ContractTag("1"), ContractTag("2"), ContractTag("3"))),
            decorator(tags = listOf(ContractTag("1"), ContractTag("2"), ContractTag("extra"))),
            decorator(tags = listOf(ContractTag("3"), ContractTag("extra"))),
            decorator(tags = listOf(ContractTag("1"), ContractTag("2"), ContractTag("3"), ContractTag("extra")))
        )
        val nonMatching = listOf(
            decorator(tags = listOf(ContractTag("1"))),
            decorator(tags = listOf(ContractTag("2"))),
            decorator(tags = listOf(ContractTag("extra")))
        )
        val all = matching + nonMatching

        val repository = InMemoryContractDecoratorRepository()

        suppose("some contract decorators are stored") {
            all.forEach { repository.store(it) }
        }

        val filters = ContractDecoratorFilters(
            contractTags = OrList(
                AndList(ContractTag("1"), ContractTag("2")),
                AndList(ContractTag("3"))
            ),
            contractImplements = OrList()
        )

        verify("correct contract decorators are returned") {
            assertThat(repository.getAll(filters)).withMessage()
                .containsExactlyInAnyOrderElementsOf(matching)
            assertThat(repository.getAll(ContractDecoratorFilters(OrList(), OrList()))).withMessage()
                .containsExactlyInAnyOrderElementsOf(all)
        }
    }

    @Test
    fun mustCorrectlyGetAllContractDecoratorsWithSomeImplementsFilters() {
        val matching = listOf(
            decorator(implements = listOf(ContractTrait("1"), ContractTrait("2"))),
            decorator(implements = listOf(ContractTrait("3"))),
            decorator(implements = listOf(ContractTrait("1"), ContractTrait("2"), ContractTrait("3"))),
            decorator(implements = listOf(ContractTrait("1"), ContractTrait("2"), ContractTrait("extra"))),
            decorator(implements = listOf(ContractTrait("3"), ContractTrait("tag"))),
            decorator(
                implements = listOf(
                    ContractTrait("1"), ContractTrait("2"), ContractTrait("3"), ContractTrait("extra")
                )
            )
        )
        val nonMatching = listOf(
            decorator(implements = listOf(ContractTrait("1"))),
            decorator(implements = listOf(ContractTrait("2"))),
            decorator(implements = listOf(ContractTrait("extra")))
        )
        val all = matching + nonMatching

        val repository = InMemoryContractDecoratorRepository()

        suppose("some contract decorators are stored") {
            all.forEach { repository.store(it) }
        }

        val filters = ContractDecoratorFilters(
            contractTags = OrList(),
            contractImplements = OrList(
                AndList(ContractTrait("1"), ContractTrait("2")),
                AndList(ContractTrait("3"))
            )
        )

        verify("correct contract decorators are returned") {
            assertThat(repository.getAll(filters)).withMessage()
                .containsExactlyInAnyOrderElementsOf(matching)
            assertThat(repository.getAll(ContractDecoratorFilters(OrList(), OrList()))).withMessage()
                .containsExactlyInAnyOrderElementsOf(all)
        }
    }

    private fun decorator(tags: List<ContractTag> = emptyList(), implements: List<ContractTrait> = emptyList()) =
        ContractDecorator(
            id = ContractId(UUID.randomUUID().toString()),
            binary = ContractBinaryData("0x0"),
            tags = tags,
            implements = implements,
            constructors = emptyList(),
            functions = emptyList(),
            events = emptyList()
        )
}
