package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.model.result.AddressBookEntry
import com.ampnet.blockchainapiservice.util.WalletAddress
import java.util.UUID

interface AddressBookRepository {
    fun store(addressBookEntry: AddressBookEntry): AddressBookEntry
    fun update(addressBookEntry: AddressBookEntry): AddressBookEntry?
    fun delete(id: UUID): Boolean
    fun getById(id: UUID): AddressBookEntry?
    fun getByAliasAndUserId(alias: String, userId: UUID): AddressBookEntry?
    fun getAllByWalletAddress(walletAddress: WalletAddress): List<AddressBookEntry>
}
