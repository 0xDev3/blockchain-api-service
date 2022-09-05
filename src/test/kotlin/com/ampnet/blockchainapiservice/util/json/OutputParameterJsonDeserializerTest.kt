package com.ampnet.blockchainapiservice.util.json

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.config.JsonConfig
import com.ampnet.blockchainapiservice.model.params.OutputParameter
import com.ampnet.blockchainapiservice.util.AddressType
import com.ampnet.blockchainapiservice.util.BoolType
import com.ampnet.blockchainapiservice.util.DynamicArrayType
import com.ampnet.blockchainapiservice.util.DynamicBytesType
import com.ampnet.blockchainapiservice.util.IntType
import com.ampnet.blockchainapiservice.util.StaticArrayType
import com.ampnet.blockchainapiservice.util.StaticBytesType
import com.ampnet.blockchainapiservice.util.StringType
import com.ampnet.blockchainapiservice.util.StructType
import com.ampnet.blockchainapiservice.util.UintType
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OutputParameterJsonDeserializerTest : TestBase() {

    companion object {
        private data class Result(val args: List<OutputParameter>)
    }

    private val objectMapper = JsonConfig().objectMapper()

    @Test
    fun mustCorrectlyDeserializeBaseTypes() {
        val json = """{
            |  "args": [
            |    "address", "bool", "string", "bytes", "byte", "uint", "uint8", "uint16", "uint24", "uint32", "uint40",
            |    "uint48", "uint56", "uint64", "uint72", "uint80", "uint88", "uint96", "uint104", "uint112", "uint120",
            |    "uint128", "uint136", "uint144", "uint152", "uint160", "uint168", "uint176", "uint184", "uint192",
            |    "uint200", "uint208", "uint216", "uint224", "uint232", "uint240", "uint248", "uint256", "int", "int8",
            |    "int16", "int24", "int32", "int40", "int48", "int56", "int64", "int72", "int80", "int88", "int96",
            |    "int104", "int112", "int120", "int128", "int136", "int144", "int152", "int160", "int168", "int176",
            |    "int184", "int192", "int200", "int208", "int216", "int224", "int232", "int240", "int248", "int256",
            |    "bytes1", "bytes2", "bytes3", "bytes4", "bytes5", "bytes6", "bytes7", "bytes8", "bytes9", "bytes10",
            |    "bytes11", "bytes12", "bytes13", "bytes14", "bytes15", "bytes16", "bytes17", "bytes18", "bytes19",
            |    "bytes20", "bytes21", "bytes22", "bytes23", "bytes24", "bytes25", "bytes26", "bytes27", "bytes28",
            |    "bytes29", "bytes30", "bytes31", "bytes32"
            |  ]
            |}""".trimMargin()

        verify("must correctly parse base types") {
            val result = objectMapper.readValue(json, Result::class.java).args.map { it.deserializedType }

            assertThat(result).withMessage()
                .isEqualTo(
                    listOf(
                        AddressType, BoolType, StringType, DynamicBytesType, UintType, UintType, UintType, UintType,
                        UintType, UintType, UintType, UintType, UintType, UintType, UintType, UintType, UintType,
                        UintType, UintType, UintType, UintType, UintType, UintType, UintType, UintType, UintType,
                        UintType, UintType, UintType, UintType, UintType, UintType, UintType, UintType, UintType,
                        UintType, UintType, UintType, IntType, IntType, IntType, IntType, IntType, IntType, IntType,
                        IntType, IntType, IntType, IntType, IntType, IntType, IntType, IntType, IntType, IntType,
                        IntType, IntType, IntType, IntType, IntType, IntType, IntType, IntType, IntType, IntType,
                        IntType, IntType, IntType, IntType, IntType, IntType, StaticBytesType(1), StaticBytesType(2),
                        StaticBytesType(3), StaticBytesType(4), StaticBytesType(5), StaticBytesType(6),
                        StaticBytesType(7), StaticBytesType(8), StaticBytesType(9), StaticBytesType(10),
                        StaticBytesType(11), StaticBytesType(12), StaticBytesType(13), StaticBytesType(14),
                        StaticBytesType(15), StaticBytesType(16), StaticBytesType(17), StaticBytesType(18),
                        StaticBytesType(19), StaticBytesType(20), StaticBytesType(21), StaticBytesType(22),
                        StaticBytesType(23), StaticBytesType(24), StaticBytesType(25), StaticBytesType(26),
                        StaticBytesType(27), StaticBytesType(28), StaticBytesType(29), StaticBytesType(30),
                        StaticBytesType(31), StaticBytesType(32)
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyDeserializeDynamicArrayType() {
        val json = """{
            |  "args": ["string[]"]
            |}""".trimMargin()

        verify("must correctly parse dynamic array type") {
            val result = objectMapper.readValue(json, Result::class.java).args.map { it.deserializedType }

            assertThat(result).withMessage()
                .isEqualTo(listOf(DynamicArrayType(StringType)))
        }
    }

    @Test
    fun mustCorrectlyDeserializeSizedArrayType() {
        val json = """{
            |  "args": ["bool[2]"]
            |}""".trimMargin()

        verify("must correctly parse sized array type") {
            val result = objectMapper.readValue(json, Result::class.java).args.map { it.deserializedType }

            assertThat(result).withMessage()
                .isEqualTo(listOf(StaticArrayType(BoolType, 2)))
        }
    }

    @Test
    fun mustCorrectlyDeserializeNestedArrayType() {
        val json = """{
            |  "args": ["uint[][2][3]"]
            |}""".trimMargin()

        verify("must correctly parse nested array type") {
            val result = objectMapper.readValue(json, Result::class.java).args.map { it.deserializedType }

            assertThat(result).withMessage()
                .isEqualTo(listOf(StaticArrayType(StaticArrayType(DynamicArrayType(UintType), 2), 3)))
        }
    }

    @Test
    fun mustCorrectlyDeserializeStruct() {
        val json = """{
            |  "args": [
            |    {
            |      "type": "struct",
            |      "elems": ["string", "uint"]
            |    }
            |  ]
            |}""".trimMargin()

        verify("must correctly parse struct") {
            val result = objectMapper.readValue(json, Result::class.java).args.map { it.deserializedType }

            assertThat(result).withMessage()
                .isEqualTo(listOf(StructType(StringType, UintType)))
        }
    }

    @Test
    fun mustCorrectlyDeserializeNestedStruct() {
        val json = """{
            |  "args": [
            |    {
            |      "type": "struct",
            |      "elems": [
            |        "string",
            |        {
            |          "type": "struct",
            |          "elems": [
            |            "string",
            |            {
            |              "type": "struct",
            |              "elems": ["string", "bool", "bytes5"]
            |            },
            |            "int"
            |          ]
            |        },
            |        "uint"
            |      ]
            |    }
            |  ]
            |}""".trimMargin()

        verify("must correctly parse nested struct") {
            val result = objectMapper.readValue(json, Result::class.java).args.map { it.deserializedType }

            assertThat(result).withMessage()
                .isEqualTo(
                    listOf(
                        StructType(
                            StringType,
                            StructType(
                                StringType,
                                StructType(StringType, BoolType, StaticBytesType(5)),
                                IntType
                            ),
                            UintType
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyDeserializeStructWithArrayElements() {
        val json = """{
            |  "args": [
            |    {
            |      "type": "struct",
            |      "elems": ["string[]", "uint[]"]
            |    }
            |  ]
            |}""".trimMargin()

        verify("must correctly parse struct with array elements") {
            val result = objectMapper.readValue(json, Result::class.java).args.map { it.deserializedType }

            assertThat(result).withMessage()
                .isEqualTo(
                    listOf(
                        StructType(
                            DynamicArrayType(StringType),
                            DynamicArrayType(UintType)
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyDeserializeStructArray() {
        val json = """{
            |  "args": [
            |    {
            |      "type": "struct[]",
            |      "elems": ["string", "uint"]
            |    }
            |  ]
            |}""".trimMargin()

        verify("must correctly parse struct array") {
            val result = objectMapper.readValue(json, Result::class.java).args.map { it.deserializedType }

            assertThat(result).withMessage()
                .isEqualTo(
                    listOf(
                        DynamicArrayType(
                            StructType(StringType, UintType)
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyDeserializeArrayOfNestedStructsWithArrays() {
        val json = """{
            |  "args": [
            |    {
            |      "type": "struct[][2]",
            |      "elems": [
            |        "string[][1]",
            |        {
            |          "type": "struct[]",
            |          "elems": [
            |            "address[]",
            |            {
            |              "type": "struct",
            |              "elems": ["int"]
            |            },
            |            "bool",
            |            {
            |              "type": "struct[][]",
            |              "elems": ["uint"]
            |            }
            |          ]
            |        },
            |        "uint"
            |      ]
            |    }
            |  ]
            |}""".trimMargin()

        verify("must correctly parse array of nested structs with arrays") {
            val result = objectMapper.readValue(json, Result::class.java).args.map { it.deserializedType }

            assertThat(result).withMessage()
                .isEqualTo(
                    listOf(
                        StaticArrayType(
                            DynamicArrayType(
                                StructType(
                                    StaticArrayType(DynamicArrayType(StringType), 1),
                                    DynamicArrayType(
                                        StructType(
                                            DynamicArrayType(AddressType),
                                            StructType(IntType),
                                            BoolType,
                                            DynamicArrayType(DynamicArrayType(StructType(UintType)))
                                        )
                                    ),
                                    UintType
                                )
                            ),
                            2
                        )
                    )
                )
        }
    }

    @Test
    fun mustThrowJsonMappingExceptionWithJsonParseExceptionCauseForUnknownType() {
        val json = """{
            |  "args": ["unknown-type"]
            |}""".trimMargin()

        verify("JsonMappingException is thrown") {
            val ex = assertThrows<JsonMappingException>(message) {
                objectMapper.readValue(json, Result::class.java)
            }

            assertThat(ex.cause).withMessage()
                .isInstanceOf(JsonParseException::class.java)
        }
    }

    @Test
    fun mustThrowJsonMappingExceptionWithJsonParseExceptionCauseForInvalidValueType() {
        val json = """{
            |  "args": [[]]
            |}""".trimMargin()

        verify("JsonMappingException is thrown") {
            val ex = assertThrows<JsonMappingException>(message) {
                objectMapper.readValue(json, Result::class.java)
            }

            assertThat(ex.cause).withMessage()
                .isInstanceOf(JsonParseException::class.java)
        }
    }

    @Test
    fun mustThrowJsonMappingExceptionWithJsonParseExceptionCauseForMissingStructType() {
        val json = """{
            |  "args": [
            |    {
            |      "elems": []
            |    }
            |  ]
            |}""".trimMargin()

        verify("JsonMappingException is thrown") {
            val ex = assertThrows<JsonMappingException>(message) {
                objectMapper.readValue(json, Result::class.java)
            }

            assertThat(ex.cause).withMessage()
                .isInstanceOf(JsonParseException::class.java)
        }
    }

    @Test
    fun mustThrowJsonMappingExceptionWithJsonParseExceptionCauseForInvalidStructType() {
        val json = """{
            |  "args": [
            |    {
            |      "type": "non-struct",
            |      "elems": []
            |    }
            |  ]
            |}""".trimMargin()

        verify("JsonMappingException is thrown") {
            val ex = assertThrows<JsonMappingException>(message) {
                objectMapper.readValue(json, Result::class.java)
            }

            assertThat(ex.cause).withMessage()
                .isInstanceOf(JsonParseException::class.java)
        }
    }

    @Test
    fun mustThrowJsonMappingExceptionWithJsonParseExceptionCauseForMissingStructElements() {
        val json = """{
            |  "args": [
            |    {
            |      "type": "struct"
            |    }
            |  ]
            |}""".trimMargin()

        verify("JsonMappingException is thrown") {
            val ex = assertThrows<JsonMappingException>(message) {
                objectMapper.readValue(json, Result::class.java)
            }

            assertThat(ex.cause).withMessage()
                .isInstanceOf(JsonParseException::class.java)
        }
    }

    @Test
    fun mustThrowJsonMappingExceptionWithJsonParseExceptionCauseForInvalidStructElement() {
        val json = """{
            |  "args": [
            |    {
            |      "type": "struct",
            |      "elems": [[]]
            |    }
            |  ]
            |}""".trimMargin()

        verify("JsonMappingException is thrown") {
            val ex = assertThrows<JsonMappingException>(message) {
                objectMapper.readValue(json, Result::class.java)
            }

            assertThat(ex.cause).withMessage()
                .isInstanceOf(JsonParseException::class.java)
        }
    }
}
