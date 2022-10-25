package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.model.request.CreateOrUpdateAddressBookEntryRequest
import dev3.blockchainapiservice.model.result.AddressBookEntry
import dev3.blockchainapiservice.model.result.UserIdentifier
import dev3.blockchainapiservice.util.WalletAddress
import java.util.UUID

interface AddressBookService {
    fun createAddressBookEntry(
        request: CreateOrUpdateAddressBookEntryRequest,
        userIdentifier: UserIdentifier
    ): AddressBookEntry

    fun updateAddressBookEntry(
        addressBookEntryId: UUID,
        request: CreateOrUpdateAddressBookEntryRequest,
        userIdentifier: UserIdentifier
    ): AddressBookEntry

    fun deleteAddressBookEntryById(id: UUID, userIdentifier: UserIdentifier)
    fun getAddressBookEntryById(id: UUID): AddressBookEntry
    fun getAddressBookEntryByAlias(alias: String, userIdentifier: UserIdentifier): AddressBookEntry
    fun getAddressBookEntriesByWalletAddress(walletAddress: WalletAddress): List<AddressBookEntry>
}
