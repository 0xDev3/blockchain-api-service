package com.ampnet.blockchainapiservice.util.json

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.JsonNode
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Bool
import org.web3j.abi.datatypes.DynamicBytes
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.Uint
import org.web3j.abi.datatypes.Utf8String
import org.web3j.abi.datatypes.generated.Bytes1
import org.web3j.abi.datatypes.generated.Bytes10
import org.web3j.abi.datatypes.generated.Bytes11
import org.web3j.abi.datatypes.generated.Bytes12
import org.web3j.abi.datatypes.generated.Bytes13
import org.web3j.abi.datatypes.generated.Bytes14
import org.web3j.abi.datatypes.generated.Bytes15
import org.web3j.abi.datatypes.generated.Bytes16
import org.web3j.abi.datatypes.generated.Bytes17
import org.web3j.abi.datatypes.generated.Bytes18
import org.web3j.abi.datatypes.generated.Bytes19
import org.web3j.abi.datatypes.generated.Bytes2
import org.web3j.abi.datatypes.generated.Bytes20
import org.web3j.abi.datatypes.generated.Bytes21
import org.web3j.abi.datatypes.generated.Bytes22
import org.web3j.abi.datatypes.generated.Bytes23
import org.web3j.abi.datatypes.generated.Bytes24
import org.web3j.abi.datatypes.generated.Bytes25
import org.web3j.abi.datatypes.generated.Bytes26
import org.web3j.abi.datatypes.generated.Bytes27
import org.web3j.abi.datatypes.generated.Bytes28
import org.web3j.abi.datatypes.generated.Bytes29
import org.web3j.abi.datatypes.generated.Bytes3
import org.web3j.abi.datatypes.generated.Bytes30
import org.web3j.abi.datatypes.generated.Bytes31
import org.web3j.abi.datatypes.generated.Bytes32
import org.web3j.abi.datatypes.generated.Bytes4
import org.web3j.abi.datatypes.generated.Bytes5
import org.web3j.abi.datatypes.generated.Bytes6
import org.web3j.abi.datatypes.generated.Bytes7
import org.web3j.abi.datatypes.generated.Bytes8
import org.web3j.abi.datatypes.generated.Bytes9
import org.web3j.abi.datatypes.generated.Int104
import org.web3j.abi.datatypes.generated.Int112
import org.web3j.abi.datatypes.generated.Int120
import org.web3j.abi.datatypes.generated.Int128
import org.web3j.abi.datatypes.generated.Int136
import org.web3j.abi.datatypes.generated.Int144
import org.web3j.abi.datatypes.generated.Int152
import org.web3j.abi.datatypes.generated.Int16
import org.web3j.abi.datatypes.generated.Int160
import org.web3j.abi.datatypes.generated.Int168
import org.web3j.abi.datatypes.generated.Int176
import org.web3j.abi.datatypes.generated.Int184
import org.web3j.abi.datatypes.generated.Int192
import org.web3j.abi.datatypes.generated.Int200
import org.web3j.abi.datatypes.generated.Int208
import org.web3j.abi.datatypes.generated.Int216
import org.web3j.abi.datatypes.generated.Int224
import org.web3j.abi.datatypes.generated.Int232
import org.web3j.abi.datatypes.generated.Int24
import org.web3j.abi.datatypes.generated.Int240
import org.web3j.abi.datatypes.generated.Int248
import org.web3j.abi.datatypes.generated.Int256
import org.web3j.abi.datatypes.generated.Int32
import org.web3j.abi.datatypes.generated.Int40
import org.web3j.abi.datatypes.generated.Int48
import org.web3j.abi.datatypes.generated.Int56
import org.web3j.abi.datatypes.generated.Int64
import org.web3j.abi.datatypes.generated.Int72
import org.web3j.abi.datatypes.generated.Int8
import org.web3j.abi.datatypes.generated.Int80
import org.web3j.abi.datatypes.generated.Int88
import org.web3j.abi.datatypes.generated.Int96
import org.web3j.abi.datatypes.generated.Uint104
import org.web3j.abi.datatypes.generated.Uint112
import org.web3j.abi.datatypes.generated.Uint120
import org.web3j.abi.datatypes.generated.Uint128
import org.web3j.abi.datatypes.generated.Uint136
import org.web3j.abi.datatypes.generated.Uint144
import org.web3j.abi.datatypes.generated.Uint152
import org.web3j.abi.datatypes.generated.Uint16
import org.web3j.abi.datatypes.generated.Uint160
import org.web3j.abi.datatypes.generated.Uint168
import org.web3j.abi.datatypes.generated.Uint176
import org.web3j.abi.datatypes.generated.Uint184
import org.web3j.abi.datatypes.generated.Uint192
import org.web3j.abi.datatypes.generated.Uint200
import org.web3j.abi.datatypes.generated.Uint208
import org.web3j.abi.datatypes.generated.Uint216
import org.web3j.abi.datatypes.generated.Uint224
import org.web3j.abi.datatypes.generated.Uint232
import org.web3j.abi.datatypes.generated.Uint24
import org.web3j.abi.datatypes.generated.Uint240
import org.web3j.abi.datatypes.generated.Uint248
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.abi.datatypes.generated.Uint32
import org.web3j.abi.datatypes.generated.Uint40
import org.web3j.abi.datatypes.generated.Uint48
import org.web3j.abi.datatypes.generated.Uint56
import org.web3j.abi.datatypes.generated.Uint64
import org.web3j.abi.datatypes.generated.Uint72
import org.web3j.abi.datatypes.generated.Uint8
import org.web3j.abi.datatypes.generated.Uint80
import org.web3j.abi.datatypes.generated.Uint88
import org.web3j.abi.datatypes.generated.Uint96
import java.math.BigInteger
import kotlin.Int
import org.web3j.abi.datatypes.Int as Web3Int
import org.web3j.abi.datatypes.primitive.Byte as Web3Byte

object Web3TypeMappings {

    private const val VALUE_ERROR = "invalid value type"
    private val TYPE_MAPPINGS = mapOf<String, (JsonNode, JsonParser) -> Type<*>>(
        "address" to { v: JsonNode, p: JsonParser -> Address(v.parseText(p)) },
        "bool" to { v: JsonNode, p: JsonParser -> Bool(v.parseBoolean(p)) },
        "string" to { v: JsonNode, p: JsonParser -> Utf8String(v.parseText(p)) },
        "bytes" to { v: JsonNode, p: JsonParser -> DynamicBytes(v.parseBytes(p)) },
        "byte" to { v: JsonNode, p: JsonParser -> Web3Byte(v.parseBigInt(p).toByte()) },
        "uint" to { v: JsonNode, p: JsonParser -> Uint(v.parseBigInt(p)) },
        "uint8" to { v: JsonNode, p: JsonParser -> Uint8(v.parseBigInt(p)) },
        "uint16" to { v: JsonNode, p: JsonParser -> Uint16(v.parseBigInt(p)) },
        "uint24" to { v: JsonNode, p: JsonParser -> Uint24(v.parseBigInt(p)) },
        "uint32" to { v: JsonNode, p: JsonParser -> Uint32(v.parseBigInt(p)) },
        "uint40" to { v: JsonNode, p: JsonParser -> Uint40(v.parseBigInt(p)) },
        "uint48" to { v: JsonNode, p: JsonParser -> Uint48(v.parseBigInt(p)) },
        "uint56" to { v: JsonNode, p: JsonParser -> Uint56(v.parseBigInt(p)) },
        "uint64" to { v: JsonNode, p: JsonParser -> Uint64(v.parseBigInt(p)) },
        "uint72" to { v: JsonNode, p: JsonParser -> Uint72(v.parseBigInt(p)) },
        "uint80" to { v: JsonNode, p: JsonParser -> Uint80(v.parseBigInt(p)) },
        "uint88" to { v: JsonNode, p: JsonParser -> Uint88(v.parseBigInt(p)) },
        "uint96" to { v: JsonNode, p: JsonParser -> Uint96(v.parseBigInt(p)) },
        "uint104" to { v: JsonNode, p: JsonParser -> Uint104(v.parseBigInt(p)) },
        "uint112" to { v: JsonNode, p: JsonParser -> Uint112(v.parseBigInt(p)) },
        "uint120" to { v: JsonNode, p: JsonParser -> Uint120(v.parseBigInt(p)) },
        "uint128" to { v: JsonNode, p: JsonParser -> Uint128(v.parseBigInt(p)) },
        "uint136" to { v: JsonNode, p: JsonParser -> Uint136(v.parseBigInt(p)) },
        "uint144" to { v: JsonNode, p: JsonParser -> Uint144(v.parseBigInt(p)) },
        "uint152" to { v: JsonNode, p: JsonParser -> Uint152(v.parseBigInt(p)) },
        "uint160" to { v: JsonNode, p: JsonParser -> Uint160(v.parseBigInt(p)) },
        "uint168" to { v: JsonNode, p: JsonParser -> Uint168(v.parseBigInt(p)) },
        "uint176" to { v: JsonNode, p: JsonParser -> Uint176(v.parseBigInt(p)) },
        "uint184" to { v: JsonNode, p: JsonParser -> Uint184(v.parseBigInt(p)) },
        "uint192" to { v: JsonNode, p: JsonParser -> Uint192(v.parseBigInt(p)) },
        "uint200" to { v: JsonNode, p: JsonParser -> Uint200(v.parseBigInt(p)) },
        "uint208" to { v: JsonNode, p: JsonParser -> Uint208(v.parseBigInt(p)) },
        "uint216" to { v: JsonNode, p: JsonParser -> Uint216(v.parseBigInt(p)) },
        "uint224" to { v: JsonNode, p: JsonParser -> Uint224(v.parseBigInt(p)) },
        "uint232" to { v: JsonNode, p: JsonParser -> Uint232(v.parseBigInt(p)) },
        "uint240" to { v: JsonNode, p: JsonParser -> Uint240(v.parseBigInt(p)) },
        "uint248" to { v: JsonNode, p: JsonParser -> Uint248(v.parseBigInt(p)) },
        "uint256" to { v: JsonNode, p: JsonParser -> Uint256(v.parseBigInt(p)) },
        "int" to { v: JsonNode, p: JsonParser -> Web3Int(v.parseBigInt(p)) },
        "int8" to { v: JsonNode, p: JsonParser -> Int8(v.parseBigInt(p)) },
        "int16" to { v: JsonNode, p: JsonParser -> Int16(v.parseBigInt(p)) },
        "int24" to { v: JsonNode, p: JsonParser -> Int24(v.parseBigInt(p)) },
        "int32" to { v: JsonNode, p: JsonParser -> Int32(v.parseBigInt(p)) },
        "int40" to { v: JsonNode, p: JsonParser -> Int40(v.parseBigInt(p)) },
        "int48" to { v: JsonNode, p: JsonParser -> Int48(v.parseBigInt(p)) },
        "int56" to { v: JsonNode, p: JsonParser -> Int56(v.parseBigInt(p)) },
        "int64" to { v: JsonNode, p: JsonParser -> Int64(v.parseBigInt(p)) },
        "int72" to { v: JsonNode, p: JsonParser -> Int72(v.parseBigInt(p)) },
        "int80" to { v: JsonNode, p: JsonParser -> Int80(v.parseBigInt(p)) },
        "int88" to { v: JsonNode, p: JsonParser -> Int88(v.parseBigInt(p)) },
        "int96" to { v: JsonNode, p: JsonParser -> Int96(v.parseBigInt(p)) },
        "int104" to { v: JsonNode, p: JsonParser -> Int104(v.parseBigInt(p)) },
        "int112" to { v: JsonNode, p: JsonParser -> Int112(v.parseBigInt(p)) },
        "int120" to { v: JsonNode, p: JsonParser -> Int120(v.parseBigInt(p)) },
        "int128" to { v: JsonNode, p: JsonParser -> Int128(v.parseBigInt(p)) },
        "int136" to { v: JsonNode, p: JsonParser -> Int136(v.parseBigInt(p)) },
        "int144" to { v: JsonNode, p: JsonParser -> Int144(v.parseBigInt(p)) },
        "int152" to { v: JsonNode, p: JsonParser -> Int152(v.parseBigInt(p)) },
        "int160" to { v: JsonNode, p: JsonParser -> Int160(v.parseBigInt(p)) },
        "int168" to { v: JsonNode, p: JsonParser -> Int168(v.parseBigInt(p)) },
        "int176" to { v: JsonNode, p: JsonParser -> Int176(v.parseBigInt(p)) },
        "int184" to { v: JsonNode, p: JsonParser -> Int184(v.parseBigInt(p)) },
        "int192" to { v: JsonNode, p: JsonParser -> Int192(v.parseBigInt(p)) },
        "int200" to { v: JsonNode, p: JsonParser -> Int200(v.parseBigInt(p)) },
        "int208" to { v: JsonNode, p: JsonParser -> Int208(v.parseBigInt(p)) },
        "int216" to { v: JsonNode, p: JsonParser -> Int216(v.parseBigInt(p)) },
        "int224" to { v: JsonNode, p: JsonParser -> Int224(v.parseBigInt(p)) },
        "int232" to { v: JsonNode, p: JsonParser -> Int232(v.parseBigInt(p)) },
        "int240" to { v: JsonNode, p: JsonParser -> Int240(v.parseBigInt(p)) },
        "int248" to { v: JsonNode, p: JsonParser -> Int248(v.parseBigInt(p)) },
        "int256" to { v: JsonNode, p: JsonParser -> Int256(v.parseBigInt(p)) },
        "bytes1" to { v: JsonNode, p: JsonParser -> Bytes1(v.parseBytes(p, length = 1)) },
        "bytes2" to { v: JsonNode, p: JsonParser -> Bytes2(v.parseBytes(p, length = 2)) },
        "bytes3" to { v: JsonNode, p: JsonParser -> Bytes3(v.parseBytes(p, length = 3)) },
        "bytes4" to { v: JsonNode, p: JsonParser -> Bytes4(v.parseBytes(p, length = 4)) },
        "bytes5" to { v: JsonNode, p: JsonParser -> Bytes5(v.parseBytes(p, length = 5)) },
        "bytes6" to { v: JsonNode, p: JsonParser -> Bytes6(v.parseBytes(p, length = 6)) },
        "bytes7" to { v: JsonNode, p: JsonParser -> Bytes7(v.parseBytes(p, length = 7)) },
        "bytes8" to { v: JsonNode, p: JsonParser -> Bytes8(v.parseBytes(p, length = 8)) },
        "bytes9" to { v: JsonNode, p: JsonParser -> Bytes9(v.parseBytes(p, length = 9)) },
        "bytes10" to { v: JsonNode, p: JsonParser -> Bytes10(v.parseBytes(p, length = 10)) },
        "bytes11" to { v: JsonNode, p: JsonParser -> Bytes11(v.parseBytes(p, length = 11)) },
        "bytes12" to { v: JsonNode, p: JsonParser -> Bytes12(v.parseBytes(p, length = 12)) },
        "bytes13" to { v: JsonNode, p: JsonParser -> Bytes13(v.parseBytes(p, length = 13)) },
        "bytes14" to { v: JsonNode, p: JsonParser -> Bytes14(v.parseBytes(p, length = 14)) },
        "bytes15" to { v: JsonNode, p: JsonParser -> Bytes15(v.parseBytes(p, length = 15)) },
        "bytes16" to { v: JsonNode, p: JsonParser -> Bytes16(v.parseBytes(p, length = 16)) },
        "bytes17" to { v: JsonNode, p: JsonParser -> Bytes17(v.parseBytes(p, length = 17)) },
        "bytes18" to { v: JsonNode, p: JsonParser -> Bytes18(v.parseBytes(p, length = 18)) },
        "bytes19" to { v: JsonNode, p: JsonParser -> Bytes19(v.parseBytes(p, length = 19)) },
        "bytes20" to { v: JsonNode, p: JsonParser -> Bytes20(v.parseBytes(p, length = 20)) },
        "bytes21" to { v: JsonNode, p: JsonParser -> Bytes21(v.parseBytes(p, length = 21)) },
        "bytes22" to { v: JsonNode, p: JsonParser -> Bytes22(v.parseBytes(p, length = 22)) },
        "bytes23" to { v: JsonNode, p: JsonParser -> Bytes23(v.parseBytes(p, length = 23)) },
        "bytes24" to { v: JsonNode, p: JsonParser -> Bytes24(v.parseBytes(p, length = 24)) },
        "bytes25" to { v: JsonNode, p: JsonParser -> Bytes25(v.parseBytes(p, length = 25)) },
        "bytes26" to { v: JsonNode, p: JsonParser -> Bytes26(v.parseBytes(p, length = 26)) },
        "bytes27" to { v: JsonNode, p: JsonParser -> Bytes27(v.parseBytes(p, length = 27)) },
        "bytes28" to { v: JsonNode, p: JsonParser -> Bytes28(v.parseBytes(p, length = 28)) },
        "bytes29" to { v: JsonNode, p: JsonParser -> Bytes29(v.parseBytes(p, length = 29)) },
        "bytes30" to { v: JsonNode, p: JsonParser -> Bytes30(v.parseBytes(p, length = 30)) },
        "bytes31" to { v: JsonNode, p: JsonParser -> Bytes31(v.parseBytes(p, length = 31)) },
        "bytes32" to { v: JsonNode, p: JsonParser -> Bytes32(v.parseBytes(p, length = 32)) }
    )

    operator fun get(argumentType: String): ((JsonNode, JsonParser) -> Type<*>)? = TYPE_MAPPINGS[argumentType]

    private fun JsonNode.parseText(p: JsonParser): String =
        if (this.isTextual) this.asText() else throw JsonParseException(p, VALUE_ERROR)

    private fun JsonNode.parseBoolean(p: JsonParser): Boolean =
        if (this.isBoolean) this.asBoolean() else throw JsonParseException(p, VALUE_ERROR)

    private fun JsonNode.parseBigInt(p: JsonParser): BigInteger =
        if (this.isNumber) {
            this.bigIntegerValue()
        } else if (this.isTextual) {
            BigInteger(this.asText())
        } else {
            throw JsonParseException(p, VALUE_ERROR)
        }

    private fun JsonNode.parseBytes(p: JsonParser, length: Int? = null): ByteArray =
        if (this.isArray) {
            this.elements().asSequence().map { it.parseBigInt(p).toByte() }.toList().toByteArray().takeIf {
                length == null || it.size == length
            } ?: throw JsonParseException(p, "invalid byte array length")
        } else {
            throw JsonParseException(p, VALUE_ERROR)
        }
}
