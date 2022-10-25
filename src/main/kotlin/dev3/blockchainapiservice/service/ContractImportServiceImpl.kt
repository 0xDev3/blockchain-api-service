package dev3.blockchainapiservice.service

import com.fasterxml.jackson.databind.ObjectMapper
import dev3.blockchainapiservice.blockchain.BlockchainService
import dev3.blockchainapiservice.blockchain.properties.ChainSpec
import dev3.blockchainapiservice.exception.ContractDecoratorBinaryMismatchException
import dev3.blockchainapiservice.exception.ContractNotFoundException
import dev3.blockchainapiservice.exception.ResourceNotFoundException
import dev3.blockchainapiservice.model.params.ImportContractParams
import dev3.blockchainapiservice.model.params.OutputParameter
import dev3.blockchainapiservice.model.params.StoreContractDeploymentRequestParams
import dev3.blockchainapiservice.model.result.ContractDeploymentTransactionInfo
import dev3.blockchainapiservice.model.result.ContractMetadata
import dev3.blockchainapiservice.model.result.ContractParameter
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.repository.ContractDecoratorRepository
import dev3.blockchainapiservice.repository.ContractDeploymentRequestRepository
import dev3.blockchainapiservice.repository.ContractMetadataRepository
import dev3.blockchainapiservice.repository.ImportedContractDecoratorRepository
import dev3.blockchainapiservice.util.Constants
import dev3.blockchainapiservice.util.ContractBinaryData
import dev3.blockchainapiservice.util.ContractId
import dev3.blockchainapiservice.util.Tuple
import mu.KLogging
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@Suppress("LongParameterList")
class ContractImportServiceImpl(
    private val abiDecoderService: AbiDecoderService,
    private val contractDecompilerService: ContractDecompilerService,
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

        private data class OutputParams(val params: List<OutputParameter>)
        data class TypeAndValue(val type: String, val value: Any)
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

    @Suppress("ThrowsCount")
    private fun importContractWithExistingDecorator(
        params: ImportContractParams,
        project: Project,
        contractId: ContractId
    ): UUID {
        val decoratorNotFoundMessage = "Contract decorator not found for contract ID: ${contractId.value}"

        // TODO add support for imported contract decorators
        val contractDecorator = contractDecoratorRepository.getById(contractId)
            ?: throw ResourceNotFoundException(decoratorNotFoundMessage)

        // TODO add support for imported contract decorators
        if (!contractMetadataRepository.exists(contractId, Constants.NIL_UUID)) {
            throw ResourceNotFoundException(decoratorNotFoundMessage)
        }

        val contractDeploymentTransactionInfo = findContractDeploymentTransaction(params, project)

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
            metadataProjectId = Constants.NIL_UUID
        )
    }

    private fun importAndDecompileContract(params: ImportContractParams, project: Project): UUID {
        val contractDeploymentTransactionInfo = findContractDeploymentTransaction(params, project)

        val fullBinary = contractDeploymentTransactionInfo.data.value
        val shortBinary = contractDeploymentTransactionInfo.binary.value
        val constructorParamsStart = fullBinary.indexOf(shortBinary) + shortBinary.length

        val constructorParams = fullBinary.substring(constructorParamsStart)
        val constructorInputs = List(constructorParams.length / ETH_VALUE_LENGTH) {
            ContractParameter(
                name = "",
                description = "",
                solidityName = "",
                solidityType = "bytes32",
                recommendedTypes = emptyList(),
                parameters = null
            )
        }

        val decompiledContract = contractDecompilerService.decompile(contractDeploymentTransactionInfo.binary).let {
            it.copy(
                artifact = it.artifact.copy(
                    bytecode = contractDeploymentTransactionInfo.data.withoutPrefix.removeSuffix(constructorParams),
                    deployedBytecode = contractDeploymentTransactionInfo.binary.value
                )
            )
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

        return storeContractDeploymentRequest(
            constructorParams = constructorParams,
            constructorInputs = constructorInputs,
            params = params,
            contractId = contractId,
            contractDeploymentTransactionInfo = contractDeploymentTransactionInfo,
            project = project,
            metadataProjectId = project.id
        )
    }

    private fun findContractDeploymentTransaction(params: ImportContractParams, project: Project) =
        blockchainService.findContractDeploymentTransaction(
            chainSpec = ChainSpec(
                chainId = project.chainId,
                customRpcUrl = project.customRpcUrl
            ),
            contractAddress = params.contractAddress
        ) ?: throw ContractNotFoundException(params.contractAddress)

    @Suppress("LongParameterList")
    private fun storeContractDeploymentRequest(
        constructorParams: String,
        constructorInputs: List<ContractParameter>,
        params: ImportContractParams,
        contractId: ContractId,
        contractDeploymentTransactionInfo: ContractDeploymentTransactionInfo,
        project: Project,
        metadataProjectId: UUID
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
        val storeParams = StoreContractDeploymentRequestParams(
            id = id,
            alias = params.alias,
            contractId = contractId,
            contractData = ContractBinaryData(contractDeploymentTransactionInfo.data.value),
            constructorParams = objectMapper.valueToTree(inputArgs(constructorInputs, decodedConstructorParams)),
            deployerAddress = contractDeploymentTransactionInfo.from,
            initialEthAmount = contractDeploymentTransactionInfo.value,
            chainId = project.chainId,
            redirectUrl = project.createRedirectUrl(params.redirectUrl, id, StoreContractDeploymentRequestParams.PATH),
            projectId = project.id,
            createdAt = utcDateTimeProvider.getUtcDateTime(),
            arbitraryData = params.arbitraryData,
            screenConfig = params.screenConfig,
            imported = true
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
