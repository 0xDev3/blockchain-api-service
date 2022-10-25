package dev3.blockchainapiservice.repository

import dev3.blockchainapiservice.model.params.StoreAuthorizationRequestParams
import dev3.blockchainapiservice.model.result.AuthorizationRequest
import dev3.blockchainapiservice.util.SignedMessage
import dev3.blockchainapiservice.util.WalletAddress
import java.util.UUID

interface AuthorizationRequestRepository {
    fun store(params: StoreAuthorizationRequestParams): AuthorizationRequest
    fun delete(id: UUID)
    fun getById(id: UUID): AuthorizationRequest?
    fun getAllByProjectId(projectId: UUID): List<AuthorizationRequest>
    fun setSignedMessage(id: UUID, walletAddress: WalletAddress, signedMessage: SignedMessage): Boolean
}
