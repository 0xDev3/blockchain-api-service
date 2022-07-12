package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.exception.CannotAttachTxInfoException
import com.ampnet.blockchainapiservice.model.filters.ContractDeploymentRequestFilters
import com.ampnet.blockchainapiservice.model.params.CreateContractDeploymentRequestParams
import com.ampnet.blockchainapiservice.model.params.PreStoreContractDeploymentRequestParams
import com.ampnet.blockchainapiservice.model.params.StoreContractDeploymentRequestParams
import com.ampnet.blockchainapiservice.model.result.BlockchainTransactionInfo
import com.ampnet.blockchainapiservice.model.result.ContractDeploymentRequest
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.repository.ContractDecoratorRepository
import com.ampnet.blockchainapiservice.repository.ContractDeploymentRequestRepository
import com.ampnet.blockchainapiservice.repository.ProjectRepository
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.FunctionData
import com.ampnet.blockchainapiservice.util.Status
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.ampnet.blockchainapiservice.util.WithTransactionData
import mu.KLogging
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@Suppress("TooManyFunctions")
class ContractDeploymentRequestServiceImpl(
    private val functionEncoderService: FunctionEncoderService,
    private val contractDeploymentRequestRepository: ContractDeploymentRequestRepository,
    private val contractDecoratorRepository: ContractDecoratorRepository,
    private val ethCommonService: EthCommonService,
    private val projectRepository: ProjectRepository
) : ContractDeploymentRequestService {

    companion object : KLogging()

    override fun createContractDeploymentRequest(
        params: CreateContractDeploymentRequestParams,
        project: Project
    ): ContractDeploymentRequest {
        logger.info { "Creating contract deployment request, params: $params, project: $project" }

        val contractDecorator = ethCommonService.fetchResource(
            contractDecoratorRepository.getById(params.contractId),
            "Contract decorator not found for contract ID: ${params.contractId.value}"
        )
        // TODO check if constructor exists (out of MVP scope)
        val encodedConstructor = functionEncoderService.encodeConstructor(params.constructorParams)
        val preStoreParams = PreStoreContractDeploymentRequestParams(
            createParams = params,
            contractDecorator = contractDecorator,
            encodedConstructor = encodedConstructor
        )
        val databaseParams = ethCommonService.createDatabaseParams(
            factory = StoreContractDeploymentRequestParams,
            params = preStoreParams,
            project = project
        )

        return contractDeploymentRequestRepository.store(databaseParams)
    }

    override fun getContractDeploymentRequest(id: UUID): WithTransactionData<ContractDeploymentRequest> {
        logger.debug { "Fetching contract deployment request, id: $id" }

        val contractDeploymentRequest = ethCommonService.fetchResource(
            contractDeploymentRequestRepository.getById(id),
            "Contract deployment request not found for ID: $id"
        )
        val project = projectRepository.getById(contractDeploymentRequest.projectId)!!

        return contractDeploymentRequest.appendTransactionData(project)
    }

    override fun getContractDeploymentRequestsByProjectIdAndFilters(
        projectId: UUID,
        filters: ContractDeploymentRequestFilters
    ): List<WithTransactionData<ContractDeploymentRequest>> {
        logger.debug { "Fetching contract deployment requests for projectId: $projectId, filters: $filters" }
        return projectRepository.getById(projectId)?.let {
            contractDeploymentRequestRepository.getAllByProjectId(projectId, filters)
                .map { req -> req.appendTransactionData(it) }
        } ?: emptyList()
    }

    override fun attachTxInfo(id: UUID, txHash: TransactionHash, deployer: WalletAddress) {
        logger.info { "Attach txInfo to contract deployment request, id: $id, txHash: $txHash, deployer: $deployer" }

        val contractDeploymentRequest = ethCommonService.fetchResource(
            contractDeploymentRequestRepository.getById(id),
            "Contract deployment request not found for ID: $id"
        )
        val project = projectRepository.getById(contractDeploymentRequest.projectId)!!
        val transactionInfo = ethCommonService.fetchTransactionInfo(
            txHash = txHash,
            chainId = contractDeploymentRequest.chainId,
            customRpcUrl = project.customRpcUrl
        ) ?: throw CannotAttachTxInfoException(
            "Transaction not found for hash: ${txHash.value}; the contract must be deployed before attaching" +
                " the transaction hash"
        )
        val contractAddress = transactionInfo.deployedContractAddress ?: throw CannotAttachTxInfoException(
            "Transaction with hash: ${txHash.value} is missing deployed contract address"
        )

        val txInfoAttached = contractDeploymentRequestRepository.setTxInfo(id, txHash, contractAddress, deployer)

        if (txInfoAttached.not()) {
            throw CannotAttachTxInfoException(
                "Unable to attach transaction info to contract deployment request with ID: $id"
            )
        }
    }

    private fun ContractDeploymentRequest.appendTransactionData(
        project: Project
    ): WithTransactionData<ContractDeploymentRequest> {
        val transactionInfo = ethCommonService.fetchTransactionInfo(
            txHash = txHash,
            chainId = chainId,
            customRpcUrl = project.customRpcUrl
        )
        val status = determineStatus(transactionInfo)

        return withTransactionData(
            status = status,
            transactionInfo = transactionInfo
        )
    }

    private fun ContractDeploymentRequest.determineStatus(
        transactionInfo: BlockchainTransactionInfo?
    ): Status =
        if (transactionInfo == null) { // implies that either txHash is null or transaction is not yet mined
            Status.PENDING
        } else if (isSuccess(transactionInfo)) {
            Status.SUCCESS
        } else {
            Status.FAILED
        }

    private fun ContractDeploymentRequest.isSuccess(
        transactionInfo: BlockchainTransactionInfo
    ): Boolean =
        transactionInfo.success &&
            transactionInfo.hashMatches(txHash) &&
            transactionInfo.deployerAddressMatches(deployerAddress) &&
            transactionInfo.contractAddressMatches(contractAddress) &&
            transactionInfo.dataMatches(FunctionData(contractData.value)) &&
            transactionInfo.valueMatches(initialEthAmount)

    private fun BlockchainTransactionInfo.hashMatches(expectedHash: TransactionHash?): Boolean =
        hash == expectedHash

    private fun BlockchainTransactionInfo.deployerAddressMatches(deployerAddress: WalletAddress?): Boolean =
        deployerAddress == null || from == deployerAddress

    private fun BlockchainTransactionInfo.contractAddressMatches(contractAddress: ContractAddress?): Boolean =
        contractAddress != null && contractAddress == deployedContractAddress

    private fun BlockchainTransactionInfo.dataMatches(expectedData: FunctionData): Boolean =
        data == expectedData

    private fun BlockchainTransactionInfo.valueMatches(expectedValue: Balance?): Boolean =
        expectedValue == null || value == expectedValue
}
