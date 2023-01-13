package dev3.blockchainapiservice.features.payout.model.response

import com.fasterxml.jackson.annotation.JsonIgnore
import dev3.blockchainapiservice.features.payout.util.HashFunction
import dev3.blockchainapiservice.features.payout.util.MerkleHash
import dev3.blockchainapiservice.features.payout.util.MerkleTree
import dev3.blockchainapiservice.util.annotation.SchemaAnyOf
import dev3.blockchainapiservice.util.annotation.SchemaIgnore
import dev3.blockchainapiservice.util.annotation.SchemaName
import dev3.blockchainapiservice.util.annotation.SchemaNotNull
import java.math.BigInteger

private data class MerkleTreeSchema(
    val depth: Int,
    val hash: MerkleHash,
    val hashFn: HashFunction,
    val left: NodeSchema,
    val right: NodeSchema
)

@SchemaAnyOf
private data class NodeSchema(
    val node1: NilNodeSchema,
    val node2: LeafNodeSchema,
    val node3: PathNodeSchema
)

private data class NilNodeSchema(
    val hash: MerkleHash
)

private data class LeafNodeSchema(
    val hash: MerkleHash,
    val data: LeafNodeDataSchema
)

private data class LeafNodeDataSchema(
    val address: String,
    val balance: BigInteger
)

private data class PathNodeSchema(
    val hash: MerkleHash,
    val left: NodeSchema,
    val right: NodeSchema
)

data class FetchMerkleTreeResponse(
    @SchemaIgnore
    val merkleTree: MerkleTree
) {
    @Suppress("unused") // used for JSON schema generation
    @JsonIgnore
    @SchemaName("merkle_tree")
    @SchemaNotNull
    private val schemaMerkleTree: MerkleTreeSchema? = null
}
