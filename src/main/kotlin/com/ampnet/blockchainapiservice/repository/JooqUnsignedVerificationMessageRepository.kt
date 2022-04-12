package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.generated.jooq.tables.UnsignedVerificationMessageTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.UnsignedVerificationMessageRecord
import com.ampnet.blockchainapiservice.model.result.UnsignedVerificationMessage
import com.ampnet.blockchainapiservice.service.UtcDateTimeProvider
import com.ampnet.blockchainapiservice.util.UtcDateTime
import com.ampnet.blockchainapiservice.util.WalletAddress
import mu.KLogging
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JooqUnsignedVerificationMessageRepository(
    private val dslContext: DSLContext,
    private val utcDateTimeProvider: UtcDateTimeProvider
) : UnsignedVerificationMessageRepository {

    companion object : KLogging()

    override fun getById(id: UUID): UnsignedVerificationMessage? {
        logger.debug { "Get unsigned verification message by ID: $id" }
        return dslContext.selectFrom(UnsignedVerificationMessageTable.UNSIGNED_VERIFICATION_MESSAGE)
            .where(UnsignedVerificationMessageTable.UNSIGNED_VERIFICATION_MESSAGE.ID.eq(id))
            .fetchOne { it.toModel() }
    }

    override fun store(message: UnsignedVerificationMessage): UnsignedVerificationMessage {
        logger.info { "Storing unsigned verification message: $message" }
        dslContext.executeInsert(message.toRecord())
        return message
    }

    override fun deleteById(id: UUID): Boolean {
        logger.info { "Delete unsigned verification message by ID: $id" }
        return dslContext.deleteFrom(UnsignedVerificationMessageTable.UNSIGNED_VERIFICATION_MESSAGE)
            .where(UnsignedVerificationMessageTable.UNSIGNED_VERIFICATION_MESSAGE.ID.eq(id))
            .execute() > 0
    }

    override fun deleteAllExpired(): Int {
        val now = utcDateTimeProvider.getUtcDateTime()
        logger.info { "Delete all unsigned verification messages expired at: $now" }
        return dslContext.deleteFrom(UnsignedVerificationMessageTable.UNSIGNED_VERIFICATION_MESSAGE)
            .where(UnsignedVerificationMessageTable.UNSIGNED_VERIFICATION_MESSAGE.VALID_UNTIL.le(now.value))
            .execute()
    }

    private fun UnsignedVerificationMessage.toRecord(): UnsignedVerificationMessageRecord =
        UnsignedVerificationMessageRecord(
            id = id,
            walletAddress = walletAddress.rawValue,
            createdAt = createdAt.value,
            validUntil = validUntil.value
        )

    private fun UnsignedVerificationMessageRecord.toModel(): UnsignedVerificationMessage =
        UnsignedVerificationMessage(
            id = id!!,
            walletAddress = WalletAddress(walletAddress!!),
            createdAt = UtcDateTime(createdAt!!),
            validUntil = UtcDateTime(validUntil!!)
        )
}
