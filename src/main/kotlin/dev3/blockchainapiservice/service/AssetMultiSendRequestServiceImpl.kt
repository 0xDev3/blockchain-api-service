package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.exception.CannotAttachTxInfoException
import dev3.blockchainapiservice.model.params.CreateAssetMultiSendRequestParams
import dev3.blockchainapiservice.model.params.StoreAssetMultiSendRequestParams
import dev3.blockchainapiservice.model.result.AssetMultiSendRequest
import dev3.blockchainapiservice.model.result.BlockchainTransactionInfo
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.repository.AssetMultiSendRequestRepository
import dev3.blockchainapiservice.repository.ProjectRepository
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.FunctionArgument
import dev3.blockchainapiservice.util.FunctionData
import dev3.blockchainapiservice.util.PredefinedEvents
import dev3.blockchainapiservice.util.Status
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress
import dev3.blockchainapiservice.util.WithFunctionDataOrEthValue
import dev3.blockchainapiservice.util.WithMultiTransactionData
import mu.KLogging
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@Suppress("TooManyFunctions")
class AssetMultiSendRequestServiceImpl(
    private val functionEncoderService: FunctionEncoderService,
    private val assetMultiSendRequestRepository: AssetMultiSendRequestRepository,
    private val ethCommonService: EthCommonService,
    private val projectRepository: ProjectRepository
) : AssetMultiSendRequestService {

    companion object : KLogging()

    override fun createAssetMultiSendRequest(
        params: CreateAssetMultiSendRequestParams,
        project: Project
    ): WithFunctionDataOrEthValue<AssetMultiSendRequest> {
        logger.info { "Creating asset multi-send request, params: $params, project: $project" }

        val databaseParams = ethCommonService.createDatabaseParams(StoreAssetMultiSendRequestParams, params, project)
        val totalAssetAmount = Balance(params.assetAmounts.sumOf { it.rawValue })

        return if (params.tokenAddress != null) {
            val data = encodeApproveFunctionData(params.disperseContractAddress, totalAssetAmount)
            val assetMultiSendRequest = assetMultiSendRequestRepository.store(databaseParams)

            WithFunctionDataOrEthValue(assetMultiSendRequest, data, null)
        } else {
            val data = encodeDisperseEtherFunctionData(params.assetRecipientAddresses, params.assetAmounts)
            val assetMultiSendRequest = assetMultiSendRequestRepository.store(databaseParams)

            WithFunctionDataOrEthValue(assetMultiSendRequest, data, totalAssetAmount)
        }
    }

    override fun getAssetMultiSendRequest(id: UUID): WithMultiTransactionData<AssetMultiSendRequest> {
        logger.debug { "Fetching asset multi-send request, id: $id" }

        val assetMultiSendRequest = ethCommonService.fetchResource(
            assetMultiSendRequestRepository.getById(id),
            "Asset multi-send request not found for ID: $id"
        )
        val project = projectRepository.getById(assetMultiSendRequest.projectId)!!

        return assetMultiSendRequest.appendTransactionData(project)
    }

    override fun getAssetMultiSendRequestsByProjectId(
        projectId: UUID
    ): List<WithMultiTransactionData<AssetMultiSendRequest>> {
        logger.debug { "Fetching asset multi-send requests for projectId: $projectId" }
        return projectRepository.getById(projectId)?.let {
            assetMultiSendRequestRepository.getAllByProjectId(projectId).map { req -> req.appendTransactionData(it) }
        } ?: emptyList()
    }

    override fun getAssetMultiSendRequestsBySender(
        sender: WalletAddress
    ): List<WithMultiTransactionData<AssetMultiSendRequest>> {
        logger.debug { "Fetching asset multi-send requests for sender: $sender" }
        return assetMultiSendRequestRepository.getBySender(sender).map {
            val project = projectRepository.getById(it.projectId)!!
            it.appendTransactionData(project)
        }
    }

    override fun attachApproveTxInfo(id: UUID, txHash: TransactionHash, caller: WalletAddress) {
        logger.info { "Attach approve txInfo to asset multi-send request, id: $id, txHash: $txHash, caller: $caller" }

        val txInfoAttached = assetMultiSendRequestRepository.setApproveTxInfo(id, txHash, caller)

        if (txInfoAttached.not()) {
            throw CannotAttachTxInfoException(
                "Unable to attach approve transaction info to asset multi-send request with ID: $id"
            )
        }
    }

    override fun attachDisperseTxInfo(id: UUID, txHash: TransactionHash, caller: WalletAddress) {
        logger.info { "Attach disperse txInfo to asset multi-send request, id: $id, txHash: $txHash, caller: $caller" }

        val txInfoAttached = assetMultiSendRequestRepository.setDisperseTxInfo(id, txHash, caller)

        if (txInfoAttached.not()) {
            throw CannotAttachTxInfoException(
                "Unable to attach disperse transaction info to asset multi-send request with ID: $id"
            )
        }
    }

    private fun encodeApproveFunctionData(
        disperseContractAddress: ContractAddress,
        totalTokenAmount: Balance
    ): FunctionData =
        functionEncoderService.encode(
            functionName = "approve",
            arguments = listOf(
                FunctionArgument(disperseContractAddress),
                FunctionArgument(totalTokenAmount)
            )
        )

    private fun encodeDisperseEtherFunctionData(
        recipients: List<WalletAddress>,
        amounts: List<Balance>
    ): FunctionData =
        functionEncoderService.encode(
            functionName = "disperseEther",
            arguments = listOf(
                FunctionArgument.fromAddresses(recipients),
                FunctionArgument.fromUint256s(amounts)
            )
        )

    private fun encodeDisperseTokenFunctionData(
        tokenAddress: ContractAddress,
        recipients: List<WalletAddress>,
        amounts: List<Balance>
    ): FunctionData =
        functionEncoderService.encode(
            functionName = "disperseToken",
            arguments = listOf(
                FunctionArgument(tokenAddress),
                FunctionArgument.fromAddresses(recipients),
                FunctionArgument.fromUint256s(amounts)
            )
        )

    private fun AssetMultiSendRequest.appendTransactionData(
        project: Project
    ): WithMultiTransactionData<AssetMultiSendRequest> {
        val totalAssetAmount = Balance(assetAmounts.sumOf { it.rawValue })
        val approveTx = fetchApproveTransaction(project, totalAssetAmount)
        val approveStatus = approveTx?.first

        return if (approveStatus == null || approveStatus == Status.SUCCESS) {
            val disperseTransactionInfo = ethCommonService.fetchTransactionInfo(
                txHash = disperseTxHash,
                chainId = chainId,
                customRpcUrl = project.customRpcUrl,
                events = listOf(PredefinedEvents.ERC20_TRANSFER)
            )
            val disperseData =
                tokenAddress?.let { encodeDisperseTokenFunctionData(it, assetRecipientAddresses, assetAmounts) }
                    ?: encodeDisperseEtherFunctionData(assetRecipientAddresses, assetAmounts)
            val disperseValue = if (tokenAddress == null) totalAssetAmount else Balance.ZERO
            val disperseStatus = determineDisperseStatus(disperseTransactionInfo, disperseData, totalAssetAmount)

            withMultiTransactionData(
                approveStatus = approveStatus,
                approveData = approveTx?.third,
                approveTransactionInfo = approveTx?.second,
                disperseStatus = disperseStatus,
                disperseData = disperseData,
                disperseValue = disperseValue,
                disperseTransactionInfo = disperseTransactionInfo
            )
        } else {
            withMultiTransactionData(
                approveStatus = approveStatus,
                approveData = approveTx.third,
                approveTransactionInfo = approveTx.second,
                disperseStatus = null,
                disperseData = null,
                disperseValue = null,
                disperseTransactionInfo = null
            )
        }
    }

    private fun AssetMultiSendRequest.fetchApproveTransaction(
        project: Project,
        totalAssetAmount: Balance
    ): Triple<Status, BlockchainTransactionInfo?, FunctionData>? =
        if (tokenAddress != null) {
            val approveTransactionInfo = ethCommonService.fetchTransactionInfo(
                txHash = approveTxHash,
                chainId = chainId,
                customRpcUrl = project.customRpcUrl,
                events = listOf(PredefinedEvents.ERC20_APPROVAL)
            )
            val approveData = encodeApproveFunctionData(disperseContractAddress, totalAssetAmount)
            val approveStatus = determineApproveStatus(approveTransactionInfo, approveData, tokenAddress)

            Triple(approveStatus, approveTransactionInfo, approveData)
        } else null

    private fun AssetMultiSendRequest.determineApproveStatus(
        transactionInfo: BlockchainTransactionInfo?,
        expectedData: FunctionData,
        nonNullTokenAddress: ContractAddress
    ): Status =
        if (transactionInfo == null) { // implies that either txHash is null or transaction is not yet mined
            Status.PENDING
        } else if (isApproveSuccess(transactionInfo, expectedData, nonNullTokenAddress)) {
            Status.SUCCESS
        } else {
            Status.FAILED
        }

    private fun AssetMultiSendRequest.isApproveSuccess(
        transactionInfo: BlockchainTransactionInfo,
        expectedData: FunctionData,
        nonNullTokenAddress: ContractAddress
    ): Boolean =
        transactionInfo.success &&
            transactionInfo.hashMatches(approveTxHash) &&
            transactionInfo.fromAddressOptionallyMatches(assetSenderAddress) &&
            transactionInfo.toAddressMatches(nonNullTokenAddress) &&
            transactionInfo.deployedContractAddressIsNull() &&
            transactionInfo.dataMatches(expectedData) &&
            transactionInfo.valueMatches(Balance.ZERO)

    private fun AssetMultiSendRequest.determineDisperseStatus(
        transactionInfo: BlockchainTransactionInfo?,
        expectedData: FunctionData,
        sendValue: Balance
    ): Status =
        if (transactionInfo == null) { // implies that either txHash is null or transaction is not yet mined
            Status.PENDING
        } else if (isDisperseSuccess(transactionInfo, expectedData, sendValue)) {
            Status.SUCCESS
        } else {
            Status.FAILED
        }

    private fun AssetMultiSendRequest.isDisperseSuccess(
        transactionInfo: BlockchainTransactionInfo,
        expectedData: FunctionData,
        sendValue: Balance
    ): Boolean =
        transactionInfo.success &&
            transactionInfo.hashMatches(disperseTxHash) &&
            transactionInfo.fromAddressOptionallyMatches(assetSenderAddress) &&
            transactionInfo.toAddressMatches(disperseContractAddress) &&
            transactionInfo.deployedContractAddressIsNull() &&
            transactionInfo.dataMatches(expectedData) &&
            transactionInfo.valueMatches(if (tokenAddress == null) sendValue else Balance.ZERO)
}
