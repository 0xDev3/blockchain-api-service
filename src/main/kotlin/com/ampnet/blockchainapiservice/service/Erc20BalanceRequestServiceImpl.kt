package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.blockchain.BlockchainService
import com.ampnet.blockchainapiservice.blockchain.properties.ChainSpec
import com.ampnet.blockchainapiservice.blockchain.properties.RpcUrlSpec
import com.ampnet.blockchainapiservice.exception.CannotAttachSignedMessageException
import com.ampnet.blockchainapiservice.model.params.CreateErc20BalanceRequestParams
import com.ampnet.blockchainapiservice.model.params.StoreErc20BalanceRequestParams
import com.ampnet.blockchainapiservice.model.result.Erc20BalanceRequest
import com.ampnet.blockchainapiservice.model.result.FullErc20BalanceRequest
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.repository.Erc20BalanceRequestRepository
import com.ampnet.blockchainapiservice.util.AccountBalance
import com.ampnet.blockchainapiservice.util.BlockName
import com.ampnet.blockchainapiservice.util.SignedMessage
import com.ampnet.blockchainapiservice.util.Status
import com.ampnet.blockchainapiservice.util.WalletAddress
import mu.KLogging
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class Erc20BalanceRequestServiceImpl(
    private val signatureCheckerService: SignatureCheckerService,
    private val blockchainService: BlockchainService,
    private val erc20BalanceRequestRepository: Erc20BalanceRequestRepository,
    private val erc20CommonService: Erc20CommonService
) : Erc20BalanceRequestService {

    companion object : KLogging()

    override fun createErc20BalanceRequest(
        params: CreateErc20BalanceRequestParams,
        project: Project
    ): Erc20BalanceRequest {
        logger.info { "Creating ERC20 balance request, params: $params, project: $project" }
        return erc20BalanceRequestRepository.store(
            erc20CommonService.createDatabaseParams(StoreErc20BalanceRequestParams, params, project)
        )
    }

    override fun getErc20BalanceRequest(id: UUID, rpcSpec: RpcUrlSpec): FullErc20BalanceRequest {
        logger.debug { "Fetching ERC20 balance request, id: $id, rpcSpec: $rpcSpec" }

        val erc20BalanceRequest = erc20CommonService.fetchResource(
            erc20BalanceRequestRepository.getById(id),
            "ERC20 balance check request not found for ID: $id"
        )

        return erc20BalanceRequest.appendBalanceData(rpcSpec)
    }

    override fun getErc20BalanceRequestsByProjectId(
        projectId: UUID,
        rpcSpec: RpcUrlSpec
    ): List<FullErc20BalanceRequest> {
        logger.debug { "Fetching ERC20 balance requests for projectId: $projectId, rpcSpec: $rpcSpec" }
        return erc20BalanceRequestRepository.getAllByProjectId(projectId).map { it.appendBalanceData(rpcSpec) }
    }

    override fun attachWalletAddressAndSignedMessage(
        id: UUID,
        walletAddress: WalletAddress,
        signedMessage: SignedMessage
    ) {
        logger.info {
            "Attach walletAddress and signedMessage to ERC20 balance request, id: $id, walletAddress: $walletAddress," +
                " signedMessage: $signedMessage"
        }

        val attached = erc20BalanceRequestRepository.setSignedMessage(id, walletAddress, signedMessage)

        if (attached.not()) {
            throw CannotAttachSignedMessageException(
                "Unable to attach signed message to ERC20 balance request with ID: $id"
            )
        }
    }

    private fun Erc20BalanceRequest.appendBalanceData(rpcSpec: RpcUrlSpec): FullErc20BalanceRequest {
        val balance = actualWalletAddress?.let { fetchBalance(it, rpcSpec) }
        val status = determineStatus(balance)

        return FullErc20BalanceRequest.fromErc20BalanceRequest(
            request = this,
            status = status,
            balance = balance
        )
    }

    private fun Erc20BalanceRequest.fetchBalance(walletAddress: WalletAddress, rpcSpec: RpcUrlSpec): AccountBalance {
        val chainSpec = ChainSpec(
            chainId = chainId,
            rpcSpec = rpcSpec
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

    private fun Erc20BalanceRequest.determineStatus(balance: AccountBalance?): Status =
        if (balance == null || this.signedMessage == null) {
            Status.PENDING
        } else if (isSuccess()) {
            Status.SUCCESS
        } else {
            Status.FAILED
        }

    private fun Erc20BalanceRequest.isSuccess(): Boolean {
        return walletAddressMatches(this.requestedWalletAddress, this.actualWalletAddress) &&
            signedMessage != null && actualWalletAddress != null &&
            signatureCheckerService.signatureMatches(messageToSign, signedMessage, actualWalletAddress)
    }

    private fun walletAddressMatches(requestedAddress: WalletAddress?, actualAddress: WalletAddress?): Boolean =
        requestedAddress == null || requestedAddress == actualAddress
}
