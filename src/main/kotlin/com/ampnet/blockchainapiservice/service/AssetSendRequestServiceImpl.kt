package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.exception.CannotAttachTxInfoException
import com.ampnet.blockchainapiservice.model.params.CreateAssetSendRequestParams
import com.ampnet.blockchainapiservice.model.params.StoreAssetSendRequestParams
import com.ampnet.blockchainapiservice.model.result.AssetSendRequest
import com.ampnet.blockchainapiservice.model.result.BlockchainTransactionInfo
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.repository.AssetSendRequestRepository
import com.ampnet.blockchainapiservice.repository.ProjectRepository
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.FunctionArgument
import com.ampnet.blockchainapiservice.util.FunctionData
import com.ampnet.blockchainapiservice.util.PrimitiveAbiType
import com.ampnet.blockchainapiservice.util.Status
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.ampnet.blockchainapiservice.util.WithFunctionDataOrEthValue
import com.ampnet.blockchainapiservice.util.WithTransactionData
import mu.KLogging
import org.springframework.stereotype.Service
import java.util.UUID

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

    override fun getAssetSendRequest(id: UUID): WithTransactionData<AssetSendRequest> {
        logger.debug { "Fetching asset send request, id: $id" }

        val assetSendRequest = ethCommonService.fetchResource(
            assetSendRequestRepository.getById(id),
            "Asset send request not found for ID: $id"
        )
        val project = projectRepository.getById(assetSendRequest.projectId)!!

        return assetSendRequest.appendTransactionData(project)
    }

    override fun getAssetSendRequestsByProjectId(projectId: UUID): List<WithTransactionData<AssetSendRequest>> {
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

    override fun attachTxInfo(id: UUID, txHash: TransactionHash, caller: WalletAddress) {
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
            ),
            abiOutputTypes = listOf(PrimitiveAbiType.BOOL)
        )

    private fun AssetSendRequest.appendTransactionData(project: Project): WithTransactionData<AssetSendRequest> {
        val transactionInfo = ethCommonService.fetchTransactionInfo(
            txHash = txHash,
            chainId = chainId,
            customRpcUrl = project.customRpcUrl
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
            transactionInfo.tokenAddressMatches(tokenAddress) &&
            transactionInfo.recipientAddressMatches(tokenAddress, assetRecipientAddress) &&
            transactionInfo.senderAddressMatches(assetSenderAddress) &&
            transactionInfo.dataMatches(expectedData) &&
            transactionInfo.valueMatches(if (tokenAddress == null) assetAmount else null)

    private fun BlockchainTransactionInfo.tokenAddressMatches(tokenAddress: ContractAddress?): Boolean =
        (tokenAddress != null && to.toContractAddress() == tokenAddress) || tokenAddress == null

    private fun BlockchainTransactionInfo.recipientAddressMatches(
        tokenAddress: ContractAddress?,
        recipientAddress: WalletAddress
    ): Boolean = (tokenAddress == null && to.toWalletAddress() == recipientAddress) || tokenAddress != null

    private fun BlockchainTransactionInfo.hashMatches(expectedHash: TransactionHash?): Boolean =
        hash == expectedHash

    private fun BlockchainTransactionInfo.senderAddressMatches(senderAddress: WalletAddress?): Boolean =
        senderAddress == null || from == senderAddress

    private fun BlockchainTransactionInfo.dataMatches(expectedData: FunctionData?): Boolean =
        expectedData == null || data == expectedData

    private fun BlockchainTransactionInfo.valueMatches(expectedValue: Balance?): Boolean =
        expectedValue == null || value == expectedValue
}
