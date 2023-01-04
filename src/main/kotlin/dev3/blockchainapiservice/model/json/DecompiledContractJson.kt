package dev3.blockchainapiservice.model.json

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import dev3.blockchainapiservice.features.contract.deployment.model.json.ArtifactJson
import dev3.blockchainapiservice.features.contract.deployment.model.json.ManifestJson

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
data class DecompiledContractJson(
    val manifest: ManifestJson,
    val artifact: ArtifactJson,
    val infoMarkdown: String?
)
