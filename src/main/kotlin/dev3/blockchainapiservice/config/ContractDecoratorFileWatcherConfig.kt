package dev3.blockchainapiservice.config

import com.fasterxml.jackson.databind.ObjectMapper
import dev3.blockchainapiservice.repository.ContractDecoratorRepository
import dev3.blockchainapiservice.repository.ContractInterfacesRepository
import dev3.blockchainapiservice.repository.ContractMetadataRepository
import dev3.blockchainapiservice.service.UuidProvider
import mu.KLogging
import org.springframework.boot.devtools.filewatch.FileSystemWatcher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ContractDecoratorFileWatcherConfig {

    companion object : KLogging()

    @Bean
    @Suppress("LongParameterList")
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
            addSourceDirectories(listOfNotNull(interfacesDir?.toFile(), contractsDir.toFile()))
            addListener(listener)
            start()
        }
    }
}
