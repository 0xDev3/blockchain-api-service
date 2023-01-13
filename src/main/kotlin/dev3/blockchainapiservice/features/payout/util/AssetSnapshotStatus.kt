package dev3.blockchainapiservice.features.payout.util

import dev3.blockchainapiservice.generated.jooq.enums.AssetSnapshotStatus as DbAssetSnapshotStatus

enum class AssetSnapshotStatus(val toDbEnum: DbAssetSnapshotStatus) {
    PENDING(DbAssetSnapshotStatus.PENDING),
    SUCCESS(DbAssetSnapshotStatus.SUCCESS),
    FAILED(DbAssetSnapshotStatus.FAILED);

    companion object {
        fun fromDbEnum(value: DbAssetSnapshotStatus): AssetSnapshotStatus {
            return values().find { it.toDbEnum == value }
                ?: throw IllegalStateException("DB enum not added to code: $value")
        }
    }
}
