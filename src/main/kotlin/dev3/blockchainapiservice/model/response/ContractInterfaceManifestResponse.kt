package dev3.blockchainapiservice.model.response

import dev3.blockchainapiservice.model.json.EventDecorator
import dev3.blockchainapiservice.model.json.FunctionDecorator
import dev3.blockchainapiservice.model.json.InterfaceManifestJson
import dev3.blockchainapiservice.model.json.InterfaceManifestJsonWithId
import dev3.blockchainapiservice.util.InterfaceId

data class ContractInterfaceManifestResponse(
    val id: String,
    val name: String?,
    val tags: List<String>,
    val description: String?,
    val eventDecorators: List<EventDecorator>,
    val functionDecorators: List<FunctionDecorator>
) {
    constructor(manifest: InterfaceManifestJsonWithId) : this(
        id = manifest.id.value,
        name = manifest.name,
        description = manifest.description,
        tags = manifest.tags.toList(),
        eventDecorators = manifest.eventDecorators,
        functionDecorators = manifest.functionDecorators
    )

    constructor(id: InterfaceId, manifest: InterfaceManifestJson) : this(
        id = id.value,
        name = manifest.name,
        description = manifest.description,
        tags = manifest.tags.toList(),
        eventDecorators = manifest.eventDecorators,
        functionDecorators = manifest.functionDecorators
    )
}
