package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.model.params.StoreErc20BalanceRequestParams
import com.ampnet.blockchainapiservice.model.result.Erc20BalanceRequest
import com.ampnet.blockchainapiservice.util.SignedMessage
import com.ampnet.blockchainapiservice.util.WalletAddress
import java.util.UUID

interface Erc20BalanceRequestRepository {
    fun store(params: StoreErc20BalanceRequestParams): Erc20BalanceRequest
    fun getById(id: UUID): Erc20BalanceRequest?
    fun getAllByProjectId(projectId: UUID): List<Erc20BalanceRequest>
    fun setSignedMessage(id: UUID, walletAddress: WalletAddress, signedMessage: SignedMessage): Boolean
}
