package com.ampnet.blockchainapiservice.model.result

import com.ampnet.blockchainapiservice.generated.jooq.enums.UserIdentifierType
import com.ampnet.blockchainapiservice.util.WalletAddress
import java.util.UUID

data class UserIdentifier(
    val id: UUID,
    val userIdentifier: String,
    val identifierType: UserIdentifierType
)

data class UserWalletAddressIdentifier(
    val id: UUID,
    val walletAddress: WalletAddress
)
