package dev3.blockchainapiservice.model.result

import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.UtcDateTime
import dev3.blockchainapiservice.util.WalletAddress
import java.util.UUID

sealed interface ItemsState

object NoItems : ItemsState

@JvmInline
value class WithItems(val value: List<MultiPaymentTemplateItem>) : ItemsState

data class MultiPaymentTemplate<T : ItemsState>(
    val id: UUID,
    val items: T,
    val templateName: String,
    val tokenAddress: ContractAddress?,
    val chainId: ChainId,
    val userId: UUID,
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
    val id: UUID,
    val templateId: UUID,
    val walletAddress: WalletAddress,
    val itemName: String?,
    val assetAmount: Balance,
    val createdAt: UtcDateTime
)
