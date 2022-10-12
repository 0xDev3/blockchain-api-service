package com.ampnet.blockchainapiservice.config

import com.ampnet.blockchainapiservice.model.json.ArtifactJson
import com.ampnet.blockchainapiservice.model.json.ManifestJson
import com.ampnet.blockchainapiservice.model.result.ContractDecorator
import com.ampnet.blockchainapiservice.model.result.ContractDecorator.Companion.ContractDecoratorException
import com.ampnet.blockchainapiservice.model.result.ContractMetadata
import com.ampnet.blockchainapiservice.repository.ContractDecoratorRepository
import com.ampnet.blockchainapiservice.repository.ContractInterfacesRepository
import com.ampnet.blockchainapiservice.repository.ContractMetadataRepository
import com.ampnet.blockchainapiservice.service.UuidProvider
import com.ampnet.blockchainapiservice.util.Constants
import com.ampnet.blockchainapiservice.util.ContractId
import com.fasterxml.jackson.databind.DatabindException
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KLogging
import org.springframework.boot.devtools.filewatch.ChangedFiles
import org.springframework.boot.devtools.filewatch.FileChangeListener
import java.io.File
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.reflect.KClass

@Suppress("TooManyFunctions")
class ContractDecoratorFileChangeListener(
    private val uuidProvider: UuidProvider,
    private val contractDecoratorRepository: ContractDecoratorRepository,
    private val contractInterfacesRepository: ContractInterfacesRepository,
    private val contractMetadataRepository: ContractMetadataRepository,
    private val objectMapper: ObjectMapper,
    private val contractsDir: Path,
    private val ignoredDirs: List<String>
) : FileChangeListener {

    companion object : KLogging()

    init {
        contractsDir.listDirectoryEntries()
            .filter { it.filterDirs() }
            .forEach { set ->
                logger.info { "Processing contract decorators in ${set.name}..." }
                set.listDirectoryEntries()
                    .filter { entry -> entry.filterDirs() }
                    .forEach { dir -> processNestedDecorators(dir, emptyList(), set.name) }
            }
    }

    @Suppress("MagicNumber")
    override fun onChange(changeSet: Set<ChangedFiles>) {
        logger.info { "Detected contract decorator changes: $changeSet" }

        val changedDirs = changeSet.flatMap {
            it.files.mapNotNull { file ->
                file.relativeName.split('/')
                    .let { elems -> Pair(elems.dropLast(1), file.type) }
            }
        }.distinct()

        logger.info { "Detected contract directory changes: $changedDirs" }

        changedDirs.forEach {
            val setName = it.first.first()
            val parts = it.first.drop(1).dropLast(1)
            val decorator = it.first.last()
            val contractDecoratorDir = contractsDir.resolve(setName).resolve(parts.joinToString("/")).resolve(decorator)
            processContractDecorator(contractDecoratorDir, parts, setName)
        }
    }

    private fun Path.filterDirs(): Boolean = this.isDirectory() && !ignoredDirs.contains(this.name)

    private fun processNestedDecorators(dir: Path, parts: List<String>, setName: String) {
        if (dir.resolve("artifact.json").isRegularFile() || dir.resolve("manifest.json").isRegularFile()) {
            processContractDecorator(dir, parts, setName)
        } else {
            dir.listDirectoryEntries().forEach { processNestedDecorators(it, parts + dir.name, setName) }
        }
    }

    private fun processContractDecorator(contractDecoratorDir: Path, parts: List<String>, setName: String) {
        val nestedParts = parts.joinToString("/")
        val id = ContractId("$setName/$nestedParts/${contractDecoratorDir.name}".replace("//", "/"))
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
                    manifest = manifestJson,
                    interfacesProvider = contractInterfacesRepository::getById
                )

                contractDecoratorRepository.store(decorator)
                contractDecoratorRepository.store(decorator.id, manifestJson)
                contractDecoratorRepository.store(decorator.id, artifactJson)
                contractDecoratorRepository.store(decorator.id, infoMarkdown)
                contractMetadataRepository.createOrUpdate(
                    ContractMetadata(
                        id = uuidProvider.getUuid(),
                        name = decorator.name,
                        description = decorator.description,
                        contractId = decorator.id,
                        contractTags = decorator.tags,
                        contractImplements = decorator.implements,
                        projectId = Constants.NIL_UUID
                    )
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
