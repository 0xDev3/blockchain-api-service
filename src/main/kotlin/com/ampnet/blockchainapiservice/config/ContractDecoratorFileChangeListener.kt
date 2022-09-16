package com.ampnet.blockchainapiservice.config

import com.ampnet.blockchainapiservice.model.json.ArtifactJson
import com.ampnet.blockchainapiservice.model.json.ManifestJson
import com.ampnet.blockchainapiservice.model.result.ContractDecorator
import com.ampnet.blockchainapiservice.model.result.ContractDecorator.Companion.ContractDecoratorException
import com.ampnet.blockchainapiservice.repository.ContractDecoratorRepository
import com.ampnet.blockchainapiservice.repository.ContractMetadataRepository
import com.ampnet.blockchainapiservice.service.UuidProvider
import com.ampnet.blockchainapiservice.util.ContractId
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

    companion object : KLogging()

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
                val decorator = ContractDecorator(
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
                    name = decorator.name,
                    description = decorator.description,
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
}
