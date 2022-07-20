package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.model.request.CreateOrUpdateAddressBookEntryRequest
import com.ampnet.blockchainapiservice.model.result.AddressBookEntry
import com.ampnet.blockchainapiservice.model.result.Project
import java.util.UUID

interface AddressBookService {
    fun createAddressBookEntry(request: CreateOrUpdateAddressBookEntryRequest, project: Project): AddressBookEntry
    fun updateAddressBookEntry(
        addressBookEntryId: UUID,
        request: CreateOrUpdateAddressBookEntryRequest,
        project: Project
    ): AddressBookEntry

    fun deleteAddressBookEntryById(id: UUID)
    fun getAddressBookEntryById(id: UUID, project: Project): AddressBookEntry
    fun getAddressBookEntryByAlias(alias: String, project: Project): AddressBookEntry
    fun getAddressBookEntriesByProjectId(projectId: UUID): List<AddressBookEntry>
}
