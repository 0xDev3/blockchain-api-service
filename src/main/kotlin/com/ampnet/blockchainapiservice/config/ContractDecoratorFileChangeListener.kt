package com.ampnet.blockchainapiservice.config

import com.ampnet.blockchainapiservice.model.result.ContractDecorator
import com.ampnet.blockchainapiservice.repository.ContractDecoratorRepository
import com.ampnet.blockchainapiservice.util.ContractBinaryData
import com.ampnet.blockchainapiservice.util.ContractId
import com.ampnet.blockchainapiservice.util.ContractTag
import com.ampnet.blockchainapiservice.util.ContractTrait
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KLogging
import org.springframework.boot.devtools.filewatch.ChangedFiles
import org.springframework.boot.devtools.filewatch.FileChangeListener
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

class ContractDecoratorFileChangeListener(
    val repository: ContractDecoratorRepository,
    val rootDir: Path,
    val ignoredDirs: List<String>
) : FileChangeListener {

    companion object : KLogging()

    private val objectMapper = ObjectMapper()

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

    // TODO add nice error handling here, for now it's assumed repo will have correct directory and JSON structures...
    private fun processContractDecorator(contractDecoratorDir: Path, setName: String) {
        val id = ContractId("$setName/${contractDecoratorDir.name}")
        logger.info { "Processing contract decorator $id..." }

        val artifact = contractDecoratorDir.resolve("artifact.json").toFile()
        val manifest = contractDecoratorDir.resolve("manifest.json").toFile()

        if (artifact.isFile && manifest.isFile) {
            val binary = objectMapper.readTree(artifact)
                .get("bytecode")
                .asText()
                .let { ContractBinaryData(it) }

            val manifestJson = objectMapper.readTree(manifest)
            val tags = manifestJson.get("tags")
                .elements()
                .asSequence()
                .map { ContractTag(it.asText()) }
                .toList()
            val implements = manifestJson.get("implements")
                .elements()
                .asSequence()
                .map { ContractTrait(it.asText()) }
                .toList()

            val decorator = ContractDecorator(
                id = id,
                binary = binary,
                tags = tags,
                implements = implements
            )

            repository.store(decorator)
        } else {
            logger.warn { "Contract decorator $id is missing either artifact.json or manifest.json, skipping..." }
            repository.delete(id)
        }
    }
}
