package com.ampnet.blockchainapiservice.config

import com.ampnet.blockchainapiservice.model.json.AbiInputOutput
import com.ampnet.blockchainapiservice.model.json.AbiObject
import com.ampnet.blockchainapiservice.model.json.ArtifactJson
import com.ampnet.blockchainapiservice.model.json.ManifestJson
import com.ampnet.blockchainapiservice.model.json.TypeDecorator
import com.ampnet.blockchainapiservice.model.result.ContractConstructor
import com.ampnet.blockchainapiservice.model.result.ContractDecorator
import com.ampnet.blockchainapiservice.model.result.ContractEvent
import com.ampnet.blockchainapiservice.model.result.ContractFunction
import com.ampnet.blockchainapiservice.model.result.ContractParameter
import com.ampnet.blockchainapiservice.repository.ContractDecoratorRepository
import com.ampnet.blockchainapiservice.repository.ContractMetadataRepository
import com.ampnet.blockchainapiservice.service.UuidProvider
import com.ampnet.blockchainapiservice.util.ContractBinaryData
import com.ampnet.blockchainapiservice.util.ContractId
import com.ampnet.blockchainapiservice.util.ContractTag
import com.ampnet.blockchainapiservice.util.ContractTrait
import com.fasterxml.jackson.databind.DatabindException
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KLogging
import org.springframework.boot.devtools.filewatch.ChangedFiles
import org.springframework.boot.devtools.filewatch.FileChangeListener
import java.io.File
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.reflect.KClass

@Suppress("TooManyFunctions")
class ContractDecoratorFileChangeListener(
    private val uuidProvider: UuidProvider,
    private val contractDecoratorRepository: ContractDecoratorRepository,
    private val contractMetadataRepository: ContractMetadataRepository,
    private val objectMapper: ObjectMapper,
    private val rootDir: Path,
    private val ignoredDirs: List<String>
) : FileChangeListener {

    companion object : KLogging() {
        private class ContractDecoratorException(override val message: String) : RuntimeException() {
            companion object {
                private const val serialVersionUID: Long = -4648452291836117997L
            }
        }
    }

    init {
        rootDir.listDirectoryEntries()
            .filter { it.filterDirs() }
            .forEach { set ->
                logger.info { "Processing contract decorators in ${set.name}..." }
                set.listDirectoryEntries()
                    .filter { decorator -> decorator.filterDirs() }
                    .forEach { decorator -> processContractDecorator(decorator, set.name) }
            }
    }

    @Suppress("MagicNumber")
    override fun onChange(changeSet: Set<ChangedFiles>) {
        logger.info { "Detected contract decorator changes: $changeSet" }

        val changedDirs = changeSet.flatMap {
            it.files.mapNotNull { file ->
                file.relativeName.split('/')
                    .takeIf { elems -> elems.size == 3 }
                    ?.let { elems -> Pair(elems.take(2).joinToString("/"), file.type) }
            }
        }.distinct()

        logger.info { "Detected contract directory changes: $changedDirs" }

        changedDirs.forEach {
            val (setName, decorator) = it.first.split('/', limit = 2)
            val contractDecoratorDir = rootDir.resolve(setName).resolve(decorator)
            processContractDecorator(contractDecoratorDir, setName)
        }
    }

    private fun Path.filterDirs(): Boolean = this.isDirectory() && !ignoredDirs.contains(this.name)

    private fun processContractDecorator(contractDecoratorDir: Path, setName: String) {
        val id = ContractId("$setName/${contractDecoratorDir.name}")
        logger.info { "Processing contract decorator $id..." }

        val artifact = contractDecoratorDir.resolve("artifact.json").toFile()
        val manifest = contractDecoratorDir.resolve("manifest.json").toFile()
        val infoMd = contractDecoratorDir.resolve("info.md").toFile()
        val artifactJson = objectMapper.tryParse(id, artifact, ArtifactJson::class)
        val manifestJson = objectMapper.tryParse(id, manifest, ManifestJson::class)
        val infoMarkdown = infoMd.takeIf { it.isFile }?.readText() ?: ""

        if (artifactJson != null && manifestJson != null) {
            try {
                val decorator = contractDecorator(
                    id = id,
                    artifact = artifactJson,
                    manifest = manifestJson
                )
                contractDecoratorRepository.store(decorator)
                contractDecoratorRepository.store(decorator.id, manifestJson)
                contractDecoratorRepository.store(decorator.id, artifactJson)
                contractDecoratorRepository.store(decorator.id, infoMarkdown)
                contractMetadataRepository.createOrUpdate(
                    id = uuidProvider.getUuid(),
                    contractId = decorator.id,
                    contractTags = decorator.tags,
                    contractImplements = decorator.implements
                )
            } catch (e: ContractDecoratorException) {
                logger.warn(e) { "${e.message} for contract decorator: $id, skipping..." }
                contractDecoratorRepository.delete(id)
            }
        } else {
            contractDecoratorRepository.delete(id)
        }
    }

    private fun <T : Any> ObjectMapper.tryParse(id: ContractId, file: File, valueType: KClass<T>): T? =
        if (file.isFile) {
            try {
                readValue(file, valueType.java)
            } catch (e: DatabindException) {
                logger.warn(e) { "Unable to parse ${file.name} for contract decorator: $id, skipping..." }
                null
            }
        } else {
            logger.warn { "${file.name} is missing for contract decorator: $id, skipping..." }
            null
        }

    private fun contractDecorator(id: ContractId, artifact: ArtifactJson, manifest: ManifestJson) = ContractDecorator(
        id = id,
        binary = ContractBinaryData(artifact.bytecode),
        tags = manifest.tags.map { ContractTag(it) },
        implements = manifest.implements.map { ContractTrait(it) },
        constructors = decorateConstructors(artifact, manifest),
        functions = decorateFunctions(artifact, manifest),
        events = decorateEvents(artifact, manifest)
    )

    private fun decorateConstructors(artifact: ArtifactJson, manifest: ManifestJson): List<ContractConstructor> {
        val constructors = artifact.abi.filter { it.type == "constructor" }
            .associateBy { "constructor(${it.inputs.toTypeList()})" }
        return manifest.constructorDecorators.map {
            val artifactConstructor = constructors.getAbiObjectBySignature(it.signature)

            ContractConstructor(
                inputs = it.parameterDecorators.toContractParameters(artifactConstructor.inputs),
                description = it.description,
                payable = artifactConstructor.stateMutability == "payable"
            )
        }
    }

    private fun decorateFunctions(artifact: ArtifactJson, manifest: ManifestJson): List<ContractFunction> {
        val functions = artifact.abi.filter { it.type == "function" }
            .associateBy { "${it.name}(${it.inputs.toTypeList()})" }
        return manifest.functionDecorators.map {
            val artifactFunction = functions.getAbiObjectBySignature(it.signature)

            ContractFunction(
                name = it.name,
                description = it.description,
                solidityName = artifactFunction.name ?: throw ContractDecoratorException(
                    "Function ${it.signature} is missing function name in artifact.json"
                ),
                inputs = it.parameterDecorators.toContractParameters(artifactFunction.inputs),
                outputs = it.returnDecorators.toContractParameters(
                    artifactFunction.outputs ?: throw ContractDecoratorException(
                        "Function ${it.signature} is missing is missing outputs in artifact.json"
                    )
                ),
                emittableEvents = it.emittableEvents,
                readOnly = artifactFunction.stateMutability == "view" || artifactFunction.stateMutability == "pure"
            )
        }
    }

    private fun decorateEvents(artifact: ArtifactJson, manifest: ManifestJson): List<ContractEvent> {
        val events = artifact.abi.filter { it.type == "event" }
            .associateBy { "${it.name}(${it.inputs.toTypeList()})" }
        return manifest.eventDecorators.map {
            val artifactEvent = events.getAbiObjectBySignature(it.signature)

            ContractEvent(
                name = it.name,
                description = it.description,
                solidityName = artifactEvent.name ?: throw ContractDecoratorException(
                    "Event ${it.signature} is missing event name in artifact.json"
                ),
                inputs = it.parameterDecorators.toContractParameters(artifactEvent.inputs)
            )
        }
    }

    private fun Map<String, AbiObject>.getAbiObjectBySignature(signature: String): AbiObject =
        this[signature] ?: throw ContractDecoratorException("Decorator signature $signature not found in artifact.json")

    private fun List<AbiInputOutput>.toTypeList(): String =
        joinToString(separator = ",") { if (it.type == "tuple") it.buildStructType() else it.type }

    private fun AbiInputOutput.buildStructType(): String = "struct(${components.orEmpty().toTypeList()})"

    private fun List<TypeDecorator>.toContractParameters(abi: List<AbiInputOutput>): List<ContractParameter> =
        zip(abi).map {
            ContractParameter(
                name = it.first.name,
                description = it.first.description,
                solidityName = it.second.name,
                solidityType = it.second.type,
                recommendedTypes = it.first.recommendedTypes
            )
        }
}
