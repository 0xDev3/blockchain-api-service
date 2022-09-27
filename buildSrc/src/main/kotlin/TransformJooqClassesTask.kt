import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

abstract class TransformJooqClassesTask : DefaultTask() {

    companion object {
        private data class RecordClassInfo(val name: String, val properties: Map<String, RecordProperty>)

        private data class RecordProperty(val name: String, val type: String, val index: Int, val nonNull: Boolean) {
            val actualType = if (nonNull) type else "${type}?"
        }

        private const val PLACEHOLDER = "<< PLACEHOLDER >>"
        private val ALL_PLACEHOLDERS_REGEX = "($PLACEHOLDER\n)+".toRegex()
        private val RECORD_PROPERTY_REGEX = "[ ]+var[^\n]+\n[ ]+set[^\n]+\n([ ]+@NotNull\n)?[ ]+get[^\n]+\n".toRegex()
        private val RECORD_PROPERTY_VALUES_REGEX =
            "var ([^:]+): ([^\n]+)\n[ ]+set\\(value\\): Unit = set\\((\\d+), value\\)".toRegex()
        private val RECORD_CONSTRUCTOR_REGEX = "constructor\\([^)]+\\)".toRegex()
        private val TABLE_COLUMN_REGEX =
            "val ([^:]+): TableField<([^,]+), (Array<[^>]+>[^>]+|[^>]+)>( = createField[^\n]+)".toRegex()
        private val UPPERCASE_TO_CAMEL_CASE_REGEX = "_([a-z])".toRegex()
    }

    @get:Input
    abstract val jooqClassesPath: Property<String>

    @TaskAction
    fun transformJooqClasses() {
        val rootPath = Paths.get(jooqClassesPath.get())

        val recordInfos = transformRecordClasses(rootPath)

        transformTableClasses(rootPath, recordInfos)
    }

    private fun transformRecordClasses(rootPath: Path): Map<String, RecordClassInfo> {
        val path = rootPath.resolve("tables/records")

        return path.toFile().listFiles().filter { it.isFile }
            .map { transformRecordClass(it.toPath()) }
            .associateBy { it.name }
    }

    private fun transformRecordClass(path: Path): RecordClassInfo {
        val recordSource = Files.readString(path)

        val properties = RECORD_PROPERTY_REGEX.findAll(recordSource).map {
            val (_, propertyName, propertyType, index) = RECORD_PROPERTY_VALUES_REGEX.find(it.value)!!.groupValues
            val nonNull = it.value.contains("@NotNull")
            RecordProperty(propertyName, propertyType.replace("?", ""), index.toInt(), nonNull)
        }

        val propertiesSource = properties.joinToString("\n") {
            """|    var ${it.name}: ${it.actualType}
               |        private set(value): Unit = set(${it.index}, value)
               |        get(): ${it.actualType} = get(${it.index}) as ${it.actualType}
               |""".trimMargin()
        }
        val constructorProperties = properties.joinToString(prefix = "constructor(", separator = ", ", postfix = ")") {
            "${it.name}: ${it.actualType}"
        }

        val modifiedSource = recordSource
            .replace(RECORD_PROPERTY_REGEX, PLACEHOLDER)
            .replace(ALL_PLACEHOLDERS_REGEX, PLACEHOLDER)
            .replace(PLACEHOLDER, propertiesSource)
            .replace(RECORD_CONSTRUCTOR_REGEX, constructorProperties)

        Files.writeString(path, modifiedSource)

        return RecordClassInfo(path.fileName.toString().replace(".kt", ""), properties.associateBy { it.name })
    }

    private fun transformTableClasses(rootPath: Path, recordInfos: Map<String, RecordClassInfo>) {
        val path = rootPath.resolve("tables")

        path.toFile().listFiles().filter { it.isFile }
            .forEach { transformTableClass(it.toPath(), recordInfos) }
    }

    private fun transformTableClass(path: Path, recordInfos: Map<String, RecordClassInfo>) {
        val recordSource = Files.readString(path)

        val modifiedSource = TABLE_COLUMN_REGEX
            .replace(recordSource) {
                val (_, uppercaseName, recordName, _, rest) = it.groupValues
                val record = recordInfos[recordName]!!
                val camelCaseName = UPPERCASE_TO_CAMEL_CASE_REGEX
                    .replace(uppercaseName.toLowerCase()) { it.groupValues[1].toUpperCase() }
                    .removeSuffix("_")
                val field = record.properties[camelCaseName]!!

                "val $uppercaseName: TableField<$recordName, ${field.actualType}>$rest"
            }

        Files.writeString(path, modifiedSource)
    }
}
