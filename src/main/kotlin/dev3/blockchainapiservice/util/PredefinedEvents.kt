package dev3.blockchainapiservice.util

import dev3.blockchainapiservice.model.DeserializableEvent
import dev3.blockchainapiservice.model.DeserializableEventInput

object PredefinedEvents {
    val ERC20_TRANSFER = DeserializableEvent(
        signature = "Transfer(address,address,uint256)",
        inputsOrder = listOf("from", "to", "value"),
        indexedInputs = listOf(
            DeserializableEventInput("from", AddressType),
            DeserializableEventInput("to", AddressType)
        ),
        regularInputs = listOf(
            DeserializableEventInput("value", UintType)
        )
    )
    val ERC20_APPROVAL = DeserializableEvent(
        signature = "Approval(address,address,uint256)",
        inputsOrder = listOf("owner", "spender", "value"),
        indexedInputs = listOf(
            DeserializableEventInput("owner", AddressType),
            DeserializableEventInput("spender", AddressType)
        ),
        regularInputs = listOf(
            DeserializableEventInput("value", UintType)
        )
    )
}
