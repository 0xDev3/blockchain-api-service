package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.generated.jooq.enums.UserIdentifierType
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.UserIdentifierRecord
import com.ampnet.blockchainapiservice.model.result.MultiPaymentTemplate
import com.ampnet.blockchainapiservice.model.result.MultiPaymentTemplateItem
import com.ampnet.blockchainapiservice.model.result.WithItems
import com.ampnet.blockchainapiservice.testcontainers.SharedTestContainers
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.Import
import java.math.BigInteger
import java.time.Duration
import java.util.UUID

@JooqTest
@Import(JooqMultiPaymentTemplateRepository::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JooqMultiPaymentTemplateRepositoryIntegTest : TestBase() {

    companion object {
        private val TEMPLATE_ID = UUID.randomUUID()
        private const val TEMPLATE_NAME = "templateName"
        private val CHAIN_ID = ChainId(1337L)
        private val WALLET_ADDRESS = WalletAddress("a")
        private const val ITEM_NAME = "itemName"
        private val TOKEN_ADDRESS = ContractAddress("b")
        private val ASSET_AMOUNT = Balance(BigInteger.TEN)
        private val OWNER_ID = UUID.randomUUID()
        private val OWNER_ADDRESS = WalletAddress("cafebabe")
        private val TEMPLATE = MultiPaymentTemplate(
            id = TEMPLATE_ID,
            items = WithItems(
                listOf(
                    MultiPaymentTemplateItem(
                        id = UUID.randomUUID(),
                        templateId = TEMPLATE_ID,
                        walletAddress = WALLET_ADDRESS,
                        itemName = ITEM_NAME,
                        tokenAddress = TOKEN_ADDRESS,
                        assetAmount = ASSET_AMOUNT,
                        createdAt = TestData.TIMESTAMP
                    )
                )
            ),
            templateName = TEMPLATE_NAME,
            chainId = CHAIN_ID,
            userId = OWNER_ID,
            createdAt = TestData.TIMESTAMP,
            updatedAt = null
        )
    }

    @Suppress("unused")
    private val postgresContainer = SharedTestContainers.postgresContainer

    @Autowired
    private lateinit var repository: JooqMultiPaymentTemplateRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        postgresContainer.cleanAllDatabaseTables(dslContext)

        dslContext.executeInsert(
            UserIdentifierRecord(
                id = OWNER_ID,
                userIdentifier = OWNER_ADDRESS.rawValue,
                identifierType = UserIdentifierType.ETH_WALLET_ADDRESS
            )
        )
    }

    @Test
    fun mustCorrectlyStoreMultiPaymentTemplate() {
        val storedTemplate = suppose("multi-payment template is stored in database") {
            repository.store(TEMPLATE)
        }

        verify("storing multi-payment template returns correct result") {
            assertThat(storedTemplate).withMessage()
                .isEqualTo(TEMPLATE)
        }

        verify("multi-payment template is stored into the database") {
            assertThat(repository.getById(TEMPLATE_ID)).withMessage()
                .isEqualTo(TEMPLATE)
        }
    }

    @Test
    fun mustCorrectlyUpdateMultiPaymentTemplate() {
        suppose("multi-payment template is stored in database") {
            repository.store(TEMPLATE)
        }

        val templateUpdate = TEMPLATE.copy(
            templateName = "newName",
            chainId = ChainId(123L),
            updatedAt = TestData.TIMESTAMP
        )

        val updatedTemplate = suppose("multi-payment template is updated in database") {
            repository.update(templateUpdate)
        }

        verify("updating template returns correct result") {
            assertThat(updatedTemplate).withMessage()
                .isEqualTo(templateUpdate)
        }

        verify("template is updated in the database") {
            assertThat(repository.getById(TEMPLATE_ID)).withMessage()
                .isEqualTo(templateUpdate)
        }
    }

    @Test
    fun mustReturnNullWhenUpdatingNonExistentMultiPaymentTemplate() {
        verify("null is returned when updating non-existent multi-payment template") {
            assertThat(repository.update(TEMPLATE)).withMessage()
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyDeleteMultiPaymentTemplate() {
        suppose("multi-payment template is stored in database") {
            repository.store(TEMPLATE)
        }

        verify("multi-payment template is stored into the database") {
            assertThat(repository.getById(TEMPLATE_ID)).withMessage()
                .isEqualTo(TEMPLATE)
        }

        suppose("multi-payment template is deleted from the database") {
            repository.delete(TEMPLATE_ID)
        }

        verify("multi-payment template was deleted from the database") {
            assertThat(repository.getById(TEMPLATE_ID)).withMessage()
                .isNull()
        }
    }

    @Test
    fun mustReturnNullWhenFetchingNonExistentMultiPaymentTemplateById() {
        verify("null is returned for non-existent multi-payment template") {
            assertThat(repository.getById(TEMPLATE_ID)).withMessage()
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyFetchMultiPaymentTemplateItemsById() {
        suppose("multi-payment template is stored in database") {
            repository.store(TEMPLATE)
        }

        verify("correct multi-payment template items are returned") {
            assertThat(repository.getItemsById(TEMPLATE_ID)).withMessage()
                .isEqualTo(TEMPLATE.items.value)
        }
    }

    @Test
    fun mustCorrectlyFetchAllMultiPaymentTemplatesByWalletAddress() {
        suppose("multi-payment template is stored in database") {
            repository.store(TEMPLATE)
        }

        verify("multi-payment templates are correctly fetched by wallet address") {
            assertThat(repository.getAllByWalletAddress(OWNER_ADDRESS)).withMessage()
                .isEqualTo(listOf(TEMPLATE.withoutItems()))
        }
    }

    @Test
    fun mustCorrectlyAddItemToMultiPaymentTemplate() {
        suppose("multi-payment template is stored in database") {
            repository.store(TEMPLATE)
        }

        val newItem = MultiPaymentTemplateItem(
            id = UUID.randomUUID(),
            templateId = TEMPLATE_ID,
            walletAddress = WalletAddress("abc"),
            itemName = "newItemName",
            tokenAddress = ContractAddress("def"),
            assetAmount = Balance(BigInteger.TWO),
            createdAt = TestData.TIMESTAMP + Duration.ofSeconds(1L)
        )

        val templateWithAddedItem = suppose("item is added to multi-payment template") {
            repository.addItem(newItem, TestData.TIMESTAMP)
        }

        verify("item was added to multi-payment template") {
            assertThat(templateWithAddedItem).withMessage()
                .isEqualTo(
                    TEMPLATE.copy(
                        items = WithItems(TEMPLATE.items.value + newItem),
                        updatedAt = TestData.TIMESTAMP
                    )
                )
        }

        verify("item was added to the database") {
            assertThat(repository.getById(TEMPLATE_ID)).withMessage()
                .isEqualTo(templateWithAddedItem)
        }
    }

    @Test
    fun mustReturnNullWhenAddingItemToNonExistentMultiPaymentTemplate() {
        val item = MultiPaymentTemplateItem(
            id = UUID.randomUUID(),
            templateId = TEMPLATE_ID,
            walletAddress = WalletAddress("abc"),
            itemName = "newItemName",
            tokenAddress = ContractAddress("def"),
            assetAmount = Balance(BigInteger.TWO),
            createdAt = TestData.TIMESTAMP + Duration.ofSeconds(1L)
        )

        verify("null is returned when adding item to non-existent multi-payment template") {
            assertThat(repository.addItem(item, TestData.TIMESTAMP)).withMessage()
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyUpdateMultiPaymentTemplateItem() {
        suppose("multi-payment template is stored in database") {
            repository.store(TEMPLATE)
        }

        val updatedItem = TEMPLATE.items.value[0].copy(
            walletAddress = WalletAddress("fff"),
            itemName = "updatedItem",
            tokenAddress = ContractAddress("111"),
            assetAmount = Balance(BigInteger.ONE)
        )

        val templateWithUpdatedItem = suppose("multi-payment template item is updated in database") {
            repository.updateItem(updatedItem, TestData.TIMESTAMP)
        }

        verify("item was updated in multi-payment tempalte") {
            assertThat(templateWithUpdatedItem).withMessage()
                .isEqualTo(
                    TEMPLATE.copy(
                        items = WithItems(listOf(updatedItem)),
                        updatedAt = TestData.TIMESTAMP
                    )
                )
        }

        verify("item was updated in the database") {
            assertThat(repository.getById(TEMPLATE_ID)).withMessage()
                .isEqualTo(templateWithUpdatedItem)
        }
    }

    @Test
    fun mustReturnNullWhenUpdatingItemForNonExistentMultiPaymentTemplate() {
        val updatedItem = TEMPLATE.items.value[0].copy(
            walletAddress = WalletAddress("fff"),
            itemName = "updatedItem",
            tokenAddress = ContractAddress("111"),
            assetAmount = Balance(BigInteger.ONE)
        )

        verify("null is returned when updating item for non-existent multi-payment template") {
            assertThat(repository.updateItem(updatedItem, TestData.TIMESTAMP)).withMessage()
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyDeleteMultiPaymentTemplateItem() {
        suppose("multi-payment template is stored in database") {
            repository.store(TEMPLATE)
        }

        val templateWithDeletedItem = suppose("item is deleted from the database") {
            repository.deleteItem(TEMPLATE_ID, TEMPLATE.items.value[0].id, TestData.TIMESTAMP)
        }

        verify("item was deleted from multi-payment template") {
            assertThat(templateWithDeletedItem).withMessage()
                .isEqualTo(
                    TEMPLATE.copy(
                        items = WithItems(emptyList()),
                        updatedAt = TestData.TIMESTAMP
                    )
                )
        }

        verify("item was deleted from the database") {
            assertThat(repository.getById(TEMPLATE_ID)).withMessage()
                .isEqualTo(templateWithDeletedItem)
        }
    }

    @Test
    fun mustReturnNullWhenDeletingItemFromNonExistentMultiPaymentTemplate() {
        verify("null is returned when deleting item from non-existent multi-payment template") {
            assertThat(repository.deleteItem(UUID.randomUUID(), UUID.randomUUID(), TestData.TIMESTAMP)).withMessage()
                .isNull()
        }
    }
}
