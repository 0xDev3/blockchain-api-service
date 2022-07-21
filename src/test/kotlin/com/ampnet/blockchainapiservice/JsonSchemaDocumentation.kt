package com.ampnet.blockchainapiservice

import com.ampnet.blockchainapiservice.config.JsonConfig
import com.ampnet.blockchainapiservice.util.annotation.SchemaIgnore
import com.ampnet.blockchainapiservice.util.annotation.SchemaName
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.github.victools.jsonschema.generator.Option
import com.github.victools.jsonschema.generator.OptionPreset
import com.github.victools.jsonschema.generator.SchemaGenerator
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder
import com.github.victools.jsonschema.generator.SchemaVersion
import java.lang.reflect.Type
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

object JsonSchemaDocumentation {

    private val generator = SchemaGeneratorConfigBuilder(
        JsonConfig().objectMapper(),
        SchemaVersion.DRAFT_2020_12,
        OptionPreset.PLAIN_JSON
    ).apply {
        forFields().apply {
            withPropertyNameOverrideResolver { field ->
                val nameOverride = field.getAnnotation(SchemaName::class.java)?.name
                nameOverride ?: PropertyNamingStrategies.SnakeCaseStrategy().translate(field.name)
            }
            withNullableCheck { field ->
                Class.forName(field.declaringType.typeName)
                    .kotlin.members.find { it.name == field.declaredName }?.returnType?.isMarkedNullable ?: false
            }
            withIgnoreCheck { field ->
                field.getAnnotation(SchemaIgnore::class.java) != null
            }
        }
        with(
            Option.ADDITIONAL_FIXED_TYPES,
            Option.EXTRA_OPEN_API_FORMAT_VALUES
        )
    }.let { SchemaGenerator(it.build()) }

    fun createSchema(type: Type) {
        Files.createDirectories(Paths.get("build/generated-snippets"))
        Files.writeString(
            Paths.get("build/generated-snippets/${type.typeName}.adoc"),
            "[%collapsible]\n" +
                "====\n" +
                "[source,options=\"nowrap\"]\n" +
                "----\n" +
                "${generator.generateSchema(type).toPrettyString()}\n" +
                "----\n" +
                "====\n",
            StandardOpenOption.CREATE
        )
    }
}
