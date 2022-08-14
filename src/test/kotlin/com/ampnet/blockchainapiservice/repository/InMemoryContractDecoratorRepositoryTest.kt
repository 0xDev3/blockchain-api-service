package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.model.filters.AndList
import com.ampnet.blockchainapiservice.model.filters.ContractDecoratorFilters
import com.ampnet.blockchainapiservice.model.filters.OrList
import com.ampnet.blockchainapiservice.model.json.ArtifactJson
import com.ampnet.blockchainapiservice.model.json.ManifestJson
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
            description = "description",
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
    fun mustCorrectlyStoreAndThenGetContractManifestJsonById() {
        val repository = InMemoryContractDecoratorRepository()
        val manifestJson = ManifestJson(
            description = "description",
            tags = emptyList(),
            implements = emptyList(),
            eventDecorators = emptyList(),
            constructorDecorators = emptyList(),
            functionDecorators = emptyList()
        )
        val id = ContractId("test")

        val storedManifest = suppose("some contract manifest.json is stored") {
            repository.store(id, manifestJson)
        }

        verify("correct contract manifest.json is returned") {
            assertThat(storedManifest).withMessage()
                .isEqualTo(manifestJson)
            assertThat(repository.getManifestJsonById(id)).withMessage()
                .isEqualTo(manifestJson)
        }
    }

    @Test
    fun mustCorrectlyStoreAndThenGetContractArtifactJsonById() {
        val repository = InMemoryContractDecoratorRepository()
        val artifactJson = ArtifactJson(
            contractName = "example",
            sourceName = "Example",
            abi = emptyList(),
            bytecode = "0x0",
            deployedBytecode = "0x0",
            linkReferences = null,
            deployedLinkReferences = null
        )
        val id = ContractId("test")

        val storedArtifact = suppose("some contract artifact.json is stored") {
            repository.store(id, artifactJson)
        }

        verify("correct contract artifact.json is returned") {
            assertThat(storedArtifact).withMessage()
                .isEqualTo(artifactJson)
            assertThat(repository.getArtifactJsonById(id)).withMessage()
                .isEqualTo(artifactJson)
        }
    }

    @Test
    fun mustCorrectlyStoreAndThenGetContractInfoMarkdownById() {
        val repository = InMemoryContractDecoratorRepository()
        val infoMd = "info-md"
        val id = ContractId("test")

        val storedInfoMd = suppose("some contract info.md is stored") {
            repository.store(id, infoMd)
        }

        verify("correct contract info.md is returned") {
            assertThat(storedInfoMd).withMessage()
                .isEqualTo(infoMd)
            assertThat(repository.getInfoMarkdownById(id)).withMessage()
                .isEqualTo(infoMd)
        }
    }

    @Test
    fun mustCorrectlyDeleteContractDecoratorAndThenReturnNullWhenGettingById() {
        val repository = InMemoryContractDecoratorRepository()
        val decorator = ContractDecorator(
            id = ContractId("example"),
            description = "description",
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

        val manifestJson = ManifestJson(
            description = "description",
            tags = emptyList(),
            implements = emptyList(),
            eventDecorators = emptyList(),
            constructorDecorators = emptyList(),
            functionDecorators = emptyList()
        )

        suppose("some contract manifest.json is stored") {
            repository.store(decorator.id, manifestJson)
        }

        verify("correct contract manifest.json is returned") {
            assertThat(repository.getManifestJsonById(decorator.id)).withMessage()
                .isEqualTo(manifestJson)
        }

        val artifactJson = ArtifactJson(
            contractName = "example",
            sourceName = "Example",
            abi = emptyList(),
            bytecode = "0x0",
            deployedBytecode = "0x0",
            linkReferences = null,
            deployedLinkReferences = null
        )

        suppose("some contract artifact.json is stored") {
            repository.store(decorator.id, artifactJson)
        }

        verify("correct contract artifact.json is returned") {
            assertThat(repository.getArtifactJsonById(decorator.id)).withMessage()
                .isEqualTo(artifactJson)
        }

        val infoMd = "info-md"

        suppose("some contract info.md is stored") {
            repository.store(decorator.id, infoMd)
        }

        verify("correct contract info.md is returned") {
            assertThat(repository.getInfoMarkdownById(decorator.id)).withMessage()
                .isEqualTo(infoMd)
        }

        suppose("contract decorator is deleted") {
            repository.delete(decorator.id)
        }

        verify("null is returned") {
            assertThat(repository.getById(decorator.id)).withMessage()
                .isNull()
            assertThat(repository.getManifestJsonById(decorator.id)).withMessage()
                .isNull()
            assertThat(repository.getArtifactJsonById(decorator.id)).withMessage()
                .isNull()
            assertThat(repository.getInfoMarkdownById(decorator.id)).withMessage()
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

    @Test
    fun mustCorrectlyGetAllContractManifestJsonsWithSomeTagFilters() {
        val matching = listOf(
            decoratorAndManifest(tags = listOf(ContractTag("1"), ContractTag("2"))),
            decoratorAndManifest(tags = listOf(ContractTag("3"))),
            decoratorAndManifest(tags = listOf(ContractTag("1"), ContractTag("2"), ContractTag("3"))),
            decoratorAndManifest(tags = listOf(ContractTag("1"), ContractTag("2"), ContractTag("extra"))),
            decoratorAndManifest(tags = listOf(ContractTag("3"), ContractTag("extra"))),
            decoratorAndManifest(
                tags = listOf(ContractTag("1"), ContractTag("2"), ContractTag("3"), ContractTag("extra"))
            )
        )
        val nonMatching = listOf(
            decoratorAndManifest(tags = listOf(ContractTag("1"))),
            decoratorAndManifest(tags = listOf(ContractTag("2"))),
            decoratorAndManifest(tags = listOf(ContractTag("extra")))
        )
        val all = matching + nonMatching

        val repository = InMemoryContractDecoratorRepository()

        suppose("some contract decorators and manifest.json files are stored") {
            all.forEach {
                repository.store(it.first)
                repository.store(it.first.id, it.second)
            }
        }

        val filters = ContractDecoratorFilters(
            contractTags = OrList(
                AndList(ContractTag("1"), ContractTag("2")),
                AndList(ContractTag("3"))
            ),
            contractImplements = OrList()
        )

        verify("correct contract manifest.json files are returned") {
            assertThat(repository.getAllManifestJsonFiles(filters)).withMessage()
                .containsExactlyInAnyOrderElementsOf(matching.map { it.second })
            assertThat(repository.getAllManifestJsonFiles(ContractDecoratorFilters(OrList(), OrList()))).withMessage()
                .containsExactlyInAnyOrderElementsOf(all.map { it.second })
        }
    }

    @Test
    fun mustCorrectlyGetAllContractManifestJsonsWithSomeImplementsFilters() {
        val matching = listOf(
            decoratorAndManifest(implements = listOf(ContractTrait("1"), ContractTrait("2"))),
            decoratorAndManifest(implements = listOf(ContractTrait("3"))),
            decoratorAndManifest(implements = listOf(ContractTrait("1"), ContractTrait("2"), ContractTrait("3"))),
            decoratorAndManifest(implements = listOf(ContractTrait("1"), ContractTrait("2"), ContractTrait("extra"))),
            decoratorAndManifest(implements = listOf(ContractTrait("3"), ContractTrait("tag"))),
            decoratorAndManifest(
                implements = listOf(
                    ContractTrait("1"), ContractTrait("2"), ContractTrait("3"), ContractTrait("extra")
                )
            )
        )
        val nonMatching = listOf(
            decoratorAndManifest(implements = listOf(ContractTrait("1"))),
            decoratorAndManifest(implements = listOf(ContractTrait("2"))),
            decoratorAndManifest(implements = listOf(ContractTrait("extra")))
        )
        val all = matching + nonMatching

        val repository = InMemoryContractDecoratorRepository()

        suppose("some contract manifest.json files are stored") {
            all.forEach {
                repository.store(it.first)
                repository.store(it.first.id, it.second)
            }
        }

        val filters = ContractDecoratorFilters(
            contractTags = OrList(),
            contractImplements = OrList(
                AndList(ContractTrait("1"), ContractTrait("2")),
                AndList(ContractTrait("3"))
            )
        )

        verify("correct contract manifest.json files are returned") {
            assertThat(repository.getAllManifestJsonFiles(filters)).withMessage()
                .containsExactlyInAnyOrderElementsOf(matching.map { it.second })
            assertThat(repository.getAllManifestJsonFiles(ContractDecoratorFilters(OrList(), OrList()))).withMessage()
                .containsExactlyInAnyOrderElementsOf(all.map { it.second })
        }
    }

    @Test
    fun mustCorrectlyGetAllContractArtifactJsonsWithSomeTagFilters() {
        val matching = listOf(
            decoratorAndArtifact(tags = listOf(ContractTag("1"), ContractTag("2"))),
            decoratorAndArtifact(tags = listOf(ContractTag("3"))),
            decoratorAndArtifact(tags = listOf(ContractTag("1"), ContractTag("2"), ContractTag("3"))),
            decoratorAndArtifact(tags = listOf(ContractTag("1"), ContractTag("2"), ContractTag("extra"))),
            decoratorAndArtifact(tags = listOf(ContractTag("3"), ContractTag("extra"))),
            decoratorAndArtifact(
                tags = listOf(ContractTag("1"), ContractTag("2"), ContractTag("3"), ContractTag("extra"))
            )
        )
        val nonMatching = listOf(
            decoratorAndArtifact(tags = listOf(ContractTag("1"))),
            decoratorAndArtifact(tags = listOf(ContractTag("2"))),
            decoratorAndArtifact(tags = listOf(ContractTag("extra")))
        )
        val all = matching + nonMatching

        val repository = InMemoryContractDecoratorRepository()

        suppose("some contract decorators and artifact.json files are stored") {
            all.forEach {
                repository.store(it.first)
                repository.store(it.first.id, it.second)
            }
        }

        val filters = ContractDecoratorFilters(
            contractTags = OrList(
                AndList(ContractTag("1"), ContractTag("2")),
                AndList(ContractTag("3"))
            ),
            contractImplements = OrList()
        )

        verify("correct contract artifact.json files are returned") {
            assertThat(repository.getAllArtifactJsonFiles(filters)).withMessage()
                .containsExactlyInAnyOrderElementsOf(matching.map { it.second })
            assertThat(repository.getAllArtifactJsonFiles(ContractDecoratorFilters(OrList(), OrList()))).withMessage()
                .containsExactlyInAnyOrderElementsOf(all.map { it.second })
        }
    }

    @Test
    fun mustCorrectlyGetAllContractArtifactJsonsWithSomeImplementsFilters() {
        val matching = listOf(
            decoratorAndArtifact(implements = listOf(ContractTrait("1"), ContractTrait("2"))),
            decoratorAndArtifact(implements = listOf(ContractTrait("3"))),
            decoratorAndArtifact(implements = listOf(ContractTrait("1"), ContractTrait("2"), ContractTrait("3"))),
            decoratorAndArtifact(implements = listOf(ContractTrait("1"), ContractTrait("2"), ContractTrait("extra"))),
            decoratorAndArtifact(implements = listOf(ContractTrait("3"), ContractTrait("tag"))),
            decoratorAndArtifact(
                implements = listOf(
                    ContractTrait("1"), ContractTrait("2"), ContractTrait("3"), ContractTrait("extra")
                )
            )
        )
        val nonMatching = listOf(
            decoratorAndArtifact(implements = listOf(ContractTrait("1"))),
            decoratorAndArtifact(implements = listOf(ContractTrait("2"))),
            decoratorAndArtifact(implements = listOf(ContractTrait("extra")))
        )
        val all = matching + nonMatching

        val repository = InMemoryContractDecoratorRepository()

        suppose("some contract artifact.json files are stored") {
            all.forEach {
                repository.store(it.first)
                repository.store(it.first.id, it.second)
            }
        }

        val filters = ContractDecoratorFilters(
            contractTags = OrList(),
            contractImplements = OrList(
                AndList(ContractTrait("1"), ContractTrait("2")),
                AndList(ContractTrait("3"))
            )
        )

        verify("correct contract artifact.json files are returned") {
            assertThat(repository.getAllArtifactJsonFiles(filters)).withMessage()
                .containsExactlyInAnyOrderElementsOf(matching.map { it.second })
            assertThat(repository.getAllArtifactJsonFiles(ContractDecoratorFilters(OrList(), OrList()))).withMessage()
                .containsExactlyInAnyOrderElementsOf(all.map { it.second })
        }
    }

    @Test
    fun mustCorrectlyGetAllContractInfoMarkdownsWithSomeTagFilters() {
        val matching = listOf(
            decoratorAndInfoMd(tags = listOf(ContractTag("1"), ContractTag("2"))),
            decoratorAndInfoMd(tags = listOf(ContractTag("3"))),
            decoratorAndInfoMd(tags = listOf(ContractTag("1"), ContractTag("2"), ContractTag("3"))),
            decoratorAndInfoMd(tags = listOf(ContractTag("1"), ContractTag("2"), ContractTag("extra"))),
            decoratorAndInfoMd(tags = listOf(ContractTag("3"), ContractTag("extra"))),
            decoratorAndInfoMd(
                tags = listOf(ContractTag("1"), ContractTag("2"), ContractTag("3"), ContractTag("extra"))
            )
        )
        val nonMatching = listOf(
            decoratorAndInfoMd(tags = listOf(ContractTag("1"))),
            decoratorAndInfoMd(tags = listOf(ContractTag("2"))),
            decoratorAndInfoMd(tags = listOf(ContractTag("extra")))
        )
        val all = matching + nonMatching

        val repository = InMemoryContractDecoratorRepository()

        suppose("some contract decorators and info.md files are stored") {
            all.forEach {
                repository.store(it.first)
                repository.store(it.first.id, it.second)
            }
        }

        val filters = ContractDecoratorFilters(
            contractTags = OrList(
                AndList(ContractTag("1"), ContractTag("2")),
                AndList(ContractTag("3"))
            ),
            contractImplements = OrList()
        )

        verify("correct contract info.md files are returned") {
            assertThat(repository.getAllInfoMarkdownFiles(filters)).withMessage()
                .containsExactlyInAnyOrderElementsOf(matching.map { it.second })
            assertThat(repository.getAllInfoMarkdownFiles(ContractDecoratorFilters(OrList(), OrList()))).withMessage()
                .containsExactlyInAnyOrderElementsOf(all.map { it.second })
        }
    }

    @Test
    fun mustCorrectlyGetAllContractInfoMarkdownsWithSomeImplementsFilters() {
        val matching = listOf(
            decoratorAndInfoMd(implements = listOf(ContractTrait("1"), ContractTrait("2"))),
            decoratorAndInfoMd(implements = listOf(ContractTrait("3"))),
            decoratorAndInfoMd(implements = listOf(ContractTrait("1"), ContractTrait("2"), ContractTrait("3"))),
            decoratorAndInfoMd(implements = listOf(ContractTrait("1"), ContractTrait("2"), ContractTrait("extra"))),
            decoratorAndInfoMd(implements = listOf(ContractTrait("3"), ContractTrait("tag"))),
            decoratorAndInfoMd(
                implements = listOf(
                    ContractTrait("1"), ContractTrait("2"), ContractTrait("3"), ContractTrait("extra")
                )
            )
        )
        val nonMatching = listOf(
            decoratorAndInfoMd(implements = listOf(ContractTrait("1"))),
            decoratorAndInfoMd(implements = listOf(ContractTrait("2"))),
            decoratorAndInfoMd(implements = listOf(ContractTrait("extra")))
        )
        val all = matching + nonMatching

        val repository = InMemoryContractDecoratorRepository()

        suppose("some contract info.md files are stored") {
            all.forEach {
                repository.store(it.first)
                repository.store(it.first.id, it.second)
            }
        }

        val filters = ContractDecoratorFilters(
            contractTags = OrList(),
            contractImplements = OrList(
                AndList(ContractTrait("1"), ContractTrait("2")),
                AndList(ContractTrait("3"))
            )
        )

        verify("correct contract info.md files are returned") {
            assertThat(repository.getAllInfoMarkdownFiles(filters)).withMessage()
                .containsExactlyInAnyOrderElementsOf(matching.map { it.second })
            assertThat(repository.getAllInfoMarkdownFiles(ContractDecoratorFilters(OrList(), OrList()))).withMessage()
                .containsExactlyInAnyOrderElementsOf(all.map { it.second })
        }
    }

    private fun decorator(tags: List<ContractTag> = emptyList(), implements: List<ContractTrait> = emptyList()) =
        ContractDecorator(
            id = ContractId(UUID.randomUUID().toString()),
            description = "description",
            binary = ContractBinaryData("0x0"),
            tags = tags,
            implements = implements,
            constructors = emptyList(),
            functions = emptyList(),
            events = emptyList()
        )

    private fun decoratorAndManifest(
        tags: List<ContractTag> = emptyList(),
        implements: List<ContractTrait> = emptyList()
    ) = Pair(
        decorator(tags, implements),
        ManifestJson(
            description = "description",
            tags = tags.map { it.value },
            implements = implements.map { it.value },
            eventDecorators = emptyList(),
            constructorDecorators = emptyList(),
            functionDecorators = emptyList()
        )
    )

    private fun decoratorAndArtifact(
        tags: List<ContractTag> = emptyList(),
        implements: List<ContractTrait> = emptyList()
    ) = Pair(
        decorator(tags, implements),
        ArtifactJson(
            contractName = UUID.randomUUID().toString(),
            sourceName = "Example",
            abi = emptyList(),
            bytecode = "0x0",
            deployedBytecode = "0x0",
            linkReferences = null,
            deployedLinkReferences = null
        )
    )

    private fun decoratorAndInfoMd(
        tags: List<ContractTag> = emptyList(),
        implements: List<ContractTrait> = emptyList()
    ) = Pair(
        decorator(tags, implements),
        "info-md-${UUID.randomUUID()}"
    )
}
