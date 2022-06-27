package com.ampnet.blockchainapiservice.exception

enum class ErrorCode {
    RESOURCE_NOT_FOUND,
    UNSUPPORTED_CHAIN_ID,
    BLOCKCHAIN_READ_ERROR,
    TX_INFO_ALREADY_SET,
    SIGNED_MESSAGE_ALREADY_SET,
    BAD_AUTHENTICATION,
    API_KEY_ALREADY_EXISTS,
    NON_EXISTENT_API_KEY
}
