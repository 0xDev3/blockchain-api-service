package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.model.request.CreateMultiPaymentTemplateRequest
import com.ampnet.blockchainapiservice.model.request.MultiPaymentTemplateItemRequest
import com.ampnet.blockchainapiservice.model.request.UpdateMultiPaymentTemplateRequest
import com.ampnet.blockchainapiservice.model.result.MultiPaymentTemplate
import com.ampnet.blockchainapiservice.model.result.NoItems
import com.ampnet.blockchainapiservice.model.result.UserIdentifier
import com.ampnet.blockchainapiservice.model.result.WithItems
import com.ampnet.blockchainapiservice.util.WalletAddress
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
