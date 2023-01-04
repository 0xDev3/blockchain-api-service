package dev3.blockchainapiservice.features.api.access.repository

import dev3.blockchainapiservice.features.api.access.model.result.UserIdentifier
import dev3.blockchainapiservice.features.api.access.model.result.UserWalletAddressIdentifier
import dev3.blockchainapiservice.generated.jooq.enums.UserIdentifierType
import dev3.blockchainapiservice.generated.jooq.id.UserId
import dev3.blockchainapiservice.util.WalletAddress

interface UserIdentifierRepository {
    fun store(userIdentifier: UserIdentifier): UserIdentifier
    fun getById(id: UserId): UserIdentifier?
    fun getByUserIdentifier(userIdentifier: String, identifierType: UserIdentifierType): UserIdentifier?

    fun getByWalletAddress(walletAddress: WalletAddress): UserWalletAddressIdentifier? =
        getByUserIdentifier(walletAddress.rawValue, UserIdentifierType.ETH_WALLET_ADDRESS)?.let {
            UserWalletAddressIdentifier(
                id = it.id,
                stripeClientId = it.stripeClientId,
                walletAddress = WalletAddress(it.userIdentifier)
            )
        }

    fun getByStripeClientId(stripeClientId: String): UserIdentifier?
    fun setStripeClientId(id: UserId, stripeClientId: String): Boolean
}
