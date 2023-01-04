package dev3.blockchainapiservice.features.wallet.addressbook.repository

import dev3.blockchainapiservice.features.wallet.addressbook.model.result.AddressBookEntry
import dev3.blockchainapiservice.generated.jooq.id.AddressBookId
import dev3.blockchainapiservice.generated.jooq.id.UserId
import dev3.blockchainapiservice.util.WalletAddress

interface AddressBookRepository {
    fun store(addressBookEntry: AddressBookEntry): AddressBookEntry
    fun update(addressBookEntry: AddressBookEntry): AddressBookEntry?
    fun delete(id: AddressBookId): Boolean
    fun getById(id: AddressBookId): AddressBookEntry?
    fun getByAliasAndUserId(alias: String, userId: UserId): AddressBookEntry?
    fun getAllByWalletAddress(walletAddress: WalletAddress): List<AddressBookEntry>
}
