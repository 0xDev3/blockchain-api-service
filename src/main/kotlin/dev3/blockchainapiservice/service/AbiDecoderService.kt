package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.util.AbiType

interface AbiDecoderService {
    fun decode(types: List<AbiType>, encodedInput: String): List<Any>
}
