package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.exception.CannotAttachTxInfoException
import com.ampnet.blockchainapiservice.model.filters.ContractFunctionCallRequestFilters
import com.ampnet.blockchainapiservice.model.params.CreateContractFunctionCallRequestParams
import com.ampnet.blockchainapiservice.model.params.PreStoreContractFunctionCallRequestParams
import com.ampnet.blockchainapiservice.model.params.StoreContractFunctionCallRequestParams
import com.ampnet.blockchainapiservice.model.result.BlockchainTransactionInfo
import com.ampnet.blockchainapiservice.model.result.ContractFunctionCallRequest
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.repository.ContractFunctionCallRequestRepository
import com.ampnet.blockchainapiservice.repository.ProjectRepository
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.FunctionArgument
import com.ampnet.blockchainapiservice.util.FunctionData
import com.ampnet.blockchainapiservice.util.Status
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.ampnet.blockchainapiservice.util.WithFunctionData
import com.ampnet.blockchainapiservice.util.WithTransactionAndFunctionData
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KLogging
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@Suppress("TooManyFunctions")
class ContractFunctionCallRequestServiceImpl(
    private val functionEncoderService: FunctionEncoderService,
    private val contractFunctionCallRequestRepository: ContractFunctionCallRequestRepository,
    private val deployedContractIdentifierResolverService: DeployedContractIdentifierResolverService,
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
        )

        val contractFunctionCallRequest = contractFunctionCallRequestRepository.store(databaseParams)

        return WithFunctionData(contractFunctionCallRequest, data)
    }

    override fun getContractFunctionCallRequest(id: UUID): WithTransactionAndFunctionData<ContractFunctionCallRequest> {
        logger.debug { "Fetching contract function call request, id: $id" }

        val contractFunctionCallRequest = ethCommonService.fetchResource(
            contractFunctionCallRequestRepository.getById(id),
            "Contract function call request not found for ID: $id"
        )
        val project = projectRepository.getById(contractFunctionCallRequest.projectId)!!

        return contractFunctionCallRequest.appendTransactionData(project)
    }

    override fun getContractFunctionCallRequestsByProjectIdAndFilters(
        projectId: UUID,
        filters: ContractFunctionCallRequestFilters
    ): List<WithTransactionAndFunctionData<ContractFunctionCallRequest>> {
        logger.debug { "Fetching contract function call requests for projectId: $projectId, filters: $filters" }
        return projectRepository.getById(projectId)?.let {
            contractFunctionCallRequestRepository.getAllByProjectId(projectId, filters)
                .map { req -> req.appendTransactionData(it) }
        } ?: emptyList()
    }

    override fun attachTxInfo(id: UUID, txHash: TransactionHash, caller: WalletAddress) {
        logger.info { "Attach txInfo to contract function call request, id: $id, txHash: $txHash, caller: $caller" }

        val txInfoAttached = contractFunctionCallRequestRepository.setTxInfo(id, txHash, caller)

        if (txInfoAttached.not()) {
            throw CannotAttachTxInfoException(
                "Unable to attach transaction info to contract function call request with ID: $id"
            )
        }
    }

    private fun ContractFunctionCallRequest.appendTransactionData(
        project: Project
    ): WithTransactionAndFunctionData<ContractFunctionCallRequest> {
        val transactionInfo = ethCommonService.fetchTransactionInfo(
            txHash = txHash,
            chainId = chainId,
            customRpcUrl = project.customRpcUrl
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
            transactionInfo.callerAddressMatches(callerAddress) &&
            transactionInfo.contractAddressMatches(contractAddress) &&
            transactionInfo.dataMatches(expectedData) &&
            transactionInfo.valueMatches(ethAmount)

    private fun BlockchainTransactionInfo.hashMatches(expectedHash: TransactionHash?): Boolean =
        hash == expectedHash

    private fun BlockchainTransactionInfo.callerAddressMatches(callerAddress: WalletAddress?): Boolean =
        callerAddress == null || from == callerAddress

    private fun BlockchainTransactionInfo.contractAddressMatches(contractAddress: ContractAddress?): Boolean =
        to.toContractAddress() == contractAddress

    private fun BlockchainTransactionInfo.dataMatches(expectedData: FunctionData): Boolean =
        data == expectedData

    private fun BlockchainTransactionInfo.valueMatches(expectedValue: Balance): Boolean =
        value == expectedValue
}
