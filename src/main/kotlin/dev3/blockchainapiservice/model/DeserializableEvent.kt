package dev3.blockchainapiservice.model

import dev3.blockchainapiservice.util.AbiType

data class DeserializableEvent(
    val signature: String,
    val inputsOrder: List<String>,
    val indexedInputs: List<DeserializableEventInput>,
    val regularInputs: List<DeserializableEventInput>
) {
    val selector = signature.replace("tuple", "")
}

data class DeserializableEventInput(val name: String, val abiType: AbiType)
