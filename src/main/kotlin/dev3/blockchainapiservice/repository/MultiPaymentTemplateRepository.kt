package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.model.result.MultiPaymentTemplate
import com.ampnet.blockchainapiservice.model.result.MultiPaymentTemplateItem
import com.ampnet.blockchainapiservice.model.result.NoItems
import com.ampnet.blockchainapiservice.model.result.WithItems
import com.ampnet.blockchainapiservice.util.UtcDateTime
import com.ampnet.blockchainapiservice.util.WalletAddress
import java.util.UUID

interface MultiPaymentTemplateRepository {
    fun store(multiPaymentTemplate: MultiPaymentTemplate<WithItems>): MultiPaymentTemplate<WithItems>
    fun update(multiPaymentTemplate: MultiPaymentTemplate<*>): MultiPaymentTemplate<WithItems>?
    fun delete(id: UUID): Boolean
    fun getById(id: UUID): MultiPaymentTemplate<WithItems>?
    fun getItemsById(id: UUID): List<MultiPaymentTemplateItem>
    fun getAllByWalletAddress(walletAddress: WalletAddress): List<MultiPaymentTemplate<NoItems>>
    fun addItem(item: MultiPaymentTemplateItem, updatedAt: UtcDateTime): MultiPaymentTemplate<WithItems>?
    fun updateItem(item: MultiPaymentTemplateItem, updatedAt: UtcDateTime): MultiPaymentTemplate<WithItems>?
    fun deleteItem(templateId: UUID, itemId: UUID, updatedAt: UtcDateTime): MultiPaymentTemplate<WithItems>?
}
