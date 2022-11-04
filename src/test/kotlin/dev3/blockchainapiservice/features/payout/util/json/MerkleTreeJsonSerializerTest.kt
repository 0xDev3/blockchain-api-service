package dev3.blockchainapiservice.features.payout.util.json

import com.fasterxml.jackson.databind.JsonNode
import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.config.JsonConfig
import dev3.blockchainapiservice.features.payout.util.HashFunction
import dev3.blockchainapiservice.features.payout.util.MerkleTree
import dev3.blockchainapiservice.features.payout.util.MerkleTree.Companion.NilNode
import dev3.blockchainapiservice.features.payout.util.PayoutAccountBalance
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigInteger

class MerkleTreeJsonSerializerTest : TestBase() {

    private val objectMapper = JsonConfig().objectMapper()

    @Test
    fun mustCorrectlySerializeSimpleMerkleTree() {
        val accountBalance = PayoutAccountBalance(WalletAddress("0x0"), Balance.ZERO)
        val tree = suppose("simple Merkle tree is created") {
            MerkleTree(
                listOf(accountBalance),
                HashFunction.IDENTITY
            )
        }

        val serializedTree = suppose("simple Merkle tree is serialized to JSON") {
            objectMapper.valueToTree<JsonNode>(tree)
        }

        verify("simple Merkle tree is correctly serialized") {
            assertThat(serializedTree).withMessage().isEqualTo(
                objectMapper.readTree(
                    """
                    {
                        "depth": ${tree.root.depth},
                        "hash": "${tree.root.hash.value}",
                        "hash_fn": "${tree.hashFn.name}",
                        "left": {
                            "hash": "${accountBalance.abiEncode()}",
                            "data": {
                                "address": "${accountBalance.address.rawValue}",
                                "balance": "${accountBalance.balance.rawValue}"
                            }
                        },
                        "right": {
                            "hash": "${NilNode.hash.value}"
                        }
                    }
                    """.trimIndent()
                )
            )
        }
    }

    @Test
    fun mustCorrectlySerializeMultiNodeMerkleTree() {
        val accountBalances = listOf(
            PayoutAccountBalance(WalletAddress("0x0"), Balance(BigInteger("0"))),
            PayoutAccountBalance(WalletAddress("0x1"), Balance(BigInteger("1"))),
            PayoutAccountBalance(WalletAddress("0x2"), Balance(BigInteger("2"))),
            PayoutAccountBalance(WalletAddress("0x3"), Balance(BigInteger("3")))
        )
        val tree = suppose("multi-node Merkle is created") {
            MerkleTree(
                accountBalances,
                HashFunction.IDENTITY
            )
        }

        val serializedTree = suppose("multi-node Merkle tree is serialized to JSON") {
            objectMapper.valueToTree<JsonNode>(tree)
        }

        verify("multi-node Merkle tree is correctly serialized") {
            assertThat(serializedTree).withMessage().isEqualTo(
                objectMapper.readTree(
                    """
                    {
                        "depth": ${tree.root.depth},
                        "hash": "${tree.root.hash.value}",
                        "hash_fn": "${tree.hashFn.name}",
                        "left": {
                            "hash": "${accountBalances[0].abiEncode() + accountBalances[1].abiEncode()}",
                            "left": {
                                "hash": "${accountBalances[0].abiEncode()}",
                                "data": {
                                    "address": "${accountBalances[0].address.rawValue}",
                                    "balance": "${accountBalances[0].balance.rawValue}"
                                }
                            },
                            "right": {
                                "hash": "${accountBalances[1].abiEncode()}",
                                "data": {
                                    "address": "${accountBalances[1].address.rawValue}",
                                    "balance": "${accountBalances[1].balance.rawValue}"
                                }
                            }
                        },
                        "right": {
                            "hash": "${accountBalances[2].abiEncode() + accountBalances[3].abiEncode()}",
                            "left": {
                                "hash": "${accountBalances[2].abiEncode()}",
                                "data": {
                                    "address": "${accountBalances[2].address.rawValue}",
                                    "balance": "${accountBalances[2].balance.rawValue}"
                                }
                            },
                            "right": {
                                "hash": "${accountBalances[3].abiEncode()}",
                                "data": {
                                    "address": "${accountBalances[3].address.rawValue}",
                                    "balance": "${accountBalances[3].balance.rawValue}"
                                }
                            }
                        }
                    }
                    """.trimIndent()
                )
            )
        }
    }
}
