package dev3.blockchainapiservice.model.result

import dev3.blockchainapiservice.model.request.CreateOrUpdateAddressBookEntryRequest
import dev3.blockchainapiservice.util.UtcDateTime
import dev3.blockchainapiservice.util.WalletAddress
import java.util.UUID

data class AddressBookEntry(
    val id: UUID,
    val alias: String,
    val address: WalletAddress,
    val phoneNumber: String?,
    val email: String?,
    val createdAt: UtcDateTime,
    val userId: UUID
) {
    constructor(
        id: UUID,
        createdAt: UtcDateTime,
        request: CreateOrUpdateAddressBookEntryRequest,
        userIdentifier: UserIdentifier
    ) : this(
        id = id,
        alias = request.alias,
        address = WalletAddress(request.address),
        phoneNumber = request.phoneNumber,
        email = request.email,
        createdAt = createdAt,
        userId = userIdentifier.id
    )
}
