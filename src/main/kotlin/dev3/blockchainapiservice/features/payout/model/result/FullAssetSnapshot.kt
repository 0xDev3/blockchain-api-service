package dev3.blockchainapiservice.features.payout.model.result

import dev3.blockchainapiservice.features.payout.model.response.AssetSnapshotResponse
import dev3.blockchainapiservice.features.payout.util.AssetSnapshotFailureCause
import dev3.blockchainapiservice.features.payout.util.AssetSnapshotStatus
import dev3.blockchainapiservice.features.payout.util.HashFunction
import dev3.blockchainapiservice.features.payout.util.IpfsHash
import dev3.blockchainapiservice.features.payout.util.MerkleHash
import dev3.blockchainapiservice.generated.jooq.id.AssetSnapshotId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.BlockNumber
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.WalletAddress

data class FullAssetSnapshot(
    val id: AssetSnapshotId,
    val projectId: ProjectId,
    val name: String,
    val chainId: ChainId,
    val assetContractAddress: ContractAddress,
    val blockNumber: BlockNumber,
    val ignoredHolderAddresses: Set<WalletAddress>,
    val snapshotStatus: AssetSnapshotStatus,
    val snapshotFailureCause: AssetSnapshotFailureCause?,
    val data: FullAssetSnapshotData?
) {
    fun toAssetSnapshotResponse(): AssetSnapshotResponse =
        AssetSnapshotResponse(
            id = id,
            projectId = projectId,
            name = name,
            chainId = chainId.value,
            status = snapshotStatus,
            failureCause = snapshotFailureCause,
            asset = assetContractAddress.rawValue,
            totalAssetAmount = data?.totalAssetAmount?.rawValue,
            ignoredHolderAddresses = ignoredHolderAddresses.mapTo(HashSet()) { it.rawValue },
            assetSnapshotMerkleRoot = data?.merkleRootHash?.value,
            assetSnapshotMerkleDepth = data?.merkleTreeDepth,
            assetSnapshotBlockNumber = blockNumber.value,
            assetSnapshotMerkleIpfsHash = data?.merkleTreeIpfsHash?.value
        )
}

data class FullAssetSnapshotData(
    val totalAssetAmount: Balance,
    val merkleRootHash: MerkleHash,
    val merkleTreeIpfsHash: IpfsHash,
    val merkleTreeDepth: Int,
    val hashFn: HashFunction
)
