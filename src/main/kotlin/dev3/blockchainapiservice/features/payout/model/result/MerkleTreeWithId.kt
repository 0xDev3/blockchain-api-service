package dev3.blockchainapiservice.features.payout.model.result

import dev3.blockchainapiservice.features.payout.util.MerkleTree
import dev3.blockchainapiservice.generated.jooq.id.MerkleTreeRootId

data class MerkleTreeWithId(val treeId: MerkleTreeRootId, val tree: MerkleTree)
