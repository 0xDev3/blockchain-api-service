package com.ampnet.blockchainapiservice.model.request

data class CreateOrUpdateAddressBookEntryRequest(
    val alias: String,
    val address: String,
    val phoneNumber: String?,
    val email: String?
)
