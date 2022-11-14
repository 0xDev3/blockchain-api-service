package dev3.blockchainapiservice.service

import com.fasterxml.jackson.databind.ObjectMapper
import dev3.blockchainapiservice.blockchain.BlockchainService
import dev3.blockchainapiservice.blockchain.properties.ChainSpec
import dev3.blockchainapiservice.exception.ContractDecoratorBinaryMismatchException
import dev3.blockchainapiservice.exception.ContractNotFoundException
import dev3.blockchainapiservice.exception.ResourceNotFoundException
import dev3.blockchainapiservice.model.json.ArtifactJson
import dev3.blockchainapiservice.model.json.DecompiledContractJson
import dev3.blockchainapiservice.model.json.ManifestJson
import dev3.blockchainapiservice.model.params.ExecuteReadonlyFunctionCallParams
import dev3.blockchainapiservice.model.params.ImportContractParams
import dev3.blockchainapiservice.model.params.OutputParameter
import dev3.blockchainapiservice.model.params.StoreContractDeploymentRequestParams
import dev3.blockchainapiservice.model.result.ContractDeploymentRequest
import dev3.blockchainapiservice.model.result.ContractDeploymentTransactionInfo
import dev3.blockchainapiservice.model.result.ContractMetadata
import dev3.blockchainapiservice.model.result.ContractParameter
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.repository.ContractDecoratorRepository
import dev3.blockchainapiservice.repository.ContractDeploymentRequestRepository
import dev3.blockchainapiservice.repository.ContractMetadataRepository
import dev3.blockchainapiservice.repository.ImportedContractDecoratorRepository
import dev3.blockchainapiservice.util.AddressType
import dev3.blockchainapiservice.util.Constants
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.ContractId
import dev3.blockchainapiservice.util.Tuple
import dev3.blockchainapiservice.util.ZeroAddress
import mu.KLogging
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@Suppress("LongParameterList", "TooManyFunctions")
class ContractImportServiceImpl(
    private val abiDecoderService: AbiDecoderService,
    private val contractDecompilerService: ContractDecompilerService,
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
            importAndDecompileContract(params, project)
        }
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

        contractDeploymentRequestRepository.setTxInfo(id, request.txHash!!, request.deployerAddress!!)
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
            importedAt = utcDateTimeProvider.getUtcDateTime()
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

        contractDeploymentRequestRepository.setTxInfo(id, request.txHash!!, request.deployerAddress!!)
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

        val contractDeploymentTransactionInfo = findContractDeploymentTransaction(params.contractAddress, project)

        val decoratorBinary = contractDecorator.binary.withPrefix
        val deployedBinary = contractDeploymentTransactionInfo.data.value

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

    @Suppress("LongMethod")
    private fun importAndDecompileContract(params: ImportContractParams, project: Project): UUID {
        val contractDeploymentTransactionInfo = findContractDeploymentTransaction(params.contractAddress, project)

        val fullBinary = contractDeploymentTransactionInfo.data.value
        val shortBinary = contractDeploymentTransactionInfo.binary.value
        val constructorParamsStart = fullBinary.indexOf(shortBinary) + shortBinary.length

        val constructorParams = fullBinary.substring(constructorParamsStart)

        val (decompiledContract, implementationAddress) = contractDecompilerService
            .decompile(contractDeploymentTransactionInfo.binary).let {
                it.copy(
                    artifact = it.artifact.copy(
                        bytecode = contractDeploymentTransactionInfo.data.withoutPrefix.removeSuffix(constructorParams),
                        deployedBytecode = contractDeploymentTransactionInfo.binary.value
                    )
                ).resolveProxyContract(params, project)
            }
        val contractAddress = contractDeploymentTransactionInfo.deployedContractAddress
        val contractId = ContractId("imported-${contractAddress.rawValue}-${project.chainId.value}")

        val contractDecorator =
            importedContractDecoratorRepository.getByContractIdAndProjectId(contractId, project.id)
                ?: importedContractDecoratorRepository.store(
                    id = uuidProvider.getUuid(),
                    projectId = project.id,
                    contractId = contractId,
                    manifestJson = decompiledContract.manifest,
                    artifactJson = decompiledContract.artifact,
                    infoMarkdown = decompiledContract.infoMarkdown ?: "",
                    importedAt = utcDateTimeProvider.getUtcDateTime()
                )

        contractMetadataRepository.createOrUpdate(
            ContractMetadata(
                id = uuidProvider.getUuid(),
                name = contractDecorator.name,
                description = contractDecorator.description,
                contractId = contractId,
                contractTags = contractDecorator.tags,
                contractImplements = contractDecorator.implements,
                projectId = project.id
            )
        )

        val constructorInputs = List(constructorParams.length / ETH_VALUE_LENGTH) {
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
            constructorParams = constructorParams,
            constructorInputs = constructorInputs,
            params = params,
            contractId = contractId,
            contractDeploymentTransactionInfo = contractDeploymentTransactionInfo,
            project = project,
            metadataProjectId = project.id,
            proxy = implementationAddress != null,
            implementationContractAddress = implementationAddress
        )
    }

    private fun findContractDeploymentTransaction(contractAddress: ContractAddress, project: Project) =
        blockchainService.findContractDeploymentTransaction(
            chainSpec = ChainSpec(
                chainId = project.chainId,
                customRpcUrl = project.customRpcUrl
            ),
            contractAddress = contractAddress
        ) ?: throw ContractNotFoundException(contractAddress)

    private fun DecompiledContractJson.resolveProxyContract(
        params: ImportContractParams,
        project: Project
    ): Pair<DecompiledContractJson, ContractAddress?> =
        if (this.manifest.functionDecorators.any { it.signature == "$PROXY_FUNCTION_NAME()" }) {
            val implementationAddress = findContractProxyImplementation(params.contractAddress, project)
            val implementationTransactionInfo = findContractDeploymentTransaction(implementationAddress, project)
            val decompiledImplementation = contractDecompilerService.decompile(implementationTransactionInfo.binary)

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

    private fun findContractProxyImplementation(contractAddress: ContractAddress, project: Project): ContractAddress {
        val data = functionEncoderService.encode(
            functionName = PROXY_FUNCTION_NAME,
            arguments = emptyList()
        )

        val implementation = blockchainService.callReadonlyFunction(
            chainSpec = ChainSpec(
                chainId = project.chainId,
                customRpcUrl = project.customRpcUrl
            ),
            params = ExecuteReadonlyFunctionCallParams(
                contractAddress = contractAddress,
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
        contractDeploymentRequestRepository.setTxInfo(
            id = id,
            txHash = contractDeploymentTransactionInfo.hash,
            deployer = contractDeploymentTransactionInfo.from
        )

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
}
