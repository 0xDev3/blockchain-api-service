package dev3.blockchainapiservice.features.payout.model.params

import dev3.blockchainapiservice.features.payout.util.MerkleHash
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress

data class FetchMerkleTreeParams(
    val rootHash: MerkleHash,
    val chainId: ChainId,
    val assetContractAddress: ContractAddress
)
