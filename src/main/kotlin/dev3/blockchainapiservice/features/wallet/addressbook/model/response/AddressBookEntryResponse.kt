package dev3.blockchainapiservice.features.wallet.addressbook.model.response

import dev3.blockchainapiservice.features.wallet.addressbook.model.result.AddressBookEntry
import dev3.blockchainapiservice.generated.jooq.id.AddressBookId
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
