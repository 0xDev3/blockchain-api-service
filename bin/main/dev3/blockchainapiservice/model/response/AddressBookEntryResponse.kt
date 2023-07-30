package dev3.blockchainapiservice.model.response

import dev3.blockchainapiservice.model.result.AddressBookEntry
import java.time.OffsetDateTime
import java.util.UUID

data class AddressBookEntryResponse(
    val id: UUID,
    val alias: String,
    val address: String,
    val phoneNumber: String?,
    val email: String?,
    val createdAt: OffsetDateTime
) {
    constructor(entry: AddressBookEntry) : this(
        id = entry.id,
        alias = entry.alias,
        address = entry.address.rawValue,
        phoneNumber = entry.phoneNumber,
        email = entry.email,
        createdAt = entry.createdAt.value
    )
}
