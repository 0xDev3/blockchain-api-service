package com.ampnet.blockchainapiservice.model.response

import com.ampnet.blockchainapiservice.model.json.EventDecorator
import com.ampnet.blockchainapiservice.model.json.FunctionDecorator
import com.ampnet.blockchainapiservice.model.json.InterfaceManifestJson
import com.ampnet.blockchainapiservice.model.json.PartiallyMatchingInterfaceManifest
import com.ampnet.blockchainapiservice.util.ContractId

data class ContractInterfaceManifestResponse(
    val id: String,
    val name: String?,
    val description: String?,
    val eventDecorators: List<EventDecorator>,
    val functionDecorators: List<FunctionDecorator>
) {
    constructor(manifest: PartiallyMatchingInterfaceManifest) : this(
        id = manifest.id.value,
        name = manifest.name,
        description = manifest.description,
        eventDecorators = manifest.eventDecorators,
        functionDecorators = manifest.functionDecorators
    )

    constructor(id: ContractId, manifest: InterfaceManifestJson) : this(
        id = id.value,
        name = manifest.name,
        description = manifest.description,
        eventDecorators = manifest.eventDecorators,
        functionDecorators = manifest.functionDecorators
    )
}
