package com.ampnet.blockchainapiservice.model.json

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
data class ManifestJson(
    val description: String?,
    val tags: List<String>,
    val implements: List<String>,
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

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
data class EventDecorator(
    val signature: String,
    val name: String,
    val description: String,
    val parameterDecorators: List<TypeDecorator>
)

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
data class ConstructorDecorator(
    val signature: String,
    val description: String,
    val parameterDecorators: List<TypeDecorator>
)

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
data class FunctionDecorator(
    val signature: String,
    val name: String,
    val description: String,
    val parameterDecorators: List<TypeDecorator>,
    val returnDecorators: List<TypeDecorator>,
    val emittableEvents: List<String>
)
