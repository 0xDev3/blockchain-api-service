package dev3.blockchainapiservice.features.asset.send.service

import dev3.blockchainapiservice.exception.CannotAttachTxInfoException
import dev3.blockchainapiservice.features.api.access.model.result.Project
import dev3.blockchainapiservice.features.api.access.repository.ProjectRepository
import dev3.blockchainapiservice.features.asset.send.model.params.CreateAssetSendRequestParams
import dev3.blockchainapiservice.features.asset.send.model.params.StoreAssetSendRequestParams
import dev3.blockchainapiservice.features.asset.send.model.result.AssetSendRequest
import dev3.blockchainapiservice.features.asset.send.repository.AssetSendRequestRepository
import dev3.blockchainapiservice.features.functions.encoding.model.FunctionArgument
import dev3.blockchainapiservice.features.functions.encoding.service.FunctionEncoderService
import dev3.blockchainapiservice.generated.jooq.id.AssetSendRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.model.result.BlockchainTransactionInfo
import dev3.blockchainapiservice.service.EthCommonService
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.FunctionData
import dev3.blockchainapiservice.util.PredefinedEvents
import dev3.blockchainapiservice.util.Status
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress
import dev3.blockchainapiservice.util.WithFunctionDataOrEthValue
import dev3.blockchainapiservice.util.WithTransactionData
import mu.KLogging
import org.springframework.stereotype.Service

@Service
@Suppress("TooManyFunctions")
class AssetSendRequestServiceImpl(
    private val functionEncoderService: FunctionEncoderService,
    private val assetSendRequestRepository: AssetSendRequestRepository,
    private val ethCommonService: EthCommonService,
    private val projectRepository: ProjectRepository
) : AssetSendRequestService {

    companion object : KLogging()

    override fun createAssetSendRequest(
        params: CreateAssetSendRequestParams,
        project: Project
    ): WithFunctionDataOrEthValue<AssetSendRequest> {
        logger.info { "Creating asset send request, params: $params, project: $project" }

        val databaseParams = ethCommonService.createDatabaseParams(StoreAssetSendRequestParams, params, project)
        val data = databaseParams.tokenAddress?.let {
            encodeFunctionData(params.assetRecipientAddress, params.assetAmount)
        }
        val ethValue = if (databaseParams.tokenAddress == null) databaseParams.assetAmount else null
        val assetSendRequest = assetSendRequestRepository.store(databaseParams)

        return WithFunctionDataOrEthValue(assetSendRequest, data, ethValue)
    }

    override fun getAssetSendRequest(id: AssetSendRequestId): WithTransactionData<AssetSendRequest> {
        logger.debug { "Fetching asset send request, id: $id" }

        val assetSendRequest = ethCommonService.fetchResource(
            assetSendRequestRepository.getById(id),
            "Asset send request not found for ID: $id"
        )
        val project = projectRepository.getById(assetSendRequest.projectId)!!

        return assetSendRequest.appendTransactionData(project)
    }

    override fun getAssetSendRequestsByProjectId(projectId: ProjectId): List<WithTransactionData<AssetSendRequest>> {
        logger.debug { "Fetching asset send requests for projectId: $projectId" }
        return projectRepository.getById(projectId)?.let {
            assetSendRequestRepository.getAllByProjectId(projectId).map { req -> req.appendTransactionData(it) }
        } ?: emptyList()
    }

    override fun getAssetSendRequestsBySender(sender: WalletAddress): List<WithTransactionData<AssetSendRequest>> {
        logger.debug { "Fetching asset send requests for sender: $sender" }
        return assetSendRequestRepository.getBySender(sender).map {
            val project = projectRepository.getById(it.projectId)!!
            it.appendTransactionData(project)
        }
    }

    override fun getAssetSendRequestsByRecipient(
        recipient: WalletAddress
    ): List<WithTransactionData<AssetSendRequest>> {
        logger.debug { "Fetching asset send requests for recipient: $recipient" }
        return assetSendRequestRepository.getByRecipient(recipient).map {
            val project = projectRepository.getById(it.projectId)!!
            it.appendTransactionData(project)
        }
    }

    override fun attachTxInfo(id: AssetSendRequestId, txHash: TransactionHash, caller: WalletAddress) {
        logger.info { "Attach txInfo to asset send request, id: $id, txHash: $txHash, caller: $caller" }

        val txInfoAttached = assetSendRequestRepository.setTxInfo(id, txHash, caller)

        if (txInfoAttached.not()) {
            throw CannotAttachTxInfoException("Unable to attach transaction info to asset send request with ID: $id")
        }
    }

    private fun encodeFunctionData(tokenRecipientAddress: WalletAddress, tokenAmount: Balance): FunctionData =
        functionEncoderService.encode(
            functionName = "transfer",
            arguments = listOf(
                FunctionArgument(tokenRecipientAddress),
                FunctionArgument(tokenAmount)
            )
        )

    private fun AssetSendRequest.appendTransactionData(project: Project): WithTransactionData<AssetSendRequest> {
        val transactionInfo = ethCommonService.fetchTransactionInfo(
            txHash = txHash,
            chainId = chainId,
            customRpcUrl = project.customRpcUrl,
            events = listOf(PredefinedEvents.ERC20_TRANSFER)
        )
        val data = tokenAddress?.let { encodeFunctionData(assetRecipientAddress, assetAmount) }
        val status = determineStatus(transactionInfo, data)

        return withTransactionData(
            status = status,
            data = data,
            value = if (tokenAddress == null) assetAmount else null,
            transactionInfo = transactionInfo
        )
    }

    private fun AssetSendRequest.determineStatus(
        transactionInfo: BlockchainTransactionInfo?,
        expectedData: FunctionData?
    ): Status =
        if (transactionInfo == null) { // implies that either txHash is null or transaction is not yet mined
            Status.PENDING
        } else if (isSuccess(transactionInfo, expectedData)) {
            Status.SUCCESS
        } else {
            Status.FAILED
        }

    private fun AssetSendRequest.isSuccess(
        transactionInfo: BlockchainTransactionInfo,
        expectedData: FunctionData?
    ): Boolean =
        transactionInfo.success &&
            transactionInfo.hashMatches(txHash) &&
            transactionInfo.fromAddressOptionallyMatches(assetSenderAddress) &&
            transactionInfo.toAddressMatches(tokenAddress ?: assetRecipientAddress) &&
            transactionInfo.deployedContractAddressIsNull() &&
            transactionInfo.dataMatches(expectedData ?: FunctionData.EMPTY) &&
            transactionInfo.valueMatches(if (tokenAddress == null) assetAmount else Balance.ZERO)
}
