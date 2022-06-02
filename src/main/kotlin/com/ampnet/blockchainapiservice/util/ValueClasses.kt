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

    operator fun plus(duration: DurationSeconds): UtcDateTime = UtcDateTime(
        value + Duration.ofSeconds(duration.rawValue.longValueExact())
    )

    operator fun minus(duration: DurationSeconds): UtcDateTime = UtcDateTime(
        value - Duration.ofSeconds(duration.rawValue.longValueExact())
    )

    fun isAfter(other: UtcDateTime): Boolean = value.isAfter(other.value)
}

sealed interface EthereumValue<T> {
    val rawValue: T
}

@JvmInline
value class EthereumString(val value: String) : EthereumValue<String> {
    override val rawValue: String
        get() = value
}

sealed interface EthereumAddress : EthereumValue<String> {
    val value: Address
    override val rawValue: String
        get() = value.value
}

@JvmInline
value class WalletAddress private constructor(override val value: Address) : EthereumAddress {
    companion object {
        operator fun invoke(value: Address) = WalletAddress(value.toString())
    }

    constructor(value: String) : this(Address(value.lowercase()))

    fun toContractAddress(): ContractAddress = ContractAddress(value)
}

@JvmInline
value class ContractAddress private constructor(override val value: Address) : EthereumAddress {
    companion object {
        operator fun invoke(value: Address) = ContractAddress(value.toString())
    }

    constructor(value: String) : this(Address(value.lowercase()))

    fun toWalletAddress(): WalletAddress = WalletAddress(value)
}

sealed interface EthereumUint : EthereumValue<BigInteger> {
    val value: Uint
    override val rawValue: BigInteger
        get() = value.value
}

@JvmInline
value class Balance(override val value: Uint) : EthereumUint {
    constructor(value: BigInteger) : this(Uint(value))
}

@JvmInline
value class DurationSeconds(override val value: Uint) : EthereumUint {
    constructor(value: BigInteger) : this(Uint(value))
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
        operator fun invoke(value: String) = FunctionData(value.lowercase())
    }
}

@JvmInline
value class TransactionHash private constructor(val value: String) {
    companion object {
        operator fun invoke(value: String) = TransactionHash(value.lowercase())
    }
}

@JvmInline
value class SignedMessage private constructor(val value: String) {
    companion object {
        operator fun invoke(value: String) = SignedMessage(value.lowercase())
    }
}
