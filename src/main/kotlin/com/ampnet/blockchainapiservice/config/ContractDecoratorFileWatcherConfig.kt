package com.ampnet.blockchainapiservice.config

import com.ampnet.blockchainapiservice.repository.ContractDecoratorRepository
import mu.KLogging
import org.springframework.boot.devtools.filewatch.FileSystemWatcher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ContractDecoratorFileWatcherConfig {

    companion object : KLogging()

    @Bean
    fun setUpContractDecoratorFileWatcher(
        repository: ContractDecoratorRepository,
        properties: ApplicationProperties
    ): FileSystemWatcher? {
        val rootDir = properties.contractDecorators.rootDirectory

        if (rootDir == null) {
            logger.warn { "Contract decorator root directory not set, no contract decorators will be loaded" }
            return null
        }

        logger.info { "Watching for contract decorator changes in $rootDir" }

        val listener = ContractDecoratorFileChangeListener(
            repository = repository,
            rootDir = rootDir,
            ignoredDirs = properties.contractDecorators.ignoredDirs
        )

        return FileSystemWatcher(
            true,
            properties.contractDecorators.fillChangePollInterval,
            properties.contractDecorators.fileChangeQuietInterval
        ).apply {
            addSourceDirectory(rootDir.toFile())
            addListener(listener)
            start()
        }
    }
}
