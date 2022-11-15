package dev3.blockchainapiservice.config

import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.model.json.ArtifactJson
import dev3.blockchainapiservice.model.json.InterfaceManifestJson
import dev3.blockchainapiservice.model.json.ManifestJson
import dev3.blockchainapiservice.model.result.ContractConstructor
import dev3.blockchainapiservice.model.result.ContractDecorator
import dev3.blockchainapiservice.model.result.ContractEvent
import dev3.blockchainapiservice.model.result.ContractFunction
import dev3.blockchainapiservice.model.result.ContractParameter
import dev3.blockchainapiservice.repository.ContractInterfacesRepository
import dev3.blockchainapiservice.repository.ContractMetadataRepository
import dev3.blockchainapiservice.repository.InMemoryContractDecoratorRepository
import dev3.blockchainapiservice.repository.InMemoryContractInterfacesRepository
import dev3.blockchainapiservice.repository.JooqContractMetadataRepository
import dev3.blockchainapiservice.service.RandomUuidProvider
import dev3.blockchainapiservice.testcontainers.SharedTestContainers
import dev3.blockchainapiservice.util.Constants
import dev3.blockchainapiservice.util.ContractBinaryData
import dev3.blockchainapiservice.util.ContractId
import dev3.blockchainapiservice.util.ContractTag
import dev3.blockchainapiservice.util.InterfaceId
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.devtools.filewatch.ChangedFile
import org.springframework.boot.devtools.filewatch.ChangedFiles
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.Import
import java.nio.file.Paths
import org.mockito.kotlin.verify as verifyMock

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
                        parameters = null,
                        hints = null
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
                        parameters = null,
                        hints = null
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
                signature = "viewFn(address)",
                inputs = listOf(
                    ContractParameter(
                        name = "Arg",
                        description = "Arg description",
                        solidityName = "arg",
                        solidityType = "address",
                        recommendedTypes = listOf("example"),
                        parameters = null,
                        hints = null
                    )
                ),
                outputs = listOf(
                    ContractParameter(
                        name = "Return value",
                        description = "Return value description",
                        solidityName = "",
                        solidityType = "string",
                        recommendedTypes = listOf("example"),
                        parameters = null,
                        hints = null
                    )
                ),
                emittableEvents = listOf("ExampleEvent(address)"),
                readOnly = true
            ),
            ContractFunction(
                name = "Pure function",
                description = "Pure function description",
                solidityName = "pureFn",
                signature = "pureFn(address)",
                inputs = listOf(
                    ContractParameter(
                        name = "Arg",
                        description = "Arg description",
                        solidityName = "arg",
                        solidityType = "address",
                        recommendedTypes = listOf("example"),
                        parameters = null,
                        hints = null
                    )
                ),
                outputs = listOf(
                    ContractParameter(
                        name = "Return value",
                        description = "Return value description",
                        solidityName = "",
                        solidityType = "string",
                        recommendedTypes = listOf("example"),
                        parameters = null,
                        hints = null
                    )
                ),
                emittableEvents = listOf("ExampleEvent(address)"),
                readOnly = true
            ),
            ContractFunction(
                name = "Modifying function",
                description = "Modifying function description",
                solidityName = "modifyingFn",
                signature = "modifyingFn(address)",
                inputs = listOf(
                    ContractParameter(
                        name = "Arg",
                        description = "Arg description",
                        solidityName = "arg",
                        solidityType = "address",
                        recommendedTypes = listOf("example"),
                        parameters = null,
                        hints = null
                    )
                ),
                outputs = listOf(
                    ContractParameter(
                        name = "Return value",
                        description = "Return value description",
                        solidityName = "",
                        solidityType = "string",
                        recommendedTypes = listOf("example"),
                        parameters = null,
                        hints = null
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
                signature = "ExampleEvent(address)",
                inputs = listOf(
                    ContractParameter(
                        name = "Arg",
                        description = "Arg description",
                        solidityName = "arg",
                        solidityType = "address",
                        recommendedTypes = listOf("example"),
                        parameters = null,
                        hints = null
                    )
                )
            )
        )
        private val EMPTY_CONTRACT_INTERFACE = InterfaceManifestJson(null, null, emptySet(), emptyList(), emptyList())
    }

    private val interfacesDir = Paths.get(javaClass.classLoader.getResource("dummyInterfaces")!!.path)
    private val parsableContractsDir = Paths.get(javaClass.classLoader.getResource("dummyContracts")!!.path)
    private val unparsableContractsDir = Paths.get(javaClass.classLoader.getResource("unparsableContracts")!!.path)
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
    fun mustCorrectlyLoadInitialContractDecoratorsAndInterfaces() {
        val contractDecoratorRepository = InMemoryContractDecoratorRepository()
        val contractInterfacesRepository = mock<ContractInterfacesRepository>()

        suppose("mock contract interfaces will be returned") {
            given(contractInterfacesRepository.getById(anyValueClass(InterfaceId(""))))
                .willReturn(
                    InterfaceManifestJson(
                        name = null,
                        description = null,
                        tags = emptySet(),
                        eventDecorators = emptyList(),
                        functionDecorators = emptyList()
                    )
                )
        }

        suppose("initial contract decorators and interfaces will be loaded from file system") {
            ContractDecoratorFileChangeListener(
                uuidProvider = RandomUuidProvider(),
                contractDecoratorRepository = contractDecoratorRepository,
                contractInterfacesRepository = contractInterfacesRepository,
                contractMetadataRepository = contractMetadataRepository,
                objectMapper = JsonConfig().objectMapper(),
                contractsDir = parsableContractsDir,
                interfacesDir = interfacesDir,
                ignoredDirs = ignoredDirs
            )
        }

        val exampleInterfaceId = InterfaceId("example")
        val nestedInterfaceId = InterfaceId("nested")
        val otherNestedInterfaceId = InterfaceId("nested/other")

        verify("correct contract interfaces have been loaded") {
            verifyMock(contractInterfacesRepository)
                .store(exampleInterfaceId, EMPTY_CONTRACT_INTERFACE)
            verifyMock(contractInterfacesRepository)
                .store(exampleInterfaceId, "")

            verifyMock(contractInterfacesRepository)
                .store(nestedInterfaceId, EMPTY_CONTRACT_INTERFACE)
            verifyMock(contractInterfacesRepository)
                .store(nestedInterfaceId, "")

            verifyMock(contractInterfacesRepository)
                .store(otherNestedInterfaceId, EMPTY_CONTRACT_INTERFACE)
            verifyMock(contractInterfacesRepository)
                .store(otherNestedInterfaceId, "")
        }

        val dummyContractId = ContractId("DummyContractSet/DummyContract")
        val deeplyNestedContractId = ContractId("DummyContractSet/Deeply/Nested/Contract")
        val anotherContractId = ContractId("AnotherContractSet/AnotherContract")
        val contractWithoutArtifactId = ContractId("DummyContractSet/ContractWithoutArtifact")
        val contractWithoutManifestId = ContractId("DummyContractSet/ContractWithoutManifest")
        val ignoredContractId = ContractId("AnotherContractSet/IgnoredContract")

        verify("correct contract decorators have been loaded") {
            val dummyDecorator = contractDecoratorRepository.getById(dummyContractId)!!

            assertThat(dummyDecorator).withMessage()
                .isEqualTo(
                    ContractDecorator(
                        id = dummyContractId,
                        name = "name",
                        description = "description",
                        binary = ContractBinaryData("0x0"),
                        tags = listOf(ContractTag("tag.example")),
                        implements = listOf(InterfaceId("trait.example")),
                        constructors = CONSTRUCTORS,
                        functions = FUNCTIONS,
                        events = EVENTS,
                        manifest = dummyDecorator.manifest,
                        artifact = dummyDecorator.artifact
                    )
                )

            val deeplyNestedDecorator = contractDecoratorRepository.getById(deeplyNestedContractId)!!

            assertThat(deeplyNestedDecorator).withMessage()
                .isEqualTo(
                    ContractDecorator(
                        id = deeplyNestedContractId,
                        name = "name",
                        description = "description",
                        binary = ContractBinaryData("0x0123456"),
                        tags = listOf(ContractTag("tag.example")),
                        implements = listOf(InterfaceId("trait.example")),
                        constructors = CONSTRUCTORS,
                        functions = FUNCTIONS,
                        events = EVENTS,
                        manifest = deeplyNestedDecorator.manifest,
                        artifact = deeplyNestedDecorator.artifact
                    )
                )

            assertThat(contractDecoratorRepository.getById(contractWithoutArtifactId)).withMessage()
                .isNull()
            assertThat(contractDecoratorRepository.getById(contractWithoutManifestId)).withMessage()
                .isNull()

            val anotherDecorator = contractDecoratorRepository.getById(anotherContractId)!!

            assertThat(anotherDecorator).withMessage()
                .isEqualTo(
                    ContractDecorator(
                        id = anotherContractId,
                        name = "name",
                        description = "description",
                        binary = ContractBinaryData("0x2"),
                        tags = listOf(ContractTag("tag.another")),
                        implements = listOf(InterfaceId("trait.another")),
                        constructors = CONSTRUCTORS,
                        functions = FUNCTIONS,
                        events = EVENTS,
                        manifest = anotherDecorator.manifest,
                        artifact = anotherDecorator.artifact
                    )
                )

            assertThat(contractDecoratorRepository.getById(ignoredContractId)).withMessage()
                .isNull()
        }

        verify("correct contract metadata exists in the database") {
            assertThat(contractMetadataRepository.exists(dummyContractId, Constants.NIL_UUID)).withMessage()
                .isTrue()
            assertThat(contractMetadataRepository.exists(deeplyNestedContractId, Constants.NIL_UUID)).withMessage()
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
    fun mustCorrectlyReloadContractsAndInterfacesAfterSomeFileChangesHaveBeenDetected() {
        val contractDecoratorRepository = InMemoryContractDecoratorRepository()
        val contractInterfacesRepository = mock<ContractInterfacesRepository>()

        suppose("mock contract interfaces will be returned") {
            given(contractInterfacesRepository.getById(anyValueClass(InterfaceId(""))))
                .willReturn(
                    InterfaceManifestJson(
                        name = null,
                        description = null,
                        tags = emptySet(),
                        eventDecorators = emptyList(),
                        functionDecorators = emptyList()
                    )
                )
        }

        val listener = suppose("initial contract decorators will be loaded from file system") {
            ContractDecoratorFileChangeListener(
                uuidProvider = RandomUuidProvider(),
                contractDecoratorRepository = contractDecoratorRepository,
                contractInterfacesRepository = contractInterfacesRepository,
                contractMetadataRepository = contractMetadataRepository,
                objectMapper = JsonConfig().objectMapper(),
                contractsDir = parsableContractsDir,
                interfacesDir = interfacesDir,
                ignoredDirs = ignoredDirs
            )
        }

        val dummyContractId = ContractId("DummyContractSet/DummyContract")
        val deeplyNestedContractId = ContractId("DummyContractSet/Deeply/Nested/Contract")
        val anotherContractId = ContractId("AnotherContractSet/AnotherContract")

        suppose("existing contracts will be removed from repository") {
            contractDecoratorRepository.delete(dummyContractId)
            contractDecoratorRepository.delete(deeplyNestedContractId)
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
            implements = listOf(InterfaceId("trait.no.manifest")),
            constructors = CONSTRUCTORS,
            functions = FUNCTIONS,
            events = EVENTS,
            manifest = ManifestJson.EMPTY,
            artifact = ArtifactJson.EMPTY
        )

        suppose("contract which needs to be deleted is in repository") {
            contractDecoratorRepository.store(
                ContractDecorator(
                    id = contractWithoutArtifactId,
                    name = "name",
                    description = "description",
                    binary = ContractBinaryData("0x4"),
                    tags = listOf(ContractTag("tag.no.artifact")),
                    implements = listOf(InterfaceId("trait.no.artifact")),
                    constructors = CONSTRUCTORS,
                    functions = FUNCTIONS,
                    events = EVENTS,
                    manifest = ManifestJson.EMPTY,
                    artifact = ArtifactJson.EMPTY
                )
            )
            contractDecoratorRepository.store(withoutManifestDecorator)
        }

        suppose("listener will get some file changes") {
            listener.onChange(
                setOf(
                    ChangedFiles(
                        parsableContractsDir.toFile(),
                        setOf(
                            ChangedFile(
                                parsableContractsDir.toFile(),
                                parsableContractsDir.resolve("DummyContractSet/DummyContract/artifact.json").toFile(),
                                ChangedFile.Type.ADD
                            ),
                            ChangedFile(
                                parsableContractsDir.toFile(),
                                parsableContractsDir.resolve("DummyContractSet/Deeply/Nested/Contract/artifact.json")
                                    .toFile(),
                                ChangedFile.Type.ADD
                            ),
                            ChangedFile(
                                parsableContractsDir.toFile(),
                                parsableContractsDir.resolve("DummyContractSet/ContractWithoutArtifact/artifact.json")
                                    .toFile(),
                                ChangedFile.Type.DELETE
                            )
                        )
                    ),
                    ChangedFiles(
                        interfacesDir.toFile(),
                        setOf(
                            ChangedFile(
                                interfacesDir.toFile(),
                                interfacesDir.resolve("NonExistentInterface/manifest.json").toFile(),
                                ChangedFile.Type.DELETE
                            ),
                            ChangedFile(
                                interfacesDir.toFile(),
                                interfacesDir.resolve("AnotherNonExistentInterface/info.md").toFile(),
                                ChangedFile.Type.DELETE
                            ),
                            ChangedFile(
                                interfacesDir.toFile(),
                                interfacesDir.resolve("AnotherNonExistentInterface/example.info.md").toFile(),
                                ChangedFile.Type.DELETE
                            )
                        )
                    )
                )
            )
        }

        verify("correct contract have been updated in database") {
            verifyMock(contractInterfacesRepository)
                .delete(InterfaceId("NonExistentInterface"))

            verifyMock(contractInterfacesRepository)
                .delete(InterfaceId("AnotherNonExistentInterface"))

            verifyMock(contractInterfacesRepository)
                .delete(InterfaceId("AnotherNonExistentInterface/example"))
        }

        val ignoredContractId = ContractId("AnotherContractSet/IgnoredContract")

        verify("correct contract decorators have been updated in database") {
            val dummyDecorator = contractDecoratorRepository.getById(dummyContractId)!!

            assertThat(dummyDecorator).withMessage()
                .isEqualTo(
                    ContractDecorator(
                        id = dummyContractId,
                        name = "name",
                        description = "description",
                        binary = ContractBinaryData("0x0"),
                        tags = listOf(ContractTag("tag.example")),
                        implements = listOf(InterfaceId("trait.example")),
                        constructors = CONSTRUCTORS,
                        functions = FUNCTIONS,
                        events = EVENTS,
                        manifest = dummyDecorator.manifest,
                        artifact = dummyDecorator.artifact
                    )
                )

            val deeplyNestedDecorator = contractDecoratorRepository.getById(deeplyNestedContractId)!!

            assertThat(deeplyNestedDecorator).withMessage()
                .isEqualTo(
                    ContractDecorator(
                        id = deeplyNestedContractId,
                        name = "name",
                        description = "description",
                        binary = ContractBinaryData("0x0123456"),
                        tags = listOf(ContractTag("tag.example")),
                        implements = listOf(InterfaceId("trait.example")),
                        constructors = CONSTRUCTORS,
                        functions = FUNCTIONS,
                        events = EVENTS,
                        manifest = deeplyNestedDecorator.manifest,
                        artifact = deeplyNestedDecorator.artifact
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
            assertThat(contractMetadataRepository.exists(deeplyNestedContractId, Constants.NIL_UUID)).withMessage()
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
        val contractInterfacesRepository = InMemoryContractInterfacesRepository()

        suppose("initial contract decorators will be loaded from file system") {
            ContractDecoratorFileChangeListener(
                uuidProvider = RandomUuidProvider(),
                contractDecoratorRepository = contractDecoratorRepository,
                contractInterfacesRepository = contractInterfacesRepository,
                contractMetadataRepository = contractMetadataRepository,
                objectMapper = JsonConfig().objectMapper(),
                contractsDir = unparsableContractsDir,
                interfacesDir = interfacesDir,
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
