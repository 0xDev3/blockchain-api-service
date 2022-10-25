package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.model.request.CreateMultiPaymentTemplateRequest
import dev3.blockchainapiservice.model.request.MultiPaymentTemplateItemRequest
import dev3.blockchainapiservice.model.request.UpdateMultiPaymentTemplateRequest
import dev3.blockchainapiservice.model.result.MultiPaymentTemplate
import dev3.blockchainapiservice.model.result.NoItems
import dev3.blockchainapiservice.model.result.UserIdentifier
import dev3.blockchainapiservice.model.result.WithItems
import dev3.blockchainapiservice.util.WalletAddress
import java.util.UUID

interface MultiPaymentTemplateService {
    fun createMultiPaymentTemplate(
        request: CreateMultiPaymentTemplateRequest,
        userIdentifier: UserIdentifier
    ): MultiPaymentTemplate<WithItems>

    fun updateMultiPaymentTemplate(
        templateId: UUID,
        request: UpdateMultiPaymentTemplateRequest,
        userIdentifier: UserIdentifier
    ): MultiPaymentTemplate<WithItems>

    fun deleteMultiPaymentTemplateById(templateId: UUID, userIdentifier: UserIdentifier)
    fun getMultiPaymentTemplateById(templateId: UUID): MultiPaymentTemplate<WithItems>
    fun getAllMultiPaymentTemplatesByWalletAddress(walletAddress: WalletAddress): List<MultiPaymentTemplate<NoItems>>
    fun addItemToMultiPaymentTemplate(
        templateId: UUID,
        request: MultiPaymentTemplateItemRequest,
        userIdentifier: UserIdentifier
    ): MultiPaymentTemplate<WithItems>

    fun updateMultiPaymentTemplateItem(
        templateId: UUID,
        itemId: UUID,
        request: MultiPaymentTemplateItemRequest,
        userIdentifier: UserIdentifier
    ): MultiPaymentTemplate<WithItems>

    fun deleteMultiPaymentTemplateItem(
        templateId: UUID,
        itemId: UUID,
        userIdentifier: UserIdentifier
    ): MultiPaymentTemplate<WithItems>
}
