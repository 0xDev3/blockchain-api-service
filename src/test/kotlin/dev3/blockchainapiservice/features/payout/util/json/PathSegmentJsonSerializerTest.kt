package dev3.blockchainapiservice.features.payout.util.json

import com.fasterxml.jackson.databind.JsonNode
import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.config.JsonConfig
import dev3.blockchainapiservice.features.payout.util.MerkleHash
import dev3.blockchainapiservice.features.payout.util.MerkleTree.Companion.PathSegment
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PathSegmentJsonSerializerTest : TestBase() {

    private val objectMapper = JsonConfig().objectMapper()

    @Test
    fun mustCorrectlySerializePathSegment() {
        val pathSegment = PathSegment(MerkleHash("test"), true)

        val serializedPathSegment = suppose("path segment is serialized to JSON") {
            objectMapper.valueToTree<JsonNode>(pathSegment)
        }

        verify("path segment is correctly serialized") {
            assertThat(serializedPathSegment).withMessage().isEqualTo(
                objectMapper.readTree(
                    """
                    {
                        "sibling_hash": "${pathSegment.siblingHash.value}",
                        "is_left": ${pathSegment.isLeft}
                    }
                    """.trimIndent()
                )
            )
        }
    }
}
