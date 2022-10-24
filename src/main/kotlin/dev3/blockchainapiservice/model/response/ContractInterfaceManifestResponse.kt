package com.ampnet.blockchainapiservice.model.response

import com.ampnet.blockchainapiservice.model.json.EventDecorator
import com.ampnet.blockchainapiservice.model.json.FunctionDecorator
import com.ampnet.blockchainapiservice.model.json.InterfaceManifestJson
import com.ampnet.blockchainapiservice.model.json.InterfaceManifestJsonWithId
import com.ampnet.blockchainapiservice.util.InterfaceId

data class ContractInterfaceManifestResponse(
    val id: String,
    val name: String?,
    val description: String?,
    val eventDecorators: List<EventDecorator>,
    val functionDecorators: List<FunctionDecorator>
) {
    constructor(manifest: InterfaceManifestJsonWithId) : this(
        id = manifest.id.value,
        name = manifest.name,
        description = manifest.description,
        eventDecorators = manifest.eventDecorators,
        functionDecorators = manifest.functionDecorators
    )

    constructor(id: InterfaceId, manifest: InterfaceManifestJson) : this(
        id = id.value,
        name = manifest.name,
        description = manifest.description,
        eventDecorators = manifest.eventDecorators,
        functionDecorators = manifest.functionDecorators
    )
}
