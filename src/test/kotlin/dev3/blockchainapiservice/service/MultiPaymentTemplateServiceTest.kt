package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.exception.ResourceNotFoundException
import com.ampnet.blockchainapiservice.model.request.CreateMultiPaymentTemplateRequest
import com.ampnet.blockchainapiservice.model.request.MultiPaymentTemplateItemRequest
import com.ampnet.blockchainapiservice.model.request.UpdateMultiPaymentTemplateRequest
import com.ampnet.blockchainapiservice.model.result.MultiPaymentTemplate
import com.ampnet.blockchainapiservice.model.result.MultiPaymentTemplateItem
import com.ampnet.blockchainapiservice.model.result.UserWalletAddressIdentifier
import com.ampnet.blockchainapiservice.model.result.WithItems
import com.ampnet.blockchainapiservice.repository.MultiPaymentTemplateRepository
import com.ampnet.blockchainapiservice.util.AssetType
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoMoreInteractions
import java.math.BigInteger
import java.time.Duration
import java.util.UUID
import org.mockito.kotlin.verify as verifyMock

class MultiPaymentTemplateServiceTest : TestBase() {

    companion object {
        private val USER_IDENTIFIER = UserWalletAddressIdentifier(
            id = UUID.randomUUID(),
            walletAddress = WalletAddress("cafebabe")
        )
        private val TEMPLATE_ID = UUID.randomUUID()
        private val ITEM = MultiPaymentTemplateItem(
            id = UUID.randomUUID(),
            templateId = TEMPLATE_ID,
            walletAddress = WalletAddress("a"),
            itemName = "itemName",
            assetAmount = Balance(BigInteger.TEN),
            createdAt = TestData.TIMESTAMP
        )
        private val TEMPLATE = MultiPaymentTemplate(
            id = TEMPLATE_ID,
            items = WithItems(listOf(ITEM)),
            templateName = "templateName",
            tokenAddress = ContractAddress("b"),
            chainId = ChainId(1337L),
            userId = USER_IDENTIFIER.id,
            createdAt = TestData.TIMESTAMP,
            updatedAt = null
        )
        private val ITEM_REQUEST = MultiPaymentTemplateItemRequest(
            walletAddress = ITEM.walletAddress.rawValue,
            itemName = ITEM.itemName,
            amount = ITEM.assetAmount.rawValue
        )
        private val CREATE_REQUEST = CreateMultiPaymentTemplateRequest(
            templateName = TEMPLATE.templateName,
            assetType = AssetType.TOKEN,
            tokenAddress = TEMPLATE.tokenAddress?.rawValue,
            chainId = TEMPLATE.chainId.value,
            items = listOf(ITEM_REQUEST)
        )
        private val UPDATE_REQUEST = UpdateMultiPaymentTemplateRequest(
            templateName = "newName",
            assetType = AssetType.NATIVE,
            tokenAddress = null,
            chainId = 123L
        )
    }

    @Test
    fun mustSuccessfullyCreateMultiPaymentTemplate() {
        val uuidProvider = mock<UuidProvider>()

        suppose("some UUID will be returned") {
            given(uuidProvider.getUuid())
                .willReturn(TEMPLATE_ID, ITEM.id)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some timestamp will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(ITEM.createdAt, TEMPLATE.createdAt)
        }

        val multiPaymentTemplateRepository = mock<MultiPaymentTemplateRepository>()

        suppose("multi-payment template will be stored into the database") {
            given(multiPaymentTemplateRepository.store(TEMPLATE))
                .willReturn(TEMPLATE)
        }

        val service = MultiPaymentTemplateServiceImpl(
            multiPaymentTemplateRepository = multiPaymentTemplateRepository,
            uuidProvider = uuidProvider,
            utcDateTimeProvider = utcDateTimeProvider
        )

        verify("multi-payment template is stored into the database") {
            assertThat(service.createMultiPaymentTemplate(CREATE_REQUEST, USER_IDENTIFIER)).withMessage()
                .isEqualTo(TEMPLATE)

            verifyMock(multiPaymentTemplateRepository)
                .store(TEMPLATE)
            verifyNoMoreInteractions(multiPaymentTemplateRepository)
        }
    }

    @Test
    fun mustCorrectlyUpdateMultiPaymentTemplate() {
        val utcDateTimeProvider = mock<UtcDateTimeProvider>()
        val updatedAt = TestData.TIMESTAMP + Duration.ofSeconds(10L)

        suppose("some timestamp will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(updatedAt)
        }

        val multiPaymentTemplateRepository = mock<MultiPaymentTemplateRepository>()

        suppose("multi-payment template is fetched by id") {
            given(multiPaymentTemplateRepository.getById(TEMPLATE_ID))
                .willReturn(TEMPLATE)
        }

        val updatedTemplate = TEMPLATE.copy(
            templateName = UPDATE_REQUEST.templateName,
            tokenAddress = UPDATE_REQUEST.tokenAddress?.let { ContractAddress(it) },
            chainId = ChainId(UPDATE_REQUEST.chainId),
            updatedAt = updatedAt
        )

        suppose("multi-payment template will be updated in the database") {
            given(multiPaymentTemplateRepository.update(updatedTemplate))
                .willReturn(updatedTemplate)
        }

        val service = MultiPaymentTemplateServiceImpl(
            multiPaymentTemplateRepository = multiPaymentTemplateRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = utcDateTimeProvider
        )

        verify("multi-payment template is updated in the database") {
            assertThat(service.updateMultiPaymentTemplate(TEMPLATE_ID, UPDATE_REQUEST, USER_IDENTIFIER)).withMessage()
                .isEqualTo(updatedTemplate)

            verifyMock(multiPaymentTemplateRepository)
                .getById(TEMPLATE_ID)
            verifyMock(multiPaymentTemplateRepository)
                .update(updatedTemplate)
            verifyNoMoreInteractions(multiPaymentTemplateRepository)
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenUpdatingNonOwnedMultiPaymentTemplate() {
        val multiPaymentTemplateRepository = mock<MultiPaymentTemplateRepository>()

        suppose("non-owned multi-payment template is fetched by id") {
            given(multiPaymentTemplateRepository.getById(TEMPLATE_ID))
                .willReturn(TEMPLATE.copy(userId = UUID.randomUUID()))
        }

        val service = MultiPaymentTemplateServiceImpl(
            multiPaymentTemplateRepository = multiPaymentTemplateRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.updateMultiPaymentTemplate(TEMPLATE_ID, UPDATE_REQUEST, USER_IDENTIFIER)
            }

            verifyMock(multiPaymentTemplateRepository)
                .getById(TEMPLATE_ID)
            verifyNoMoreInteractions(multiPaymentTemplateRepository)
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenUpdatingNonExistentMultiPaymentTemplate() {
        val multiPaymentTemplateRepository = mock<MultiPaymentTemplateRepository>()

        suppose("multi-payment template is fetched by id") {
            given(multiPaymentTemplateRepository.getById(TEMPLATE_ID))
                .willReturn(TEMPLATE)
        }

        val updatedTemplate = TEMPLATE.copy(
            templateName = UPDATE_REQUEST.templateName,
            tokenAddress = UPDATE_REQUEST.tokenAddress?.let { ContractAddress(it) },
            chainId = ChainId(UPDATE_REQUEST.chainId)
        )

        suppose("null will be returned when updating multi-payment template") {
            given(multiPaymentTemplateRepository.update(updatedTemplate))
                .willReturn(null)
        }

        val service = MultiPaymentTemplateServiceImpl(
            multiPaymentTemplateRepository = multiPaymentTemplateRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.updateMultiPaymentTemplate(TEMPLATE_ID, UPDATE_REQUEST, USER_IDENTIFIER)
            }

            verifyMock(multiPaymentTemplateRepository)
                .getById(TEMPLATE_ID)
            verifyMock(multiPaymentTemplateRepository)
                .update(updatedTemplate)
            verifyNoMoreInteractions(multiPaymentTemplateRepository)
        }
    }

    @Test
    fun mustSuccessfullyDeleteMultiPaymentTemplate() {
        val multiPaymentTemplateRepository = mock<MultiPaymentTemplateRepository>()

        suppose("multi-payment template is fetched by id") {
            given(multiPaymentTemplateRepository.getById(TEMPLATE_ID))
                .willReturn(TEMPLATE)
        }

        val service = MultiPaymentTemplateServiceImpl(
            multiPaymentTemplateRepository = multiPaymentTemplateRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = mock()
        )

        suppose("multi-payment template is deleted from the database") {
            service.deleteMultiPaymentTemplateById(TEMPLATE_ID, USER_IDENTIFIER)
        }

        verify("multi-payment template was deleted from the database") {
            verifyMock(multiPaymentTemplateRepository)
                .getById(TEMPLATE_ID)
            verifyMock(multiPaymentTemplateRepository)
                .delete(TEMPLATE_ID)
            verifyNoMoreInteractions(multiPaymentTemplateRepository)
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenDeletingNonOwnedMultiPaymentTemplate() {
        val multiPaymentTemplateRepository = mock<MultiPaymentTemplateRepository>()

        suppose("non-owned multi-payment template is fetched by id") {
            given(multiPaymentTemplateRepository.getById(TEMPLATE_ID))
                .willReturn(TEMPLATE.copy(userId = UUID.randomUUID()))
        }

        val service = MultiPaymentTemplateServiceImpl(
            multiPaymentTemplateRepository = multiPaymentTemplateRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.deleteMultiPaymentTemplateById(TEMPLATE_ID, USER_IDENTIFIER)
            }

            verifyMock(multiPaymentTemplateRepository)
                .getById(TEMPLATE_ID)
            verifyNoMoreInteractions(multiPaymentTemplateRepository)
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenDeletingNonExistentMultiPaymentTemplate() {
        val multiPaymentTemplateRepository = mock<MultiPaymentTemplateRepository>()

        suppose("null is returned when fetching multi-payment template by id") {
            given(multiPaymentTemplateRepository.getById(TEMPLATE_ID))
                .willReturn(null)
        }

        val service = MultiPaymentTemplateServiceImpl(
            multiPaymentTemplateRepository = multiPaymentTemplateRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.deleteMultiPaymentTemplateById(TEMPLATE_ID, USER_IDENTIFIER)
            }

            verifyMock(multiPaymentTemplateRepository)
                .getById(TEMPLATE_ID)
            verifyNoMoreInteractions(multiPaymentTemplateRepository)
        }
    }

    @Test
    fun mustSuccessfullyGetMultiPaymentTemplateById() {
        val multiPaymentTemplateRepository = mock<MultiPaymentTemplateRepository>()

        suppose("multi-payment template is fetched by id") {
            given(multiPaymentTemplateRepository.getById(TEMPLATE_ID))
                .willReturn(TEMPLATE)
        }

        val service = MultiPaymentTemplateServiceImpl(
            multiPaymentTemplateRepository = multiPaymentTemplateRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = mock()
        )

        verify("multi-payment template is updated in the database") {
            assertThat(service.getMultiPaymentTemplateById(TEMPLATE_ID)).withMessage()
                .isEqualTo(TEMPLATE)

            verifyMock(multiPaymentTemplateRepository)
                .getById(TEMPLATE_ID)
            verifyNoMoreInteractions(multiPaymentTemplateRepository)
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenFetchingNonExistentMultiPaymentTemplate() {
        val multiPaymentTemplateRepository = mock<MultiPaymentTemplateRepository>()

        suppose("null is returned when fetching multi-payment template by id") {
            given(multiPaymentTemplateRepository.getById(TEMPLATE_ID))
                .willReturn(null)
        }

        val service = MultiPaymentTemplateServiceImpl(
            multiPaymentTemplateRepository = multiPaymentTemplateRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.getMultiPaymentTemplateById(TEMPLATE_ID)
            }

            verifyMock(multiPaymentTemplateRepository)
                .getById(TEMPLATE_ID)
            verifyNoMoreInteractions(multiPaymentTemplateRepository)
        }
    }

    @Test
    fun mustSuccessfullyGetMultiPaymentTemplatesByWalletAddress() {
        val multiPaymentTemplateRepository = mock<MultiPaymentTemplateRepository>()

        suppose("multi-payment templates are fetched by wallet address") {
            given(multiPaymentTemplateRepository.getAllByWalletAddress(USER_IDENTIFIER.walletAddress))
                .willReturn(listOf(TEMPLATE.withoutItems()))
        }

        val service = MultiPaymentTemplateServiceImpl(
            multiPaymentTemplateRepository = multiPaymentTemplateRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = mock()
        )

        verify("multi-payment template is updated in the database") {
            assertThat(service.getAllMultiPaymentTemplatesByWalletAddress(USER_IDENTIFIER.walletAddress)).withMessage()
                .isEqualTo(listOf(TEMPLATE.withoutItems()))

            verifyMock(multiPaymentTemplateRepository)
                .getAllByWalletAddress(USER_IDENTIFIER.walletAddress)
            verifyNoMoreInteractions(multiPaymentTemplateRepository)
        }
    }

    @Test
    fun mustSuccessfullyAddItemToMultiPaymentTemplate() {
        val uuidProvider = mock<UuidProvider>()
        val newItemId = UUID.randomUUID()

        suppose("some UUID will be returned") {
            given(uuidProvider.getUuid())
                .willReturn(newItemId)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()
        val newItemTimestamp = TestData.TIMESTAMP + Duration.ofSeconds(1L)

        suppose("some timestamp will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(newItemTimestamp)
        }

        val multiPaymentTemplateRepository = mock<MultiPaymentTemplateRepository>()

        suppose("multi-payment template is fetched by id") {
            given(multiPaymentTemplateRepository.getById(TEMPLATE_ID))
                .willReturn(TEMPLATE)
        }

        val newItem = MultiPaymentTemplateItem(
            id = newItemId,
            templateId = TEMPLATE_ID,
            walletAddress = WalletAddress(ITEM_REQUEST.walletAddress),
            itemName = ITEM_REQUEST.itemName,
            assetAmount = Balance(ITEM_REQUEST.amount),
            createdAt = newItemTimestamp
        )
        val updatedTemplate = TEMPLATE.copy(
            items = WithItems(TEMPLATE.items.value + newItem),
            updatedAt = newItemTimestamp
        )

        suppose("item will be added to multi-payment template in the database") {
            given(multiPaymentTemplateRepository.addItem(newItem, newItemTimestamp))
                .willReturn(updatedTemplate)
        }

        val service = MultiPaymentTemplateServiceImpl(
            multiPaymentTemplateRepository = multiPaymentTemplateRepository,
            uuidProvider = uuidProvider,
            utcDateTimeProvider = utcDateTimeProvider
        )

        verify("item is added to multi-payment template the database") {
            assertThat(service.addItemToMultiPaymentTemplate(TEMPLATE_ID, ITEM_REQUEST, USER_IDENTIFIER)).withMessage()
                .isEqualTo(updatedTemplate)

            verifyMock(multiPaymentTemplateRepository)
                .getById(TEMPLATE_ID)
            verifyMock(multiPaymentTemplateRepository)
                .addItem(newItem, newItemTimestamp)
            verifyNoMoreInteractions(multiPaymentTemplateRepository)
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenAddingMultiTemplateItemToNonOwnedTemplate() {
        val multiPaymentTemplateRepository = mock<MultiPaymentTemplateRepository>()

        suppose("non-owned multi-payment template is fetched by id") {
            given(multiPaymentTemplateRepository.getById(TEMPLATE_ID))
                .willReturn(TEMPLATE.copy(userId = UUID.randomUUID()))
        }

        val service = MultiPaymentTemplateServiceImpl(
            multiPaymentTemplateRepository = multiPaymentTemplateRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.addItemToMultiPaymentTemplate(TEMPLATE_ID, ITEM_REQUEST, USER_IDENTIFIER)
            }

            verifyMock(multiPaymentTemplateRepository)
                .getById(TEMPLATE_ID)
            verifyNoMoreInteractions(multiPaymentTemplateRepository)
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenAddingMultiTemplateItemToNonExistentTemplate() {
        val uuidProvider = mock<UuidProvider>()
        val newItemId = UUID.randomUUID()

        suppose("some UUID will be returned") {
            given(uuidProvider.getUuid())
                .willReturn(newItemId)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()
        val newItemTimestamp = TestData.TIMESTAMP + Duration.ofSeconds(1L)

        suppose("some timestamp will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(newItemTimestamp)
        }

        val multiPaymentTemplateRepository = mock<MultiPaymentTemplateRepository>()

        suppose("multi-payment template is fetched by id") {
            given(multiPaymentTemplateRepository.getById(TEMPLATE_ID))
                .willReturn(TEMPLATE)
        }

        val newItem = MultiPaymentTemplateItem(
            id = newItemId,
            templateId = TEMPLATE_ID,
            walletAddress = WalletAddress(ITEM_REQUEST.walletAddress),
            itemName = ITEM_REQUEST.itemName,
            assetAmount = Balance(ITEM_REQUEST.amount),
            createdAt = newItemTimestamp
        )

        suppose("null will be returned when adding item to multi-payment template in the database") {
            given(multiPaymentTemplateRepository.addItem(newItem, newItemTimestamp))
                .willReturn(null)
        }

        val service = MultiPaymentTemplateServiceImpl(
            multiPaymentTemplateRepository = multiPaymentTemplateRepository,
            uuidProvider = uuidProvider,
            utcDateTimeProvider = utcDateTimeProvider
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.addItemToMultiPaymentTemplate(TEMPLATE_ID, ITEM_REQUEST, USER_IDENTIFIER)
            }

            verifyMock(multiPaymentTemplateRepository)
                .getById(TEMPLATE_ID)
            verifyMock(multiPaymentTemplateRepository)
                .addItem(newItem, newItemTimestamp)
            verifyNoMoreInteractions(multiPaymentTemplateRepository)
        }
    }

    @Test
    fun mustSuccessfullyUpdateMultiPaymentTemplateItem() {
        val utcDateTimeProvider = mock<UtcDateTimeProvider>()
        val updateTimestamp = TestData.TIMESTAMP + Duration.ofSeconds(1L)

        suppose("some timestamp will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(updateTimestamp)
        }

        val multiPaymentTemplateRepository = mock<MultiPaymentTemplateRepository>()

        suppose("multi-payment template is fetched by id") {
            given(multiPaymentTemplateRepository.getById(TEMPLATE_ID))
                .willReturn(TEMPLATE)
        }

        val updatedItem = MultiPaymentTemplateItem(
            id = ITEM.id,
            templateId = TEMPLATE_ID,
            walletAddress = WalletAddress("abc123"),
            itemName = "updatedItemName",
            assetAmount = Balance(BigInteger.valueOf(123L)),
            createdAt = updateTimestamp
        )
        val updatedTemplate = TEMPLATE.copy(
            items = WithItems(listOf(updatedItem)),
            updatedAt = updateTimestamp
        )

        suppose("item will be updated for multi-payment template in the database") {
            given(multiPaymentTemplateRepository.updateItem(updatedItem, updateTimestamp))
                .willReturn(updatedTemplate)
        }

        val service = MultiPaymentTemplateServiceImpl(
            multiPaymentTemplateRepository = multiPaymentTemplateRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = utcDateTimeProvider
        )

        val updateItemRequest = MultiPaymentTemplateItemRequest(
            walletAddress = updatedItem.walletAddress.rawValue,
            itemName = updatedItem.itemName,
            amount = updatedItem.assetAmount.rawValue
        )

        verify("item is updated for multi-payment template the database") {
            assertThat(service.updateMultiPaymentTemplateItem(TEMPLATE_ID, ITEM.id, updateItemRequest, USER_IDENTIFIER))
                .withMessage()
                .isEqualTo(updatedTemplate)

            verifyMock(multiPaymentTemplateRepository)
                .getById(TEMPLATE_ID)
            verifyMock(multiPaymentTemplateRepository)
                .updateItem(updatedItem, updateTimestamp)
            verifyNoMoreInteractions(multiPaymentTemplateRepository)
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenUpdatingMultiTemplateItemForNonOwnedTemplate() {
        val multiPaymentTemplateRepository = mock<MultiPaymentTemplateRepository>()

        suppose("non-owned multi-payment template is fetched by id") {
            given(multiPaymentTemplateRepository.getById(TEMPLATE_ID))
                .willReturn(TEMPLATE.copy(userId = UUID.randomUUID()))
        }

        val service = MultiPaymentTemplateServiceImpl(
            multiPaymentTemplateRepository = multiPaymentTemplateRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.updateMultiPaymentTemplateItem(TEMPLATE_ID, ITEM.id, ITEM_REQUEST, USER_IDENTIFIER)
            }

            verifyMock(multiPaymentTemplateRepository)
                .getById(TEMPLATE_ID)
            verifyNoMoreInteractions(multiPaymentTemplateRepository)
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenUpdatingMultiTemplateItemForNonExistentTemplate() {
        val utcDateTimeProvider = mock<UtcDateTimeProvider>()
        val updateTimestamp = TestData.TIMESTAMP + Duration.ofSeconds(1L)

        suppose("some timestamp will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(updateTimestamp)
        }

        val multiPaymentTemplateRepository = mock<MultiPaymentTemplateRepository>()

        suppose("multi-payment template is fetched by id") {
            given(multiPaymentTemplateRepository.getById(TEMPLATE_ID))
                .willReturn(TEMPLATE)
        }

        val updatedItem = MultiPaymentTemplateItem(
            id = ITEM.id,
            templateId = TEMPLATE_ID,
            walletAddress = WalletAddress("abc123"),
            itemName = "updatedItemName",
            assetAmount = Balance(BigInteger.valueOf(123L)),
            createdAt = updateTimestamp
        )

        suppose("null will be returned when updating multi-payment template item in the database") {
            given(multiPaymentTemplateRepository.updateItem(updatedItem, updateTimestamp))
                .willReturn(null)
        }

        val service = MultiPaymentTemplateServiceImpl(
            multiPaymentTemplateRepository = multiPaymentTemplateRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = utcDateTimeProvider
        )

        val updateItemRequest = MultiPaymentTemplateItemRequest(
            walletAddress = updatedItem.walletAddress.rawValue,
            itemName = updatedItem.itemName,
            amount = updatedItem.assetAmount.rawValue
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.updateMultiPaymentTemplateItem(TEMPLATE_ID, ITEM.id, updateItemRequest, USER_IDENTIFIER)
            }

            verifyMock(multiPaymentTemplateRepository)
                .getById(TEMPLATE_ID)
            verifyMock(multiPaymentTemplateRepository)
                .updateItem(updatedItem, updateTimestamp)
            verifyNoMoreInteractions(multiPaymentTemplateRepository)
        }
    }

    @Test
    fun mustSuccessfullyDeleteMultiPaymentTemplateItem() {
        val utcDateTimeProvider = mock<UtcDateTimeProvider>()
        val updateTimestamp = TestData.TIMESTAMP + Duration.ofSeconds(1L)

        suppose("some timestamp will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(updateTimestamp)
        }

        val multiPaymentTemplateRepository = mock<MultiPaymentTemplateRepository>()

        suppose("multi-payment template is fetched by id") {
            given(multiPaymentTemplateRepository.getById(TEMPLATE_ID))
                .willReturn(TEMPLATE)
        }

        suppose("multi-payment template item will be deleted from the database") {
            given(multiPaymentTemplateRepository.deleteItem(TEMPLATE_ID, ITEM.id, updateTimestamp))
                .willReturn(TEMPLATE.copy(items = WithItems(emptyList())))
        }

        val service = MultiPaymentTemplateServiceImpl(
            multiPaymentTemplateRepository = multiPaymentTemplateRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = utcDateTimeProvider
        )

        verify("multi-payment template item was deleted from the database") {
            assertThat(service.deleteMultiPaymentTemplateItem(TEMPLATE_ID, ITEM.id, USER_IDENTIFIER)).withMessage()
                .isEqualTo(TEMPLATE.copy(items = WithItems(emptyList())))

            verifyMock(multiPaymentTemplateRepository)
                .getById(TEMPLATE_ID)
            verifyMock(multiPaymentTemplateRepository)
                .deleteItem(TEMPLATE_ID, ITEM.id, updateTimestamp)
            verifyNoMoreInteractions(multiPaymentTemplateRepository)
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenDeletingMultiTemplateItemForNonOwnedTemplate() {
        val multiPaymentTemplateRepository = mock<MultiPaymentTemplateRepository>()

        suppose("non-owned multi-payment template is fetched by id") {
            given(multiPaymentTemplateRepository.getById(TEMPLATE_ID))
                .willReturn(TEMPLATE.copy(userId = UUID.randomUUID()))
        }

        val service = MultiPaymentTemplateServiceImpl(
            multiPaymentTemplateRepository = multiPaymentTemplateRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.deleteMultiPaymentTemplateItem(TEMPLATE_ID, ITEM.id, USER_IDENTIFIER)
            }

            verifyMock(multiPaymentTemplateRepository)
                .getById(TEMPLATE_ID)
            verifyNoMoreInteractions(multiPaymentTemplateRepository)
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenDeletingMultiTemplateItemForNonExistentTemplate() {
        val utcDateTimeProvider = mock<UtcDateTimeProvider>()
        val updateTimestamp = TestData.TIMESTAMP + Duration.ofSeconds(1L)

        suppose("some timestamp will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(updateTimestamp)
        }

        val multiPaymentTemplateRepository = mock<MultiPaymentTemplateRepository>()

        suppose("multi-payment template is fetched by id") {
            given(multiPaymentTemplateRepository.getById(TEMPLATE_ID))
                .willReturn(TEMPLATE)
        }

        suppose("null will be returned when deleting multi-payment template item from the database") {
            given(multiPaymentTemplateRepository.deleteItem(TEMPLATE_ID, ITEM.id, updateTimestamp))
                .willReturn(null)
        }

        val service = MultiPaymentTemplateServiceImpl(
            multiPaymentTemplateRepository = multiPaymentTemplateRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = utcDateTimeProvider
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.deleteMultiPaymentTemplateItem(TEMPLATE_ID, ITEM.id, USER_IDENTIFIER)
            }

            verifyMock(multiPaymentTemplateRepository)
                .getById(TEMPLATE_ID)
            verifyMock(multiPaymentTemplateRepository)
                .deleteItem(TEMPLATE_ID, ITEM.id, updateTimestamp)
            verifyNoMoreInteractions(multiPaymentTemplateRepository)
        }
    }
}
