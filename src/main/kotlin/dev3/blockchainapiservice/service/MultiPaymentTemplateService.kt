package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.generated.jooq.id.MultiPaymentTemplateId
import dev3.blockchainapiservice.generated.jooq.id.MultiPaymentTemplateItemId
import dev3.blockchainapiservice.model.request.CreateMultiPaymentTemplateRequest
import dev3.blockchainapiservice.model.request.MultiPaymentTemplateItemRequest
import dev3.blockchainapiservice.model.request.UpdateMultiPaymentTemplateRequest
import dev3.blockchainapiservice.model.result.MultiPaymentTemplate
import dev3.blockchainapiservice.model.result.NoItems
import dev3.blockchainapiservice.model.result.UserIdentifier
import dev3.blockchainapiservice.model.result.WithItems
import dev3.blockchainapiservice.util.WalletAddress

interface MultiPaymentTemplateService {
    fun createMultiPaymentTemplate(
        request: CreateMultiPaymentTemplateRequest,
        userIdentifier: UserIdentifier
    ): MultiPaymentTemplate<WithItems>

    fun updateMultiPaymentTemplate(
        templateId: MultiPaymentTemplateId,
        request: UpdateMultiPaymentTemplateRequest,
        userIdentifier: UserIdentifier
    ): MultiPaymentTemplate<WithItems>

    fun deleteMultiPaymentTemplateById(templateId: MultiPaymentTemplateId, userIdentifier: UserIdentifier)
    fun getMultiPaymentTemplateById(templateId: MultiPaymentTemplateId): MultiPaymentTemplate<WithItems>
    fun getAllMultiPaymentTemplatesByWalletAddress(walletAddress: WalletAddress): List<MultiPaymentTemplate<NoItems>>
    fun addItemToMultiPaymentTemplate(
        templateId: MultiPaymentTemplateId,
        request: MultiPaymentTemplateItemRequest,
        userIdentifier: UserIdentifier
    ): MultiPaymentTemplate<WithItems>

    fun updateMultiPaymentTemplateItem(
        templateId: MultiPaymentTemplateId,
        itemId: MultiPaymentTemplateItemId,
        request: MultiPaymentTemplateItemRequest,
        userIdentifier: UserIdentifier
    ): MultiPaymentTemplate<WithItems>

    fun deleteMultiPaymentTemplateItem(
        templateId: MultiPaymentTemplateId,
        itemId: MultiPaymentTemplateItemId,
        userIdentifier: UserIdentifier
    ): MultiPaymentTemplate<WithItems>
}
