package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.generated.jooq.enums.UserIdentifierType
import com.ampnet.blockchainapiservice.model.result.UserIdentifier
import com.ampnet.blockchainapiservice.model.result.UserWalletAddressIdentifier
import com.ampnet.blockchainapiservice.util.WalletAddress
import java.util.UUID

interface UserIdentifierRepository {
    fun getById(id: UUID): UserIdentifier?
    fun getByUserIdentifier(userIdentifier: String, identifierType: UserIdentifierType): UserIdentifier?
    fun create(userIdentifier: UserIdentifier): UserIdentifier

    fun getByWalletAddress(walletAddress: WalletAddress): UserWalletAddressIdentifier? =
        getByUserIdentifier(walletAddress.rawValue, UserIdentifierType.ETH_WALLET_ADDRESS)?.let {
            UserWalletAddressIdentifier(it.id, WalletAddress(it.userIdentifier))
        }
}
