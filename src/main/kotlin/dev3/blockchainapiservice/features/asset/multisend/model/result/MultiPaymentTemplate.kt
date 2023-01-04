package dev3.blockchainapiservice.features.asset.multisend.model.result

import dev3.blockchainapiservice.generated.jooq.id.MultiPaymentTemplateId
import dev3.blockchainapiservice.generated.jooq.id.MultiPaymentTemplateItemId
import dev3.blockchainapiservice.generated.jooq.id.UserId
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.UtcDateTime
import dev3.blockchainapiservice.util.WalletAddress

sealed interface ItemsState

object NoItems : ItemsState

@JvmInline
value class WithItems(val value: List<MultiPaymentTemplateItem>) : ItemsState

data class MultiPaymentTemplate<T : ItemsState>(
    val id: MultiPaymentTemplateId,
    val items: T,
    val templateName: String,
    val tokenAddress: ContractAddress?,
    val chainId: ChainId,
    val userId: UserId,
    val createdAt: UtcDateTime,
    val updatedAt: UtcDateTime?
) {
    fun withItems(items: List<MultiPaymentTemplateItem>) =
        MultiPaymentTemplate(
            id = id,
            items = WithItems(items),
            templateName = templateName,
            tokenAddress = tokenAddress,
            chainId = chainId,
            userId = userId,
            createdAt = createdAt,
            updatedAt = updatedAt
        )

    fun withoutItems() =
        MultiPaymentTemplate(
            id = id,
            items = NoItems,
            templateName = templateName,
            tokenAddress = tokenAddress,
            chainId = chainId,
            userId = userId,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
}

data class MultiPaymentTemplateItem(
    val id: MultiPaymentTemplateItemId,
    val templateId: MultiPaymentTemplateId,
    val walletAddress: WalletAddress,
    val itemName: String?,
    val assetAmount: Balance,
    val createdAt: UtcDateTime
)
