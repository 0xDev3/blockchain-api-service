package dev3.blockchainapiservice.model.result

import dev3.blockchainapiservice.generated.jooq.enums.UserIdentifierType
import dev3.blockchainapiservice.generated.jooq.id.UserId
import dev3.blockchainapiservice.util.WalletAddress

sealed interface UserIdentifier {
    val id: UserId
    val userIdentifier: String
    val identifierType: UserIdentifierType
    val stripeClientId: String?
}

data class UserWalletAddressIdentifier(
    override val id: UserId,
    override val stripeClientId: String?,
    val walletAddress: WalletAddress
) : UserIdentifier {
    override val userIdentifier = walletAddress.rawValue
    override val identifierType = UserIdentifierType.ETH_WALLET_ADDRESS
}
