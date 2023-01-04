package dev3.blockchainapiservice.features.functions.decoding.model

import dev3.blockchainapiservice.features.functions.encoding.model.FunctionArgument

data class EthFunction(val name: String, val arguments: List<FunctionArgument>?)
