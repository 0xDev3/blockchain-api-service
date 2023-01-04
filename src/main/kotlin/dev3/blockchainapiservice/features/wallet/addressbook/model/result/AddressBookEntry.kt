package dev3.blockchainapiservice.features.wallet.addressbook.model.result

import dev3.blockchainapiservice.features.api.access.model.result.UserIdentifier
import dev3.blockchainapiservice.features.wallet.addressbook.model.request.CreateOrUpdateAddressBookEntryRequest
import dev3.blockchainapiservice.generated.jooq.id.AddressBookId
import dev3.blockchainapiservice.generated.jooq.id.UserId
import dev3.blockchainapiservice.util.UtcDateTime
import dev3.blockchainapiservice.util.WalletAddress

data class AddressBookEntry(
    val id: AddressBookId,
    val alias: String,
    val address: WalletAddress,
    val phoneNumber: String?,
    val email: String?,
    val createdAt: UtcDateTime,
    val userId: UserId
) {
    constructor(
        id: AddressBookId,
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
