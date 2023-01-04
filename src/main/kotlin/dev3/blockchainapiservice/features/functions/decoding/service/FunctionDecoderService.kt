package dev3.blockchainapiservice.features.functions.decoding.service

import dev3.blockchainapiservice.features.functions.decoding.model.EthFunction
import dev3.blockchainapiservice.util.FunctionData

interface FunctionDecoderService {
    fun decode(data: FunctionData): EthFunction?
}
