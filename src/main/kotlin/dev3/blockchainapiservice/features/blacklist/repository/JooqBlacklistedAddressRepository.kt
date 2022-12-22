package dev3.blockchainapiservice.features.blacklist.repository

import dev3.blockchainapiservice.generated.jooq.tables.BlacklistedAddressTable
import dev3.blockchainapiservice.generated.jooq.tables.records.BlacklistedAddressRecord
import dev3.blockchainapiservice.util.EthereumAddress
import dev3.blockchainapiservice.util.WalletAddress
import mu.KLogging
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class JooqBlacklistedAddressRepository(private val dslContext: DSLContext) : BlacklistedAddressRepository {

    companion object : KLogging()

    override fun addAddress(address: EthereumAddress) {
        logger.info { "Add address to blacklist: $address" }
        dslContext.insertInto(BlacklistedAddressTable)
            .set(BlacklistedAddressRecord(address.toWalletAddress()))
            .onConflictDoNothing()
            .execute()
    }

    override fun removeAddress(address: EthereumAddress) {
        logger.info { "Remove address from blacklist: $address" }
        dslContext.deleteFrom(BlacklistedAddressTable)
            .where(BlacklistedAddressTable.WALLET_ADDRESS.eq(address.toWalletAddress()))
            .execute()
    }

    override fun exists(address: EthereumAddress): Boolean {
        logger.debug { "Check if address is on blacklist: $address" }
        return dslContext.fetchExists(
            BlacklistedAddressTable,
            BlacklistedAddressTable.WALLET_ADDRESS.eq(address.toWalletAddress())
        )
    }

    override fun listAddresses(): List<WalletAddress> {
        logger.debug { "List blacklisted addresses" }
        return dslContext.selectFrom(BlacklistedAddressTable)
            .fetch { it.walletAddress }
    }
}
