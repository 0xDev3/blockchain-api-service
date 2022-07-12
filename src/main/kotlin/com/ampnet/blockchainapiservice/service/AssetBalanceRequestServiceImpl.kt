package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.blockchain.BlockchainService
import com.ampnet.blockchainapiservice.blockchain.properties.ChainSpec
import com.ampnet.blockchainapiservice.exception.CannotAttachSignedMessageException
import com.ampnet.blockchainapiservice.model.params.CreateAssetBalanceRequestParams
import com.ampnet.blockchainapiservice.model.params.StoreAssetBalanceRequestParams
import com.ampnet.blockchainapiservice.model.result.AssetBalanceRequest
import com.ampnet.blockchainapiservice.model.result.FullAssetBalanceRequest
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.repository.AssetBalanceRequestRepository
import com.ampnet.blockchainapiservice.repository.ProjectRepository
import com.ampnet.blockchainapiservice.util.AccountBalance
import com.ampnet.blockchainapiservice.util.BlockName
import com.ampnet.blockchainapiservice.util.SignedMessage
import com.ampnet.blockchainapiservice.util.Status
import com.ampnet.blockchainapiservice.util.WalletAddress
import mu.KLogging
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AssetBalanceRequestServiceImpl(
    private val signatureCheckerService: SignatureCheckerService,
    private val blockchainService: BlockchainService,
    private val assetBalanceRequestRepository: AssetBalanceRequestRepository,
    private val ethCommonService: EthCommonService,
    private val projectRepository: ProjectRepository
) : AssetBalanceRequestService {

    companion object : KLogging()

    override fun createAssetBalanceRequest(
        params: CreateAssetBalanceRequestParams,
        project: Project
    ): AssetBalanceRequest {
        logger.info { "Creating asset balance request, params: $params, project: $project" }
        return assetBalanceRequestRepository.store(
            ethCommonService.createDatabaseParams(StoreAssetBalanceRequestParams, params, project)
        )
    }

    override fun getAssetBalanceRequest(id: UUID): FullAssetBalanceRequest {
        logger.debug { "Fetching asset balance request, id: $id" }

        val assetBalanceRequest = ethCommonService.fetchResource(
            assetBalanceRequestRepository.getById(id),
            "Asset balance check request not found for ID: $id"
        )
        val project = projectRepository.getById(assetBalanceRequest.projectId)!!

        return assetBalanceRequest.appendBalanceData(project)
    }

    override fun getAssetBalanceRequestsByProjectId(projectId: UUID): List<FullAssetBalanceRequest> {
        logger.debug { "Fetching asset balance requests for projectId: $projectId" }
        return projectRepository.getById(projectId)?.let {
            assetBalanceRequestRepository.getAllByProjectId(projectId).map { req -> req.appendBalanceData(it) }
        } ?: emptyList()
    }

    override fun attachWalletAddressAndSignedMessage(
        id: UUID,
        walletAddress: WalletAddress,
        signedMessage: SignedMessage
    ) {
        logger.info {
            "Attach walletAddress and signedMessage to asset balance request, id: $id, walletAddress: $walletAddress," +
                " signedMessage: $signedMessage"
        }

        val attached = assetBalanceRequestRepository.setSignedMessage(id, walletAddress, signedMessage)

        if (attached.not()) {
            throw CannotAttachSignedMessageException(
                "Unable to attach signed message to asset balance request with ID: $id"
            )
        }
    }

    private fun AssetBalanceRequest.appendBalanceData(project: Project): FullAssetBalanceRequest {
        val balance = actualWalletAddress?.let { fetchBalance(it, project) }
        val status = determineStatus(balance)

        return FullAssetBalanceRequest.fromAssetBalanceRequest(
            request = this,
            status = status,
            balance = balance
        )
    }

    private fun AssetBalanceRequest.fetchBalance(walletAddress: WalletAddress, project: Project): AccountBalance {
        val chainSpec = ChainSpec(
            chainId = chainId,
            customRpcUrl = project.customRpcUrl
        )

        return if (tokenAddress != null) {
            blockchainService.fetchErc20AccountBalance(
                chainSpec = chainSpec,
                contractAddress = tokenAddress,
                walletAddress = walletAddress,
                blockParameter = blockNumber ?: BlockName.LATEST
            )
        } else {
            blockchainService.fetchAccountBalance(
                chainSpec = chainSpec,
                walletAddress = walletAddress,
                blockParameter = blockNumber ?: BlockName.LATEST
            )
        }
    }

    private fun AssetBalanceRequest.determineStatus(balance: AccountBalance?): Status =
        if (balance == null || this.signedMessage == null) {
            Status.PENDING
        } else if (isSuccess()) {
            Status.SUCCESS
        } else {
            Status.FAILED
        }

    private fun AssetBalanceRequest.isSuccess(): Boolean {
        return walletAddressMatches(this.requestedWalletAddress, this.actualWalletAddress) &&
            signedMessage != null && actualWalletAddress != null &&
            signatureCheckerService.signatureMatches(messageToSign, signedMessage, actualWalletAddress)
    }

    private fun walletAddressMatches(requestedAddress: WalletAddress?, actualAddress: WalletAddress?): Boolean =
        requestedAddress == null || requestedAddress == actualAddress
}
