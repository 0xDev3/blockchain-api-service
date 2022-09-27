package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.exception.AliasAlreadyInUseException
import com.ampnet.blockchainapiservice.generated.jooq.enums.UserIdentifierType
import com.ampnet.blockchainapiservice.generated.jooq.tables.AddressBookTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.UserIdentifierTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.AddressBookRecord
import com.ampnet.blockchainapiservice.model.result.AddressBookEntry
import com.ampnet.blockchainapiservice.util.WalletAddress
import mu.KLogging
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JooqAddressBookRepository(private val dslContext: DSLContext) : AddressBookRepository {

    companion object : KLogging() {
        private val ADDRESS_BOOK_TABLE = AddressBookTable.ADDRESS_BOOK
        private val USER_IDENTIFIER_TABLE = UserIdentifierTable.USER_IDENTIFIER
    }

    override fun store(addressBookEntry: AddressBookEntry): AddressBookEntry {
        logger.info { "Store address book entry, addressBookEntry: $addressBookEntry" }
        return handleDuplicateAlias(addressBookEntry.alias) {
            val record = addressBookEntry.toRecord()
            dslContext.executeInsert(record)
            record.toModel()
        }
    }

    override fun update(addressBookEntry: AddressBookEntry): AddressBookEntry? {
        logger.info { "Update address book entry, addressBookEntry: $addressBookEntry" }
        return handleDuplicateAlias(addressBookEntry.alias) {
            dslContext.update(ADDRESS_BOOK_TABLE)
                .set(
                    addressBookEntry.toRecord().apply {
                        changed(true)
                        changed(ADDRESS_BOOK_TABLE.ID, false)
                        changed(ADDRESS_BOOK_TABLE.CREATED_AT, false)
                        changed(ADDRESS_BOOK_TABLE.USER_ID, false)
                    }
                )
                .where(
                    DSL.and(
                        ADDRESS_BOOK_TABLE.ID.eq(addressBookEntry.id),
                        ADDRESS_BOOK_TABLE.USER_ID.eq(addressBookEntry.userId)
                    )
                )
                .returning()
                .fetchOne { it.toModel() }
        }
    }

    override fun delete(id: UUID): Boolean {
        logger.info { "Delete address book entry, id: $id" }
        return dslContext.deleteFrom(ADDRESS_BOOK_TABLE)
            .where(ADDRESS_BOOK_TABLE.ID.eq(id))
            .execute() > 0
    }

    override fun getById(id: UUID): AddressBookEntry? {
        logger.debug { "Get address book entry by id: $id" }
        return dslContext.selectFrom(ADDRESS_BOOK_TABLE)
            .where(ADDRESS_BOOK_TABLE.ID.eq(id))
            .fetchOne { it.toModel() }
    }

    override fun getByAliasAndUserId(alias: String, userId: UUID): AddressBookEntry? {
        logger.debug { "Get address book entry by alias and project ID, alias: $alias, userId: $userId" }
        return dslContext.selectFrom(ADDRESS_BOOK_TABLE)
            .where(
                DSL.and(
                    ADDRESS_BOOK_TABLE.ALIAS.eq(alias),
                    ADDRESS_BOOK_TABLE.USER_ID.eq(userId)
                )
            )
            .fetchOne { it.toModel() }
    }

    override fun getAllByWalletAddress(walletAddress: WalletAddress): List<AddressBookEntry> {
        logger.debug { "Get address book entries by walletAddress: $walletAddress" }
        return dslContext.select(ADDRESS_BOOK_TABLE.fields().toList())
            .from(
                ADDRESS_BOOK_TABLE.join(USER_IDENTIFIER_TABLE)
                    .on(ADDRESS_BOOK_TABLE.USER_ID.eq(USER_IDENTIFIER_TABLE.ID))
            )
            .where(
                DSL.and(
                    USER_IDENTIFIER_TABLE.IDENTIFIER_TYPE.eq(UserIdentifierType.ETH_WALLET_ADDRESS),
                    USER_IDENTIFIER_TABLE.USER_IDENTIFIER_.eq(walletAddress.rawValue)
                )
            )
            .orderBy(ADDRESS_BOOK_TABLE.CREATED_AT.asc())
            .fetch { it.into(ADDRESS_BOOK_TABLE).toModel() }
    }

    private fun AddressBookEntry.toRecord() =
        AddressBookRecord(
            id = id,
            alias = alias,
            walletAddress = address,
            phoneNumber = phoneNumber,
            email = email,
            createdAt = createdAt,
            userId = userId
        )

    private fun AddressBookRecord.toModel() =
        AddressBookEntry(
            id = id,
            alias = alias,
            address = walletAddress,
            phoneNumber = phoneNumber,
            email = email,
            createdAt = createdAt,
            userId = userId
        )

    private fun <T> handleDuplicateAlias(alias: String, fn: () -> T): T =
        try {
            fn()
        } catch (e: DuplicateKeyException) {
            throw AliasAlreadyInUseException(alias)
        }
}
