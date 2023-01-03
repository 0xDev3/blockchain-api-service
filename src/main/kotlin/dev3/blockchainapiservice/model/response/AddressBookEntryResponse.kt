package dev3.blockchainapiservice.model.response

import dev3.blockchainapiservice.generated.jooq.id.AddressBookId
import dev3.blockchainapiservice.model.result.AddressBookEntry
import java.time.OffsetDateTime

data class AddressBookEntryResponse(
    val id: AddressBookId,
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
