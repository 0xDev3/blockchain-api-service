package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.blockchain.properties.RpcUrlSpec
import com.ampnet.blockchainapiservice.model.params.CreateErc20BalanceRequestParams
import com.ampnet.blockchainapiservice.model.result.Erc20BalanceRequest
import com.ampnet.blockchainapiservice.model.result.FullErc20BalanceRequest
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.util.SignedMessage
import com.ampnet.blockchainapiservice.util.WalletAddress
import java.util.UUID

interface Erc20BalanceRequestService {
    fun createErc20BalanceRequest(params: CreateErc20BalanceRequestParams, project: Project): Erc20BalanceRequest
    fun getErc20BalanceRequest(id: UUID, rpcSpec: RpcUrlSpec): FullErc20BalanceRequest
    fun getErc20BalanceRequestsByProjectId(
        projectId: UUID,
        rpcSpec: RpcUrlSpec
    ): List<FullErc20BalanceRequest>

    fun attachWalletAddressAndSignedMessage(id: UUID, walletAddress: WalletAddress, signedMessage: SignedMessage)
}
