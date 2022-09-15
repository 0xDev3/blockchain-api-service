package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.model.json.ArtifactJson
import com.ampnet.blockchainapiservice.model.json.ManifestJson
import com.ampnet.blockchainapiservice.model.result.ContractDecorator
import com.ampnet.blockchainapiservice.testcontainers.SharedTestContainers
import com.ampnet.blockchainapiservice.util.ContractId
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import java.util.UUID

@JooqTest
@Import(JooqImportedContractDecoratorRepository::class)
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JooqImportedContractDecoratorRepositoryIntegTest : TestBase() {

    @Suppress("unused")
    private val postgresContainer = SharedTestContainers.postgresContainer

    @Autowired
    private lateinit var repository: JooqImportedContractDecoratorRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        postgresContainer.cleanAllDatabaseTables(dslContext)
    }

    @Test
    fun mustCorrectlyStoreAndFetchImportedContractDecorator() {
        val id = UUID.randomUUID()
        val contractId = ContractId("imported-contract")
        val manifestJson = ManifestJson(
            name = "name",
            description = "description",
            tags = listOf("tag-1"),
            implements = listOf("trait-1"),
            eventDecorators = emptyList(),
            constructorDecorators = emptyList(),
            functionDecorators = emptyList()
        )
        val artifactJson = ArtifactJson(
            contractName = "imported-contract",
            sourceName = "imported.sol",
            abi = emptyList(),
            bytecode = "0x0",
            deployedBytecode = "0x0",
            linkReferences = null,
            deployedLinkReferences = null
        )
        val infoMarkdown = "markdown"

        val storedContractDecorator = suppose("imported contract decorator will be stored into the database") {
            repository.store(id, contractId, manifestJson, artifactJson, infoMarkdown)
        }

        val expectedDecorator = ContractDecorator(contractId, artifactJson, manifestJson)

        verify("storing imported contract decorator returns correct result") {
            assertThat(storedContractDecorator).withMessage()
                .isEqualTo(expectedDecorator)
        }

        verify("imported contract decorator was correctly stored into the database") {
            assertThat(repository.getByContractId(contractId)).withMessage()
                .isEqualTo(expectedDecorator)
        }
    }

    @Test
    fun mustReturnNullWhenFetchingNonExistentImportedContractDecoratorByContractId() {
        verify("null is returned when fetching non-existent imported contract decorator by contract id") {
            assertThat(repository.getByContractId(ContractId("abc"))).withMessage()
                .isNull()
        }
    }
}
