package com.ampnet.blockchainapiservice.config

import com.ampnet.blockchainapiservice.repository.ContractDecoratorRepository
import com.ampnet.blockchainapiservice.repository.ContractInterfacesRepository
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
    @Suppress("LongParameterList", "ReturnCount")
    fun setUpContractDecoratorFileWatcher(
        uuidProvider: UuidProvider,
        contractDecoratorRepository: ContractDecoratorRepository,
        contractInterfacesRepository: ContractInterfacesRepository,
        contractMetadataRepository: ContractMetadataRepository,
        objectMapper: ObjectMapper,
        properties: ApplicationProperties,
    ): FileSystemWatcher? {
        val interfacesDir = properties.contractDecorators.interfacesDirectory

        if (interfacesDir == null) {
            logger.warn { "Contract interfaces directory not set, no contract interfaces will be loaded" }
            return null
        }

        logger.info { "Watching for contract interface changes in $interfacesDir" }

        val contractsDir = properties.contractDecorators.contractsDirectory

        if (contractsDir == null) {
            logger.warn { "Contract decorator contracts directory not set, no contract decorators will be loaded" }
            return null
        }

        logger.info { "Watching for contract decorator changes in $contractsDir" }

        val listener = ContractDecoratorFileChangeListener(
            uuidProvider = uuidProvider,
            contractDecoratorRepository = contractDecoratorRepository,
            contractInterfacesRepository = contractInterfacesRepository,
            contractMetadataRepository = contractMetadataRepository,
            objectMapper = objectMapper,
            contractsDir = contractsDir,
            interfacesDir = interfacesDir,
            ignoredDirs = properties.contractDecorators.ignoredDirs
        )

        return FileSystemWatcher(
            true,
            properties.contractDecorators.fillChangePollInterval,
            properties.contractDecorators.fileChangeQuietInterval
        ).apply {
            addSourceDirectories(listOf(interfacesDir.toFile(), contractsDir.toFile()))
            addListener(listener)
            start()
        }
    }
}
