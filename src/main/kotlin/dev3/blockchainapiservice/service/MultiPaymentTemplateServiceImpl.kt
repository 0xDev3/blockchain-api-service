package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.exception.ResourceNotFoundException
import dev3.blockchainapiservice.generated.jooq.id.MultiPaymentTemplateId
import dev3.blockchainapiservice.generated.jooq.id.MultiPaymentTemplateItemId
import dev3.blockchainapiservice.model.request.CreateMultiPaymentTemplateRequest
import dev3.blockchainapiservice.model.request.MultiPaymentTemplateItemRequest
import dev3.blockchainapiservice.model.request.UpdateMultiPaymentTemplateRequest
import dev3.blockchainapiservice.model.result.MultiPaymentTemplate
import dev3.blockchainapiservice.model.result.MultiPaymentTemplateItem
import dev3.blockchainapiservice.model.result.NoItems
import dev3.blockchainapiservice.model.result.UserIdentifier
import dev3.blockchainapiservice.model.result.WithItems
import dev3.blockchainapiservice.repository.MultiPaymentTemplateRepository
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.WalletAddress
import mu.KLogging
import org.springframework.stereotype.Service

@Service
class MultiPaymentTemplateServiceImpl(
    private val multiPaymentTemplateRepository: MultiPaymentTemplateRepository,
    private val uuidProvider: UuidProvider,
    private val utcDateTimeProvider: UtcDateTimeProvider
) : MultiPaymentTemplateService {

    companion object : KLogging()

    override fun createMultiPaymentTemplate(
        request: CreateMultiPaymentTemplateRequest,
        userIdentifier: UserIdentifier
    ): MultiPaymentTemplate<WithItems> {
        logger.info { "Creating multi-payment template, request: $request" }
        val templateId = uuidProvider.getUuid(MultiPaymentTemplateId)
        return multiPaymentTemplateRepository.store(
            MultiPaymentTemplate(
                id = templateId,
                items = WithItems(
                    request.items.map {
                        MultiPaymentTemplateItem(
                            id = uuidProvider.getUuid(MultiPaymentTemplateItemId),
                            templateId = templateId,
                            walletAddress = WalletAddress(it.walletAddress),
                            itemName = it.itemName,
                            assetAmount = Balance(it.amount),
                            createdAt = utcDateTimeProvider.getUtcDateTime()
                        )
                    }
                ),
                templateName = request.templateName,
                tokenAddress = request.tokenAddress?.let { ContractAddress(it) },
                chainId = ChainId(request.chainId),
                userId = userIdentifier.id,
                createdAt = utcDateTimeProvider.getUtcDateTime(),
                updatedAt = null
            )
        )
    }

    override fun updateMultiPaymentTemplate(
        templateId: MultiPaymentTemplateId,
        request: UpdateMultiPaymentTemplateRequest,
        userIdentifier: UserIdentifier
    ): MultiPaymentTemplate<WithItems> {
        logger.info {
            "Update multi-payment template, templateId: $templateId, request: $request," +
                " userIdentifier: $userIdentifier"
        }
        val template = getOwnedMultiPaymentTemplateById(templateId, userIdentifier)
        return multiPaymentTemplateRepository.update(
            template.copy(
                templateName = request.templateName,
                tokenAddress = request.tokenAddress?.let { ContractAddress(it) },
                chainId = ChainId(request.chainId),
                updatedAt = utcDateTimeProvider.getUtcDateTime()
            )
        ) ?: throw ResourceNotFoundException("Multi-payment template not found for ID: $templateId")
    }

    override fun deleteMultiPaymentTemplateById(templateId: MultiPaymentTemplateId, userIdentifier: UserIdentifier) {
        logger.info { "Delete multi-payment template by id: $templateId, userIdentifier: $userIdentifier" }
        multiPaymentTemplateRepository.delete(getOwnedMultiPaymentTemplateById(templateId, userIdentifier).id)
    }

    override fun getMultiPaymentTemplateById(templateId: MultiPaymentTemplateId): MultiPaymentTemplate<WithItems> {
        logger.debug { "Get multi-payment template by id: $templateId" }
        return multiPaymentTemplateRepository.getById(templateId)
            ?: throw ResourceNotFoundException("Multi-payment template not found for ID: $templateId")
    }

    override fun getAllMultiPaymentTemplatesByWalletAddress(
        walletAddress: WalletAddress
    ): List<MultiPaymentTemplate<NoItems>> {
        logger.debug { "Get multi-payment templates by walletAddress: $walletAddress" }
        return multiPaymentTemplateRepository.getAllByWalletAddress(walletAddress)
    }

    override fun addItemToMultiPaymentTemplate(
        templateId: MultiPaymentTemplateId,
        request: MultiPaymentTemplateItemRequest,
        userIdentifier: UserIdentifier
    ): MultiPaymentTemplate<WithItems> {
        logger.info {
            "Add item to multi-payment template, templateId: $templateId, request: $request," +
                " userIdentifier: $userIdentifier"
        }

        val template = getOwnedMultiPaymentTemplateById(templateId, userIdentifier)
        val updatedAt = utcDateTimeProvider.getUtcDateTime()

        return multiPaymentTemplateRepository.addItem(
            item = MultiPaymentTemplateItem(
                id = uuidProvider.getUuid(MultiPaymentTemplateItemId),
                templateId = template.id,
                walletAddress = WalletAddress(request.walletAddress),
                itemName = request.itemName,
                assetAmount = Balance(request.amount),
                createdAt = updatedAt
            ),
            updatedAt = updatedAt
        ) ?: throw ResourceNotFoundException("Multi-payment template not found for ID: $templateId")
    }

    override fun updateMultiPaymentTemplateItem(
        templateId: MultiPaymentTemplateId,
        itemId: MultiPaymentTemplateItemId,
        request: MultiPaymentTemplateItemRequest,
        userIdentifier: UserIdentifier
    ): MultiPaymentTemplate<WithItems> {
        logger.info {
            "Update multi-payment template item, templateId: $templateId, itemId: $itemId, request: $request," +
                " userIdentifier: $userIdentifier"
        }

        val template = getOwnedMultiPaymentTemplateById(templateId, userIdentifier)
        val updatedAt = utcDateTimeProvider.getUtcDateTime()

        return multiPaymentTemplateRepository.updateItem(
            item = MultiPaymentTemplateItem(
                id = itemId,
                templateId = template.id,
                walletAddress = WalletAddress(request.walletAddress),
                itemName = request.itemName,
                assetAmount = Balance(request.amount),
                createdAt = updatedAt
            ),
            updatedAt = updatedAt
        ) ?: throw ResourceNotFoundException(
            "Multi-payment template item not found for templateId: $templateId, itemId: $itemId"
        )
    }

    override fun deleteMultiPaymentTemplateItem(
        templateId: MultiPaymentTemplateId,
        itemId: MultiPaymentTemplateItemId,
        userIdentifier: UserIdentifier
    ): MultiPaymentTemplate<WithItems> {
        logger.info {
            "Delete multi-payment template item, templateId: $templateId, itemId: $itemId," +
                " userIdentifier: $userIdentifier"
        }

        val template = getOwnedMultiPaymentTemplateById(templateId, userIdentifier)
        val updatedAt = utcDateTimeProvider.getUtcDateTime()

        return multiPaymentTemplateRepository.deleteItem(
            templateId = template.id,
            itemId = itemId,
            updatedAt = updatedAt
        ) ?: throw ResourceNotFoundException(
            "Multi-payment template item not found for templateId: $templateId, itemId: $itemId"
        )
    }

    private fun getOwnedMultiPaymentTemplateById(id: MultiPaymentTemplateId, userIdentifier: UserIdentifier) =
        getMultiPaymentTemplateById(id).takeIf { it.userId == userIdentifier.id }
            ?: throw ResourceNotFoundException("Multi-payment template not found for ID: $id")
}
