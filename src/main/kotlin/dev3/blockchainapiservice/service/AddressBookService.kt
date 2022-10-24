package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.model.request.CreateOrUpdateAddressBookEntryRequest
import com.ampnet.blockchainapiservice.model.result.AddressBookEntry
import com.ampnet.blockchainapiservice.model.result.UserIdentifier
import com.ampnet.blockchainapiservice.util.WalletAddress
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
