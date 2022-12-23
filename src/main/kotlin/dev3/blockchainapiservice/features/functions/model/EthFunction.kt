package dev3.blockchainapiservice.features.functions.model

import dev3.blockchainapiservice.util.FunctionArgument

data class EthFunction(val name: String, val arguments: List<FunctionArgument>?)
