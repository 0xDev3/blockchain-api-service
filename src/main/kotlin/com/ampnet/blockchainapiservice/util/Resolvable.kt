package com.ampnet.blockchainapiservice.util

import com.ampnet.blockchainapiservice.exception.IncompleteRequestException

data class Resolvable<T>(val alternative: T?, val message: String) {
    fun resolve(value: T?): T = value ?: alternative ?: throw IncompleteRequestException(message)
}
