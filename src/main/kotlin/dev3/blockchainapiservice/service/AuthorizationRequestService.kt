package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.generated.jooq.id.AuthorizationRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.model.params.CreateAuthorizationRequestParams
import dev3.blockchainapiservice.model.result.AuthorizationRequest
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.util.SignedMessage
import dev3.blockchainapiservice.util.WalletAddress
import dev3.blockchainapiservice.util.WithStatus

interface AuthorizationRequestService {
    fun createAuthorizationRequest(params: CreateAuthorizationRequestParams, project: Project): AuthorizationRequest
    fun getAuthorizationRequest(id: AuthorizationRequestId): WithStatus<AuthorizationRequest>
    fun getAuthorizationRequestsByProjectId(projectId: ProjectId): List<WithStatus<AuthorizationRequest>>
    fun attachWalletAddressAndSignedMessage(
        id: AuthorizationRequestId,
        walletAddress: WalletAddress,
        signedMessage: SignedMessage
    )
}
