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

        private data class TableClassInfo(val className: String, val valueName: String)

        private const val PLACEHOLDER = "<< PLACEHOLDER >>"
        private val ALL_PLACEHOLDERS_REGEX = "($PLACEHOLDER\n)+".toRegex()
        private val RECORD_PROPERTY_REGEX = "[ ]+var[^\n]+\n[ ]+set[^\n]+\n([ ]+@NotNull\n)?[ ]+get[^\n]+\n".toRegex()
        private val RECORD_PROPERTY_VALUES_REGEX =
            "var ([^:]+): ([^\n]+)\n[ ]+set\\(value\\): Unit = set\\((\\d+), value\\)".toRegex()
        private val RECORD_CONSTRUCTOR_REGEX = "constructor\\([^)]+\\)".toRegex()
        private val TABLE_COLUMN_REGEX =
            "val ([^:]+): TableField<([^,]+), (Array<[^>]+>[^>]+|[^>]+)>( = createField[^\n]+)".toRegex()
        private val UPPERCASE_TO_CAMEL_CASE_REGEX = "_([a-z])".toRegex()
        private val COMPANION_OBJECT_REGEX = "companion object \\{\n\n[ ]+val ([^:]+):[^\n]+\n[ ]+}".toRegex()
    }

    @get:Input
    abstract val jooqClassesPath: Property<String>

    @TaskAction
    fun transformJooqClasses() {
        val rootPath = Paths.get(jooqClassesPath.get())

        val recordInfos = transformRecordClasses(rootPath)
        val tableInfos = transformTableClasses(rootPath, recordInfos)

        renameTableReferences(rootPath, tableInfos)
    }

    private fun transformRecordClasses(rootPath: Path): Map<String, RecordClassInfo> =
        rootPath.resolve("tables/records")
            .toFile().listFiles().filter { it.isFile }
            .map { transformRecordClass(it.toPath()) }
            .associateBy { it.name }

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

        return RecordClassInfo(path.fileName.toString().removeSuffix(".kt"), properties.associateBy { it.name })
    }

    private fun transformTableClasses(rootPath: Path, recordInfos: Map<String, RecordClassInfo>): List<TableClassInfo> =
        rootPath.resolve("tables")
            .toFile().listFiles().filter { it.isFile }
            .map { transformTableClass(it.toPath(), recordInfos) }

    private fun transformTableClass(path: Path, recordInfos: Map<String, RecordClassInfo>): TableClassInfo {
        val recordSource = Files.readString(path)
        val tableClassName = path.fileName.toString().removeSuffix(".kt")
        var tableValueName = ""

        val modifiedSource = recordSource
            .replace(TABLE_COLUMN_REGEX) {
                val (_, uppercaseName, recordName, _, rest) = it.groupValues
                val record = recordInfos[recordName]!!
                val camelCaseName = UPPERCASE_TO_CAMEL_CASE_REGEX
                    .replace(uppercaseName.toLowerCase()) { it.groupValues[1].toUpperCase() }
                    .removeSuffix("_")
                val field = record.properties[camelCaseName]!!

                "val $uppercaseName: TableField<$recordName, ${field.actualType}>$rest"
            }
            .replace(COMPANION_OBJECT_REGEX) {
                val (_, valueName) = it.groupValues
                tableValueName = valueName
                "companion object : $tableClassName()"
            }

        Files.writeString(path, modifiedSource)

        return TableClassInfo(tableClassName, tableValueName)
    }

    private fun renameTableReferences(rootPath: Path, tableInfos: List<TableClassInfo>) {
        val replacements = tableInfos.map { Pair("${it.className}.${it.valueName}", it.className) }
        val recordFiles = rootPath.resolve("tables/records").toFile().listFiles().asSequence()
        val tableFiles = rootPath.resolve("tables").toFile().listFiles().asSequence()
        val baseFiles = rootPath.toFile().listFiles().asSequence()

        sequenceOf(recordFiles, tableFiles, baseFiles)
            .flatMap { it }.filter { it.isFile }
            .map { it.toPath() }
            .forEach { path ->
                val source = Files.readString(path)
                val modifiedSource = replacements.fold(source) { acc, pair -> acc.replace(pair.first, pair.second) }
                Files.writeString(path, modifiedSource)
            }
    }
}
