package com.ampnet.blockchainapiservice.config

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.model.result.ContractConstructor
import com.ampnet.blockchainapiservice.model.result.ContractDecorator
import com.ampnet.blockchainapiservice.model.result.ContractEvent
import com.ampnet.blockchainapiservice.model.result.ContractFunction
import com.ampnet.blockchainapiservice.model.result.ContractParameter
import com.ampnet.blockchainapiservice.repository.ContractMetadataRepository
import com.ampnet.blockchainapiservice.repository.InMemoryContractDecoratorRepository
import com.ampnet.blockchainapiservice.repository.JooqContractMetadataRepository
import com.ampnet.blockchainapiservice.service.RandomUuidProvider
import com.ampnet.blockchainapiservice.testcontainers.SharedTestContainers
import com.ampnet.blockchainapiservice.util.Constants
import com.ampnet.blockchainapiservice.util.ContractBinaryData
import com.ampnet.blockchainapiservice.util.ContractId
import com.ampnet.blockchainapiservice.util.ContractTag
import com.ampnet.blockchainapiservice.util.ContractTrait
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.devtools.filewatch.ChangedFile
import org.springframework.boot.devtools.filewatch.ChangedFiles
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.Import
import java.nio.file.Paths

@JooqTest
@Import(JooqContractMetadataRepository::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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
                        recommendedTypes = listOf("example"),
                        parameters = null
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
                        recommendedTypes = listOf("example"),
                        parameters = null
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
                        recommendedTypes = listOf("example"),
                        parameters = null
                    )
                ),
                outputs = listOf(
                    ContractParameter(
                        name = "Return value",
                        description = "Return value description",
                        solidityName = "",
                        solidityType = "bool",
                        recommendedTypes = listOf("example"),
                        parameters = null
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
                        recommendedTypes = listOf("example"),
                        parameters = null
                    )
                ),
                outputs = listOf(
                    ContractParameter(
                        name = "Return value",
                        description = "Return value description",
                        solidityName = "",
                        solidityType = "bool",
                        recommendedTypes = listOf("example"),
                        parameters = null
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
                        recommendedTypes = listOf("example"),
                        parameters = null
                    )
                ),
                outputs = listOf(
                    ContractParameter(
                        name = "Return value",
                        description = "Return value description",
                        solidityName = "",
                        solidityType = "bool",
                        recommendedTypes = listOf("example"),
                        parameters = null
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
                        recommendedTypes = listOf("example"),
                        parameters = null
                    )
                )
            )
        )
    }

    private val parsableRootDir = Paths.get(javaClass.classLoader.getResource("dummyContracts")!!.path)
    private val unparsableRootDir = Paths.get(javaClass.classLoader.getResource("unparsableContracts")!!.path)
    private val ignoredDirs = listOf("IgnoredContract")

    @Suppress("unused")
    private val postgresContainer = SharedTestContainers.postgresContainer

    @Autowired
    private lateinit var contractMetadataRepository: ContractMetadataRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        postgresContainer.cleanAllDatabaseTables(dslContext)
    }

    @Test
    fun mustCorrectlyLoadInitialContractDecorators() {
        val contractDecoratorRepository = InMemoryContractDecoratorRepository()

        suppose("initial contract decorators will be loaded from file system") {
            ContractDecoratorFileChangeListener(
                uuidProvider = RandomUuidProvider(),
                contractDecoratorRepository = contractDecoratorRepository,
                contractMetadataRepository = contractMetadataRepository,
                objectMapper = JsonConfig().objectMapper(),
                rootDir = parsableRootDir,
                ignoredDirs = ignoredDirs
            )
        }

        val dummyContractId = ContractId("DummyContractSet/DummyContract")
        val anotherContractId = ContractId("AnotherContractSet/AnotherContract")
        val contractWithoutArtifactId = ContractId("DummyContractSet/ContractWithoutArtifact")
        val contractWithoutManifestId = ContractId("DummyContractSet/ContractWithoutManifest")
        val ignoredContractId = ContractId("AnotherContractSet/IgnoredContract")

        verify("correct contract decorators have been loaded") {

            assertThat(contractDecoratorRepository.getById(dummyContractId)).withMessage()
                .isEqualTo(
                    ContractDecorator(
                        id = dummyContractId,
                        name = "name",
                        description = "description",
                        binary = ContractBinaryData("0x0"),
                        tags = listOf(ContractTag("tag.example")),
                        implements = listOf(ContractTrait("trait.example")),
                        constructors = CONSTRUCTORS,
                        functions = FUNCTIONS,
                        events = EVENTS
                    )
                )

            assertThat(contractDecoratorRepository.getById(contractWithoutArtifactId)).withMessage()
                .isNull()
            assertThat(contractDecoratorRepository.getById(contractWithoutManifestId)).withMessage()
                .isNull()

            assertThat(contractDecoratorRepository.getById(anotherContractId)).withMessage()
                .isEqualTo(
                    ContractDecorator(
                        id = anotherContractId,
                        name = "name",
                        description = "description",
                        binary = ContractBinaryData("0x2"),
                        tags = listOf(ContractTag("tag.another")),
                        implements = listOf(ContractTrait("trait.another")),
                        constructors = CONSTRUCTORS,
                        functions = FUNCTIONS,
                        events = EVENTS
                    )
                )

            assertThat(contractDecoratorRepository.getById(ignoredContractId)).withMessage()
                .isNull()
        }

        verify("correct contract metadata exists in the database") {
            assertThat(contractMetadataRepository.exists(dummyContractId, Constants.NIL_UUID)).withMessage()
                .isTrue()
            assertThat(contractMetadataRepository.exists(anotherContractId, Constants.NIL_UUID)).withMessage()
                .isTrue()

            assertThat(contractMetadataRepository.exists(contractWithoutArtifactId, Constants.NIL_UUID)).withMessage()
                .isFalse()
            assertThat(contractMetadataRepository.exists(contractWithoutManifestId, Constants.NIL_UUID)).withMessage()
                .isFalse()
            assertThat(contractMetadataRepository.exists(ignoredContractId, Constants.NIL_UUID)).withMessage()
                .isFalse()
        }
    }

    @Test
    fun mustCorrectlyReloadContractsAfterSomeFileChangesHaveBeenDetected() {
        val contractDecoratorRepository = InMemoryContractDecoratorRepository()

        val listener = suppose("initial contract decorators will be loaded from file system") {
            ContractDecoratorFileChangeListener(
                uuidProvider = RandomUuidProvider(),
                contractDecoratorRepository = contractDecoratorRepository,
                contractMetadataRepository = contractMetadataRepository,
                objectMapper = JsonConfig().objectMapper(),
                rootDir = parsableRootDir,
                ignoredDirs = ignoredDirs
            )
        }

        val dummyContractId = ContractId("DummyContractSet/DummyContract")
        val anotherContractId = ContractId("AnotherContractSet/AnotherContract")

        suppose("existing contracts will be removed from repository") {
            contractDecoratorRepository.delete(dummyContractId)
            contractDecoratorRepository.delete(anotherContractId)
        }

        val contractWithoutArtifactId = ContractId("DummyContractSet/ContractWithoutArtifact")
        val contractWithoutManifestId = ContractId("DummyContractSet/ContractWithoutManifest")

        val withoutManifestDecorator = ContractDecorator(
            id = contractWithoutManifestId,
            name = "name",
            description = "description",
            binary = ContractBinaryData("0x1"),
            tags = listOf(ContractTag("tag.no.manifest")),
            implements = listOf(ContractTrait("trait.no.manifest")),
            constructors = CONSTRUCTORS,
            functions = FUNCTIONS,
            events = EVENTS
        )

        suppose("contract which needs to be deleted is in repository") {
            contractDecoratorRepository.store(
                ContractDecorator(
                    id = contractWithoutArtifactId,
                    name = "name",
                    description = "description",
                    binary = ContractBinaryData("0x4"),
                    tags = listOf(ContractTag("tag.no.artifact")),
                    implements = listOf(ContractTrait("trait.no.artifact")),
                    constructors = CONSTRUCTORS,
                    functions = FUNCTIONS,
                    events = EVENTS
                )
            )
            contractDecoratorRepository.store(withoutManifestDecorator)
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

        val ignoredContractId = ContractId("AnotherContractSet/IgnoredContract")

        verify("correct contract decorators have been updated in database") {
            assertThat(contractDecoratorRepository.getById(dummyContractId)).withMessage()
                .isEqualTo(
                    ContractDecorator(
                        id = dummyContractId,
                        name = "name",
                        description = "description",
                        binary = ContractBinaryData("0x0"),
                        tags = listOf(ContractTag("tag.example")),
                        implements = listOf(ContractTrait("trait.example")),
                        constructors = CONSTRUCTORS,
                        functions = FUNCTIONS,
                        events = EVENTS
                    )
                )

            assertThat(contractDecoratorRepository.getById(contractWithoutArtifactId)).withMessage()
                .isNull()
            assertThat(contractDecoratorRepository.getById(contractWithoutManifestId)).withMessage()
                .isEqualTo(withoutManifestDecorator)

            assertThat(contractDecoratorRepository.getById(anotherContractId)).withMessage()
                .isNull()
            assertThat(contractDecoratorRepository.getById(ignoredContractId)).withMessage()
                .isNull()
        }

        verify("correct contract metadata exists in the database") {
            assertThat(contractMetadataRepository.exists(dummyContractId, Constants.NIL_UUID)).withMessage()
                .isTrue()
            assertThat(contractMetadataRepository.exists(anotherContractId, Constants.NIL_UUID)).withMessage()
                .isTrue()

            assertThat(contractMetadataRepository.exists(contractWithoutArtifactId, Constants.NIL_UUID)).withMessage()
                .isFalse()
            assertThat(contractMetadataRepository.exists(contractWithoutManifestId, Constants.NIL_UUID)).withMessage()
                .isFalse()
            assertThat(contractMetadataRepository.exists(ignoredContractId, Constants.NIL_UUID)).withMessage()
                .isFalse()
        }
    }

    @Test
    fun mustSkipUnparsableContractDecorators() {
        val contractDecoratorRepository = InMemoryContractDecoratorRepository()

        suppose("initial contract decorators will be loaded from file system") {
            ContractDecoratorFileChangeListener(
                uuidProvider = RandomUuidProvider(),
                contractDecoratorRepository = contractDecoratorRepository,
                contractMetadataRepository = contractMetadataRepository,
                objectMapper = JsonConfig().objectMapper(),
                rootDir = unparsableRootDir,
                ignoredDirs = ignoredDirs
            )
        }

        val unparsableArtifactContractId = ContractId("InvalidJson/UnparsableArtifact")
        val unparsableManifestContractId = ContractId("InvalidJson/UnparsableManifest")
        val missingConstructorSignatureContractId = ContractId("MissingValue/MissingConstructorSignature")
        val missingEventNameContractId = ContractId("MissingValue/MissingEventName")
        val missingFunctionNameContractId = ContractId("MissingValue/MissingFunctionName")
        val missingFunctionOutputsContractId = ContractId("MissingValue/MissingFunctionOutputs")

        verify("no contract decorators have been loaded") {
            assertThat(contractDecoratorRepository.getById(unparsableArtifactContractId)).withMessage()
                .isNull()
            assertThat(contractDecoratorRepository.getById(unparsableManifestContractId)).withMessage()
                .isNull()
            assertThat(contractDecoratorRepository.getById(missingConstructorSignatureContractId)).withMessage()
                .isNull()
            assertThat(contractDecoratorRepository.getById(missingEventNameContractId)).withMessage()
                .isNull()
            assertThat(contractDecoratorRepository.getById(missingFunctionNameContractId)).withMessage()
                .isNull()
            assertThat(contractDecoratorRepository.getById(missingFunctionOutputsContractId)).withMessage()
                .isNull()
        }

        verify("correct contract metadata exists in the database") {
            assertThat(contractMetadataRepository.exists(unparsableArtifactContractId, Constants.NIL_UUID))
                .withMessage()
                .isFalse()
            assertThat(contractMetadataRepository.exists(unparsableManifestContractId, Constants.NIL_UUID))
                .withMessage()
                .isFalse()
            assertThat(contractMetadataRepository.exists(missingConstructorSignatureContractId, Constants.NIL_UUID))
                .withMessage()
                .isFalse()
            assertThat(contractMetadataRepository.exists(missingEventNameContractId, Constants.NIL_UUID))
                .withMessage()
                .isFalse()
            assertThat(contractMetadataRepository.exists(missingFunctionNameContractId, Constants.NIL_UUID))
                .withMessage()
                .isFalse()
            assertThat(contractMetadataRepository.exists(missingFunctionOutputsContractId, Constants.NIL_UUID))
                .withMessage()
                .isFalse()
        }
    }
}
