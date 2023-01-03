package dev3.blockchainapiservice.repository

import dev3.blockchainapiservice.generated.jooq.enums.UserIdentifierType
import dev3.blockchainapiservice.generated.jooq.id.UserId
import dev3.blockchainapiservice.generated.jooq.tables.UserIdentifierTable
import dev3.blockchainapiservice.generated.jooq.tables.records.UserIdentifierRecord
import dev3.blockchainapiservice.model.result.UserIdentifier
import dev3.blockchainapiservice.model.result.UserWalletAddressIdentifier
import dev3.blockchainapiservice.util.WalletAddress
import mu.KLogging
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class JooqUserIdentifierRepository(private val dslContext: DSLContext) : UserIdentifierRepository {

    companion object : KLogging()

    override fun store(userIdentifier: UserIdentifier): UserIdentifier {
        logger.info { "Store user identifier: $userIdentifier" }
        val record = UserIdentifierRecord(
            id = userIdentifier.id,
            userIdentifier = userIdentifier.userIdentifier,
            identifierType = userIdentifier.identifierType,
            stripeClientId = null
        )
        dslContext.executeInsert(record)
        return record.toModel()
    }

    override fun getById(id: UserId): UserIdentifier? {
        logger.debug { "Get user identifier by id: $id" }
        return dslContext.selectFrom(UserIdentifierTable)
            .where(UserIdentifierTable.ID.eq(id))
            .fetchOne { it.toModel() }
    }

    override fun getByUserIdentifier(userIdentifier: String, identifierType: UserIdentifierType): UserIdentifier? {
        logger.debug { "Get user identifier by userIdentifier: $userIdentifier, identifierType: $identifierType" }
        return dslContext.selectFrom(UserIdentifierTable)
            .where(
                DSL.and(
                    UserIdentifierTable.USER_IDENTIFIER_.eq(userIdentifier),
                    UserIdentifierTable.IDENTIFIER_TYPE.eq(identifierType)
                )
            )
            .fetchOne { it.toModel() }
    }

    override fun getByStripeClientId(stripeClientId: String): UserIdentifier? {
        logger.debug { "Get user by Stripe client id, stripeClientId: $stripeClientId" }
        return dslContext.selectFrom(UserIdentifierTable)
            .where(UserIdentifierTable.STRIPE_CLIENT_ID.eq(stripeClientId))
            .fetchOne { it.toModel() }
    }

    override fun setStripeClientId(id: UserId, stripeClientId: String): Boolean {
        logger.info { "Set Stripe client id, id: $id, stripeClientId: $stripeClientId" }
        return dslContext.update(UserIdentifierTable)
            .set(UserIdentifierTable.STRIPE_CLIENT_ID, stripeClientId)
            .where(UserIdentifierTable.ID.eq(id))
            .execute() > 0
    }

    private fun UserIdentifierRecord.toModel(): UserIdentifier =
        when (identifierType) {
            UserIdentifierType.ETH_WALLET_ADDRESS ->
                UserWalletAddressIdentifier(
                    id = id,
                    walletAddress = WalletAddress(userIdentifier),
                    stripeClientId = stripeClientId
                )
        }
}
