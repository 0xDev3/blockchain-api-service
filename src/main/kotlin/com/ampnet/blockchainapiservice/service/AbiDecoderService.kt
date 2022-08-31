package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.util.AbiType

interface AbiDecoderService {
    fun decode(types: List<AbiType>, encodedInput: String): List<Any>
}
