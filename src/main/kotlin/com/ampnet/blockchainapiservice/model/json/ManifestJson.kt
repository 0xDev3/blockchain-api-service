package com.ampnet.blockchainapiservice.model.json

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
data class ManifestJson(
    val name: String?,
    val description: String?,
    val tags: List<String>,
    val implements: List<String>,
    val eventDecorators: List<EventDecorator>,
    val constructorDecorators: List<ConstructorDecorator>,
    val functionDecorators: List<FunctionDecorator>
)

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
data class InterfaceManifestJson(
    val eventDecorators: List<EventDecorator>,
    val constructorDecorators: List<ConstructorDecorator>,
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
    override val signature: String,
    val description: String,
    val parameterDecorators: List<TypeDecorator>
) : OverridableDecorator

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
data class FunctionDecorator(
    override val signature: String,
    val name: String,
    val description: String,
    val parameterDecorators: List<TypeDecorator>,
    val returnDecorators: List<TypeDecorator>,
    val emittableEvents: List<String>
) : OverridableDecorator
