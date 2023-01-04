package dev3.blockchainapiservice.features.wallet.addressbook.service

import dev3.blockchainapiservice.features.api.access.model.result.UserIdentifier
import dev3.blockchainapiservice.features.wallet.addressbook.model.request.CreateOrUpdateAddressBookEntryRequest
import dev3.blockchainapiservice.features.wallet.addressbook.model.result.AddressBookEntry
import dev3.blockchainapiservice.generated.jooq.id.AddressBookId
import dev3.blockchainapiservice.util.WalletAddress

interface AddressBookService {
    fun createAddressBookEntry(
        request: CreateOrUpdateAddressBookEntryRequest,
        userIdentifier: UserIdentifier
    ): AddressBookEntry

    fun updateAddressBookEntry(
        addressBookEntryId: AddressBookId,
        request: CreateOrUpdateAddressBookEntryRequest,
        userIdentifier: UserIdentifier
    ): AddressBookEntry

    fun deleteAddressBookEntryById(id: AddressBookId, userIdentifier: UserIdentifier)
    fun getAddressBookEntryById(id: AddressBookId): AddressBookEntry
    fun getAddressBookEntryByAlias(alias: String, userIdentifier: UserIdentifier): AddressBookEntry
    fun getAddressBookEntriesByWalletAddress(walletAddress: WalletAddress): List<AddressBookEntry>
}
