package com.ampnet.blockchainapiservice.exception

enum class ErrorCode {
    RESOURCE_NOT_FOUND,
    UNSUPPORTED_CHAIN_ID,
    BLOCKCHAIN_READ_ERROR,
    NON_EXISTENT_CLIENT_ID,
    INCOMPLETE_SEND_REQUEST,
    TX_HASH_ALREADY_SET
}
