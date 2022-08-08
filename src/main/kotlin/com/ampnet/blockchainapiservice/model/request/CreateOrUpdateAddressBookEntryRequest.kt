package com.ampnet.blockchainapiservice.model.request

import com.ampnet.blockchainapiservice.config.validation.MaxStringSize
import com.ampnet.blockchainapiservice.config.validation.ValidAlias
import javax.validation.constraints.NotNull

data class CreateOrUpdateAddressBookEntryRequest(
    @field:NotNull
    @field:ValidAlias
    val alias: String,
    @field:NotNull
    @field:MaxStringSize
    val address: String,
    @field:MaxStringSize
    val phoneNumber: String?,
    @field:MaxStringSize
    val email: String?
)
