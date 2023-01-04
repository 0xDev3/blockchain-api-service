package dev3.blockchainapiservice.features.asset.multisend.model.response

import dev3.blockchainapiservice.features.asset.multisend.model.result.MultiPaymentTemplate
import dev3.blockchainapiservice.features.asset.multisend.model.result.NoItems
import dev3.blockchainapiservice.generated.jooq.id.MultiPaymentTemplateId
import dev3.blockchainapiservice.util.AssetType
import java.time.OffsetDateTime

data class MultiPaymentTemplatesResponse(val templates: List<MultiPaymentTemplateWithoutItemsResponse>)

data class MultiPaymentTemplateWithoutItemsResponse(
    val id: MultiPaymentTemplateId,
    val templateName: String,
    val assetType: AssetType,
    val tokenAddress: String?,
    val chainId: Long,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime?
) {
    constructor(template: MultiPaymentTemplate<NoItems>) : this(
        id = template.id,
        templateName = template.templateName,
        assetType = if (template.tokenAddress == null) AssetType.NATIVE else AssetType.TOKEN,
        tokenAddress = template.tokenAddress?.rawValue,
        chainId = template.chainId.value,
        createdAt = template.createdAt.value,
        updatedAt = template.updatedAt?.value
    )
}
