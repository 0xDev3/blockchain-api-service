package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.generated.jooq.tables.records.SignedVerificationMessageRecord
import com.ampnet.blockchainapiservice.model.result.SignedVerificationMessage
import com.ampnet.blockchainapiservice.service.UtcDateTimeProvider
import com.ampnet.blockchainapiservice.util.UtcDateTime
import com.ampnet.blockchainapiservice.util.WalletAddress
import mu.KLogging
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.util.UUID
import com.ampnet.blockchainapiservice.generated.jooq.tables.SignedVerificationMessage as SignedVerificationMessageTable

@Repository
class JooqSignedVerificationMessageRepository(
    private val dslContext: DSLContext,
    private val utcDateTimeProvider: UtcDateTimeProvider
) : SignedVerificationMessageRepository {

    companion object : KLogging()

    override fun getById(id: UUID): SignedVerificationMessage? {
        logger.debug { "Get signed verification message by ID: $id" }
        return dslContext.selectFrom(SignedVerificationMessageTable.SIGNED_VERIFICATION_MESSAGE)
            .where(SignedVerificationMessageTable.SIGNED_VERIFICATION_MESSAGE.ID.eq(id))
            .fetchOne { it.toModel() }
    }

    override fun store(message: SignedVerificationMessage): SignedVerificationMessage {
        logger.info { "Storing signed verification message: $message" }
        dslContext.executeInsert(message.toRecord())
        return message
    }

    override fun deleteById(id: UUID): Boolean {
        logger.info { "Delete signed verification message by ID: $id" }
        return dslContext.deleteFrom(SignedVerificationMessageTable.SIGNED_VERIFICATION_MESSAGE)
            .where(SignedVerificationMessageTable.SIGNED_VERIFICATION_MESSAGE.ID.eq(id))
            .execute() > 0
    }

    override fun deleteAllExpired(): Int {
        val now = utcDateTimeProvider.getUtcDateTime()
        logger.info { "Delete all signed verification messages expired at: $now" }
        return dslContext.deleteFrom(SignedVerificationMessageTable.SIGNED_VERIFICATION_MESSAGE)
            .where(SignedVerificationMessageTable.SIGNED_VERIFICATION_MESSAGE.VALID_UNTIL.le(now.value))
            .execute()
    }

    private fun SignedVerificationMessage.toRecord(): SignedVerificationMessageRecord =
        SignedVerificationMessageRecord(
            id = id,
            walletAddress = walletAddress.rawValue,
            signature = signature,
            createdAt = createdAt.value,
            validUntil = validUntil.value
        )

    private fun SignedVerificationMessageRecord.toModel(): SignedVerificationMessage =
        SignedVerificationMessage(
            id = id!!,
            walletAddress = WalletAddress(walletAddress!!),
            signature = signature!!,
            createdAt = UtcDateTime(createdAt!!),
            validUntil = UtcDateTime(validUntil!!)
        )
}
