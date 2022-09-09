package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.exception.ResourceNotFoundException
import com.ampnet.blockchainapiservice.model.request.CreateMultiPaymentTemplateRequest
import com.ampnet.blockchainapiservice.model.request.MultiPaymentTemplateItemRequest
import com.ampnet.blockchainapiservice.model.request.UpdateMultiPaymentTemplateRequest
import com.ampnet.blockchainapiservice.model.result.MultiPaymentTemplate
import com.ampnet.blockchainapiservice.model.result.MultiPaymentTemplateItem
import com.ampnet.blockchainapiservice.model.result.NoItems
import com.ampnet.blockchainapiservice.model.result.UserIdentifier
import com.ampnet.blockchainapiservice.model.result.WithItems
import com.ampnet.blockchainapiservice.repository.MultiPaymentTemplateRepository
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.WalletAddress
import mu.KLogging
import org.springframework.stereotype.Service
import java.util.UUID

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
        val templateId = uuidProvider.getUuid()
        return multiPaymentTemplateRepository.store(
            MultiPaymentTemplate(
                id = templateId,
                items = WithItems(
                    request.items.map {
                        MultiPaymentTemplateItem(
                            id = uuidProvider.getUuid(),
                            templateId = templateId,
                            walletAddress = WalletAddress(it.walletAddress),
                            itemName = it.itemName,
                            tokenAddress = it.tokenAddress?.let { ta -> ContractAddress(ta) },
                            assetAmount = Balance(it.amount),
                            createdAt = utcDateTimeProvider.getUtcDateTime()
                        )
                    }
                ),
                templateName = request.templateName,
                chainId = ChainId(request.chainId),
                userId = userIdentifier.id,
                createdAt = utcDateTimeProvider.getUtcDateTime(),
                updatedAt = null
            )
        )
    }

    override fun updateMultiPaymentTemplate(
        templateId: UUID,
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
                chainId = ChainId(request.chainId),
                updatedAt = utcDateTimeProvider.getUtcDateTime()
            )
        ) ?: throw ResourceNotFoundException("Multi-payment template not found for ID: $templateId")
    }

    override fun deleteMultiPaymentTemplateById(templateId: UUID, userIdentifier: UserIdentifier) {
        logger.info { "Delete multi-payment template by id: $templateId, userIdentifier: $userIdentifier" }
        multiPaymentTemplateRepository.delete(getOwnedMultiPaymentTemplateById(templateId, userIdentifier).id)
    }

    override fun getMultiPaymentTemplateById(templateId: UUID): MultiPaymentTemplate<WithItems> {
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
        templateId: UUID,
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
                id = uuidProvider.getUuid(),
                templateId = template.id,
                walletAddress = WalletAddress(request.walletAddress),
                itemName = request.itemName,
                tokenAddress = request.tokenAddress?.let { ContractAddress(it) },
                assetAmount = Balance(request.amount),
                createdAt = updatedAt
            ),
            updatedAt = updatedAt
        ) ?: throw ResourceNotFoundException("Multi-payment template not found for ID: $templateId")
    }

    override fun updateMultiPaymentTemplateItem(
        templateId: UUID,
        itemId: UUID,
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
                tokenAddress = request.tokenAddress?.let { ContractAddress(it) },
                assetAmount = Balance(request.amount),
                createdAt = updatedAt
            ),
            updatedAt = updatedAt
        ) ?: throw ResourceNotFoundException(
            "Multi-payment template item not found for templateId: $templateId, itemId: $itemId"
        )
    }

    override fun deleteMultiPaymentTemplateItem(
        templateId: UUID,
        itemId: UUID,
        userIdentifier: UserIdentifier
    ): MultiPaymentTemplate<WithItems> {
        logger.info {
            "Delete multi-payment template item, templateId: $templateId, itemId: $itemId," +
                " userIdentifier: $userIdentifier"
        }

        val template = getOwnedMultiPaymentTemplateById(templateId, userIdentifier)
        val updatedAt = utcDateTimeProvider.getUtcDateTime()

        return multiPaymentTemplateRepository.deleteItem(
            id = template.id,
            itemId = itemId,
            updatedAt = updatedAt
        ) ?: throw ResourceNotFoundException(
            "Multi-payment template item not found for templateId: $templateId, itemId: $itemId"
        )
    }

    private fun getOwnedMultiPaymentTemplateById(id: UUID, userIdentifier: UserIdentifier) =
        getMultiPaymentTemplateById(id).takeIf { it.userId == userIdentifier.id }
            ?: throw ResourceNotFoundException("Multi-payment template not found for ID: $id")
}
