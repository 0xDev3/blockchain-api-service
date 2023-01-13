package dev3.blockchainapiservice.features.payout.repository

import dev3.blockchainapiservice.features.payout.model.params.FetchMerkleTreeParams
import dev3.blockchainapiservice.features.payout.model.params.FetchMerkleTreePathParams
import dev3.blockchainapiservice.features.payout.model.result.MerkleTreeWithId
import dev3.blockchainapiservice.features.payout.util.MerkleTree
import dev3.blockchainapiservice.generated.jooq.id.MerkleTreeRootId
import dev3.blockchainapiservice.util.BlockNumber
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress

interface MerkleTreeRepository {
    fun getById(treeId: MerkleTreeRootId): MerkleTree?
    fun storeTree(
        tree: MerkleTree,
        chainId: ChainId,
        assetContractAddress: ContractAddress,
        blockNumber: BlockNumber
    ): MerkleTreeRootId

    fun fetchTree(params: FetchMerkleTreeParams): MerkleTreeWithId?
    fun containsAddress(params: FetchMerkleTreePathParams): Boolean
}
