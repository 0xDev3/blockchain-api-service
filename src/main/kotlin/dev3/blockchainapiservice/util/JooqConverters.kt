@file:Suppress("FunctionName", "TooManyFunctions")

package dev3.blockchainapiservice.util

import dev3.blockchainapiservice.config.JsonConfig
import dev3.blockchainapiservice.features.payout.util.HashFunction
import dev3.blockchainapiservice.features.payout.util.SnapshotFailureCause
import dev3.blockchainapiservice.features.payout.util.SnapshotStatus
import dev3.blockchainapiservice.model.json.ArtifactJson
import dev3.blockchainapiservice.model.json.ManifestJson
import org.jooq.Converter
import org.jooq.JSON
import java.math.BigInteger
import java.time.OffsetDateTime
import dev3.blockchainapiservice.generated.jooq.enums.HashFunction as DbHashFunction
import dev3.blockchainapiservice.generated.jooq.enums.SnapshotFailureCause as DbSnapshotFailureCause
import dev3.blockchainapiservice.generated.jooq.enums.SnapshotStatus as DbSnapshotStatus

private class KotlinConverter<T : Any, U : Any>(
    val from: Class<T>,
    val to: Class<U>,
    val fromMapping: (T) -> U?,
    val toMapping: (U) -> T?
) : Converter<T, U> {
    override fun from(value: T?): U? = value?.let { fromMapping(it) }
    override fun to(value: U?): T? = value?.let { toMapping(it) }
    override fun fromType(): Class<T> = from
    override fun toType(): Class<U> = to

    companion object {
        private const val serialVersionUID: Long = 7668375669351902507L
    }
}

private inline fun <reified T : Any, reified U : Any> converter(
    noinline fromMapping: (T) -> U?,
    noinline toMapping: (U) -> T?
): Converter<T, U> = KotlinConverter(
    from = T::class.java,
    to = U::class.java,
    fromMapping = fromMapping,
    toMapping = toMapping
)

private val objectMapper = JsonConfig().objectMapper()

fun ChainIdConverter() = converter({ it: Long -> ChainId(it) }, { it.value })

fun ContractAddressConverter() = converter({ it: String -> ContractAddress(it) }, { it.rawValue })

fun WalletAddressConverter() = converter({ it: String -> WalletAddress(it) }, { it.rawValue })

fun BalanceConverter() = converter({ it: BigInteger -> Balance(it) }, { it.rawValue })

fun BlockNumberConverter() = converter({ it: BigInteger -> BlockNumber(it) }, { it.value })

fun TransactionHashConverter() = converter({ it: String -> TransactionHash(it) }, { it.value })

fun SignedMessageConverter() = converter({ it: String -> SignedMessage(it) }, { it.value })

fun DurationSecondsConverter() = converter({ it: BigInteger -> DurationSeconds(it) }, { it.rawValue })

fun UtcDateTimeConverter() = converter({ it: OffsetDateTime -> UtcDateTime(it) }, { it.value })

fun BaseUrlConverter() = converter({ it: String -> BaseUrl(it) }, { it.value })

fun ContractIdConverter() = converter({ it: String -> ContractId(it) }, { it.value })

fun ContractBinaryDataConverter() = converter({ it: ByteArray -> ContractBinaryData(it) }, { it.binary })

fun FunctionDataConverter() = converter({ it: ByteArray -> FunctionData(it) }, { it.binary })

fun JsonNodeConverter() = converter(
    { it: JSON -> objectMapper.readTree(it.data()) },
    { JSON.valueOf(objectMapper.writeValueAsString(it)) }
)

fun ManifestJsonConverter() = converter(
    { it: JSON -> objectMapper.readValue(it.data(), ManifestJson::class.java) },
    { JSON.valueOf(objectMapper.writeValueAsString(it)) }
)

fun ArtifactJsonConverter() = converter(
    { it: JSON -> objectMapper.readValue(it.data(), ArtifactJson::class.java) },
    { JSON.valueOf(objectMapper.writeValueAsString(it)) }
)

fun HashFunctionConverter() = converter({ it: DbHashFunction -> HashFunction.fromDbEnum(it) }, { it.toDbEnum })

fun SnapshotStatusConverter() = converter({ it: DbSnapshotStatus -> SnapshotStatus.fromDbEnum(it) }, { it.toDbEnum })

fun SnapshotFailureCauseConverter() = converter(
    { it: DbSnapshotFailureCause -> SnapshotFailureCause.fromDbEnum(it) },
    { it.toDbEnum }
)
