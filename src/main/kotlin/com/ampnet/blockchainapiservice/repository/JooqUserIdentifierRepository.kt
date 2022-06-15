package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.generated.jooq.enums.UserIdentifierType
import com.ampnet.blockchainapiservice.generated.jooq.tables.UserIdentifierTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.interfaces.IUserIdentifierRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.UserIdentifierRecord
import com.ampnet.blockchainapiservice.model.result.UserIdentifier
import mu.KLogging
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JooqUserIdentifierRepository(private val dslContext: DSLContext) : UserIdentifierRepository {

    companion object : KLogging()

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
        return dslContext.selectFrom(UserIdentifierTable.USER_IDENTIFIER)
            .where(UserIdentifierTable.USER_IDENTIFIER.ID.eq(id))
            .fetchOne { it.toModel() }
    }

    override fun getByUserIdentifier(userIdentifier: String, identifierType: UserIdentifierType): UserIdentifier? {
        logger.debug { "Get user identifier by userIdentifier: $userIdentifier, identifierType: $identifierType" }
        return dslContext.selectFrom(UserIdentifierTable.USER_IDENTIFIER)
            .where(
                DSL.and(
                    UserIdentifierTable.USER_IDENTIFIER.USER_IDENTIFIER_.eq(userIdentifier),
                    UserIdentifierTable.USER_IDENTIFIER.IDENTIFIER_TYPE.eq(identifierType)
                )
            )
            .fetchOne { it.toModel() }
    }

    private fun IUserIdentifierRecord.toModel(): UserIdentifier =
        UserIdentifier(
            id = id!!,
            userIdentifier = userIdentifier!!,
            identifierType = identifierType!!
        )
}
