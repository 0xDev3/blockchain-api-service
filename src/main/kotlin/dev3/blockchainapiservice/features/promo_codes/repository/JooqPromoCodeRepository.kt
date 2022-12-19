package dev3.blockchainapiservice.features.promo_codes.repository

import dev3.blockchainapiservice.features.promo_codes.model.result.PromoCode
import dev3.blockchainapiservice.generated.jooq.tables.PromoCodeTable
import dev3.blockchainapiservice.generated.jooq.tables.records.PromoCodeRecord
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
            .fetch { it.toModel() }
    }

    override fun useCode(code: String, currentTime: UtcDateTime): PromoCode? {
        logger.info { "Using promo code, code: $code, currentTime: $currentTime" }
        return dslContext.update(PromoCodeTable)
            .set(PromoCodeTable.NUM_OF_USAGES, PromoCodeTable.NUM_OF_USAGES + 1)
            .where(
                DSL.and(
                    PromoCodeTable.CODE.eq(code),
                    PromoCodeTable.VALID_UNTIL.ge(currentTime)
                )
            )
            .returning()
            .fetchOne { it.toModel() }
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
