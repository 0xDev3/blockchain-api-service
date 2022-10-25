package dev3.blockchainapiservice.repository

import dev3.blockchainapiservice.generated.jooq.enums.UserIdentifierType
import dev3.blockchainapiservice.model.result.UserIdentifier
import dev3.blockchainapiservice.model.result.UserWalletAddressIdentifier
import dev3.blockchainapiservice.util.WalletAddress
import java.util.UUID

interface UserIdentifierRepository {
    fun store(userIdentifier: UserIdentifier): UserIdentifier
    fun getById(id: UUID): UserIdentifier?
    fun getByUserIdentifier(userIdentifier: String, identifierType: UserIdentifierType): UserIdentifier?

    fun getByWalletAddress(walletAddress: WalletAddress): UserWalletAddressIdentifier? =
        getByUserIdentifier(walletAddress.rawValue, UserIdentifierType.ETH_WALLET_ADDRESS)?.let {
            UserWalletAddressIdentifier(it.id, WalletAddress(it.userIdentifier))
        }
}
