package dev3.blockchainapiservice.model.result

import dev3.blockchainapiservice.generated.jooq.enums.UserIdentifierType
import dev3.blockchainapiservice.util.WalletAddress
import java.util.UUID

sealed interface UserIdentifier {
    val id: UUID
    val userIdentifier: String
    val identifierType: UserIdentifierType
}

data class UserWalletAddressIdentifier(
    override val id: UUID,
    val walletAddress: WalletAddress
) : UserIdentifier {
    override val userIdentifier = walletAddress.rawValue
    override val identifierType = UserIdentifierType.ETH_WALLET_ADDRESS
}
