package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.model.result.ContractDecorator
import com.ampnet.blockchainapiservice.util.ContractBinaryData
import com.ampnet.blockchainapiservice.util.ContractId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class InMemoryContractDecoratorRepositoryTest : TestBase() {

    @Test
    fun mustCorrectlyStoreAndThenGetContractDecoratorById() {
        val repository = InMemoryContractDecoratorRepository()
        val decorator = ContractDecorator(
            id = ContractId("example"),
            binary = ContractBinaryData("0x0"),
            tags = emptyList(),
            implements = emptyList()
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
            implements = emptyList()
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
}
