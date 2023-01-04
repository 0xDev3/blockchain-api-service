package dev3.blockchainapiservice.features.asset.multisend.model.response

import dev3.blockchainapiservice.features.asset.multisend.model.result.MultiPaymentTemplate
import dev3.blockchainapiservice.features.asset.multisend.model.result.MultiPaymentTemplateItem
import dev3.blockchainapiservice.features.asset.multisend.model.result.WithItems
import dev3.blockchainapiservice.generated.jooq.id.MultiPaymentTemplateId
import dev3.blockchainapiservice.generated.jooq.id.MultiPaymentTemplateItemId
import dev3.blockchainapiservice.util.AssetType
import java.math.BigInteger
import java.time.OffsetDateTime

data class MultiPaymentTemplateWithItemsResponse(
    val id: MultiPaymentTemplateId,
    val items: List<MultiPaymentTemplateItemResponse>,
    val templateName: String,
    val assetType: AssetType,
    val tokenAddress: String?,
    val chainId: Long,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime?
) {
    constructor(template: MultiPaymentTemplate<WithItems>) : this(
        id = template.id,
        items = template.items.value.map { MultiPaymentTemplateItemResponse(it) },
        templateName = template.templateName,
        assetType = if (template.tokenAddress == null) AssetType.NATIVE else AssetType.TOKEN,
        tokenAddress = template.tokenAddress?.rawValue,
        chainId = template.chainId.value,
        createdAt = template.createdAt.value,
        updatedAt = template.updatedAt?.value
    )
}

data class MultiPaymentTemplateItemResponse(
    val id: MultiPaymentTemplateItemId,
    val templateId: MultiPaymentTemplateId,
    val walletAddress: String,
    val itemName: String?,
    val amount: BigInteger,
    val createdAt: OffsetDateTime
) {
    constructor(item: MultiPaymentTemplateItem) : this(
        id = item.id,
        templateId = item.templateId,
        walletAddress = item.walletAddress.rawValue,
        itemName = item.itemName,
        amount = item.assetAmount.rawValue,
        createdAt = item.createdAt.value
    )
}
