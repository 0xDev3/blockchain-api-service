package dev3.blockchainapiservice.features.payout.util

import dev3.blockchainapiservice.generated.jooq.enums.SnapshotFailureCause as DbSnapshotFailureCause

enum class SnapshotFailureCause(val toDbEnum: DbSnapshotFailureCause) {
    LOG_RESPONSE_LIMIT(DbSnapshotFailureCause.LOG_RESPONSE_LIMIT),
    OTHER(DbSnapshotFailureCause.OTHER);

    companion object {
        fun fromDbEnum(value: DbSnapshotFailureCause): SnapshotFailureCause {
            return values().find { it.toDbEnum == value }
                ?: throw IllegalStateException("DB enum not added to code: $value")
        }
    }
}
