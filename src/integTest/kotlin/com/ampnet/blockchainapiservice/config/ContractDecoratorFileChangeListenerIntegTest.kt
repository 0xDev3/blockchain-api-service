package com.ampnet.blockchainapiservice.config

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.model.result.ContractDecorator
import com.ampnet.blockchainapiservice.repository.InMemoryContractDecoratorRepository
import com.ampnet.blockchainapiservice.util.ContractBinaryData
import com.ampnet.blockchainapiservice.util.ContractId
import com.ampnet.blockchainapiservice.util.ContractTag
import com.ampnet.blockchainapiservice.util.ContractTrait
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.devtools.filewatch.ChangedFile
import org.springframework.boot.devtools.filewatch.ChangedFiles
import java.nio.file.Paths

class ContractDecoratorFileChangeListenerIntegTest : TestBase() {

    private val rootDir = Paths.get(javaClass.classLoader.getResource("dummyContracts")!!.path)
    private val ignoredDirs = listOf("IgnoredContract")

    @Test
    fun mustCorrectlyLoadInitialContractDecorators() {
        val repository = InMemoryContractDecoratorRepository()

        suppose("initial contract decorators will be loaded from file system") {
            ContractDecoratorFileChangeListener(
                repository = repository,
                rootDir = rootDir,
                ignoredDirs = ignoredDirs
            )
        }

        verify("correct contract decorators have been loaded") {
            val dummyContractId = ContractId("DummyContractSet/DummyContract")

            assertThat(repository.getById(dummyContractId)).withMessage()
                .isEqualTo(
                    ContractDecorator(
                        id = dummyContractId,
                        binary = ContractBinaryData("0x0"),
                        tags = listOf(ContractTag("tag.example")),
                        implements = listOf(ContractTrait("trait.example"))
                    )
                )

            assertThat(repository.getById(ContractId("DummyContractSet/ContractWithoutArtifact"))).withMessage()
                .isNull()
            assertThat(repository.getById(ContractId("DummyContractSet/ContractWithoutManifest"))).withMessage()
                .isNull()

            val anotherContractId = ContractId("AnotherContractSet/AnotherContract")

            assertThat(repository.getById(anotherContractId)).withMessage()
                .isEqualTo(
                    ContractDecorator(
                        id = anotherContractId,
                        binary = ContractBinaryData("0x2"),
                        tags = listOf(ContractTag("tag.another")),
                        implements = listOf(ContractTrait("trait.another"))
                    )
                )

            assertThat(repository.getById(ContractId("AnotherContractSet/IgnoredContract"))).withMessage()
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyReloadContractsAfterSomeFileChangesHaveBeenDetected() {
        val repository = InMemoryContractDecoratorRepository()

        val listener = suppose("initial contract decorators will be loaded from file system") {
            ContractDecoratorFileChangeListener(
                repository = repository,
                rootDir = rootDir,
                ignoredDirs = ignoredDirs
            )
        }

        val dummyContractId = ContractId("DummyContractSet/DummyContract")
        val anotherContractId = ContractId("AnotherContractSet/AnotherContract")

        suppose("existing contracts will be removed from repository") {
            repository.delete(dummyContractId)
            repository.delete(anotherContractId)
        }

        val contractWithoutArtifactId = ContractId("DummyContractSet/ContractWithoutArtifact")
        val contractWithoutManifestId = ContractId("DummyContractSet/ContractWithoutManifest")

        val withoutManifestDecorator = ContractDecorator(
            id = contractWithoutManifestId,
            binary = ContractBinaryData("0x1"),
            tags = listOf(ContractTag("tag.no.manifest")),
            implements = listOf(ContractTrait("trait.no.manifest"))
        )

        suppose("contract which needs to be deleted is in repository") {
            repository.store(
                ContractDecorator(
                    id = contractWithoutArtifactId,
                    binary = ContractBinaryData("0x4"),
                    tags = listOf(ContractTag("tag.no.artifact")),
                    implements = listOf(ContractTrait("trait.no.artifact"))
                )
            )
            repository.store(withoutManifestDecorator)
        }

        suppose("listener will get some file changes") {
            listener.onChange(
                setOf(
                    ChangedFiles(
                        rootDir.toFile(),
                        setOf(
                            ChangedFile(
                                rootDir.toFile(),
                                rootDir.resolve("DummyContractSet/DummyContract/artifact.json").toFile(),
                                ChangedFile.Type.ADD
                            ),
                            ChangedFile(
                                rootDir.toFile(),
                                rootDir.resolve("DummyContractSet/ContractWithoutArtifact/artifact.json").toFile(),
                                ChangedFile.Type.DELETE
                            )
                        )
                    )
                )
            )
        }

        verify("correct contract decorators have been updated in database") {
            assertThat(repository.getById(dummyContractId)).withMessage()
                .isEqualTo(
                    ContractDecorator(
                        id = dummyContractId,
                        binary = ContractBinaryData("0x0"),
                        tags = listOf(ContractTag("tag.example")),
                        implements = listOf(ContractTrait("trait.example"))
                    )
                )

            assertThat(repository.getById(contractWithoutArtifactId)).withMessage()
                .isNull()
            assertThat(repository.getById(contractWithoutManifestId)).withMessage()
                .isEqualTo(withoutManifestDecorator)

            assertThat(repository.getById(anotherContractId)).withMessage()
                .isNull()
            assertThat(repository.getById(ContractId("AnotherContractSet/IgnoredContract"))).withMessage()
                .isNull()
        }
    }
}
