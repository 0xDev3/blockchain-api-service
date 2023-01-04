package dev3.blockchainapiservice.features.contract.abi.service

import dev3.blockchainapiservice.features.contract.abi.model.AbiType

interface AbiDecoderService {
    fun decode(types: List<AbiType>, encodedInput: String): List<Any>
}
