package dev3.blockchainapiservice.features.asset.balance.model.result

import com.fasterxml.jackson.databind.JsonNode
import dev3.blockchainapiservice.generated.jooq.id.AssetBalanceRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.util.BlockNumber
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.SignedMessage
import dev3.blockchainapiservice.util.UtcDateTime
import dev3.blockchainapiservice.util.WalletAddress

data class AssetBalanceRequest(
    val id: AssetBalanceRequestId,
    val projectId: ProjectId,
    val chainId: ChainId,
    val redirectUrl: String,
    val tokenAddress: ContractAddress?,
    val blockNumber: BlockNumber?,
    val requestedWalletAddress: WalletAddress?,
    val actualWalletAddress: WalletAddress?,
    val signedMessage: SignedMessage?,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig,
    val createdAt: UtcDateTime
) {
    val messageToSign: String
        get() = "Verification message ID to sign: ${id.value}"
}
