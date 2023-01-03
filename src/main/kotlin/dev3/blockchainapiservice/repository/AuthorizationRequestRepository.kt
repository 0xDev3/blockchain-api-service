package dev3.blockchainapiservice.repository

import dev3.blockchainapiservice.generated.jooq.id.AuthorizationRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.model.params.StoreAuthorizationRequestParams
import dev3.blockchainapiservice.model.result.AuthorizationRequest
import dev3.blockchainapiservice.util.SignedMessage
import dev3.blockchainapiservice.util.WalletAddress

interface AuthorizationRequestRepository {
    fun store(params: StoreAuthorizationRequestParams): AuthorizationRequest
    fun delete(id: AuthorizationRequestId)
    fun getById(id: AuthorizationRequestId): AuthorizationRequest?
    fun getAllByProjectId(projectId: ProjectId): List<AuthorizationRequest>
    fun setSignedMessage(
        id: AuthorizationRequestId,
        walletAddress: WalletAddress,
        signedMessage: SignedMessage
    ): Boolean
}
