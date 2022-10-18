package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.generated.jooq.enums.UserIdentifierType
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ImportedContractDecoratorRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ProjectRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.UserIdentifierRecord
import com.ampnet.blockchainapiservice.model.filters.AndList
import com.ampnet.blockchainapiservice.model.filters.ContractDecoratorFilters
import com.ampnet.blockchainapiservice.model.filters.OrList
import com.ampnet.blockchainapiservice.model.json.ArtifactJson
import com.ampnet.blockchainapiservice.model.json.InterfaceManifestJson
import com.ampnet.blockchainapiservice.model.json.ManifestJson
import com.ampnet.blockchainapiservice.model.result.ContractDecorator
import com.ampnet.blockchainapiservice.testcontainers.SharedTestContainers
import com.ampnet.blockchainapiservice.util.BaseUrl
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.ContractId
import com.ampnet.blockchainapiservice.util.ContractTag
import com.ampnet.blockchainapiservice.util.InterfaceId
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
@Import(JooqImportedContractDecoratorRepository::class, InMemoryContractInterfacesRepository::class)
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JooqImportedContractDecoratorRepositoryIntegTest : TestBase() {

    companion object {
        private val PROJECT_ID_1 = UUID.randomUUID()
        private val PROJECT_ID_2 = UUID.randomUUID()
        private val OWNER_ID = UUID.randomUUID()
        private val EMPTY_INTERFACE_MANIFEST = InterfaceManifestJson(
            name = null,
            description = null,
            eventDecorators = emptyList(),
            functionDecorators = emptyList()
        )

        private data class DecoratorTestData(
            val contractId: ContractId,
            val manifest: ManifestJson,
            val artifact: ArtifactJson,
            val markdown: String
        )

        private data class DecoratorWithProjectId(
            val projectId: UUID,
            val decorator: ContractDecorator,
            val manifest: ManifestJson,
            val artifact: ArtifactJson,
            val markdown: String
        )
    }

    @Suppress("unused")
    private val postgresContainer = SharedTestContainers.postgresContainer

    @Autowired
    private lateinit var repository: JooqImportedContractDecoratorRepository

    @Autowired
    private lateinit var interfacesRepository: InMemoryContractInterfacesRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        postgresContainer.cleanAllDatabaseTables(dslContext)
        interfacesRepository.getAll().forEach {
            interfacesRepository.delete(it.id)
        }

        interfacesRepository.store(InterfaceId("trait-1"), EMPTY_INTERFACE_MANIFEST)
        interfacesRepository.store(InterfaceId("trait-2"), EMPTY_INTERFACE_MANIFEST)
        interfacesRepository.store(InterfaceId("trait-3"), EMPTY_INTERFACE_MANIFEST)
        interfacesRepository.store(InterfaceId("ignored-trait"), EMPTY_INTERFACE_MANIFEST)
        interfacesRepository.store(InterfaceId("new-interface"), EMPTY_INTERFACE_MANIFEST)

        dslContext.executeInsert(
            UserIdentifierRecord(
                id = OWNER_ID,
                userIdentifier = "user-identifier",
                identifierType = UserIdentifierType.ETH_WALLET_ADDRESS
            )
        )

        dslContext.executeInsert(
            ProjectRecord(
                id = PROJECT_ID_1,
                ownerId = OWNER_ID,
                issuerContractAddress = ContractAddress("0"),
                baseRedirectUrl = BaseUrl("base-redirect-url-0"),
                chainId = ChainId(1337L),
                customRpcUrl = "custom-rpc-url-0",
                createdAt = TestData.TIMESTAMP
            )
        )

        dslContext.executeInsert(
            ProjectRecord(
                id = PROJECT_ID_2,
                ownerId = OWNER_ID,
                issuerContractAddress = ContractAddress("1"),
                baseRedirectUrl = BaseUrl("base-redirect-url-1"),
                chainId = ChainId(1337L),
                customRpcUrl = "custom-rpc-url-1",
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
            repository.store(id, PROJECT_ID_1, contractId, manifestJson, artifactJson, infoMarkdown, TestData.TIMESTAMP)
        }

        val expectedDecorator = ContractDecorator(
            id = contractId,
            artifact = artifactJson,
            manifest = manifestJson,
            interfacesProvider = null
        )

        verify("storing imported contract decorator returns correct result") {
            assertThat(storedContractDecorator).withMessage()
                .isEqualTo(expectedDecorator)
        }

        verify("imported contract decorator was correctly stored into the database") {
            assertThat(repository.getByContractIdAndProjectId(contractId, PROJECT_ID_1)).withMessage()
                .isEqualTo(expectedDecorator)
            assertThat(repository.getManifestJsonByContractIdAndProjectId(contractId, PROJECT_ID_1)).withMessage()
                .isEqualTo(manifestJson)
            assertThat(
                repository.getArtifactJsonByContractIdAndProjectId(contractId, PROJECT_ID_1)
                    ?.copy(linkReferences = null, deployedLinkReferences = null)
            ).withMessage()
                .isEqualTo(artifactJson)
            assertThat(repository.getInfoMarkdownByContractIdAndProjectId(contractId, PROJECT_ID_1)).withMessage()
                .isEqualTo(infoMarkdown)
        }
    }

    @Test
    fun mustCorrectlyUpdateImportedContractDecoratorInterfaces() {
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

        suppose("imported contract decorator will be stored into the database") {
            repository.store(id, PROJECT_ID_1, contractId, manifestJson, artifactJson, infoMarkdown, TestData.TIMESTAMP)
        }

        val newInterfaces = listOf(InterfaceId("new-interface"))

        suppose("imported contract decorator interfaces are updated") {
            repository.updateInterfaces(contractId, PROJECT_ID_1, newInterfaces, manifestJson)
        }

        val expectedManifest = manifestJson.copy(implements = newInterfaces.map { it.value })
        val expectedDecorator = ContractDecorator(
            id = contractId,
            artifact = artifactJson,
            manifest = expectedManifest,
            interfacesProvider = null
        )

        verify("imported contract decorator was correctly updated in the database") {
            assertThat(repository.getByContractIdAndProjectId(contractId, PROJECT_ID_1)).withMessage()
                .isEqualTo(expectedDecorator)
            assertThat(repository.getManifestJsonByContractIdAndProjectId(contractId, PROJECT_ID_1)).withMessage()
                .isEqualTo(expectedManifest)
            assertThat(
                repository.getArtifactJsonByContractIdAndProjectId(contractId, PROJECT_ID_1)
                    ?.copy(linkReferences = null, deployedLinkReferences = null)
            ).withMessage()
                .isEqualTo(artifactJson)
            assertThat(repository.getInfoMarkdownByContractIdAndProjectId(contractId, PROJECT_ID_1)).withMessage()
                .isEqualTo(infoMarkdown)
        }
    }

    @Test
    fun mustReturnNullWhenFetchingNonExistentImportedContractDecoratorByContractId() {
        val contractId = ContractId("abc")
        val projectId = UUID.randomUUID()

        verify("null is returned when fetching non-existent imported contract decorator by contract id") {
            assertThat(repository.getByContractIdAndProjectId(contractId, projectId)).withMessage()
                .isNull()
            assertThat(repository.getManifestJsonByContractIdAndProjectId(contractId, projectId)).withMessage()
                .isNull()
            assertThat(repository.getArtifactJsonByContractIdAndProjectId(contractId, projectId)).withMessage()
                .isNull()
            assertThat(repository.getInfoMarkdownByContractIdAndProjectId(contractId, projectId)).withMessage()
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyFetchImportedContractDecoratorsByProjectIdAndFilters() {
        val tag1TestData = createTestData(contractId = ContractId("cid-tag-1"), tags = listOf("tag-1"))
        val tag2TestData = createTestData(contractId = ContractId("cid-tag-2"), tags = listOf("tag-2"))
        val tag2AndIgnoredTagTestData = createTestData(
            contractId = ContractId("cid-tag-2-ignored"),
            tags = listOf("tag-2", "ignored-tag")
        )
        val ignoredTagTestData = createTestData(
            contractId = ContractId("cid-ignored-tag"),
            tags = listOf("ignored-tag")
        )
        val trait1TestData = createTestData(contractId = ContractId("cid-trait-1"), traits = listOf("trait-1"))
        val trait2TestData = createTestData(contractId = ContractId("cid-trait-2"), traits = listOf("trait-2"))
        val trait2AndIgnoredTraitTestData = createTestData(
            contractId = ContractId("cid-trait-2-ignored"),
            traits = listOf("trait-2", "ignored-trait")
        )
        val ignoredTraitTestData = createTestData(
            contractId = ContractId("cid-ignored-trait"),
            traits = listOf("ignored-trait")
        )
        val project2TestData1 = createTestData(
            contractId = ContractId("cid-1-project-2"),
            tags = listOf("tag-1", "tag-2"),
            traits = listOf("trait-1", "trait-2")
        )
        val project2TestData2 = createTestData(
            contractId = ContractId("cid-2-project-2"),
            tags = listOf("tag-1", "tag-2"),
            traits = listOf("trait-1", "trait-2")
        )
        val project2TestData3 = createTestData(
            contractId = ContractId("cid-3-project-2"),
            tags = listOf("ignored-tag", "tag-3"),
            traits = listOf("ignored-trait", "trait-3")
        )
        val project2NonMatchingTestData1 = createTestData(
            contractId = ContractId("cid-3-project-2-missing-tag"),
            tags = listOf("tag-1"),
            traits = listOf("ignored-trait", "trait-1", "trait-2")
        )
        val project2NonMatchingTestData2 = createTestData(
            contractId = ContractId("cid-4-project-2-missing-trait"),
            tags = listOf("ignored-tag", "tag-1", "tag-2"),
            traits = listOf("trait-1")
        )
        val testDataById = listOf(
            tag1TestData,
            tag2TestData,
            tag2AndIgnoredTagTestData,
            ignoredTagTestData,
            trait1TestData,
            trait2TestData,
            trait2AndIgnoredTraitTestData,
            ignoredTraitTestData,
            project2TestData1,
            project2TestData2,
            project2TestData3,
            project2NonMatchingTestData1,
            project2NonMatchingTestData2
        ).associateBy { it.contractId }

        val project1DecoratorsWithMatchingTags = listOf(
            createDecorator(projectId = PROJECT_ID_1, testData = tag1TestData),
            createDecorator(projectId = PROJECT_ID_1, testData = tag2TestData),
            createDecorator(projectId = PROJECT_ID_1, testData = tag2AndIgnoredTagTestData)
        )
        val project1DecoratorsWithNonMatchingTags = listOf(
            createDecorator(projectId = PROJECT_ID_1, testData = ignoredTagTestData)
        )
        val project1DecoratorsWithMatchingTraits = listOf(
            createDecorator(projectId = PROJECT_ID_1, testData = trait1TestData),
            createDecorator(projectId = PROJECT_ID_1, testData = trait2TestData),
            createDecorator(projectId = PROJECT_ID_1, testData = trait2AndIgnoredTraitTestData)
        )
        val project1DecoratorsWithNonMatchingTraits = listOf(
            createDecorator(projectId = PROJECT_ID_1, testData = ignoredTraitTestData)
        )

        val project2MatchingDecorators = listOf(
            createDecorator(projectId = PROJECT_ID_2, testData = project2TestData1),
            createDecorator(projectId = PROJECT_ID_2, testData = project2TestData2),
            createDecorator(projectId = PROJECT_ID_2, testData = project2TestData3)
        )
        val project2NonMatchingDecorators = listOf(
            createDecorator(projectId = PROJECT_ID_2, testData = project2NonMatchingTestData1),
            createDecorator(projectId = PROJECT_ID_2, testData = project2NonMatchingTestData2)
        )

        suppose("some imported contract decorators exist in database") {
            dslContext.batchInsert(
                (
                    project1DecoratorsWithMatchingTags + project1DecoratorsWithNonMatchingTags +
                        project1DecoratorsWithMatchingTraits + project1DecoratorsWithNonMatchingTraits +
                        project2MatchingDecorators + project2NonMatchingDecorators
                    )
                    .map {
                        ImportedContractDecoratorRecord(
                            id = UUID.randomUUID(),
                            projectId = it.projectId,
                            contractId = it.decorator.id,
                            manifestJson = it.manifest,
                            artifactJson = it.artifact,
                            infoMarkdown = it.markdown,
                            contractTags = it.manifest.tags.toTypedArray(),
                            contractImplements = it.manifest.implements.toTypedArray(),
                            importedAt = TestData.TIMESTAMP
                        )
                    }
            ).execute()
        }

        verify("must correctly fetch project 1 imported contract decorators with matching tags") {
            val filters = ContractDecoratorFilters(
                contractTags = OrList(
                    AndList(ContractTag("tag-1")),
                    AndList(ContractTag("tag-2"))
                ),
                contractImplements = OrList()
            )

            assertThat(repository.getAll(PROJECT_ID_1, filters)).withMessage()
                .containsExactlyInAnyOrderElementsOf(project1DecoratorsWithMatchingTags.map { it.decorator })
            assertThat(repository.getAllManifestJsonFiles(PROJECT_ID_1, filters)).withMessage()
                .containsExactlyInAnyOrderElementsOf(
                    project1DecoratorsWithMatchingTags.map { testDataById[it.decorator.id]!!.manifest }
                )
            assertThat(
                repository.getAllArtifactJsonFiles(PROJECT_ID_1, filters)
                    .map { it.copy(linkReferences = null, deployedLinkReferences = null) }
            ).withMessage()
                .containsExactlyInAnyOrderElementsOf(
                    project1DecoratorsWithMatchingTags.map { testDataById[it.decorator.id]!!.artifact }
                )
            assertThat(repository.getAllInfoMarkdownFiles(PROJECT_ID_1, filters)).withMessage()
                .containsExactlyInAnyOrderElementsOf(
                    project1DecoratorsWithMatchingTags.map { testDataById[it.decorator.id]!!.markdown }
                )
        }

        verify("must correctly fetch project 1 imported contract decorators with matching traits") {
            val filters = ContractDecoratorFilters(
                contractTags = OrList(),
                contractImplements = OrList(
                    AndList(InterfaceId("trait-1")),
                    AndList(InterfaceId("trait-2"))
                )
            )
            assertThat(repository.getAll(PROJECT_ID_1, filters)).withMessage()
                .containsExactlyInAnyOrderElementsOf(project1DecoratorsWithMatchingTraits.map { it.decorator })
            assertThat(repository.getAllManifestJsonFiles(PROJECT_ID_1, filters)).withMessage()
                .containsExactlyInAnyOrderElementsOf(
                    project1DecoratorsWithMatchingTraits.map { testDataById[it.decorator.id]!!.manifest }
                )
            assertThat(
                repository.getAllArtifactJsonFiles(PROJECT_ID_1, filters)
                    .map { it.copy(linkReferences = null, deployedLinkReferences = null) }
            ).withMessage()
                .containsExactlyInAnyOrderElementsOf(
                    project1DecoratorsWithMatchingTraits.map { testDataById[it.decorator.id]!!.artifact }
                )
            assertThat(repository.getAllInfoMarkdownFiles(PROJECT_ID_1, filters)).withMessage()
                .containsExactlyInAnyOrderElementsOf(
                    project1DecoratorsWithMatchingTraits.map { testDataById[it.decorator.id]!!.markdown }
                )
        }

        verify("must correctly fetch project 2 imported contract decorators which match given filters") {
            val filters = ContractDecoratorFilters(
                contractTags = OrList(
                    AndList(ContractTag("tag-1"), ContractTag("tag-2")),
                    AndList(ContractTag("tag-3"))
                ),
                contractImplements = OrList(
                    AndList(InterfaceId("trait-1"), InterfaceId("trait-2")),
                    AndList(InterfaceId("trait-3"))
                )
            )

            assertThat(repository.getAll(PROJECT_ID_2, filters)).withMessage()
                .containsExactlyInAnyOrderElementsOf(project2MatchingDecorators.map { it.decorator })
            assertThat(repository.getAllManifestJsonFiles(PROJECT_ID_2, filters)).withMessage()
                .containsExactlyInAnyOrderElementsOf(
                    project2MatchingDecorators.map { testDataById[it.decorator.id]!!.manifest }
                )
            assertThat(
                repository.getAllArtifactJsonFiles(PROJECT_ID_2, filters)
                    .map { it.copy(linkReferences = null, deployedLinkReferences = null) }
            ).withMessage()
                .containsExactlyInAnyOrderElementsOf(
                    project2MatchingDecorators.map { testDataById[it.decorator.id]!!.artifact }
                )
            assertThat(repository.getAllInfoMarkdownFiles(PROJECT_ID_2, filters)).withMessage()
                .containsExactlyInAnyOrderElementsOf(
                    project2MatchingDecorators.map { testDataById[it.decorator.id]!!.markdown }
                )
        }
    }

    private fun createTestData(
        contractId: ContractId,
        tags: List<String> = emptyList(),
        traits: List<String> = emptyList(),
    ) = DecoratorTestData(
        contractId = contractId,
        manifest = ManifestJson(
            name = "name-${contractId.value}",
            description = "description",
            tags = tags,
            implements = traits,
            eventDecorators = emptyList(),
            constructorDecorators = emptyList(),
            functionDecorators = emptyList()
        ),
        artifact = ArtifactJson(
            contractName = "imported-contract-${contractId.value}",
            sourceName = "imported.sol",
            abi = emptyList(),
            bytecode = "0x0",
            deployedBytecode = "0x0",
            linkReferences = null,
            deployedLinkReferences = null
        ),
        markdown = "markdown-${contractId.value}"
    )

    private fun createDecorator(projectId: UUID, testData: DecoratorTestData) = DecoratorWithProjectId(
        projectId = projectId,
        decorator = ContractDecorator(
            id = testData.contractId,
            artifact = testData.artifact,
            manifest = testData.manifest,
            interfacesProvider = null
        ),
        manifest = testData.manifest,
        artifact = testData.artifact,
        markdown = testData.markdown
    )
}
