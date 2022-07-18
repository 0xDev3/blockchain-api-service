package com.ampnet.blockchainapiservice.model.response

import com.ampnet.blockchainapiservice.model.result.ContractConstructor
import com.ampnet.blockchainapiservice.model.result.ContractDecorator
import com.ampnet.blockchainapiservice.model.result.ContractEvent
import com.ampnet.blockchainapiservice.model.result.ContractFunction

data class ContractDecoratorResponse(
    val id: String,
    val binary: String,
    val tags: List<String>,
    val implements: List<String>,
    val constructors: List<ContractConstructor>,
    val functions: List<ContractFunction>,
    val events: List<ContractEvent>
) {
    constructor(contractDecorator: ContractDecorator) : this(
        id = contractDecorator.id.value,
        binary = contractDecorator.binary.value,
        tags = contractDecorator.tags.map { it.value },
        implements = contractDecorator.implements.map { it.value },
        constructors = contractDecorator.constructors,
        functions = contractDecorator.functions,
        events = contractDecorator.events
    )
}
