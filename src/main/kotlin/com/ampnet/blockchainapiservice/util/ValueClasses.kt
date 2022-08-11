package com.ampnet.blockchainapiservice.util

import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Uint
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.DefaultBlockParameterName
import java.math.BigInteger
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

@JvmInline
value class UtcDateTime private constructor(val value: OffsetDateTime) {
    companion object {
        private val ZONE_OFFSET = ZoneOffset.UTC
        operator fun invoke(value: OffsetDateTime) = UtcDateTime(value.withOffsetSameInstant(ZONE_OFFSET))
        fun ofEpochSeconds(value: Long) = UtcDateTime(
            OffsetDateTime.ofInstant(Instant.ofEpochSecond(value), ZONE_OFFSET)
        )
    }

    operator fun plus(duration: Duration): UtcDateTime = UtcDateTime(value + duration)
    operator fun minus(duration: Duration): UtcDateTime = UtcDateTime(value - duration)

    operator fun plus(duration: DurationSeconds): UtcDateTime = UtcDateTime(value + duration.toDuration())
    operator fun minus(duration: DurationSeconds): UtcDateTime = UtcDateTime(value - duration.toDuration())

    fun isAfter(other: UtcDateTime): Boolean = value.isAfter(other.value)
}

sealed interface EthereumAddress {
    val value: Address
    val rawValue: String
        get() = value.value

    fun toWalletAddress() = WalletAddress(value)
    fun toContractAddress() = ContractAddress(value)
}

object ZeroAddress : EthereumAddress {
    override val value: Address = Address("0")
}

@JvmInline
value class WalletAddress private constructor(override val value: Address) : EthereumAddress {
    companion object {
        operator fun invoke(value: Address) = WalletAddress(value.toString())
    }

    constructor(value: String) : this(Address(value.lowercase()))
}

@JvmInline
value class ContractAddress private constructor(override val value: Address) : EthereumAddress {
    companion object {
        operator fun invoke(value: Address) = ContractAddress(value.toString())
    }

    constructor(value: String) : this(Address(value.lowercase()))
}

sealed interface EthereumUint {
    val value: Uint
    val rawValue: BigInteger
        get() = value.value
}

@JvmInline
value class Balance(override val value: Uint) : EthereumUint {
    companion object {
        val ZERO = Balance(BigInteger.ZERO)
    }

    constructor(value: BigInteger) : this(Uint(value))
}

@JvmInline
value class DurationSeconds(override val value: Uint) : EthereumUint {
    constructor(value: BigInteger) : this(Uint(value))

    fun toDuration(): Duration = Duration.ofSeconds(rawValue.longValueExact())
}

@JvmInline
value class ChainId(val value: Long)

sealed interface BlockParameter {
    fun toWeb3Parameter(): DefaultBlockParameter
}

@JvmInline
value class BlockNumber(val value: BigInteger) : BlockParameter {
    override fun toWeb3Parameter() = DefaultBlockParameter.valueOf(value)
}

enum class BlockName(private val web3BlockName: DefaultBlockParameterName) : BlockParameter {
    EARLIEST(DefaultBlockParameterName.EARLIEST),
    LATEST(DefaultBlockParameterName.LATEST),
    PENDING(DefaultBlockParameterName.PENDING);

    override fun toWeb3Parameter() = web3BlockName
}

@JvmInline
value class FunctionData private constructor(val value: String) {
    companion object {
        val EMPTY = FunctionData("0x")
        operator fun invoke(value: String) = FunctionData("0x" + value.removePrefix("0x").lowercase())
    }

    val withoutPrefix
        get(): String = value.removePrefix("0x")
}

@JvmInline
value class TransactionHash private constructor(val value: String) {
    companion object {
        operator fun invoke(value: String) = TransactionHash("0x" + value.removePrefix("0x").lowercase())
    }
}

@JvmInline
value class SignedMessage private constructor(val value: String) {
    companion object {
        operator fun invoke(value: String) = SignedMessage(value.lowercase())
    }
}

@JvmInline
value class BaseUrl private constructor(val value: String) {
    companion object {
        operator fun invoke(value: String) = BaseUrl(
            if (value.endsWith('/')) value.dropLast(1) else value
        )
    }
}

@JvmInline
value class ContractId private constructor(val value: String) {
    companion object {
        operator fun invoke(value: String) = ContractId(value.replace('/', '.').lowercase())
    }
}

@JvmInline
value class ContractBinaryData private constructor(val value: String) {
    companion object {
        operator fun invoke(value: String) = ContractBinaryData(value.removePrefix("0x").lowercase())
    }

    constructor(binary: ByteArray) : this(String(binary))

    @Suppress("MagicNumber")
    val binary: ByteArray
        get() = value.toByteArray()

    val withPrefix: String
        get() = "0x$value"
}

@JvmInline
value class ContractTag private constructor(val value: String) {
    companion object {
        operator fun invoke(value: String) = ContractTag(value.replace('/', '.').lowercase())
    }
}

@JvmInline
value class ContractTrait private constructor(val value: String) {
    companion object {
        operator fun invoke(value: String) = ContractTrait(value.replace('/', '.').lowercase())
    }
}
