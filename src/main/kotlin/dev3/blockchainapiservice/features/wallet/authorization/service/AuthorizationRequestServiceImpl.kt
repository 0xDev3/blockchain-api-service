package dev3.blockchainapiservice.features.wallet.authorization.service

import dev3.blockchainapiservice.exception.CannotAttachSignedMessageException
import dev3.blockchainapiservice.features.api.access.model.result.Project
import dev3.blockchainapiservice.features.wallet.authorization.model.params.CreateAuthorizationRequestParams
import dev3.blockchainapiservice.features.wallet.authorization.model.params.StoreAuthorizationRequestParams
import dev3.blockchainapiservice.features.wallet.authorization.model.result.AuthorizationRequest
import dev3.blockchainapiservice.features.wallet.authorization.repository.AuthorizationRequestRepository
import dev3.blockchainapiservice.generated.jooq.id.AuthorizationRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.service.EthCommonService
import dev3.blockchainapiservice.util.SignedMessage
import dev3.blockchainapiservice.util.Status
import dev3.blockchainapiservice.util.WalletAddress
import dev3.blockchainapiservice.util.WithStatus
import mu.KLogging
import org.springframework.stereotype.Service

@Service
class AuthorizationRequestServiceImpl(
    private val signatureCheckerService: SignatureCheckerService,
    private val authorizationRequestRepository: AuthorizationRequestRepository,
    private val ethCommonService: EthCommonService
) : AuthorizationRequestService {

    companion object : KLogging()

    override fun createAuthorizationRequest(
        params: CreateAuthorizationRequestParams,
        project: Project
    ): AuthorizationRequest {
        logger.info { "Creating authorization request, params: $params, project: $project" }
        return authorizationRequestRepository.store(
            ethCommonService.createDatabaseParams(StoreAuthorizationRequestParams, params, project)
        )
    }

    override fun getAuthorizationRequest(id: AuthorizationRequestId): WithStatus<AuthorizationRequest> {
        logger.debug { "Fetching authorization request, id: $id" }

        val authorizationRequest = ethCommonService.fetchResource(
            authorizationRequestRepository.getById(id),
            "Authorization request not found for ID: $id"
        )

        val withStatus = authorizationRequest.determineStatus()

        if (withStatus.status == Status.SUCCESS && withStatus.value.storeIndefinitely.not()) {
            logger.info { "Deleting read-once authorization request with. id: $id" }
            authorizationRequestRepository.delete(authorizationRequest.id)
        }

        return withStatus
    }

    override fun getAuthorizationRequestsByProjectId(projectId: ProjectId): List<WithStatus<AuthorizationRequest>> {
        logger.debug { "Fetching authorization requests for projectId: $projectId" }
        return authorizationRequestRepository.getAllByProjectId(projectId).map { it.determineStatus() }
    }

    override fun attachWalletAddressAndSignedMessage(
        id: AuthorizationRequestId,
        walletAddress: WalletAddress,
        signedMessage: SignedMessage
    ) {
        logger.info {
            "Attach walletAddress and signedMessage to authorization request, id: $id, walletAddress: $walletAddress," +
                " signedMessage: $signedMessage"
        }

        val attached = authorizationRequestRepository.setSignedMessage(id, walletAddress, signedMessage)

        if (attached.not()) {
            throw CannotAttachSignedMessageException(
                "Unable to attach signed message to authorization request with ID: $id"
            )
        }
    }

    private fun AuthorizationRequest.determineStatus(): WithStatus<AuthorizationRequest> =
        if (this.signedMessage == null) {
            WithStatus(this, Status.PENDING)
        } else if (isSuccess()) {
            WithStatus(this, Status.SUCCESS)
        } else {
            WithStatus(this, Status.FAILED)
        }

    private fun AuthorizationRequest.isSuccess(): Boolean {
        return walletAddressMatches(this.requestedWalletAddress, this.actualWalletAddress) &&
            signedMessage != null && actualWalletAddress != null &&
            signatureCheckerService.signatureMatches(messageToSign, signedMessage, actualWalletAddress)
    }

    private fun walletAddressMatches(requestedAddress: WalletAddress?, actualAddress: WalletAddress?): Boolean =
        requestedAddress == null || requestedAddress == actualAddress
}
