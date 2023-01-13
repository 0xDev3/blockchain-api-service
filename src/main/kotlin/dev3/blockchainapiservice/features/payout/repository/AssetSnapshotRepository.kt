package dev3.blockchainapiservice.features.payout.repository

import dev3.blockchainapiservice.features.payout.model.params.CreateAssetSnapshotParams
import dev3.blockchainapiservice.features.payout.model.result.AssetSnapshot
import dev3.blockchainapiservice.features.payout.model.result.PendingAssetSnapshot
import dev3.blockchainapiservice.features.payout.util.AssetSnapshotFailureCause
import dev3.blockchainapiservice.features.payout.util.AssetSnapshotStatus
import dev3.blockchainapiservice.features.payout.util.IpfsHash
import dev3.blockchainapiservice.generated.jooq.id.AssetSnapshotId
import dev3.blockchainapiservice.generated.jooq.id.MerkleTreeRootId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.util.Balance

interface AssetSnapshotRepository {
    fun getById(assetSnapshotId: AssetSnapshotId): AssetSnapshot?

    fun getAllByProjectIdAndStatuses(
        projectId: ProjectId,
        statuses: Set<AssetSnapshotStatus>
    ): List<AssetSnapshot>

    fun createAssetSnapshot(params: CreateAssetSnapshotParams): AssetSnapshotId
    fun getPending(): PendingAssetSnapshot?

    fun completeAssetSnapshot(
        assetSnapshotId: AssetSnapshotId,
        merkleTreeRootId: MerkleTreeRootId,
        merkleTreeIpfsHash: IpfsHash,
        totalAssetAmount: Balance
    ): AssetSnapshot?

    fun failAssetSnapshot(assetSnapshotId: AssetSnapshotId, cause: AssetSnapshotFailureCause): AssetSnapshot?
}
