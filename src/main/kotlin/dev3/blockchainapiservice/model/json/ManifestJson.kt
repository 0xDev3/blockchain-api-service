package dev3.blockchainapiservice.model.json

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonNaming
import dev3.blockchainapiservice.util.InterfaceId

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
data class ManifestJson(
    val name: String?,
    val description: String?,
    @JsonDeserialize(`as` = LinkedHashSet::class)
    val tags: Set<String>,
    @JsonDeserialize(`as` = LinkedHashSet::class)
    val implements: Set<String>,
    val eventDecorators: List<EventDecorator>,
    val constructorDecorators: List<ConstructorDecorator>,
    val functionDecorators: List<FunctionDecorator>
)

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
data class InterfaceManifestJson(
    val name: String?,
    val description: String?,
    @JsonDeserialize(`as` = LinkedHashSet::class)
    val tags: Set<String>,
    val eventDecorators: List<EventDecorator>,
    val functionDecorators: List<FunctionDecorator>
)

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
data class InterfaceManifestJsonWithId(
    val id: InterfaceId,
    val name: String?,
    val description: String?,
    val eventDecorators: List<EventDecorator>,
    val functionDecorators: List<FunctionDecorator>
)

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
data class TypeDecorator(
    val name: String,
    val description: String,
    val recommendedTypes: List<String>,
    val parameters: List<TypeDecorator>?
)

interface OverridableDecorator {
    val signature: String
}

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
data class EventDecorator(
    override val signature: String,
    val name: String,
    val description: String,
    val parameterDecorators: List<TypeDecorator>
) : OverridableDecorator

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
data class ConstructorDecorator(
    val signature: String,
    val description: String,
    val parameterDecorators: List<TypeDecorator>
)

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
data class FunctionDecorator(
    override val signature: String,
    val name: String,
    val description: String,
    val parameterDecorators: List<TypeDecorator>,
    val returnDecorators: List<TypeDecorator>,
    val emittableEvents: List<String>
) : OverridableDecorator
