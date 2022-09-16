package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.generated.jooq.enums.UserIdentifierType
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ProjectRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.UserIdentifierRecord
import com.ampnet.blockchainapiservice.model.json.ArtifactJson
import com.ampnet.blockchainapiservice.model.json.ManifestJson
import com.ampnet.blockchainapiservice.model.result.ContractDecorator
import com.ampnet.blockchainapiservice.testcontainers.SharedTestContainers
import com.ampnet.blockchainapiservice.util.BaseUrl
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
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

    companion object {
        private val PROJECT_ID = UUID.randomUUID()
        private val OWNER_ID = UUID.randomUUID()
    }

    @Suppress("unused")
    private val postgresContainer = SharedTestContainers.postgresContainer

    @Autowired
    private lateinit var repository: JooqImportedContractDecoratorRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        postgresContainer.cleanAllDatabaseTables(dslContext)

        dslContext.executeInsert(
            UserIdentifierRecord(
                id = OWNER_ID,
                userIdentifier = "user-identifier",
                identifierType = UserIdentifierType.ETH_WALLET_ADDRESS
            )
        )

        dslContext.executeInsert(
            ProjectRecord(
                id = PROJECT_ID,
                ownerId = OWNER_ID,
                issuerContractAddress = ContractAddress("0"),
                baseRedirectUrl = BaseUrl("base-redirect-url"),
                chainId = ChainId(1337L),
                customRpcUrl = "custom-rpc-url",
                createdAt = TestData.TIMESTAMP
            )
        )
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
            repository.store(id, PROJECT_ID, contractId, manifestJson, artifactJson, infoMarkdown)
        }

        val expectedDecorator = ContractDecorator(contractId, artifactJson, manifestJson)

        verify("storing imported contract decorator returns correct result") {
            assertThat(storedContractDecorator).withMessage()
                .isEqualTo(expectedDecorator)
        }

        verify("imported contract decorator was correctly stored into the database") {
            assertThat(repository.getByContractIdAndProjectId(contractId, PROJECT_ID)).withMessage()
                .isEqualTo(expectedDecorator)
        }
    }

    @Test
    fun mustReturnNullWhenFetchingNonExistentImportedContractDecoratorByContractId() {
        verify("null is returned when fetching non-existent imported contract decorator by contract id") {
            assertThat(repository.getByContractIdAndProjectId(ContractId("abc"), UUID.randomUUID())).withMessage()
                .isNull()
        }
    }
}
