package com.ampnet.blockchainapiservice.config

import com.ampnet.blockchainapiservice.repository.ContractDecoratorRepository
import com.ampnet.blockchainapiservice.repository.ContractMetadataRepository
import com.ampnet.blockchainapiservice.service.UuidProvider
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KLogging
import org.springframework.boot.devtools.filewatch.FileSystemWatcher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ContractDecoratorFileWatcherConfig {

    companion object : KLogging()

    @Bean
    fun setUpContractDecoratorFileWatcher(
        uuidProvider: UuidProvider,
        contractDecoratorRepository: ContractDecoratorRepository,
        contractMetadataRepository: ContractMetadataRepository,
        objectMapper: ObjectMapper,
        properties: ApplicationProperties,
    ): FileSystemWatcher? {
        val contractsDir = properties.contractDecorators.contractsDirectory

        if (contractsDir == null) {
            logger.warn { "Contract decorator contracts directory not set, no contract decorators will be loaded" }
            return null
        }

        logger.info { "Watching for contract decorator changes in $contractsDir" }

        val listener = ContractDecoratorFileChangeListener(
            uuidProvider = uuidProvider,
            contractDecoratorRepository = contractDecoratorRepository,
            contractMetadataRepository = contractMetadataRepository,
            objectMapper = objectMapper,
            contractsDir = contractsDir,
            ignoredDirs = properties.contractDecorators.ignoredDirs
        )

        return FileSystemWatcher(
            true,
            properties.contractDecorators.fillChangePollInterval,
            properties.contractDecorators.fileChangeQuietInterval
        ).apply {
            addSourceDirectory(contractsDir.toFile())
            addListener(listener)
            start()
        }
    }
}
