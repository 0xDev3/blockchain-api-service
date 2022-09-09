package com.ampnet.blockchainapiservice.model.response

import com.ampnet.blockchainapiservice.model.result.MultiPaymentTemplate
import com.ampnet.blockchainapiservice.model.result.NoItems
import java.time.OffsetDateTime
import java.util.UUID

data class MultiPaymentTemplatesResponse(val templates: List<MultiPaymentTemplateWithoutItemsResponse>)

data class MultiPaymentTemplateWithoutItemsResponse(
    val id: UUID,
    val templateName: String,
    val chainId: Long,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime?
) {
    constructor(template: MultiPaymentTemplate<NoItems>) : this(
        id = template.id,
        templateName = template.templateName,
        chainId = template.chainId.value,
        createdAt = template.createdAt.value,
        updatedAt = template.updatedAt?.value
    )
}
