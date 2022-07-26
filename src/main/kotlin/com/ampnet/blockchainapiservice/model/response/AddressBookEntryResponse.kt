package com.ampnet.blockchainapiservice.model.response

import com.ampnet.blockchainapiservice.model.result.AddressBookEntry
import java.time.OffsetDateTime
import java.util.UUID

data class AddressBookEntryResponse(
    val id: UUID,
    val alias: String,
    val address: String,
    val phoneNumber: String?,
    val email: String?,
    val createdAt: OffsetDateTime,
    val projectId: UUID
) {
    constructor(entry: AddressBookEntry) : this(
        id = entry.id,
        alias = entry.alias,
        address = entry.address.rawValue,
        phoneNumber = entry.phoneNumber,
        email = entry.email,
        createdAt = entry.createdAt.value,
        projectId = entry.projectId
    )
}
