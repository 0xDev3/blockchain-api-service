package dev3.blockchainapiservice.features.payout.service

import dev3.blockchainapiservice.features.payout.model.params.CreateAssetSnapshotParams
import dev3.blockchainapiservice.features.payout.model.result.FullAssetSnapshot
import dev3.blockchainapiservice.features.payout.util.AssetSnapshotStatus
import dev3.blockchainapiservice.generated.jooq.id.AssetSnapshotId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId

interface AssetSnapshotQueueService {
    fun submitAssetSnapshot(params: CreateAssetSnapshotParams): AssetSnapshotId
    fun getAssetSnapshotById(assetSnapshotId: AssetSnapshotId): FullAssetSnapshot?

    fun getAllAssetSnapshotsByProjectIdAndStatuses(
        projectId: ProjectId,
        statuses: Set<AssetSnapshotStatus>
    ): List<FullAssetSnapshot>
}
