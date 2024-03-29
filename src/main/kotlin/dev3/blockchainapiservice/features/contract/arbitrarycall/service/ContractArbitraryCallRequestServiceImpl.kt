package dev3.blockchainapiservice.features.contract.arbitrarycall.service

import com.fasterxml.jackson.databind.ObjectMapper
import dev3.blockchainapiservice.exception.CannotAttachTxInfoException
import dev3.blockchainapiservice.features.api.access.model.result.Project
import dev3.blockchainapiservice.features.api.access.repository.ProjectRepository
import dev3.blockchainapiservice.features.blacklist.service.BlacklistCheckService
import dev3.blockchainapiservice.features.contract.arbitrarycall.model.filters.ContractArbitraryCallRequestFilters
import dev3.blockchainapiservice.features.contract.arbitrarycall.model.params.CreateContractArbitraryCallRequestParams
import dev3.blockchainapiservice.features.contract.arbitrarycall.model.params.PreStoreContractArbitraryCallRequestParams
import dev3.blockchainapiservice.features.contract.arbitrarycall.model.params.StoreContractArbitraryCallRequestParams
import dev3.blockchainapiservice.features.contract.arbitrarycall.model.result.ContractArbitraryCallRequest
import dev3.blockchainapiservice.features.contract.arbitrarycall.repository.ContractArbitraryCallRequestRepository
import dev3.blockchainapiservice.features.contract.deployment.repository.ContractDecoratorRepository
import dev3.blockchainapiservice.features.contract.deployment.repository.ContractDeploymentRequestRepository
import dev3.blockchainapiservice.features.contract.deployment.repository.ImportedContractDecoratorRepository
import dev3.blockchainapiservice.features.contract.deployment.service.DeployedContractIdentifierResolverService
import dev3.blockchainapiservice.features.functions.decoding.service.FunctionDecoderService
import dev3.blockchainapiservice.generated.jooq.id.ContractArbitraryCallRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.model.result.BlockchainTransactionInfo
import dev3.blockchainapiservice.service.EthCommonService
import dev3.blockchainapiservice.util.Status
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress
import dev3.blockchainapiservice.util.WithTransactionData
import mu.KLogging
import org.springframework.stereotype.Service

@Service
@Suppress("LongParameterList")
class ContractArbitraryCallRequestServiceImpl(
    private val functionDecoderService: FunctionDecoderService,
    private val contractArbitraryCallRequestRepository: ContractArbitraryCallRequestRepository,
    private val deployedContractIdentifierResolverService: DeployedContractIdentifierResolverService,
    private val contractDeploymentRequestRepository: ContractDeploymentRequestRepository,
    private val contractDecoratorRepository: ContractDecoratorRepository,
    private val importedContractDecoratorRepository: ImportedContractDecoratorRepository,
    private val blacklistCheckService: BlacklistCheckService,
    private val ethCommonService: EthCommonService,
    private val projectRepository: ProjectRepository,
    private val objectMapper: ObjectMapper
) : ContractArbitraryCallRequestService {

    companion object : KLogging()

    override fun createContractArbitraryCallRequest(
        params: CreateContractArbitraryCallRequestParams,
        project: Project
    ): ContractArbitraryCallRequest {
        logger.info { "Creating contract arbitrary call request, params: $params, project: $project" }

        val (deployedContractId, contractAddress) = deployedContractIdentifierResolverService
            .resolveContractIdAndAddress(params.identifier, project)
        val decodedFunction = functionDecoderService.decode(params.functionData)
        val databaseParams = ethCommonService.createDatabaseParams(
            factory = StoreContractArbitraryCallRequestParams,
            params = PreStoreContractArbitraryCallRequestParams(
                createParams = params,
                deployedContractId = deployedContractId,
                functionName = decodedFunction?.name,
                functionParams = decodedFunction?.arguments?.let { functionParams ->
                    objectMapper.createArrayNode().addAll(
                        functionParams.mapNotNull { it.rawJson }
                    )
                },
                contractAddress = contractAddress
            ),
            project = project
        ).addCautionIfNeeded()

        return contractArbitraryCallRequestRepository.store(databaseParams)
    }

    override fun getContractArbitraryCallRequest(
        id: ContractArbitraryCallRequestId
    ): WithTransactionData<ContractArbitraryCallRequest> {
        logger.debug { "Fetching contract arbitrary call request, id: $id" }

        val contractArbitraryCallRequest = ethCommonService.fetchResource(
            contractArbitraryCallRequestRepository.getById(id),
            "Contract arbitrary call request not found for ID: $id"
        )
        val project = projectRepository.getById(contractArbitraryCallRequest.projectId)!!

        return contractArbitraryCallRequest.appendTransactionData(project)
    }

    override fun getContractArbitraryCallRequestsByProjectIdAndFilters(
        projectId: ProjectId,
        filters: ContractArbitraryCallRequestFilters
    ): List<WithTransactionData<ContractArbitraryCallRequest>> {
        logger.debug { "Fetching contract arbitrary call requests for projectId: $projectId, filters: $filters" }
        return projectRepository.getById(projectId)?.let {
            contractArbitraryCallRequestRepository.getAllByProjectId(projectId, filters)
                .map { req -> req.appendTransactionData(it) }
        } ?: emptyList()
    }

    override fun attachTxInfo(id: ContractArbitraryCallRequestId, txHash: TransactionHash, caller: WalletAddress) {
        logger.info { "Attach txInfo to contract arbitrary call request, id: $id, txHash: $txHash, caller: $caller" }

        val txInfoAttached = contractArbitraryCallRequestRepository.setTxInfo(id, txHash, caller)

        if (txInfoAttached.not()) {
            throw CannotAttachTxInfoException(
                "Unable to attach transaction info to contract arbitrary call request with ID: $id"
            )
        }
    }

    private fun StoreContractArbitraryCallRequestParams.addCautionIfNeeded() =
        if (blacklistCheckService.exists(contractAddress)) copy(redirectUrl = "$redirectUrl/caution") else this

    private fun ContractArbitraryCallRequest.appendTransactionData(
        project: Project
    ): WithTransactionData<ContractArbitraryCallRequest> {
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
        val status = determineStatus(transactionInfo)

        return withTransactionData(
            status = status,
            transactionInfo = transactionInfo
        )
    }

    private fun ContractArbitraryCallRequest.determineStatus(transactionInfo: BlockchainTransactionInfo?): Status =
        if (transactionInfo == null) { // implies that either txHash is null or transaction is not yet mined
            Status.PENDING
        } else if (isSuccess(transactionInfo)) {
            Status.SUCCESS
        } else {
            Status.FAILED
        }

    private fun ContractArbitraryCallRequest.isSuccess(transactionInfo: BlockchainTransactionInfo): Boolean =
        transactionInfo.success &&
            transactionInfo.hashMatches(txHash) &&
            transactionInfo.fromAddressOptionallyMatches(callerAddress) &&
            transactionInfo.toAddressMatches(contractAddress) &&
            transactionInfo.deployedContractAddressIsNull() &&
            transactionInfo.dataMatches(functionData) &&
            transactionInfo.valueMatches(ethAmount)
}
