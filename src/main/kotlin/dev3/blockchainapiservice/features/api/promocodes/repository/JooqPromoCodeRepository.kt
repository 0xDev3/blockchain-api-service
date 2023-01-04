package dev3.blockchainapiservice.features.api.promocodes.repository

import dev3.blockchainapiservice.features.api.promocodes.model.result.PromoCode
import dev3.blockchainapiservice.features.api.promocodes.model.result.PromoCodeAlreadyUsed
import dev3.blockchainapiservice.features.api.promocodes.model.result.PromoCodeDoesNotExist
import dev3.blockchainapiservice.features.api.promocodes.model.result.PromoCodeExpired
import dev3.blockchainapiservice.features.api.promocodes.model.result.PromoCodeResult
import dev3.blockchainapiservice.generated.jooq.id.UserId
import dev3.blockchainapiservice.generated.jooq.tables.PromoCodeTable
import dev3.blockchainapiservice.generated.jooq.tables.PromoCodeUsageTable
import dev3.blockchainapiservice.generated.jooq.tables.records.PromoCodeRecord
import dev3.blockchainapiservice.generated.jooq.tables.records.PromoCodeUsageRecord
import dev3.blockchainapiservice.util.UtcDateTime
import mu.KLogging
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class JooqPromoCodeRepository(private val dslContext: DSLContext) : PromoCodeRepository {

    companion object : KLogging()

    override fun storeCode(code: String, writeRequests: Long, readRequests: Long, validUntil: UtcDateTime): PromoCode {
        logger.info {
            "Store promo code, code: $code, writeRequests: $writeRequests, readRequests: $readRequests," +
                " validUntil: $validUntil"
        }

        val record = PromoCodeRecord(
            code = code,
            writeRequests = writeRequests,
            readRequests = readRequests,
            numOfUsages = 0,
            validUntil = validUntil
        )

        dslContext.executeInsert(record)

        return record.toModel()
    }

    override fun getCodes(validFrom: UtcDateTime, validUntil: UtcDateTime): List<PromoCode> {
        logger.debug { "Get promo codes, validFrom: $validFrom, validUntil: $validUntil" }

        return dslContext.selectFrom(PromoCodeTable)
            .where(PromoCodeTable.VALID_UNTIL.between(validFrom, validUntil))
            .orderBy(PromoCodeTable.VALID_UNTIL.desc())
            .fetch { it.toModel() }
    }

    override fun useCode(code: String, userId: UserId, currentTime: UtcDateTime): PromoCodeResult {
        logger.info { "Using promo code, code: $code, userId: $userId, currentTime: $currentTime" }

        val promoCodeExists = dslContext.fetchExists(PromoCodeTable, PromoCodeTable.CODE.eq(code))

        return if (promoCodeExists) {
            dslContext.insertInto(PromoCodeUsageTable)
                .set(
                    PromoCodeUsageRecord(
                        userId = userId,
                        code = code,
                        usedAt = currentTime
                    )
                )
                .onConflictDoNothing()
                .returning()
                .fetchOne()
                ?.let { usePromoCodeIfExists(code, userId, currentTime) }
                ?: PromoCodeAlreadyUsed
        } else {
            PromoCodeDoesNotExist
        }
    }

    private fun usePromoCodeIfExists(code: String, userId: UserId, currentTime: UtcDateTime): PromoCodeResult {
        val result = dslContext.update(PromoCodeTable)
            .set(PromoCodeTable.NUM_OF_USAGES, PromoCodeTable.NUM_OF_USAGES + 1)
            .where(
                DSL.and(
                    PromoCodeTable.CODE.eq(code),
                    PromoCodeTable.VALID_UNTIL.ge(currentTime)
                )
            )
            .returning()
            .fetchOne { it.toModel() }

        return if (result != null) {
            result
        } else {
            dslContext.deleteFrom(PromoCodeUsageTable)
                .where(
                    DSL.and(
                        PromoCodeUsageTable.USER_ID.eq(userId),
                        PromoCodeUsageTable.CODE.eq(code)
                    )
                )
                .execute()
            PromoCodeExpired
        }
    }

    private fun PromoCodeRecord.toModel() =
        PromoCode(
            code = code,
            writeRequests = writeRequests,
            readRequests = readRequests,
            numOfUsages = numOfUsages,
            validUntil = validUntil
        )
}
