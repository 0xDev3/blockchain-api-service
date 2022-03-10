package com.ampnet.blockchainapiservice.util

import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Uint
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
}

@JvmInline
value class ContractAddress private constructor(val value: Address) {
    companion object {
        operator fun invoke(value: Address) = ContractAddress(value.toString())
    }

    constructor(value: String) : this(Address(value.lowercase()))

    val rawValue: String
        get() = value.value
}

@JvmInline
value class Balance(val value: Uint) {
    constructor(value: BigInteger) : this(Uint(value))

    val rawValue: BigInteger
        get() = value.value
}

@JvmInline
value class ChainId(val value: Long)

@JvmInline
value class BlockNumber(val value: BigInteger)
