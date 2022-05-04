package com.ampnet.blockchainapiservice.model.request

data class AttachSignedMessageRequest(
    val walletAddress: String,
    val signedMessage: String
)
