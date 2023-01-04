package dev3.blockchainapiservice.features.contract.importing.model.response

import dev3.blockchainapiservice.features.contract.deployment.model.json.ArtifactJson
import dev3.blockchainapiservice.features.contract.deployment.model.json.ManifestJson
import dev3.blockchainapiservice.features.contract.deployment.model.response.ContractDecoratorResponse
import dev3.blockchainapiservice.features.contract.deployment.model.result.ContractDecorator

data class ImportPreviewResponse(
    val manifest: ManifestJson,
    val artifact: ArtifactJson,
    val decorator: ContractDecoratorResponse
) {
    constructor(decorator: ContractDecorator) : this(
        manifest = decorator.manifest,
        artifact = decorator.artifact,
        decorator = ContractDecoratorResponse(decorator)
    )
}
