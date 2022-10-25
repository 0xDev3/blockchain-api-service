package dev3.blockchainapiservice.model.json

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
data class DecompiledContractJson(
    val manifest: ManifestJson,
    val artifact: ArtifactJson,
    val infoMarkdown: String?
)
