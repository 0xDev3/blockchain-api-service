package dev3.blockchainapiservice.features.blacklist.service

import dev3.blockchainapiservice.features.api.access.model.result.UserIdentifier
import dev3.blockchainapiservice.util.EthereumAddress
import dev3.blockchainapiservice.util.WalletAddress

interface BlacklistService {
    fun addAddress(userIdentifier: UserIdentifier, address: EthereumAddress)
    fun removeAddress(userIdentifier: UserIdentifier, address: EthereumAddress)
    fun listAddresses(userIdentifier: UserIdentifier): List<WalletAddress>
}
