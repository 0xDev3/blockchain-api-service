package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.model.params.StoreAuthorizationRequestParams
import com.ampnet.blockchainapiservice.model.result.AuthorizationRequest
import com.ampnet.blockchainapiservice.util.SignedMessage
import com.ampnet.blockchainapiservice.util.WalletAddress
import java.util.UUID

interface AuthorizationRequestRepository {
    fun store(params: StoreAuthorizationRequestParams): AuthorizationRequest
    fun getById(id: UUID): AuthorizationRequest?
    fun getAllByProjectId(projectId: UUID): List<AuthorizationRequest>
    fun setSignedMessage(id: UUID, walletAddress: WalletAddress, signedMessage: SignedMessage): Boolean
}
