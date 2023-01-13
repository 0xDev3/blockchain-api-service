package dev3.blockchainapiservice.features.payout.model.params

import dev3.blockchainapiservice.features.payout.util.MerkleHash
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.WalletAddress

data class FetchMerkleTreePathParams(
    val rootHash: MerkleHash,
    val chainId: ChainId,
    val assetContractAddress: ContractAddress,
    val walletAddress: WalletAddress
) {
    val toFetchMerkleTreeParams: FetchMerkleTreeParams
        get() = FetchMerkleTreeParams(rootHash, chainId, assetContractAddress)
}
