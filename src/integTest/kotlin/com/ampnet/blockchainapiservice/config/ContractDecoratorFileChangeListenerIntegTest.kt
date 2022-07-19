package com.ampnet.blockchainapiservice.config

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.model.result.ContractConstructor
import com.ampnet.blockchainapiservice.model.result.ContractDecorator
import com.ampnet.blockchainapiservice.model.result.ContractEvent
import com.ampnet.blockchainapiservice.model.result.ContractFunction
import com.ampnet.blockchainapiservice.model.result.ContractParameter
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

    companion object {
        private val CONSTRUCTORS = listOf(
            ContractConstructor(
                inputs = listOf(
                    ContractParameter(
                        name = "Arg",
                        description = "Arg description",
                        solidityName = "arg",
                        solidityType = "address",
                        recommendedTypes = listOf("example")
                    )
                ),
                description = "Constructor description",
                payable = false
            ),
            ContractConstructor(
                inputs = listOf(
                    ContractParameter(
                        name = "Arg",
                        description = "Arg description",
                        solidityName = "arg",
                        solidityType = "string",
                        recommendedTypes = listOf("example")
                    )
                ),
                description = "Payable constructor description",
                payable = true
            )
        )
        private val FUNCTIONS = listOf(
            ContractFunction(
                name = "View function",
                description = "View function description",
                solidityName = "viewFn",
                inputs = listOf(
                    ContractParameter(
                        name = "Arg",
                        description = "Arg description",
                        solidityName = "arg",
                        solidityType = "address",
                        recommendedTypes = listOf("example")
                    )
                ),
                outputs = listOf(
                    ContractParameter(
                        name = "Return value",
                        description = "Return value description",
                        solidityName = "",
                        solidityType = "bool",
                        recommendedTypes = listOf("example")
                    )
                ),
                emittableEvents = listOf("ExampleEvent(address)"),
                readOnly = true
            ),
            ContractFunction(
                name = "Pure function",
                description = "Pure function description",
                solidityName = "pureFn",
                inputs = listOf(
                    ContractParameter(
                        name = "Arg",
                        description = "Arg description",
                        solidityName = "arg",
                        solidityType = "address",
                        recommendedTypes = listOf("example")
                    )
                ),
                outputs = listOf(
                    ContractParameter(
                        name = "Return value",
                        description = "Return value description",
                        solidityName = "",
                        solidityType = "bool",
                        recommendedTypes = listOf("example")
                    )
                ),
                emittableEvents = listOf("ExampleEvent(address)"),
                readOnly = true
            ),
            ContractFunction(
                name = "Modifying function",
                description = "Modifying function description",
                solidityName = "modifyingFn",
                inputs = listOf(
                    ContractParameter(
                        name = "Arg",
                        description = "Arg description",
                        solidityName = "arg",
                        solidityType = "address",
                        recommendedTypes = listOf("example")
                    )
                ),
                outputs = listOf(
                    ContractParameter(
                        name = "Return value",
                        description = "Return value description",
                        solidityName = "",
                        solidityType = "bool",
                        recommendedTypes = listOf("example")
                    )
                ),
                emittableEvents = listOf("ExampleEvent(address)"),
                readOnly = false
            )
        )
        private val EVENTS = listOf(
            ContractEvent(
                name = "Example Event",
                description = "Example Event description",
                solidityName = "ExampleEvent",
                inputs = listOf(
                    ContractParameter(
                        name = "Arg",
                        description = "Arg description",
                        solidityName = "arg",
                        solidityType = "address",
                        recommendedTypes = listOf("example")
                    )
                )
            )
        )
    }

    private val parsableRootDir = Paths.get(javaClass.classLoader.getResource("dummyContracts")!!.path)
    private val unparsableRootDir = Paths.get(javaClass.classLoader.getResource("unparsableContracts")!!.path)
    private val ignoredDirs = listOf("IgnoredContract")

    @Test
    fun mustCorrectlyLoadInitialContractDecorators() {
        val repository = InMemoryContractDecoratorRepository()

        suppose("initial contract decorators will be loaded from file system") {
            ContractDecoratorFileChangeListener(
                repository = repository,
                rootDir = parsableRootDir,
                ignoredDirs = ignoredDirs,
                objectMapper = JsonConfig().objectMapper()
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
                        implements = listOf(ContractTrait("trait.example")),
                        constructors = CONSTRUCTORS,
                        functions = FUNCTIONS,
                        events = EVENTS
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
                        implements = listOf(ContractTrait("trait.another")),
                        constructors = CONSTRUCTORS,
                        functions = FUNCTIONS,
                        events = EVENTS
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
                rootDir = parsableRootDir,
                ignoredDirs = ignoredDirs,
                objectMapper = JsonConfig().objectMapper()
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
            implements = listOf(ContractTrait("trait.no.manifest")),
            constructors = CONSTRUCTORS,
            functions = FUNCTIONS,
            events = EVENTS
        )

        suppose("contract which needs to be deleted is in repository") {
            repository.store(
                ContractDecorator(
                    id = contractWithoutArtifactId,
                    binary = ContractBinaryData("0x4"),
                    tags = listOf(ContractTag("tag.no.artifact")),
                    implements = listOf(ContractTrait("trait.no.artifact")),
                    constructors = CONSTRUCTORS,
                    functions = FUNCTIONS,
                    events = EVENTS
                )
            )
            repository.store(withoutManifestDecorator)
        }

        suppose("listener will get some file changes") {
            listener.onChange(
                setOf(
                    ChangedFiles(
                        parsableRootDir.toFile(),
                        setOf(
                            ChangedFile(
                                parsableRootDir.toFile(),
                                parsableRootDir.resolve("DummyContractSet/DummyContract/artifact.json").toFile(),
                                ChangedFile.Type.ADD
                            ),
                            ChangedFile(
                                parsableRootDir.toFile(),
                                parsableRootDir.resolve("DummyContractSet/ContractWithoutArtifact/artifact.json")
                                    .toFile(),
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
                        implements = listOf(ContractTrait("trait.example")),
                        constructors = CONSTRUCTORS,
                        functions = FUNCTIONS,
                        events = EVENTS
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

    @Test
    fun mustSkipUnparsableContractDecorators() {
        val repository = InMemoryContractDecoratorRepository()

        suppose("initial contract decorators will be loaded from file system") {
            ContractDecoratorFileChangeListener(
                repository = repository,
                rootDir = unparsableRootDir,
                ignoredDirs = ignoredDirs,
                objectMapper = JsonConfig().objectMapper()
            )
        }

        verify("no contract decorators have been loaded") {
            assertThat(repository.getById(ContractId("InvalidJson/UnparsableArtifact"))).withMessage()
                .isNull()
            assertThat(repository.getById(ContractId("InvalidJson/UnparsableManifest"))).withMessage()
                .isNull()
            assertThat(repository.getById(ContractId("MissingValue/MissingConstructorSignature"))).withMessage()
                .isNull()
            assertThat(repository.getById(ContractId("MissingValue/MissingEventName"))).withMessage()
                .isNull()
            assertThat(repository.getById(ContractId("MissingValue/MissingFunctionName"))).withMessage()
                .isNull()
            assertThat(repository.getById(ContractId("MissingValue/MissingFunctionOutputs"))).withMessage()
                .isNull()
        }
    }
}
