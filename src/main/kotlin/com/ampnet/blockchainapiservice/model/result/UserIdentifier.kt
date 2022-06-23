package com.ampnet.blockchainapiservice.model.result

import com.ampnet.blockchainapiservice.generated.jooq.enums.UserIdentifierType
import com.ampnet.blockchainapiservice.util.WalletAddress
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
