package dev3.blockchainapiservice.features.payout.util

import dev3.blockchainapiservice.generated.jooq.enums.SnapshotStatus as DbSnapshotStatus

enum class SnapshotStatus(val toDbEnum: DbSnapshotStatus) {
    PENDING(DbSnapshotStatus.PENDING),
    SUCCESS(DbSnapshotStatus.SUCCESS),
    FAILED(DbSnapshotStatus.FAILED);

    companion object {
        fun fromDbEnum(value: DbSnapshotStatus): SnapshotStatus {
            return values().find { it.toDbEnum == value }
                ?: throw IllegalStateException("DB enum not added to code: $value")
        }
    }
}
