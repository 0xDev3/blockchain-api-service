package com.ampnet.blockchainapiservice.model.result

import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.util.BlockNumber
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.Erc20Balance
import com.ampnet.blockchainapiservice.util.SignedMessage
import com.ampnet.blockchainapiservice.util.Status
import com.ampnet.blockchainapiservice.util.UtcDateTime
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID

data class FullErc20BalanceRequest(
    val id: UUID,
    val projectId: UUID,
    val status: Status,
    val chainId: ChainId,
    val redirectUrl: String,
    val tokenAddress: ContractAddress,
    val blockNumber: BlockNumber?,
    val requestedWalletAddress: WalletAddress?,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig,
    val balance: Erc20Balance?,
    val messageToSign: String,
    val signedMessage: SignedMessage?,
    val createdAt: UtcDateTime
) {
    companion object {
        fun fromErc20BalanceRequest(
            request: Erc20BalanceRequest,
            status: Status,
            balance: Erc20Balance?
        ) = FullErc20BalanceRequest(
            id = request.id,
            projectId = request.projectId,
            status = status,
            chainId = request.chainId,
            redirectUrl = request.redirectUrl,
            tokenAddress = request.tokenAddress,
            blockNumber = request.blockNumber,
            requestedWalletAddress = request.requestedWalletAddress,
            arbitraryData = request.arbitraryData,
            screenConfig = request.screenConfig,
            balance = balance,
            messageToSign = request.messageToSign,
            signedMessage = request.signedMessage,
            createdAt = request.createdAt
        )
    }
}
