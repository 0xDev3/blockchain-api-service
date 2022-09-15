package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.blockchain.BlockchainService
import com.ampnet.blockchainapiservice.blockchain.properties.ChainSpec
import com.ampnet.blockchainapiservice.exception.ContractDecoratorBinaryMismatchException
import com.ampnet.blockchainapiservice.exception.ContractNotFoundException
import com.ampnet.blockchainapiservice.exception.ResourceNotFoundException
import com.ampnet.blockchainapiservice.model.params.ImportContractParams
import com.ampnet.blockchainapiservice.model.params.OutputParameter
import com.ampnet.blockchainapiservice.model.params.StoreContractDeploymentRequestParams
import com.ampnet.blockchainapiservice.model.result.ContractDeploymentTransactionInfo
import com.ampnet.blockchainapiservice.model.result.ContractParameter
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.repository.ContractDecoratorRepository
import com.ampnet.blockchainapiservice.repository.ContractDeploymentRequestRepository
import com.ampnet.blockchainapiservice.repository.ContractMetadataRepository
import com.ampnet.blockchainapiservice.repository.ImportedContractDecoratorRepository
import com.ampnet.blockchainapiservice.util.ContractBinaryData
import com.ampnet.blockchainapiservice.util.ContractId
import com.ampnet.blockchainapiservice.util.Tuple
import com.fasterxml.jackson.databind.ObjectMapper
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
        internal data class TypeAndValue(val type: String, val value: Any)
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
        val contractDecorator = contractDecoratorRepository.getById(contractId)
            ?: throw ResourceNotFoundException(decoratorNotFoundMessage)

        if (!contractMetadataRepository.exists(contractId)) {
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
            project = project
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

        val decompiledContract = contractDecompilerService.decompile(contractDeploymentTransactionInfo.binary)
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
                    infoMarkdown = decompiledContract.infoMarkdown ?: ""
                )

        contractMetadataRepository.createOrUpdate(
            id = uuidProvider.getUuid(),
            name = contractDecorator.name,
            description = contractDecorator.description,
            contractId = contractId,
            contractTags = contractDecorator.tags,
            contractImplements = contractDecorator.implements
        )

        return storeContractDeploymentRequest(
            constructorParams = constructorParams,
            constructorInputs = constructorInputs,
            params = params,
            contractId = contractId,
            contractDeploymentTransactionInfo = contractDeploymentTransactionInfo,
            project = project
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
        project: Project
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

        contractDeploymentRequestRepository.store(storeParams)
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
