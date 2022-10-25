package dev3.blockchainapiservice.model.response

import dev3.blockchainapiservice.model.result.ContractConstructor
import dev3.blockchainapiservice.model.result.ContractDecorator
import dev3.blockchainapiservice.model.result.ContractEvent
import dev3.blockchainapiservice.model.result.ContractFunction

data class ContractDecoratorResponse(
    val id: String,
    val name: String?,
    val description: String?,
    val binary: String,
    val tags: List<String>,
    val implements: List<String>,
    val constructors: List<ContractConstructor>,
    val functions: List<ContractFunction>,
    val events: List<ContractEvent>
) {
    constructor(contractDecorator: ContractDecorator) : this(
        id = contractDecorator.id.value,
        name = contractDecorator.name,
        description = contractDecorator.description,
        binary = contractDecorator.binary.value,
        tags = contractDecorator.tags.map { it.value },
        implements = contractDecorator.implements.map { it.value },
        constructors = contractDecorator.constructors,
        functions = contractDecorator.functions,
        events = contractDecorator.events
    )
}
