package com.ampnet.blockchainapiservice.util

import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Uint
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.DefaultBlockParameterName
import java.math.BigInteger
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset

@JvmInline
value class UtcDateTime private constructor(val value: OffsetDateTime) {
    companion object {
        private val ZONE_OFFSET = ZoneOffset.UTC
        operator fun invoke(value: OffsetDateTime) = UtcDateTime(value.withOffsetSameInstant(ZONE_OFFSET))
    }

    operator fun plus(duration: Duration): UtcDateTime = UtcDateTime(value + duration)
    operator fun minus(duration: Duration): UtcDateTime = UtcDateTime(value - duration)

    fun isAfter(other: UtcDateTime): Boolean = value.isAfter(other.value)
}

@JvmInline
value class WalletAddress private constructor(val value: Address) {
    companion object {
        operator fun invoke(value: Address) = WalletAddress(value.toString())
    }

    constructor(value: String) : this(Address(value.lowercase()))

    val rawValue: String
        get() = value.value

    fun toContractAddress(): ContractAddress = ContractAddress(value)
}

@JvmInline
value class ContractAddress private constructor(val value: Address) {
    companion object {
        operator fun invoke(value: Address) = ContractAddress(value.toString())
    }

    constructor(value: String) : this(Address(value.lowercase()))

    val rawValue: String
        get() = value.value

    fun toWalletAddress(): WalletAddress = WalletAddress(value)
}

@JvmInline
value class Balance(val value: Uint) {
    constructor(value: BigInteger) : this(Uint(value))

    val rawValue: BigInteger
        get() = value.value
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
