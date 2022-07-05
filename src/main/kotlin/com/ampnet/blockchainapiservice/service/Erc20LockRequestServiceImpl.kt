package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.exception.CannotAttachTxInfoException
import com.ampnet.blockchainapiservice.model.params.CreateErc20LockRequestParams
import com.ampnet.blockchainapiservice.model.params.StoreErc20LockRequestParams
import com.ampnet.blockchainapiservice.model.result.BlockchainTransactionInfo
import com.ampnet.blockchainapiservice.model.result.Erc20LockRequest
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.repository.Erc20LockRequestRepository
import com.ampnet.blockchainapiservice.repository.ProjectRepository
import com.ampnet.blockchainapiservice.util.AbiType.AbiType
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.DurationSeconds
import com.ampnet.blockchainapiservice.util.EthereumString
import com.ampnet.blockchainapiservice.util.FunctionArgument
import com.ampnet.blockchainapiservice.util.FunctionData
import com.ampnet.blockchainapiservice.util.Status
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.ampnet.blockchainapiservice.util.WithFunctionData
import com.ampnet.blockchainapiservice.util.WithTransactionData
import com.ampnet.blockchainapiservice.util.ZeroAddress
import mu.KLogging
import org.springframework.stereotype.Service
import java.util.UUID

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

    override fun getErc20LockRequest(id: UUID): WithTransactionData<Erc20LockRequest> {
        logger.debug { "Fetching ERC20 lock request, id: $id" }

        val erc20LockRequest = ethCommonService.fetchResource(
            erc20LockRequestRepository.getById(id),
            "ERC20 lock request not found for ID: $id"
        )
        val project = projectRepository.getById(erc20LockRequest.projectId)!!

        return erc20LockRequest.appendTransactionData(project)
    }

    override fun getErc20LockRequestsByProjectId(projectId: UUID): List<WithTransactionData<Erc20LockRequest>> {
        logger.debug { "Fetching ERC20 lock requests for projectId: $projectId" }
        val project = projectRepository.getById(projectId)!!
        return erc20LockRequestRepository.getAllByProjectId(projectId).map { it.appendTransactionData(project) }
    }

    override fun attachTxInfo(id: UUID, txHash: TransactionHash, caller: WalletAddress) {
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
        id: UUID
    ): FunctionData =
        functionEncoderService.encode(
            functionName = "lock",
            arguments = listOf(
                FunctionArgument(abiType = AbiType.Address, value = tokenAddress),
                FunctionArgument(abiType = AbiType.Uint256, value = tokenAmount),
                FunctionArgument(abiType = AbiType.Uint256, value = lockDuration),
                FunctionArgument(abiType = AbiType.Utf8String, value = EthereumString(id.toString())),
                FunctionArgument(abiType = AbiType.Address, value = ZeroAddress)
            ),
            abiOutputTypes = emptyList()
        )

    private fun Erc20LockRequest.appendTransactionData(project: Project): WithTransactionData<Erc20LockRequest> {
        val transactionInfo = ethCommonService.fetchTransactionInfo(
            txHash = txHash,
            chainId = chainId,
            customRpcUrl = project.customRpcUrl
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
        transactionInfo.success && lockContractAddress == transactionInfo.to.toContractAddress() &&
            txHash == transactionInfo.hash &&
            senderAddressMatches(tokenSenderAddress, transactionInfo.from) &&
            transactionInfo.data == expectedData

    private fun senderAddressMatches(senderAddress: WalletAddress?, fromAddress: WalletAddress): Boolean =
        senderAddress == null || senderAddress == fromAddress
}
