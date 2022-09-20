package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.generated.jooq.enums.UserIdentifierType
import com.ampnet.blockchainapiservice.generated.jooq.tables.MultiPaymentTemplateItemTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.MultiPaymentTemplateTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.UserIdentifierTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.MultiPaymentTemplateItemRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.MultiPaymentTemplateRecord
import com.ampnet.blockchainapiservice.model.result.MultiPaymentTemplate
import com.ampnet.blockchainapiservice.model.result.MultiPaymentTemplateItem
import com.ampnet.blockchainapiservice.model.result.NoItems
import com.ampnet.blockchainapiservice.model.result.WithItems
import com.ampnet.blockchainapiservice.util.UtcDateTime
import com.ampnet.blockchainapiservice.util.WalletAddress
import mu.KLogging
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
@Suppress("TooManyFunctions")
class JooqMultiPaymentTemplateRepository(private val dslContext: DSLContext) : MultiPaymentTemplateRepository {

    companion object : KLogging() {
        private val TEMPLATE_TABLE = MultiPaymentTemplateTable.MULTI_PAYMENT_TEMPLATE
        private val ITEM_TABLE = MultiPaymentTemplateItemTable.MULTI_PAYMENT_TEMPLATE_ITEM
        private val USER_IDENTIFIER_TABLE = UserIdentifierTable.USER_IDENTIFIER
    }

    override fun store(multiPaymentTemplate: MultiPaymentTemplate<WithItems>): MultiPaymentTemplate<WithItems> {
        logger.info { "Store multi-payment template: $multiPaymentTemplate" }

        val record = MultiPaymentTemplateRecord(
            id = multiPaymentTemplate.id,
            templateName = multiPaymentTemplate.templateName,
            tokenAddress = multiPaymentTemplate.tokenAddress,
            chainId = multiPaymentTemplate.chainId,
            userId = multiPaymentTemplate.userId,
            createdAt = multiPaymentTemplate.createdAt,
            updatedAt = null
        )
        dslContext.executeInsert(record)

        val itemRecords = multiPaymentTemplate.items.value.map {
            MultiPaymentTemplateItemRecord(
                id = it.id,
                templateId = multiPaymentTemplate.id,
                walletAddress = it.walletAddress,
                itemName = it.itemName,
                assetAmount = it.assetAmount,
                createdAt = it.createdAt
            )
        }
        dslContext.batchInsert(itemRecords).execute()

        return record.toModel().withItems(itemRecords.map { it.toModel() })
    }

    override fun update(multiPaymentTemplate: MultiPaymentTemplate<*>): MultiPaymentTemplate<WithItems>? {
        logger.info { "Update multi-payment record, multiPaymentTemplate: $multiPaymentTemplate" }
        return dslContext.update(TEMPLATE_TABLE)
            .set(TEMPLATE_TABLE.TEMPLATE_NAME, multiPaymentTemplate.templateName)
            .set(TEMPLATE_TABLE.TOKEN_ADDRESS, multiPaymentTemplate.tokenAddress)
            .set(TEMPLATE_TABLE.CHAIN_ID, multiPaymentTemplate.chainId)
            .set(TEMPLATE_TABLE.UPDATED_AT, multiPaymentTemplate.updatedAt)
            .where(
                DSL.and(
                    TEMPLATE_TABLE.ID.eq(multiPaymentTemplate.id),
                    TEMPLATE_TABLE.USER_ID.eq(multiPaymentTemplate.userId)
                )
            )
            .returning()
            .fetchOne { it.toModel() }
            ?.withItems(getItemsById(multiPaymentTemplate.id))
    }

    override fun delete(id: UUID): Boolean {
        logger.info { "Delete multi-payment template, id: $id" }
        dslContext.deleteFrom(ITEM_TABLE)
            .where(ITEM_TABLE.TEMPLATE_ID.eq(id))
            .execute()
        return dslContext.deleteFrom(TEMPLATE_TABLE)
            .where(TEMPLATE_TABLE.ID.eq(id))
            .execute() > 0
    }

    override fun getById(id: UUID): MultiPaymentTemplate<WithItems>? {
        logger.debug { "Get multi-payment template by id: $id" }
        return dslContext.selectFrom(TEMPLATE_TABLE)
            .where(TEMPLATE_TABLE.ID.eq(id))
            .fetchOne { it.toModel() }
            ?.withItems(getItemsById(id))
    }

    override fun getItemsById(id: UUID): List<MultiPaymentTemplateItem> {
        logger.debug { "Get multi-payment template items by id: $id" }
        return dslContext.selectFrom(ITEM_TABLE)
            .where(ITEM_TABLE.TEMPLATE_ID.eq(id))
            .orderBy(ITEM_TABLE.CREATED_AT.asc())
            .fetch { it.toModel() }
    }

    override fun getAllByWalletAddress(walletAddress: WalletAddress): List<MultiPaymentTemplate<NoItems>> {
        logger.debug { "Get multi-payment templates by walletAddress: $walletAddress" }
        return dslContext.select(TEMPLATE_TABLE.fields().toList())
            .from(TEMPLATE_TABLE.join(USER_IDENTIFIER_TABLE).on(TEMPLATE_TABLE.USER_ID.eq(USER_IDENTIFIER_TABLE.ID)))
            .where(
                DSL.and(
                    USER_IDENTIFIER_TABLE.IDENTIFIER_TYPE.eq(UserIdentifierType.ETH_WALLET_ADDRESS),
                    USER_IDENTIFIER_TABLE.USER_IDENTIFIER_.eq(walletAddress.rawValue)
                )
            )
            .orderBy(TEMPLATE_TABLE.CREATED_AT.asc())
            .fetch { it.into(TEMPLATE_TABLE).toModel() }
    }

    override fun addItem(item: MultiPaymentTemplateItem, updatedAt: UtcDateTime): MultiPaymentTemplate<WithItems>? {
        logger.info { "Store multi-payment template item, item: $item, updatedAt: $updatedAt" }

        try {
            dslContext.executeInsert(
                MultiPaymentTemplateItemRecord(
                    id = item.id,
                    templateId = item.templateId,
                    walletAddress = item.walletAddress,
                    itemName = item.itemName,
                    assetAmount = item.assetAmount,
                    createdAt = item.createdAt
                )
            )
        } catch (_: DataIntegrityViolationException) {
            return null
        }

        dslContext.update(TEMPLATE_TABLE)
            .set(TEMPLATE_TABLE.UPDATED_AT, updatedAt)
            .execute()
        return getById(item.templateId)
    }

    override fun updateItem(item: MultiPaymentTemplateItem, updatedAt: UtcDateTime): MultiPaymentTemplate<WithItems>? {
        logger.info { "Update multi-payment record item, item: $item, updatedAt: $updatedAt" }
        dslContext.update(ITEM_TABLE)
            .set(ITEM_TABLE.WALLET_ADDRESS, item.walletAddress)
            .set(ITEM_TABLE.ITEM_NAME, item.itemName)
            .set(ITEM_TABLE.ASSET_AMOUNT, item.assetAmount)
            .where(ITEM_TABLE.ID.eq(item.id))
            .execute()
        dslContext.update(TEMPLATE_TABLE)
            .set(TEMPLATE_TABLE.UPDATED_AT, updatedAt)
            .execute()
        return getById(item.templateId)
    }

    override fun deleteItem(id: UUID, itemId: UUID, updatedAt: UtcDateTime): MultiPaymentTemplate<WithItems>? {
        logger.info { "Delete multi-payment template item, id: $id" }
        dslContext.deleteFrom(ITEM_TABLE)
            .where(ITEM_TABLE.TEMPLATE_ID.eq(id))
            .execute()
        dslContext.update(TEMPLATE_TABLE)
            .set(TEMPLATE_TABLE.UPDATED_AT, updatedAt)
            .execute()
        return getById(id)
    }

    private fun MultiPaymentTemplateRecord.toModel() =
        MultiPaymentTemplate(
            id = id!!,
            items = NoItems,
            templateName = templateName!!,
            tokenAddress = tokenAddress,
            chainId = chainId!!,
            userId = userId!!,
            createdAt = createdAt!!,
            updatedAt = updatedAt
        )

    private fun MultiPaymentTemplateItemRecord.toModel() =
        MultiPaymentTemplateItem(
            id = id!!,
            templateId = templateId!!,
            walletAddress = walletAddress!!,
            itemName = itemName,
            assetAmount = assetAmount!!,
            createdAt = createdAt!!
        )
}
