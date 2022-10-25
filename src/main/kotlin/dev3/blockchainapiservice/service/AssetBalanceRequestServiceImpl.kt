package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.blockchain.BlockchainService
import dev3.blockchainapiservice.blockchain.properties.ChainSpec
import dev3.blockchainapiservice.exception.CannotAttachSignedMessageException
import dev3.blockchainapiservice.model.params.CreateAssetBalanceRequestParams
import dev3.blockchainapiservice.model.params.StoreAssetBalanceRequestParams
import dev3.blockchainapiservice.model.result.AssetBalanceRequest
import dev3.blockchainapiservice.model.result.FullAssetBalanceRequest
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.repository.AssetBalanceRequestRepository
import dev3.blockchainapiservice.repository.ProjectRepository
import dev3.blockchainapiservice.util.AccountBalance
import dev3.blockchainapiservice.util.BlockName
import dev3.blockchainapiservice.util.SignedMessage
import dev3.blockchainapiservice.util.Status
import dev3.blockchainapiservice.util.WalletAddress
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
