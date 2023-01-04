package dev3.blockchainapiservice.features.contract.functioncall.service

import com.fasterxml.jackson.databind.ObjectMapper
import dev3.blockchainapiservice.exception.CannotAttachTxInfoException
import dev3.blockchainapiservice.features.blacklist.repository.BlacklistedAddressRepository
import dev3.blockchainapiservice.features.contract.deployment.repository.ContractDecoratorRepository
import dev3.blockchainapiservice.features.contract.deployment.repository.ContractDeploymentRequestRepository
import dev3.blockchainapiservice.features.contract.deployment.repository.ImportedContractDecoratorRepository
import dev3.blockchainapiservice.features.contract.deployment.service.DeployedContractIdentifierResolverService
import dev3.blockchainapiservice.features.contract.functioncall.model.filters.ContractFunctionCallRequestFilters
import dev3.blockchainapiservice.features.contract.functioncall.model.params.CreateContractFunctionCallRequestParams
import dev3.blockchainapiservice.features.contract.functioncall.model.params.PreStoreContractFunctionCallRequestParams
import dev3.blockchainapiservice.features.contract.functioncall.model.params.StoreContractFunctionCallRequestParams
import dev3.blockchainapiservice.features.contract.functioncall.model.result.ContractFunctionCallRequest
import dev3.blockchainapiservice.features.contract.functioncall.repository.ContractFunctionCallRequestRepository
import dev3.blockchainapiservice.features.functions.encoding.model.FunctionArgument
import dev3.blockchainapiservice.features.functions.encoding.service.FunctionEncoderService
import dev3.blockchainapiservice.generated.jooq.id.ContractFunctionCallRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.model.result.BlockchainTransactionInfo
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.repository.ProjectRepository
import dev3.blockchainapiservice.service.EthCommonService
import dev3.blockchainapiservice.util.FunctionData
import dev3.blockchainapiservice.util.Status
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress
import dev3.blockchainapiservice.util.WithFunctionData
import dev3.blockchainapiservice.util.WithTransactionAndFunctionData
import mu.KLogging
import org.springframework.stereotype.Service

@Service
@Suppress("TooManyFunctions", "LongParameterList")
class ContractFunctionCallRequestServiceImpl(
    private val functionEncoderService: FunctionEncoderService,
    private val contractFunctionCallRequestRepository: ContractFunctionCallRequestRepository,
    private val deployedContractIdentifierResolverService: DeployedContractIdentifierResolverService,
    private val contractDeploymentRequestRepository: ContractDeploymentRequestRepository,
    private val contractDecoratorRepository: ContractDecoratorRepository,
    private val importedContractDecoratorRepository: ImportedContractDecoratorRepository,
    private val blacklistedAddressRepository: BlacklistedAddressRepository,
    private val ethCommonService: EthCommonService,
    private val projectRepository: ProjectRepository,
    private val objectMapper: ObjectMapper
) : ContractFunctionCallRequestService {

    companion object : KLogging()

    override fun createContractFunctionCallRequest(
        params: CreateContractFunctionCallRequestParams,
        project: Project
    ): WithFunctionData<ContractFunctionCallRequest> {
        logger.info { "Creating contract function call request, params: $params, project: $project" }

        val (deployedContractId, contractAddress) = deployedContractIdentifierResolverService
            .resolveContractIdAndAddress(params.identifier, project)
        val data = functionEncoderService.encode(
            functionName = params.functionName,
            arguments = params.functionParams
        )
        val databaseParams = ethCommonService.createDatabaseParams(
            factory = StoreContractFunctionCallRequestParams,
            params = PreStoreContractFunctionCallRequestParams(
                createParams = params,
                deployedContractId = deployedContractId,
                contractAddress = contractAddress
            ),
            project = project
        ).addCautionIfNeeded()

        val contractFunctionCallRequest = contractFunctionCallRequestRepository.store(databaseParams)

        return WithFunctionData(contractFunctionCallRequest, data)
    }

    override fun getContractFunctionCallRequest(
        id: ContractFunctionCallRequestId
    ): WithTransactionAndFunctionData<ContractFunctionCallRequest> {
        logger.debug { "Fetching contract function call request, id: $id" }

        val contractFunctionCallRequest = ethCommonService.fetchResource(
            contractFunctionCallRequestRepository.getById(id),
            "Contract function call request not found for ID: $id"
        )
        val project = projectRepository.getById(contractFunctionCallRequest.projectId)!!

        return contractFunctionCallRequest.appendTransactionData(project)
    }

    override fun getContractFunctionCallRequestsByProjectIdAndFilters(
        projectId: ProjectId,
        filters: ContractFunctionCallRequestFilters
    ): List<WithTransactionAndFunctionData<ContractFunctionCallRequest>> {
        logger.debug { "Fetching contract function call requests for projectId: $projectId, filters: $filters" }
        return projectRepository.getById(projectId)?.let {
            contractFunctionCallRequestRepository.getAllByProjectId(projectId, filters)
                .map { req -> req.appendTransactionData(it) }
        } ?: emptyList()
    }

    override fun attachTxInfo(id: ContractFunctionCallRequestId, txHash: TransactionHash, caller: WalletAddress) {
        logger.info { "Attach txInfo to contract function call request, id: $id, txHash: $txHash, caller: $caller" }

        val txInfoAttached = contractFunctionCallRequestRepository.setTxInfo(id, txHash, caller)

        if (txInfoAttached.not()) {
            throw CannotAttachTxInfoException(
                "Unable to attach transaction info to contract function call request with ID: $id"
            )
        }
    }

    private fun StoreContractFunctionCallRequestParams.addCautionIfNeeded() =
        if (blacklistedAddressRepository.exists(contractAddress)) copy(redirectUrl = "$redirectUrl/caution") else this

    private fun ContractFunctionCallRequest.appendTransactionData(
        project: Project
    ): WithTransactionAndFunctionData<ContractFunctionCallRequest> {
        val decorator = this.deployedContractId
            ?.let { contractDeploymentRequestRepository.getById(it)?.contractId }
            ?.let {
                contractDecoratorRepository.getById(it)
                    ?: importedContractDecoratorRepository.getByContractIdAndProjectId(it, projectId)
            }

        val transactionInfo = ethCommonService.fetchTransactionInfo(
            txHash = txHash,
            chainId = chainId,
            customRpcUrl = project.customRpcUrl,
            events = decorator?.getDeserializableEvents(objectMapper).orEmpty()
        )
        val data = functionEncoderService.encode(
            functionName = functionName,
            arguments = objectMapper.treeToValue(functionParams, Array<FunctionArgument>::class.java).toList()
        )
        val status = determineStatus(transactionInfo, data)

        return withTransactionAndFunctionData(
            status = status,
            data = data,
            transactionInfo = transactionInfo
        )
    }

    private fun ContractFunctionCallRequest.determineStatus(
        transactionInfo: BlockchainTransactionInfo?,
        expectedData: FunctionData
    ): Status =
        if (transactionInfo == null) { // implies that either txHash is null or transaction is not yet mined
            Status.PENDING
        } else if (isSuccess(transactionInfo, expectedData)) {
            Status.SUCCESS
        } else {
            Status.FAILED
        }

    private fun ContractFunctionCallRequest.isSuccess(
        transactionInfo: BlockchainTransactionInfo,
        expectedData: FunctionData
    ): Boolean =
        transactionInfo.success &&
            transactionInfo.hashMatches(txHash) &&
            transactionInfo.fromAddressOptionallyMatches(callerAddress) &&
            transactionInfo.toAddressMatches(contractAddress) &&
            transactionInfo.deployedContractAddressIsNull() &&
            transactionInfo.dataMatches(expectedData) &&
            transactionInfo.valueMatches(ethAmount)
}
