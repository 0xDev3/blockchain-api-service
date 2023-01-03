package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.exception.CannotAttachTxInfoException
import dev3.blockchainapiservice.generated.jooq.id.Erc20LockRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.model.params.CreateErc20LockRequestParams
import dev3.blockchainapiservice.model.params.StoreErc20LockRequestParams
import dev3.blockchainapiservice.model.result.BlockchainTransactionInfo
import dev3.blockchainapiservice.model.result.Erc20LockRequest
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.repository.Erc20LockRequestRepository
import dev3.blockchainapiservice.repository.ProjectRepository
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.DurationSeconds
import dev3.blockchainapiservice.util.FunctionArgument
import dev3.blockchainapiservice.util.FunctionData
import dev3.blockchainapiservice.util.Status
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress
import dev3.blockchainapiservice.util.WithFunctionData
import dev3.blockchainapiservice.util.WithTransactionData
import dev3.blockchainapiservice.util.ZeroAddress
import mu.KLogging
import org.springframework.stereotype.Service

@Service
class Erc20LockRequestServiceImpl(
    private val functionEncoderService: FunctionEncoderService,
    private val erc20LockRequestRepository: Erc20LockRequestRepository,
    private val ethCommonService: EthCommonService,
    private val projectRepository: ProjectRepository
) : Erc20LockRequestService {

    companion object : KLogging()

    override fun createErc20LockRequest(
        params: CreateErc20LockRequestParams,
        project: Project
    ): WithFunctionData<Erc20LockRequest> {
        logger.info { "Creating ERC20 lock request, params: $params, project: $project" }

        val databaseParams = ethCommonService.createDatabaseParams(StoreErc20LockRequestParams, params, project)
        val data = encodeFunctionData(
            tokenAddress = databaseParams.tokenAddress,
            tokenAmount = databaseParams.tokenAmount,
            lockDuration = databaseParams.lockDuration,
            id = databaseParams.id
        )

        val erc20LockRequest = erc20LockRequestRepository.store(databaseParams)

        return WithFunctionData(erc20LockRequest, data)
    }

    override fun getErc20LockRequest(id: Erc20LockRequestId): WithTransactionData<Erc20LockRequest> {
        logger.debug { "Fetching ERC20 lock request, id: $id" }

        val erc20LockRequest = ethCommonService.fetchResource(
            erc20LockRequestRepository.getById(id),
            "ERC20 lock request not found for ID: $id"
        )
        val project = projectRepository.getById(erc20LockRequest.projectId)!!

        return erc20LockRequest.appendTransactionData(project)
    }

    override fun getErc20LockRequestsByProjectId(projectId: ProjectId): List<WithTransactionData<Erc20LockRequest>> {
        logger.debug { "Fetching ERC20 lock requests for projectId: $projectId" }
        return projectRepository.getById(projectId)?.let {
            erc20LockRequestRepository.getAllByProjectId(projectId).map { req -> req.appendTransactionData(it) }
        } ?: emptyList()
    }

    override fun attachTxInfo(id: Erc20LockRequestId, txHash: TransactionHash, caller: WalletAddress) {
        logger.info { "Attach txInfo to ERC20 lock request, id: $id, txHash: $txHash, caller: $caller" }

        val txInfoAttached = erc20LockRequestRepository.setTxInfo(id, txHash, caller)

        if (txInfoAttached.not()) {
            throw CannotAttachTxInfoException("Unable to attach transaction info to ERC20 lock request with ID: $id")
        }
    }

    private fun encodeFunctionData(
        tokenAddress: ContractAddress,
        tokenAmount: Balance,
        lockDuration: DurationSeconds,
        id: Erc20LockRequestId
    ): FunctionData =
        functionEncoderService.encode(
            functionName = "lock",
            arguments = listOf(
                FunctionArgument(tokenAddress),
                FunctionArgument(tokenAmount),
                FunctionArgument(lockDuration),
                FunctionArgument(id.value.toString()),
                FunctionArgument(ZeroAddress)
            )
        )

    private fun Erc20LockRequest.appendTransactionData(project: Project): WithTransactionData<Erc20LockRequest> {
        val transactionInfo = ethCommonService.fetchTransactionInfo(
            txHash = txHash,
            chainId = chainId,
            customRpcUrl = project.customRpcUrl,
            events = emptyList()
        )
        val data = encodeFunctionData(
            tokenAddress = tokenAddress,
            tokenAmount = tokenAmount,
            lockDuration = lockDuration,
            id = id
        )
        val status = determineStatus(transactionInfo, data)

        return withTransactionData(
            status = status,
            data = data,
            transactionInfo = transactionInfo
        )
    }

    private fun Erc20LockRequest.determineStatus(
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

    private fun Erc20LockRequest.isSuccess(
        transactionInfo: BlockchainTransactionInfo,
        expectedData: FunctionData
    ): Boolean =
        transactionInfo.success &&
            transactionInfo.hashMatches(txHash) &&
            transactionInfo.fromAddressOptionallyMatches(tokenSenderAddress) &&
            transactionInfo.toAddressMatches(lockContractAddress) &&
            transactionInfo.deployedContractAddressIsNull() &&
            transactionInfo.dataMatches(expectedData) &&
            transactionInfo.valueMatches(Balance.ZERO)
}
