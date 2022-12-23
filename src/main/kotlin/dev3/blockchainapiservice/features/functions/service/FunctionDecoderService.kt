package dev3.blockchainapiservice.features.functions.service

import dev3.blockchainapiservice.features.functions.model.EthFunction
import dev3.blockchainapiservice.util.FunctionData

interface FunctionDecoderService {
    fun decode(data: FunctionData): EthFunction?
}
