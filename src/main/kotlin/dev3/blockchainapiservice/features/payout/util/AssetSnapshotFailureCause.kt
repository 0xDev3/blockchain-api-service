package dev3.blockchainapiservice.features.payout.util

import dev3.blockchainapiservice.generated.jooq.enums.AssetSnapshotFailureCause as DbAssetSnapshotFailureCause

enum class AssetSnapshotFailureCause(val toDbEnum: DbAssetSnapshotFailureCause) {
    LOG_RESPONSE_LIMIT(DbAssetSnapshotFailureCause.LOG_RESPONSE_LIMIT),
    OTHER(DbAssetSnapshotFailureCause.OTHER);

    companion object {
        fun fromDbEnum(value: DbAssetSnapshotFailureCause): AssetSnapshotFailureCause {
            return values().find { it.toDbEnum == value }
                ?: throw IllegalStateException("DB enum not added to code: $value")
        }
    }
}
