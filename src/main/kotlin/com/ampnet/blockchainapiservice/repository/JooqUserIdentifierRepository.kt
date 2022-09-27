package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.generated.jooq.enums.UserIdentifierType
import com.ampnet.blockchainapiservice.generated.jooq.tables.UserIdentifierTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.UserIdentifierRecord
import com.ampnet.blockchainapiservice.model.result.UserIdentifier
import com.ampnet.blockchainapiservice.model.result.UserWalletAddressIdentifier
import com.ampnet.blockchainapiservice.util.WalletAddress
import mu.KLogging
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JooqUserIdentifierRepository(private val dslContext: DSLContext) : UserIdentifierRepository {

    companion object : KLogging() {
        private val TABLE = UserIdentifierTable.USER_IDENTIFIER
    }

    override fun store(userIdentifier: UserIdentifier): UserIdentifier {
        logger.info { "Store user identifier: $userIdentifier" }
        val record = UserIdentifierRecord(
            id = userIdentifier.id,
            userIdentifier = userIdentifier.userIdentifier,
            identifierType = userIdentifier.identifierType
        )
        dslContext.executeInsert(record)
        return record.toModel()
    }

    override fun getById(id: UUID): UserIdentifier? {
        logger.debug { "Get user identifier by id: $id" }
        return dslContext.selectFrom(TABLE)
            .where(TABLE.ID.eq(id))
            .fetchOne { it.toModel() }
    }

    override fun getByUserIdentifier(userIdentifier: String, identifierType: UserIdentifierType): UserIdentifier? {
        logger.debug { "Get user identifier by userIdentifier: $userIdentifier, identifierType: $identifierType" }
        return dslContext.selectFrom(TABLE)
            .where(
                DSL.and(
                    TABLE.USER_IDENTIFIER_.eq(userIdentifier),
                    TABLE.IDENTIFIER_TYPE.eq(identifierType)
                )
            )
            .fetchOne { it.toModel() }
    }

    private fun UserIdentifierRecord.toModel(): UserIdentifier =
        when (identifierType) {
            UserIdentifierType.ETH_WALLET_ADDRESS ->
                UserWalletAddressIdentifier(
                    id = id,
                    walletAddress = WalletAddress(userIdentifier)
                )
        }
}
