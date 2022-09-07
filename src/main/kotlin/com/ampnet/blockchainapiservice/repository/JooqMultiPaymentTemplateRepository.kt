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
class JooqMultiPaymentTemplateRepository(private val dslContext: DSLContext) : MultiPaymentTemplateRepository {

    companion object : KLogging()

    override fun store(multiPaymentTemplate: MultiPaymentTemplate<WithItems>): MultiPaymentTemplate<WithItems> {
        logger.info { "Store multi-payment template: $multiPaymentTemplate" }

        val record = MultiPaymentTemplateRecord(
            id = multiPaymentTemplate.id,
            templateName = multiPaymentTemplate.templateName,
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
                tokenAddress = it.tokenAddress,
                assetAmount = it.assetAmount,
                createdAt = it.createdAt
            )
        }
        dslContext.batchInsert(itemRecords).execute()

        return record.toModel().withItems(itemRecords.map { it.toModel() })
    }

    override fun update(multiPaymentTemplate: MultiPaymentTemplate<*>): MultiPaymentTemplate<WithItems>? {
        logger.info { "Update multi-payment record, multiPaymentTemplate: $multiPaymentTemplate" }
        return dslContext.update(MultiPaymentTemplateTable.MULTI_PAYMENT_TEMPLATE)
            .set(MultiPaymentTemplateTable.MULTI_PAYMENT_TEMPLATE.TEMPLATE_NAME, multiPaymentTemplate.templateName)
            .set(MultiPaymentTemplateTable.MULTI_PAYMENT_TEMPLATE.CHAIN_ID, multiPaymentTemplate.chainId)
            .set(MultiPaymentTemplateTable.MULTI_PAYMENT_TEMPLATE.UPDATED_AT, multiPaymentTemplate.updatedAt)
            .where(
                DSL.and(
                    MultiPaymentTemplateTable.MULTI_PAYMENT_TEMPLATE.ID.eq(multiPaymentTemplate.id),
                    MultiPaymentTemplateTable.MULTI_PAYMENT_TEMPLATE.USER_ID.eq(multiPaymentTemplate.userId)
                )
            )
            .returning()
            .fetchOne { it.toModel() }
            ?.withItems(getItemsById(multiPaymentTemplate.id))
    }

    override fun delete(id: UUID): Boolean {
        logger.info { "Delete multi-payment template, id: $id" }
        dslContext.deleteFrom(MultiPaymentTemplateItemTable.MULTI_PAYMENT_TEMPLATE_ITEM)
            .where(MultiPaymentTemplateItemTable.MULTI_PAYMENT_TEMPLATE_ITEM.TEMPLATE_ID.eq(id))
            .execute()
        return dslContext.deleteFrom(MultiPaymentTemplateTable.MULTI_PAYMENT_TEMPLATE)
            .where(MultiPaymentTemplateTable.MULTI_PAYMENT_TEMPLATE.ID.eq(id))
            .execute() > 0
    }

    override fun getById(id: UUID): MultiPaymentTemplate<WithItems>? {
        logger.debug { "Get multi-payment template by id: $id" }
        return dslContext.selectFrom(MultiPaymentTemplateTable.MULTI_PAYMENT_TEMPLATE)
            .where(MultiPaymentTemplateTable.MULTI_PAYMENT_TEMPLATE.ID.eq(id))
            .fetchOne { it.toModel() }
            ?.withItems(getItemsById(id))
    }

    override fun getItemsById(id: UUID): List<MultiPaymentTemplateItem> {
        logger.debug { "Get multi-payment template items by id: $id" }
        return dslContext.selectFrom(MultiPaymentTemplateItemTable.MULTI_PAYMENT_TEMPLATE_ITEM)
            .where(MultiPaymentTemplateItemTable.MULTI_PAYMENT_TEMPLATE_ITEM.TEMPLATE_ID.eq(id))
            .orderBy(MultiPaymentTemplateItemTable.MULTI_PAYMENT_TEMPLATE_ITEM.CREATED_AT.asc())
            .fetch { it.toModel() }
    }

    override fun getAllByWalletAddress(walletAddress: WalletAddress): List<MultiPaymentTemplate<NoItems>> {
        logger.debug { "Get multi-payment templates by walletAddress: $walletAddress" }
        return dslContext.select(MultiPaymentTemplateTable.MULTI_PAYMENT_TEMPLATE.fields().toList())
            .from(
                MultiPaymentTemplateTable.MULTI_PAYMENT_TEMPLATE.join(UserIdentifierTable.USER_IDENTIFIER)
                    .on(
                        MultiPaymentTemplateTable.MULTI_PAYMENT_TEMPLATE.USER_ID
                            .eq(UserIdentifierTable.USER_IDENTIFIER.ID)
                    )
            )
            .where(
                DSL.and(
                    UserIdentifierTable.USER_IDENTIFIER.IDENTIFIER_TYPE.eq(UserIdentifierType.ETH_WALLET_ADDRESS),
                    UserIdentifierTable.USER_IDENTIFIER.USER_IDENTIFIER_.eq(walletAddress.rawValue)
                )
            )
            .orderBy(MultiPaymentTemplateTable.MULTI_PAYMENT_TEMPLATE.CREATED_AT.asc())
            .fetch { it.into(MultiPaymentTemplateTable.MULTI_PAYMENT_TEMPLATE).toModel() }
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
                    tokenAddress = item.tokenAddress,
                    assetAmount = item.assetAmount,
                    createdAt = item.createdAt
                )
            )
        } catch (_: DataIntegrityViolationException) {
            return null
        }

        dslContext.update(MultiPaymentTemplateTable.MULTI_PAYMENT_TEMPLATE)
            .set(MultiPaymentTemplateTable.MULTI_PAYMENT_TEMPLATE.UPDATED_AT, updatedAt)
            .execute()
        return getById(item.templateId)
    }

    override fun updateItem(item: MultiPaymentTemplateItem, updatedAt: UtcDateTime): MultiPaymentTemplate<WithItems>? {
        logger.info { "Update multi-payment record item, item: $item, updatedAt: $updatedAt" }
        dslContext.update(MultiPaymentTemplateItemTable.MULTI_PAYMENT_TEMPLATE_ITEM)
            .set(MultiPaymentTemplateItemTable.MULTI_PAYMENT_TEMPLATE_ITEM.WALLET_ADDRESS, item.walletAddress)
            .set(MultiPaymentTemplateItemTable.MULTI_PAYMENT_TEMPLATE_ITEM.ITEM_NAME, item.itemName)
            .set(MultiPaymentTemplateItemTable.MULTI_PAYMENT_TEMPLATE_ITEM.TOKEN_ADDRESS, item.tokenAddress)
            .set(MultiPaymentTemplateItemTable.MULTI_PAYMENT_TEMPLATE_ITEM.ASSET_AMOUNT, item.assetAmount)
            .where(MultiPaymentTemplateItemTable.MULTI_PAYMENT_TEMPLATE_ITEM.ID.eq(item.id))
            .execute()
        dslContext.update(MultiPaymentTemplateTable.MULTI_PAYMENT_TEMPLATE)
            .set(MultiPaymentTemplateTable.MULTI_PAYMENT_TEMPLATE.UPDATED_AT, updatedAt)
            .execute()
        return getById(item.templateId)
    }

    override fun deleteItem(id: UUID, itemId: UUID, updatedAt: UtcDateTime): MultiPaymentTemplate<WithItems>? {
        logger.info { "Delete multi-payment template item, id: $id" }
        dslContext.deleteFrom(MultiPaymentTemplateItemTable.MULTI_PAYMENT_TEMPLATE_ITEM)
            .where(MultiPaymentTemplateItemTable.MULTI_PAYMENT_TEMPLATE_ITEM.TEMPLATE_ID.eq(id))
            .execute()
        dslContext.update(MultiPaymentTemplateTable.MULTI_PAYMENT_TEMPLATE)
            .set(MultiPaymentTemplateTable.MULTI_PAYMENT_TEMPLATE.UPDATED_AT, updatedAt)
            .execute()
        return getById(id)
    }

    private fun MultiPaymentTemplateRecord.toModel() =
        MultiPaymentTemplate(
            id = id!!,
            items = NoItems,
            templateName = templateName!!,
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
            tokenAddress = tokenAddress,
            assetAmount = assetAmount!!,
            createdAt = createdAt!!
        )
}
