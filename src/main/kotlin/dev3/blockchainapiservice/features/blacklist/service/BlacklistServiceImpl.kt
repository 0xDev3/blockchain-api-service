package dev3.blockchainapiservice.features.blacklist.service

import dev3.blockchainapiservice.config.AdminProperties
import dev3.blockchainapiservice.exception.AccessForbiddenException
import dev3.blockchainapiservice.features.api.access.model.result.UserIdentifier
import dev3.blockchainapiservice.features.api.access.model.result.UserWalletAddressIdentifier
import dev3.blockchainapiservice.features.blacklist.repository.BlacklistedAddressRepository
import dev3.blockchainapiservice.util.EthereumAddress
import dev3.blockchainapiservice.util.WalletAddress
import mu.KLogging
import org.springframework.stereotype.Service

@Service
class BlacklistServiceImpl(
    private val blacklistedAddressRepository: BlacklistedAddressRepository,
    private val adminProperties: AdminProperties
) : BlacklistService {

    companion object : KLogging()

    override fun addAddress(userIdentifier: UserIdentifier, address: EthereumAddress) {
        logger.info { "Add address to blacklist, userIdentifier: $userIdentifier, address: $address" }
        userIdentifier.checkIfAllowed("Current user is not allowed to add address to blacklist")
        blacklistedAddressRepository.addAddress(address)
    }

    override fun removeAddress(userIdentifier: UserIdentifier, address: EthereumAddress) {
        logger.info { "Remove address from blacklist, userIdentifier: $userIdentifier, address: $address" }
        userIdentifier.checkIfAllowed("Current user is not allowed to remove address from blacklist")
        blacklistedAddressRepository.removeAddress(address)
    }

    override fun listAddresses(userIdentifier: UserIdentifier): List<WalletAddress> {
        logger.debug { "List blacklisted addresses, userIdentifier: $userIdentifier" }
        userIdentifier.checkIfAllowed("Current user is not allowed to fetch list of blacklisted addresses")
        return blacklistedAddressRepository.listAddresses()
    }

    private fun UserIdentifier.checkIfAllowed(message: String) {
        if (this is UserWalletAddressIdentifier && walletAddress !in adminProperties.wallets) {
            throw AccessForbiddenException(message)
        }
    }
}
