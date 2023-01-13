package dev3.blockchainapiservice.features.payout.model.result

import dev3.blockchainapiservice.features.payout.util.AssetSnapshotFailureCause
import dev3.blockchainapiservice.features.payout.util.AssetSnapshotStatus
import dev3.blockchainapiservice.features.payout.util.IpfsHash
import dev3.blockchainapiservice.generated.jooq.id.AssetSnapshotId
import dev3.blockchainapiservice.generated.jooq.id.MerkleTreeRootId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.BlockNumber
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.WalletAddress

data class AssetSnapshot(
    val id: AssetSnapshotId,
    val projectId: ProjectId,
    val name: String,
    val chainId: ChainId,
    val assetContractAddress: ContractAddress,
    val blockNumber: BlockNumber,
    val ignoredHolderAddresses: Set<WalletAddress>,
    val data: OptionalAssetSnapshotData
)

sealed interface OptionalAssetSnapshotData {
    val status: AssetSnapshotStatus
    val failureCause: AssetSnapshotFailureCause?
}

data class SuccessfulAssetSnapshotData(
    val merkleTreeRootId: MerkleTreeRootId,
    val merkleTreeIpfsHash: IpfsHash,
    val totalAssetAmount: Balance,
    override val status: AssetSnapshotStatus = AssetSnapshotStatus.SUCCESS,
    override val failureCause: AssetSnapshotFailureCause? = null
) : OptionalAssetSnapshotData

data class OtherAssetSnapshotData(
    override val status: AssetSnapshotStatus,
    override val failureCause: AssetSnapshotFailureCause?
) : OptionalAssetSnapshotData
