package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.model.result.AddressBookEntry
import java.util.UUID

interface AddressBookRepository {
    fun store(addressBookEntry: AddressBookEntry): AddressBookEntry
    fun update(addressBookEntry: AddressBookEntry): AddressBookEntry?
    fun delete(id: UUID): Boolean
    fun getById(id: UUID): AddressBookEntry?
    fun getByAliasAndProjectId(alias: String, projectId: UUID): AddressBookEntry?
    fun getAllByProjectId(projectId: UUID): List<AddressBookEntry>
}
