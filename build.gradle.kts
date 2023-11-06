import io.gitlab.arturbosch.detekt.Detekt
import nu.studer.gradle.jooq.JooqGenerate
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jooq.meta.jaxb.ForcedType
import org.jooq.meta.jaxb.GeneratedSerialVersionUID
import org.jooq.meta.jaxb.MatcherRule
import org.jooq.meta.jaxb.MatcherTransformType
import org.jooq.meta.jaxb.Matchers
import org.jooq.meta.jaxb.MatchersTableType
import java.text.SimpleDateFormat
import java.util.Date

plugins {
    kotlin("jvm").version(Versions.Compile.kotlin)
    kotlin("plugin.spring").version(Versions.Compile.kotlin)

    id("org.jlleitschuh.gradle.ktlint").version(Versions.Plugins.ktlint)
    id("io.gitlab.arturbosch.detekt").version(Versions.Plugins.detekt)
    id("org.unbroken-dome.test-sets").version(Versions.Plugins.testSets)
    id("org.springframework.boot").version(Versions.Plugins.springBoot)
    id("io.spring.dependency-management").version(Versions.Plugins.springDependencyManagement)
    id("com.google.cloud.tools.jib").version(Versions.Plugins.jib)
    id("org.asciidoctor.jvm.convert").version(Versions.Plugins.asciiDoctor)
    id("org.flywaydb.flyway").version(Versions.Plugins.flyway)
    id("nu.studer.jooq").version(Versions.Plugins.jooq)
    id("application")

    idea
    jacoco
}

extensions.configure(KtlintExtension::class.java) {
    version.set(Versions.Tools.ktlint)
}

group = "dev3"
version = Versions.project
java.sourceCompatibility = Versions.Compile.sourceCompatibility
java.targetCompatibility = Versions.Compile.targetCompatibility

allprojects {
    repositories {
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

testSets {
    Configurations.Tests.testSets.forEach { create(it) }
}

kotlin {
    sourceSets["main"].apply {
        kotlin.srcDir("$buildDir/generated/sources/jooq/main/kotlin")
    }
}

jib {
    val dockerUsername: String = System.getenv("DOCKER_USERNAME") ?: "DOCKER_USERNAME"
    val dockerPassword: String = System.getenv("DOCKER_PASSWORD") ?: "DOCKER_PASSWORD"

    to {
        image = "ampnet/${rootProject.name}:$version"
        auth {
            username = dockerUsername
            password = dockerPassword
        }
        tags = setOf("latest")
    }
    container {
        creationTime = "USE_CURRENT_TIMESTAMP"
        mainClass = "dev3.blockchainapiservice.BlockchainApiServiceApplicationKt"
    }
    from {
        image = "${Configurations.Docker.baseImage}:${Configurations.Docker.tag}@${Configurations.Docker.digest}"
    }
}

val flywayMigration by configurations.creating

fun DependencyHandler.integTestImplementation(dependencyNotation: Any): Dependency? =
    add("integTestImplementation", dependencyNotation)

fun DependencyHandler.kaptIntegTest(dependencyNotation: Any): Dependency? =
    add("kaptIntegTest", dependencyNotation)

fun DependencyHandler.apiTestImplementation(dependencyNotation: Any): Dependency? =
    add("apiTestImplementation", dependencyNotation)

fun DependencyHandler.kaptApiTest(dependencyNotation: Any): Dependency? =
    add("kaptApiTest", dependencyNotation)

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.springframework.boot:spring-boot-devtools")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.flywaydb:flyway-core")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    runtimeOnly("ch.qos.logback:logback-classic")
    runtimeOnly("org.postgresql:postgresql")
    flywayMigration(Configurations.Database.driverDependency)
    jooqGenerator(Configurations.Database.driverDependency)
    jooqGenerator("jakarta.xml.bind:jakarta.xml.bind-api:3.0.1")

    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:${Versions.Plugins.detekt}")

    implementation("org.web3j:core:${Versions.Dependencies.web3j}")
    implementation("com.github.komputing:kethereum:${Versions.Dependencies.kethereum}")
    implementation("com.squareup.okhttp3:okhttp:${Versions.Dependencies.okHttp}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.Dependencies.kotlinCoroutines}")
    implementation("io.github.microutils:kotlin-logging-jvm:${Versions.Dependencies.kotlinLogging}")
    implementation("com.github.AMPnet:jwt:${Versions.Dependencies.jwt}")
    implementation("io.sentry:sentry-spring-boot-starter:${Versions.Dependencies.sentry}")
    implementation("com.facebook.business.sdk:facebook-java-business-sdk:${Versions.Dependencies.facebookSdk}")
    implementation("com.stripe:stripe-java:${Versions.Dependencies.stripe}")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.mockito.kotlin:mockito-kotlin:${Versions.Dependencies.mockitoKotlin}")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:${Versions.Dependencies.assertk}")
    testImplementation("org.testcontainers:testcontainers:${Versions.Dependencies.testContainers}")
    testImplementation("org.testcontainers:postgresql:${Versions.Dependencies.testContainers}")
    testImplementation("com.github.tomakehurst:wiremock:${Versions.Dependencies.wireMock}")
    testImplementation("com.github.victools:jsonschema-generator:${Versions.Dependencies.jsonSchemaGenerator}")
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

    integTestImplementation(sourceSets.test.get().output)

    apiTestImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
    apiTestImplementation("org.springframework.security:spring-security-test")
    apiTestImplementation(sourceSets.test.get().output)
}

flyway {
    url = Configurations.Database.url
    user = Configurations.Database.user
    password = Configurations.Database.password
    schemas = arrayOf(Configurations.Database.schema)
    configurations = arrayOf("flywayMigration")
}

jooq {
    configurations {
        create("main") {
            jooqConfiguration.apply {
                logging = org.jooq.meta.jaxb.Logging.WARN
                jdbc.apply {
                    driver = Configurations.Database.driverClass
                    url = Configurations.Database.url
                    user = Configurations.Database.user
                    password = Configurations.Database.password
                }

                generator.apply {
                    name = "org.jooq.codegen.KotlinGenerator"
                    database.apply {
                        inputSchema = Configurations.Database.schema
                        excludes = "flyway_schema_history"
                        forcedTypes = listOf(
                            ForcedType().apply {
                                userType = "dev3.blockchainapiservice.util.ChainId"
                                converter = "dev3.blockchainapiservice.util.ChainIdConverter"
                                includeExpression = "chain_id"
                                includeTypes = "BIGINT"
                            },
                            ForcedType().apply {
                                userType = "dev3.blockchainapiservice.util.ContractAddress"
                                converter = "dev3.blockchainapiservice.util.ContractAddressConverter"
                                includeExpression = "token_address|.*_contract_address|contract_address"
                                includeTypes = "VARCHAR"
                            },
                            ForcedType().apply {
                                userType = "dev3.blockchainapiservice.util.WalletAddress"
                                converter = "dev3.blockchainapiservice.util.WalletAddressConverter"
                                includeExpression = ".*_address"
                                includeTypes = "VARCHAR"
                            },
                            ForcedType().apply {
                                userType = "dev3.blockchainapiservice.util.Balance"
                                converter = "dev3.blockchainapiservice.util.BalanceConverter"
                                includeExpression = ".*_amount"
                                includeTypes = "NUMERIC"
                            },
                            ForcedType().apply {
                                userType = "dev3.blockchainapiservice.util.BlockNumber"
                                converter = "dev3.blockchainapiservice.util.BlockNumberConverter"
                                includeExpression = "block_number"
                                includeTypes = "NUMERIC"
                            },
                            ForcedType().apply {
                                userType = "dev3.blockchainapiservice.util.TransactionHash"
                                converter = "dev3.blockchainapiservice.util.TransactionHashConverter"
                                includeExpression = ".*tx_hash"
                                includeTypes = "VARCHAR"
                            },
                            ForcedType().apply {
                                userType = "dev3.blockchainapiservice.model.json.ManifestJson"
                                converter = "dev3.blockchainapiservice.util.ManifestJsonConverter"
                                includeExpression = "manifest_json"
                                includeTypes = "JSON"
                            },
                            ForcedType().apply {
                                userType = "dev3.blockchainapiservice.model.json.ArtifactJson"
                                converter = "dev3.blockchainapiservice.util.ArtifactJsonConverter"
                                includeExpression = "artifact_json"
                                includeTypes = "JSON"
                            },
                            ForcedType().apply {
                                userType = "dev3.blockchainapiservice.util.SignedMessage"
                                converter = "dev3.blockchainapiservice.util.SignedMessageConverter"
                                includeExpression = "signed_message"
                                includeTypes = "VARCHAR"
                            },
                            ForcedType().apply {
                                userType = "dev3.blockchainapiservice.util.DurationSeconds"
                                converter = "dev3.blockchainapiservice.util.DurationSecondsConverter"
                                includeExpression = ".*_duration_seconds"
                                includeTypes = "NUMERIC"
                            },
                            ForcedType().apply {
                                userType = "com.fasterxml.jackson.databind.JsonNode"
                                converter = "dev3.blockchainapiservice.util.JsonNodeConverter"
                                includeExpression = ".*"
                                includeTypes = "JSON"
                            },
                            ForcedType().apply {
                                userType = "dev3.blockchainapiservice.util.UtcDateTime"
                                converter = "dev3.blockchainapiservice.util.UtcDateTimeConverter"
                                includeExpression = ".*"
                                includeTypes = "TIMESTAMPTZ"
                            },
                            ForcedType().apply {
                                userType = "dev3.blockchainapiservice.util.BaseUrl"
                                converter = "dev3.blockchainapiservice.util.BaseUrlConverter"
                                includeExpression = "base_redirect_url"
                                includeTypes = "VARCHAR"
                            },
                            ForcedType().apply {
                                userType = "dev3.blockchainapiservice.util.ContractId"
                                converter = "dev3.blockchainapiservice.util.ContractIdConverter"
                                includeExpression = "contract_id"
                                includeTypes = "VARCHAR"
                            },
                            ForcedType().apply {
                                userType = "dev3.blockchainapiservice.util.ContractBinaryData"
                                converter = "dev3.blockchainapiservice.util.ContractBinaryDataConverter"
                                includeExpression = "contract_data"
                                includeTypes = "BYTEA"
                            },
                            ForcedType().apply {
                                userType = "dev3.blockchainapiservice.util.FunctionData"
                                converter = "dev3.blockchainapiservice.util.FunctionDataConverter"
                                includeExpression = "tx_data|function_data"
                                includeTypes = "BYTEA"
                            },
                            ForcedType().apply {
                                userType = "dev3.blockchainapiservice.features.payout.util.HashFunction"
                                converter = "dev3.blockchainapiservice.util.HashFunctionConverter"
                                includeExpression = ".*"
                                includeTypes = "HASH_FUNCTION"
                            },
                            ForcedType().apply {
                                userType = "dev3.blockchainapiservice.features.payout.util.SnapshotStatus"
                                converter = "dev3.blockchainapiservice.util.SnapshotStatusConverter"
                                includeExpression = ".*"
                                includeTypes = "SNAPSHOT_STATUS"
                            },
                            ForcedType().apply {
                                userType = "dev3.blockchainapiservice.features.payout.util.SnapshotFailureCause"
                                converter = "dev3.blockchainapiservice.util.SnapshotFailureCauseConverter"
                                includeExpression = ".*"
                                includeTypes = "SNAPSHOT_FAILURE_CAUSE"
                            }
                        )
                    }
                    generate.apply {
                        isDeprecated = false
                        isRecords = true
                        isImmutablePojos = false
                        isImmutableInterfaces = false
                        isFluentSetters = false
                        isIndexes = false
                        isGlobalObjectReferences = false
                        isRecordsImplementingRecordN = false
                        isKeys = false
                        isJavadoc = false
                        generatedSerialVersionUID = GeneratedSerialVersionUID.HASH
                        withNonnullAnnotation(true)
                        withNonnullAnnotationType("NotNull")
                    }
                    target.apply {
                        packageName = "dev3.blockchainapiservice.generated.jooq"
                        directory = "$buildDir/generated/sources/jooq/main/kotlin"
                    }
                    strategy.apply {
                        name = "org.jooq.codegen.DefaultGeneratorStrategy"
                        matchers = Matchers().apply {
                            tables = listOf(
                                MatchersTableType().apply {
                                    tableClass = MatcherRule().apply {
                                        transform = MatcherTransformType.PASCAL
                                        expression = "$0_table"
                                    }
                                    interfaceClass = MatcherRule().apply {
                                        transform = MatcherTransformType.PASCAL
                                        expression = "i_$0_record"
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

tasks.withType<JooqGenerate> {
    dependsOn(tasks["flywayMigrate"])
}

tasks.register<TransformJooqClassesTask>("transformJooqClasses") {
    jooqClassesPath.set("$buildDir/generated/sources/jooq/main/kotlin/dev3/blockchainapiservice/generated/jooq")
    dependsOn(tasks["generateJooq"])
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = Configurations.Compile.compilerArgs
        jvmTarget = Versions.Compile.jvmTarget
    }
    dependsOn.add(tasks["transformJooqClasses"])
}

tasks.withType<Test> {
    useJUnitPlatform()

    testLogging.events("failed")
    testLogging.exceptionFormat = TestExceptionFormat.FULL
}

task("fullTest") {
    val allTests = listOf(tasks.test.get()) + Configurations.Tests.testSets.map { tasks[it] }
    for (i in 0 until (allTests.size - 1)) {
        allTests[i + 1].mustRunAfter(allTests[i])
    }
    dependsOn(*allTests.toTypedArray())
}

jacoco.toolVersion = Versions.Tools.jacoco
tasks.withType<JacocoReport> {
    val allTestExecFiles = (listOf("test") + Configurations.Tests.testSets)
        .map { "$buildDir/jacoco/$it.exec" }
    executionData(*allTestExecFiles.toTypedArray())

    reports {
        xml.required.set(true)
        xml.outputLocation.set(file("$buildDir/reports/jacoco/report.xml"))
        csv.required.set(false)
        html.outputLocation.set(file("$buildDir/reports/jacoco/html"))
    }
    sourceDirectories.setFrom(listOf(file("${project.projectDir}/src/main/kotlin")))
    classDirectories.setFrom(
        fileTree("$buildDir/classes/kotlin/main").apply {
            exclude("dev3/blockchainapiservice/generated/**")
        }
    )
    dependsOn(tasks["fullTest"])
}

tasks.withType<JacocoCoverageVerification> {
    val allTestExecFiles = (listOf("test") + Configurations.Tests.testSets)
        .map { "$buildDir/jacoco/$it.exec" }
    executionData(*allTestExecFiles.toTypedArray())

    sourceDirectories.setFrom(listOf(file("${project.projectDir}/src/main/kotlin")))
    classDirectories.setFrom(
        fileTree("$buildDir/classes/kotlin/main").apply {
            exclude("dev3/blockchainapiservice/generated/**")
        }
    )

    violationRules {
        rule {
            limit {
                minimum = Configurations.Tests.minimumCoverage
            }
        }
    }
    mustRunAfter(tasks.jacocoTestReport)
}

detekt {
    source = files("src/main/kotlin")
    config = files("detekt-config.yml")
}

tasks.withType<Detekt> {
    exclude("dev3/blockchainapiservice/generated/**")
}

ktlint {
    filter {
        exclude("dev3/blockchainapiservice/generated/**")
    }
}

tasks.asciidoctor {
    attributes(
        mapOf(
            "snippets" to file("build/generated-snippets"),
            "version" to version,
            "date" to SimpleDateFormat("yyyy-MM-dd").format(Date())
        )
    )
    dependsOn(tasks["fullTest"])
}

tasks.register<Copy>("copyDocs") {
    from(
        file("$buildDir/docs/asciidoc/index.html"),
        file("$buildDir/docs/asciidoc/internal.html")
    )
    into(file("src/main/resources/static/docs"))
    dependsOn(tasks.asciidoctor)
}

task("qualityCheck") {
    dependsOn(tasks.ktlintCheck, tasks.detekt, tasks.jacocoTestReport, tasks.jacocoTestCoverageVerification)
}
