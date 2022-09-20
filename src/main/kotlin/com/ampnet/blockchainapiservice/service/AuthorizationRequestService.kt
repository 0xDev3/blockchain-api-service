package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.model.params.CreateAuthorizationRequestParams
import com.ampnet.blockchainapiservice.model.result.AuthorizationRequest
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.util.SignedMessage
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.ampnet.blockchainapiservice.util.WithStatus
import java.util.UUID

interface AuthorizationRequestService {
    fun createAuthorizationRequest(params: CreateAuthorizationRequestParams, project: Project): AuthorizationRequest
    fun getAuthorizationRequest(id: UUID): WithStatus<AuthorizationRequest>
    fun getAuthorizationRequestsByProjectId(projectId: UUID): List<WithStatus<AuthorizationRequest>>
    fun attachWalletAddressAndSignedMessage(id: UUID, walletAddress: WalletAddress, signedMessage: SignedMessage)
}
