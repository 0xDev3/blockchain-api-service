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
import com.ampnet.blockchainapiservice.util.BlockName
import com.ampnet.blockchainapiservice.util.Erc20Balance
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

        val balance = erc20BalanceRequest.actualWalletAddress?.let {
            blockchainService.fetchErc20AccountBalance(
                chainSpec = ChainSpec(
                    chainId = erc20BalanceRequest.chainId,
                    rpcSpec = rpcSpec
                ),
                contractAddress = erc20BalanceRequest.tokenAddress,
                walletAddress = it,
                blockParameter = erc20BalanceRequest.blockNumber ?: BlockName.LATEST
            )
        }
        val status = erc20BalanceRequest.determineStatus(balance)

        return FullErc20BalanceRequest.fromErc20BalanceRequest(
            request = erc20BalanceRequest,
            status = status,
            balance = balance
        )
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

    private fun Erc20BalanceRequest.determineStatus(balance: Erc20Balance?): Status =
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
