package dev3.blockchainapiservice.model.result

import com.fasterxml.jackson.databind.JsonNode
import dev3.blockchainapiservice.generated.jooq.id.AssetBalanceRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.util.AccountBalance
import dev3.blockchainapiservice.util.BlockNumber
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.SignedMessage
import dev3.blockchainapiservice.util.Status
import dev3.blockchainapiservice.util.UtcDateTime
import dev3.blockchainapiservice.util.WalletAddress

data class FullAssetBalanceRequest(
    val id: AssetBalanceRequestId,
    val projectId: ProjectId,
    val status: Status,
    val chainId: ChainId,
    val redirectUrl: String,
    val tokenAddress: ContractAddress?,
    val blockNumber: BlockNumber?,
    val requestedWalletAddress: WalletAddress?,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig,
    val balance: AccountBalance?,
    val messageToSign: String,
    val signedMessage: SignedMessage?,
    val createdAt: UtcDateTime
) {
    companion object {
        fun fromAssetBalanceRequest(
            request: AssetBalanceRequest,
            status: Status,
            balance: AccountBalance?
        ) = FullAssetBalanceRequest(
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
