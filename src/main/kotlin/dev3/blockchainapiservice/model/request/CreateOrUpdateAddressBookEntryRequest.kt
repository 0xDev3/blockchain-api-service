package dev3.blockchainapiservice.model.request

import dev3.blockchainapiservice.config.validation.MaxStringSize
import dev3.blockchainapiservice.config.validation.ValidAlias
import dev3.blockchainapiservice.config.validation.ValidEthAddress
import javax.validation.constraints.NotNull

data class CreateOrUpdateAddressBookEntryRequest(
    @field:NotNull
    @field:ValidAlias
    val alias: String,
    @field:NotNull
    @field:ValidEthAddress
    val address: String,
    @field:MaxStringSize
    val phoneNumber: String?,
    @field:MaxStringSize
    val email: String?
)
