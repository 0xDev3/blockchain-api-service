package dev3.blockchainapiservice.service

import com.fasterxml.jackson.databind.ObjectMapper
import dev3.blockchainapiservice.blockchain.BlockchainService
import dev3.blockchainapiservice.blockchain.properties.ChainSpec
import dev3.blockchainapiservice.exception.ContractDecoratorBinaryMismatchException
import dev3.blockchainapiservice.exception.ContractNotFoundException
import dev3.blockchainapiservice.exception.ResourceNotFoundException
import dev3.blockchainapiservice.features.contract.abi.service.AbiProviderService
import dev3.blockchainapiservice.model.DeserializableEvent
import dev3.blockchainapiservice.model.json.ArtifactJson
import dev3.blockchainapiservice.model.json.DecompiledContractJson
import dev3.blockchainapiservice.model.json.ManifestJson
import dev3.blockchainapiservice.model.params.ExecuteReadonlyFunctionCallParams
import dev3.blockchainapiservice.model.params.ImportContractParams
import dev3.blockchainapiservice.model.params.OutputParameter
import dev3.blockchainapiservice.model.params.StoreContractDeploymentRequestParams
import dev3.blockchainapiservice.model.result.ContractBinaryInfo
import dev3.blockchainapiservice.model.result.ContractDecorator
import dev3.blockchainapiservice.model.result.ContractDeploymentRequest
import dev3.blockchainapiservice.model.result.ContractDeploymentTransactionInfo
import dev3.blockchainapiservice.model.result.ContractMetadata
import dev3.blockchainapiservice.model.result.ContractParameter
import dev3.blockchainapiservice.model.result.FullContractDeploymentTransactionInfo
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.repository.ContractDecoratorRepository
import dev3.blockchainapiservice.repository.ContractDeploymentRequestRepository
import dev3.blockchainapiservice.repository.ContractMetadataRepository
import dev3.blockchainapiservice.repository.ImportedContractDecoratorRepository
import dev3.blockchainapiservice.util.AddressType
import dev3.blockchainapiservice.util.Constants
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.ContractBinaryData
import dev3.blockchainapiservice.util.ContractId
import dev3.blockchainapiservice.util.EthStorageSlot
import dev3.blockchainapiservice.util.FunctionData
import dev3.blockchainapiservice.util.Tuple
import dev3.blockchainapiservice.util.ZeroAddress
import mu.KLogging
import org.springframework.stereotype.Service
import java.util.UUID
import kotlin.math.min

@Service
@Suppress("LongParameterList", "TooManyFunctions")
class ContractImportServiceImpl(
    private val abiDecoderService: AbiDecoderService,
    private val contractDecompilerService: ContractDecompilerService,
    private val abiProviderService: AbiProviderService,
    private val functionEncoderService: FunctionEncoderService,
    private val contractDeploymentRequestRepository: ContractDeploymentRequestRepository,
    private val contractMetadataRepository: ContractMetadataRepository,
    private val contractDecoratorRepository: ContractDecoratorRepository,
    private val importedContractDecoratorRepository: ImportedContractDecoratorRepository,
    private val blockchainService: BlockchainService,
    private val uuidProvider: UuidProvider,
    private val utcDateTimeProvider: UtcDateTimeProvider,
    private val objectMapper: ObjectMapper
) : ContractImportService {

    companion object : KLogging() {
        private const val ETH_VALUE_LENGTH = 64
        private const val PROXY_FUNCTION_NAME = "implementation"

        private val PROXY_IMPLEMENTATION_SLOTS =
            listOf(
                // slot according to standard: https://eips.ethereum.org/EIPS/eip-1967
                EthStorageSlot("0x360894a13ba1a3210667c828492db98dca3e2076cc3735a920a3ca505d382bbc"),
                // slot used by older OpenZeppelin proxies (keccak256("org.zeppelinos.proxy.implementation"))
                EthStorageSlot("0x7050c9e0f4ca769c69bd3a8ef740bc37934f8e2c036e5a723fd8ee048ed3f8c3")
            )
        private val PROXY_BEACON_SLOTS = listOf(
            // slot according to standard: https://eips.ethereum.org/EIPS/eip-1967
            EthStorageSlot("0xa3f0ad74e5423aebfd80d3ef4346578335a9a72aeaee59ff6cb3582b35133d50")
        )

        private data class DecompiledContract(
            val contractId: ContractId,
            val decorator: ContractDecorator,
            val constructorParams: String,
            val contractDeploymentTransactionInfo: ContractDeploymentTransactionInfo,
            val implementationAddress: ContractAddress?
        )

        private data class OutputParams(val params: List<OutputParameter>)
        data class TypeAndValue(val type: String, val value: Any)
    }

    override fun importExistingContract(params: ImportContractParams, project: Project): UUID? {
        logger.info { "Attempting to import existing smart contract, params: $params, project: $project" }

        return contractDeploymentRequestRepository.getByContractAddressAndChainId(
            contractAddress = params.contractAddress,
            chainId = project.chainId
        )?.let {
            logger.info { "Already existing contract found, params: $params, project: $project" }

            if (it.imported) {
                copyImportedContract(params, project, it)
            } else {
                copyDeployedContract(params, project, it)
            }
        }
    }

    override fun importContract(
        params: ImportContractParams,
        project: Project
    ): UUID {
        logger.info { "Importing smart contract, params: $params, project: $project" }

        return if (params.contractId != null) {
            importContractWithExistingDecorator(params, project, params.contractId)
        } else {
            val chainSpec = ChainSpec(
                chainId = project.chainId,
                customRpcUrl = project.customRpcUrl
            )
            val decompiledContract = decompileContract(
                importContractAddress = params.contractAddress,
                chainSpec = chainSpec,
                projectId = project.id,
                previewDecorator = false
            )

            storeImportedContract(decompiledContract, params, project)
        }
    }

    override fun previewImport(contractAddress: ContractAddress, chainSpec: ChainSpec): ContractDecorator {
        logger.info { "Preview for contract import, contractAddress: $contractAddress, chainSpec: $chainSpec" }

        val decorator = contractDeploymentRequestRepository.getByContractAddressAndChainId(
            contractAddress = contractAddress,
            chainId = chainSpec.chainId
        )?.let {
            logger.info { "Already existing contract found, contractAddress: $contractAddress, chainSpec: $chainSpec" }

            if (it.imported) {
                importedContractDecoratorRepository.getByContractIdAndProjectId(it.contractId, it.projectId)
            } else {
                contractDecoratorRepository.getById(it.contractId)
            }
        }

        return decorator ?: decompileContract(
            importContractAddress = contractAddress,
            chainSpec = chainSpec,
            projectId = Constants.NIL_UUID,
            previewDecorator = true
        ).decorator
    }

    private fun copyDeployedContract(
        params: ImportContractParams,
        project: Project,
        request: ContractDeploymentRequest
    ): UUID {
        val id = uuidProvider.getUuid()
        val storedRequest = contractDeploymentRequestRepository.store(
            params = StoreContractDeploymentRequestParams.fromContractDeploymentRequest(
                id = id,
                importContractParams = params,
                contractDeploymentRequest = request,
                project = project,
                createdAt = utcDateTimeProvider.getUtcDateTime(),
                imported = false
            ),
            metadataProjectId = Constants.NIL_UUID
        )

        if (request.txHash != null && request.deployerAddress != null) {
            contractDeploymentRequestRepository.setTxInfo(id, request.txHash, request.deployerAddress)
        }

        contractDeploymentRequestRepository.setContractAddress(id, params.contractAddress)

        return storedRequest.id
    }

    private fun copyImportedContract(
        params: ImportContractParams,
        project: Project,
        request: ContractDeploymentRequest
    ): UUID {
        val existingManifestJson = importedContractDecoratorRepository.getManifestJsonByContractIdAndProjectId(
            contractId = request.contractId,
            projectId = request.projectId
        )!!
        val existingArtifactJson = importedContractDecoratorRepository.getArtifactJsonByContractIdAndProjectId(
            contractId = request.contractId,
            projectId = request.projectId
        )!!
        val existingInfoMarkdown = importedContractDecoratorRepository.getInfoMarkdownByContractIdAndProjectId(
            contractId = request.contractId,
            projectId = request.projectId
        )!!

        val newContractId = ContractId("imported-${params.contractAddress.rawValue}-${project.chainId.value}")
        val newDecorator = importedContractDecoratorRepository.getByContractIdAndProjectId(
            contractId = newContractId,
            projectId = project.id
        ) ?: importedContractDecoratorRepository.store(
            id = uuidProvider.getUuid(),
            projectId = project.id,
            contractId = newContractId,
            manifestJson = existingManifestJson,
            artifactJson = existingArtifactJson,
            infoMarkdown = existingInfoMarkdown,
            importedAt = utcDateTimeProvider.getUtcDateTime(),
            previewOnly = false
        )

        contractMetadataRepository.createOrUpdate(
            ContractMetadata(
                id = uuidProvider.getUuid(),
                name = newDecorator.name,
                description = newDecorator.description,
                contractId = newContractId,
                contractTags = newDecorator.tags,
                contractImplements = newDecorator.implements,
                projectId = project.id
            )
        )

        val id = uuidProvider.getUuid()
        val storedRequest = contractDeploymentRequestRepository.store(
            params = StoreContractDeploymentRequestParams.fromContractDeploymentRequest(
                id = id,
                importContractParams = params,
                contractDeploymentRequest = request.copy(contractId = newContractId),
                project = project,
                createdAt = utcDateTimeProvider.getUtcDateTime(),
                imported = true
            ),
            metadataProjectId = project.id
        )

        if (request.txHash != null && request.deployerAddress != null) {
            contractDeploymentRequestRepository.setTxInfo(id, request.txHash, request.deployerAddress)
        }

        contractDeploymentRequestRepository.setContractAddress(id, params.contractAddress)

        return storedRequest.id
    }

    @Suppress("ThrowsCount")
    private fun importContractWithExistingDecorator(
        params: ImportContractParams,
        project: Project,
        contractId: ContractId
    ): UUID {
        val decoratorNotFoundMessage = "Contract decorator not found for contract ID: ${contractId.value}"

        val contractDecorator = contractDecoratorRepository.getById(contractId)
            ?: throw ResourceNotFoundException(decoratorNotFoundMessage)

        if (!contractMetadataRepository.exists(contractId, Constants.NIL_UUID)) {
            throw ResourceNotFoundException(decoratorNotFoundMessage)
        }

        val chainSpec = ChainSpec(
            chainId = project.chainId,
            customRpcUrl = project.customRpcUrl
        )
        val contractDeploymentTransactionInfo = findContractDeploymentTransaction(
            contractAddress = params.contractAddress,
            chainSpec = chainSpec,
            events = contractDecorator.getDeserializableEvents(objectMapper)
        )

        val decoratorBinary = contractDecorator.binary.withPrefix
        val deployedBinary = when (contractDeploymentTransactionInfo) {
            is FullContractDeploymentTransactionInfo -> contractDeploymentTransactionInfo.data.value
            is ContractBinaryInfo -> contractDeploymentTransactionInfo.binary.withPrefix
        }

        if (deployedBinary.startsWith(decoratorBinary).not()) {
            throw ContractDecoratorBinaryMismatchException(params.contractAddress, contractId)
        }

        val constructorParams = deployedBinary.removePrefix(decoratorBinary)
        val constructorInputs = contractDecorator.constructors.firstOrNull()?.inputs.orEmpty()

        return storeContractDeploymentRequest(
            constructorParams = constructorParams,
            constructorInputs = constructorInputs,
            params = params,
            contractId = contractId,
            contractDeploymentTransactionInfo = contractDeploymentTransactionInfo,
            project = project,
            metadataProjectId = Constants.NIL_UUID,
            proxy = false,
            implementationContractAddress = null
        )
    }

    private fun decompileContract(
        importContractAddress: ContractAddress,
        chainSpec: ChainSpec,
        projectId: UUID,
        previewDecorator: Boolean
    ): DecompiledContract {
        val contractDeploymentTransactionInfo = findContractDeploymentTransaction(
            contractAddress = importContractAddress,
            chainSpec = chainSpec,
            events = emptyList()
        )

        val (fullBinary, shortBinary) = when (contractDeploymentTransactionInfo) {
            is FullContractDeploymentTransactionInfo ->
                Pair(contractDeploymentTransactionInfo.data.value, contractDeploymentTransactionInfo.binary.value)

            is ContractBinaryInfo ->
                Pair(contractDeploymentTransactionInfo.binary.value, contractDeploymentTransactionInfo.binary.value)
        }
        val constructorParamsStart = min(fullBinary.indexOf(shortBinary) + shortBinary.length, fullBinary.length)

        val constructorParams = fullBinary.substring(constructorParamsStart)

        val (decompiledContract, implementationAddress) = getOrDecompileAbi(
            bytecode = fullBinary,
            deployedBytecode = ContractBinaryData(shortBinary),
            contractAddress = importContractAddress,
            chainSpec = chainSpec
        ).let {
            it.copy(
                artifact = it.artifact.copy(
                    bytecode = FunctionData(fullBinary).withoutPrefix.removeSuffix(constructorParams),
                    deployedBytecode = shortBinary
                )
            ).resolveProxyContract(importContractAddress, chainSpec)
        }
        val contractAddress = contractDeploymentTransactionInfo.deployedContractAddress
        val contractId = ContractId("imported-${contractAddress.rawValue}-${chainSpec.chainId.value}")

        val contractDecorator =
            importedContractDecoratorRepository.getByContractIdAndProjectId(contractId, projectId)
                ?: importedContractDecoratorRepository.store(
                    id = uuidProvider.getUuid(),
                    projectId = projectId,
                    contractId = contractId,
                    manifestJson = decompiledContract.manifest,
                    artifactJson = decompiledContract.artifact,
                    infoMarkdown = decompiledContract.infoMarkdown ?: "",
                    importedAt = utcDateTimeProvider.getUtcDateTime(),
                    previewOnly = previewDecorator
                )

        return DecompiledContract(
            contractId = contractId,
            decorator = contractDecorator,
            constructorParams = constructorParams,
            contractDeploymentTransactionInfo = contractDeploymentTransactionInfo,
            implementationAddress = implementationAddress
        )
    }

    private fun storeImportedContract(
        decompiledContract: DecompiledContract,
        params: ImportContractParams,
        project: Project
    ): UUID {
        contractMetadataRepository.createOrUpdate(
            ContractMetadata(
                id = uuidProvider.getUuid(),
                name = decompiledContract.decorator.name,
                description = decompiledContract.decorator.description,
                contractId = decompiledContract.contractId,
                contractTags = decompiledContract.decorator.tags,
                contractImplements = decompiledContract.decorator.implements,
                projectId = project.id
            )
        )

        val constructorInputs = List(decompiledContract.constructorParams.length / ETH_VALUE_LENGTH) {
            ContractParameter(
                name = "",
                description = "",
                solidityName = "",
                solidityType = "bytes32",
                recommendedTypes = emptyList(),
                parameters = null,
                hints = null
            )
        }

        return storeContractDeploymentRequest(
            constructorParams = decompiledContract.constructorParams,
            constructorInputs = constructorInputs,
            params = params,
            contractId = decompiledContract.contractId,
            contractDeploymentTransactionInfo = decompiledContract.contractDeploymentTransactionInfo,
            project = project,
            metadataProjectId = project.id,
            proxy = decompiledContract.implementationAddress != null,
            implementationContractAddress = decompiledContract.implementationAddress
        )
    }

    private fun findContractDeploymentTransaction(
        contractAddress: ContractAddress,
        chainSpec: ChainSpec,
        events: List<DeserializableEvent>
    ) = blockchainService.findContractDeploymentTransaction(
        chainSpec = chainSpec,
        contractAddress = contractAddress,
        events = events
    ) ?: throw ContractNotFoundException(contractAddress)

    private fun DecompiledContractJson.resolveProxyContract(
        importContractAddress: ContractAddress,
        chainSpec: ChainSpec
    ): Pair<DecompiledContractJson, ContractAddress?> =
        if (this.manifest.functionDecorators.any { it.signature == "$PROXY_FUNCTION_NAME()" }) {
            val implementationAddress = findContractProxyImplementation(importContractAddress, chainSpec)
            val implementationTransactionInfo = findContractDeploymentTransaction(
                contractAddress = implementationAddress,
                chainSpec = chainSpec,
                events = emptyList()
            )
            val decompiledImplementation = getOrDecompileAbi(
                bytecode = implementationTransactionInfo.binary.value,
                deployedBytecode = implementationTransactionInfo.binary,
                contractAddress = implementationAddress,
                chainSpec = chainSpec
            )

            val implManifest = decompiledImplementation.manifest
            val eventDecorators = this.manifest.eventDecorators + implManifest.eventDecorators
            val constructorDecorators = this.manifest.constructorDecorators + implManifest.constructorDecorators
            val functionDecorators = this.manifest.functionDecorators + implManifest.functionDecorators

            val mergedJson = DecompiledContractJson(
                manifest = ManifestJson(
                    name = this.manifest.name,
                    description = this.manifest.description,
                    tags = this.manifest.tags,
                    implements = this.manifest.implements,
                    eventDecorators = eventDecorators.distinct(),
                    constructorDecorators = constructorDecorators.distinct(),
                    functionDecorators = functionDecorators.distinct()
                ),
                artifact = ArtifactJson(
                    contractName = this.artifact.contractName,
                    sourceName = this.artifact.sourceName,
                    abi = (this.artifact.abi + decompiledImplementation.artifact.abi).distinct(),
                    bytecode = this.artifact.bytecode,
                    deployedBytecode = this.artifact.deployedBytecode,
                    linkReferences = this.artifact.linkReferences,
                    deployedLinkReferences = this.artifact.deployedLinkReferences
                ),
                infoMarkdown = this.infoMarkdown
            )

            Pair(mergedJson, implementationAddress)
        } else Pair(this, null)

    private fun findContractProxyImplementation(
        contractAddress: ContractAddress,
        chainSpec: ChainSpec
    ): ContractAddress {
        val implementationValue = PROXY_IMPLEMENTATION_SLOTS.readSlots(chainSpec, contractAddress)
        val beaconValue = implementationValue ?: PROXY_BEACON_SLOTS.readSlots(chainSpec, contractAddress)

        return implementationValue
            ?: beaconValue?.readProxyImplementationFunction(chainSpec)
            ?: contractAddress.readProxyImplementationFunction(chainSpec)
    }

    private fun List<EthStorageSlot>.readSlots(
        chainSpec: ChainSpec,
        contractAddress: ContractAddress
    ): ContractAddress? = asSequence()
        .map {
            blockchainService.readStorageSlot(
                chainSpec = chainSpec,
                contractAddress = contractAddress,
                slot = it
            ).let(::ContractAddress)
        }
        .filter { it != ZeroAddress.toContractAddress() }
        .firstOrNull()

    private fun ContractAddress.readProxyImplementationFunction(chainSpec: ChainSpec): ContractAddress {
        val data = functionEncoderService.encode(
            functionName = PROXY_FUNCTION_NAME,
            arguments = emptyList()
        )

        val implementation = blockchainService.callReadonlyFunction(
            chainSpec = chainSpec,
            params = ExecuteReadonlyFunctionCallParams(
                contractAddress = this,
                callerAddress = ZeroAddress.toWalletAddress(),
                functionName = PROXY_FUNCTION_NAME,
                functionData = data,
                outputParams = listOf(OutputParameter(AddressType))
            )
        )

        return ContractAddress(implementation.returnValues[0].toString())
    }

    @Suppress("LongParameterList")
    private fun storeContractDeploymentRequest(
        constructorParams: String,
        constructorInputs: List<ContractParameter>,
        params: ImportContractParams,
        contractId: ContractId,
        contractDeploymentTransactionInfo: ContractDeploymentTransactionInfo,
        project: Project,
        metadataProjectId: UUID,
        proxy: Boolean,
        implementationContractAddress: ContractAddress?
    ): UUID {
        val constructorInputTypes = constructorInputs
            .joinToString(separator = ",") { it.toSolidityTypeJson() }
            .let { objectMapper.readValue("{\"params\":[$it]}", OutputParams::class.java) }
            .params

        val decodedConstructorParams = abiDecoderService.decode(
            types = constructorInputTypes.map { it.deserializedType },
            encodedInput = constructorParams
        )

        val id = uuidProvider.getUuid()
        val storeParams = StoreContractDeploymentRequestParams.fromImportedContract(
            id = id,
            params = params,
            contractId = contractId,
            contractDeploymentTransactionInfo = contractDeploymentTransactionInfo,
            constructorParams = objectMapper.valueToTree(inputArgs(constructorInputs, decodedConstructorParams)),
            project = project,
            createdAt = utcDateTimeProvider.getUtcDateTime(),
            proxy = proxy,
            implementationContractAddress = implementationContractAddress
        )

        contractDeploymentRequestRepository.store(storeParams, metadataProjectId)
        contractDeploymentRequestRepository.setContractAddress(
            id = id,
            contractAddress = contractDeploymentTransactionInfo.deployedContractAddress
        )

        if (contractDeploymentTransactionInfo is FullContractDeploymentTransactionInfo) {
            contractDeploymentRequestRepository.setTxInfo(
                id = id,
                txHash = contractDeploymentTransactionInfo.hash,
                deployer = contractDeploymentTransactionInfo.from
            )
        }

        return id
    }

    private fun ContractParameter.toSolidityTypeJson(): String =
        if (solidityType.startsWith("tuple")) {
            val elems = parameters.orEmpty().joinToString(separator = ",") { it.toSolidityTypeJson() }
            "{\"type\":$solidityType,\"elems\":[$elems]}"
        } else {
            "\"$solidityType\""
        }

    internal fun inputArgs(inputTypes: List<ContractParameter>, decodedValues: List<Any>): List<TypeAndValue> =
        inputTypes.zip(decodedValues).map {
            TypeAndValue(
                type = it.first.solidityType,
                value = it.second.deepMap { tuple ->
                    inputArgs(it.first.parameters.orEmpty(), tuple.elems)
                }
            )
        }

    private fun Any.deepMap(mapFn: (Tuple) -> List<Any>): Any =
        when (this) {
            is List<*> -> this.map { it!!.deepMap(mapFn) }
            is Tuple -> mapFn(this)
            else -> this
        }

    private fun getOrDecompileAbi(
        bytecode: String,
        deployedBytecode: ContractBinaryData,
        contractAddress: ContractAddress,
        chainSpec: ChainSpec
    ): DecompiledContractJson =
        abiProviderService.getContractAbi(
            bytecode = bytecode,
            deployedBytecode = deployedBytecode.value,
            contractAddress = contractAddress,
            chainSpec = chainSpec
        ) ?: contractDecompilerService.decompile(deployedBytecode)
}
