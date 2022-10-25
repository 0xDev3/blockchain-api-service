package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.model.params.CreateAuthorizationRequestParams
import dev3.blockchainapiservice.model.result.AuthorizationRequest
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.util.SignedMessage
import dev3.blockchainapiservice.util.WalletAddress
import dev3.blockchainapiservice.util.WithStatus
import java.util.UUID

interface AuthorizationRequestService {
    fun createAuthorizationRequest(params: CreateAuthorizationRequestParams, project: Project): AuthorizationRequest
    fun getAuthorizationRequest(id: UUID): WithStatus<AuthorizationRequest>
    fun getAuthorizationRequestsByProjectId(projectId: UUID): List<WithStatus<AuthorizationRequest>>
    fun attachWalletAddressAndSignedMessage(id: UUID, walletAddress: WalletAddress, signedMessage: SignedMessage)
}
