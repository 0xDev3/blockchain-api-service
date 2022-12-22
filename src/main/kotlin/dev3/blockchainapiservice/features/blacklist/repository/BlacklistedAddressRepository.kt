package dev3.blockchainapiservice.features.blacklist.repository

import dev3.blockchainapiservice.util.EthereumAddress
import dev3.blockchainapiservice.util.WalletAddress

interface BlacklistedAddressRepository {
    fun addAddress(address: EthereumAddress)
    fun removeAddress(address: EthereumAddress)
    fun exists(address: EthereumAddress): Boolean
    fun listAddresses(): List<WalletAddress>
}
