package dev3.blockchainapiservice.features.payout.model.response

import dev3.blockchainapiservice.features.payout.util.AssetSnapshotFailureCause
import dev3.blockchainapiservice.features.payout.util.AssetSnapshotStatus
import dev3.blockchainapiservice.generated.jooq.id.AssetSnapshotId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import java.math.BigInteger

data class AssetSnapshotResponse(
    val id: AssetSnapshotId,
    val projectId: ProjectId,
    val name: String,
    val chainId: Long,
    val status: AssetSnapshotStatus,
    val failureCause: AssetSnapshotFailureCause?,
    val asset: String,
    val totalAssetAmount: BigInteger?,
    val ignoredHolderAddresses: Set<String>,
    val assetSnapshotMerkleRoot: String?,
    val assetSnapshotMerkleDepth: Int?,
    val assetSnapshotBlockNumber: BigInteger,
    val assetSnapshotMerkleIpfsHash: String?
)
