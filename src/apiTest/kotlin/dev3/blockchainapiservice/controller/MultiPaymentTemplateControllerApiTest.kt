package dev3.blockchainapiservice.controller

import dev3.blockchainapiservice.ControllerTestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.exception.ErrorCode
import dev3.blockchainapiservice.generated.jooq.enums.UserIdentifierType
import dev3.blockchainapiservice.generated.jooq.tables.records.UserIdentifierRecord
import dev3.blockchainapiservice.model.response.MultiPaymentTemplateItemResponse
import dev3.blockchainapiservice.model.response.MultiPaymentTemplateWithItemsResponse
import dev3.blockchainapiservice.model.response.MultiPaymentTemplateWithoutItemsResponse
import dev3.blockchainapiservice.model.response.MultiPaymentTemplatesResponse
import dev3.blockchainapiservice.model.result.MultiPaymentTemplate
import dev3.blockchainapiservice.model.result.MultiPaymentTemplateItem
import dev3.blockchainapiservice.model.result.WithItems
import dev3.blockchainapiservice.repository.MultiPaymentTemplateRepository
import dev3.blockchainapiservice.security.WithMockUser
import dev3.blockchainapiservice.util.AssetType
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.math.BigInteger
import java.util.UUID

class MultiPaymentTemplateControllerApiTest : ControllerTestBase() {

    companion object {
        private val OWNER_ID = UUID.randomUUID()
        private const val OWNER_ADDRESS = "abc123"
        private val OTHER_OWNER_ID = UUID.randomUUID()
        private const val OTHER_OWNER_ADDRESS = "def456"
        private const val TEMPLATE_NAME = "templateName"
        private val CHAIN_ID = TestData.CHAIN_ID
        private val WALLET_ADDRESS = WalletAddress("a")
        private const val ITEM_NAME = "itemName"
        private val TOKEN_ADDRESS = ContractAddress("b")
        private val ASSET_AMOUNT = Balance(BigInteger.TEN)
    }

    @Autowired
    private lateinit var multiPaymentTemplateRepository: MultiPaymentTemplateRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        postgresContainer.cleanAllDatabaseTables(dslContext)

        dslContext.executeInsert(
            UserIdentifierRecord(
                id = OWNER_ID,
                userIdentifier = WalletAddress(OWNER_ADDRESS).rawValue,
                identifierType = UserIdentifierType.ETH_WALLET_ADDRESS,
                stripeClientId = null
            )
        )

        dslContext.executeInsert(
            UserIdentifierRecord(
                id = OTHER_OWNER_ID,
                userIdentifier = WalletAddress(OTHER_OWNER_ADDRESS).rawValue,
                identifierType = UserIdentifierType.ETH_WALLET_ADDRESS,
                stripeClientId = null
            )
        )
    }

    @Test
    @WithMockUser(OWNER_ADDRESS)
    fun mustCorrectlyCreateMultiPaymentTemplate() {
        val response = suppose("request to create multi-payment template is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/multi-payment-template")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "template_name": "$TEMPLATE_NAME",
                                "asset_type": "${AssetType.TOKEN.name}",
                                "token_address": "${TOKEN_ADDRESS.rawValue}",
                                "chain_id": ${CHAIN_ID.value},
                                "items": [
                                    {
                                        "wallet_address": "${WALLET_ADDRESS.rawValue}",
                                        "item_name": "$ITEM_NAME",
                                        "amount": "${ASSET_AMOUNT.rawValue}"
                                    }
                                ]
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, MultiPaymentTemplateWithItemsResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    MultiPaymentTemplateWithItemsResponse(
                        id = response.id,
                        items = listOf(
                            MultiPaymentTemplateItemResponse(
                                id = response.items[0].id,
                                templateId = response.id,
                                walletAddress = WALLET_ADDRESS.rawValue,
                                itemName = ITEM_NAME,
                                amount = ASSET_AMOUNT.rawValue,
                                createdAt = response.items[0].createdAt
                            )
                        ),
                        templateName = TEMPLATE_NAME,
                        assetType = AssetType.TOKEN,
                        tokenAddress = TOKEN_ADDRESS.rawValue,
                        chainId = CHAIN_ID.value,
                        createdAt = response.createdAt,
                        updatedAt = null
                    )
                )

            assertThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            assertThat(response.items[0].createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("multi-payment template is correctly stored into the database") {
            val storedTemplate = multiPaymentTemplateRepository.getById(response.id)!!

            assertThat(storedTemplate).withMessage()
                .isEqualTo(
                    MultiPaymentTemplate(
                        id = response.id,
                        items = WithItems(
                            listOf(
                                MultiPaymentTemplateItem(
                                    id = response.items[0].id,
                                    templateId = response.id,
                                    walletAddress = WALLET_ADDRESS,
                                    itemName = ITEM_NAME,
                                    assetAmount = ASSET_AMOUNT,
                                    createdAt = storedTemplate.items.value[0].createdAt
                                )
                            )
                        ),
                        templateName = TEMPLATE_NAME,
                        tokenAddress = TOKEN_ADDRESS,
                        chainId = CHAIN_ID,
                        userId = OWNER_ID,
                        createdAt = storedTemplate.createdAt,
                        updatedAt = null
                    )
                )

            assertThat(storedTemplate.createdAt.value)
                .isCloseTo(response.createdAt, WITHIN_TIME_TOLERANCE)
            assertThat(storedTemplate.createdAt.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            assertThat(storedTemplate.items.value[0].createdAt.value)
                .isCloseTo(response.items[0].createdAt, WITHIN_TIME_TOLERANCE)
            assertThat(storedTemplate.items.value[0].createdAt.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    @WithMockUser(OWNER_ADDRESS)
    fun mustCorrectlyUpdateMultiPaymentTemplate() {
        val templateId = UUID.randomUUID()
        val template = MultiPaymentTemplate(
            id = templateId,
            items = WithItems(
                listOf(
                    MultiPaymentTemplateItem(
                        id = UUID.randomUUID(),
                        templateId = templateId,
                        walletAddress = WALLET_ADDRESS,
                        itemName = ITEM_NAME,
                        assetAmount = ASSET_AMOUNT,
                        createdAt = TestData.TIMESTAMP
                    )
                )
            ),
            templateName = TEMPLATE_NAME,
            tokenAddress = TOKEN_ADDRESS,
            chainId = CHAIN_ID,
            userId = OWNER_ID,
            createdAt = TestData.TIMESTAMP,
            updatedAt = null
        )

        suppose("multi-payment template exists in the database") {
            multiPaymentTemplateRepository.store(template)
        }

        val newTemplateName = "newTemplateName"
        val newChainId = TestData.CHAIN_ID

        val response = suppose("request to update multi-payment template is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.patch("/v1/multi-payment-template/$templateId")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "template_name": "$newTemplateName",
                                "asset_type": "${AssetType.NATIVE.name}",
                                "token_address": null,
                                "chain_id": ${newChainId.value}
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, MultiPaymentTemplateWithItemsResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    MultiPaymentTemplateWithItemsResponse(template).copy(
                        templateName = newTemplateName,
                        assetType = AssetType.NATIVE,
                        tokenAddress = null,
                        chainId = newChainId.value,
                        updatedAt = response.updatedAt
                    )
                )

            assertThat(response.updatedAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("multi-payment template is correctly updated in the database") {
            val storedTemplate = multiPaymentTemplateRepository.getById(response.id)!!

            assertThat(storedTemplate).withMessage()
                .isEqualTo(
                    template.copy(
                        templateName = newTemplateName,
                        tokenAddress = null,
                        chainId = newChainId,
                        updatedAt = storedTemplate.updatedAt
                    )
                )

            assertThat(storedTemplate.updatedAt?.value)
                .isCloseTo(response.updatedAt, WITHIN_TIME_TOLERANCE)
            assertThat(storedTemplate.updatedAt?.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    @WithMockUser(OTHER_OWNER_ADDRESS)
    fun mustReturn404NotFoundWhenUpdatingNonOwnedMultiPaymentTemplate() {
        val templateId = UUID.randomUUID()
        val template = MultiPaymentTemplate(
            id = templateId,
            items = WithItems(
                listOf(
                    MultiPaymentTemplateItem(
                        id = UUID.randomUUID(),
                        templateId = templateId,
                        walletAddress = WALLET_ADDRESS,
                        itemName = ITEM_NAME,
                        assetAmount = ASSET_AMOUNT,
                        createdAt = TestData.TIMESTAMP
                    )
                )
            ),
            templateName = TEMPLATE_NAME,
            tokenAddress = TOKEN_ADDRESS,
            chainId = CHAIN_ID,
            userId = OWNER_ID,
            createdAt = TestData.TIMESTAMP,
            updatedAt = null
        )

        suppose("multi-payment template exists in the database") {
            multiPaymentTemplateRepository.store(template)
        }

        val newTemplateName = "newTemplateName"
        val newChainId = TestData.CHAIN_ID

        verify("404 is returned for non-owned multi-payment template") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.patch("/v1/multi-payment-template/$templateId")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "template_name": "$newTemplateName",
                                "asset_type": "${AssetType.NATIVE.name}",
                                "token_address": null,
                                "chain_id": ${newChainId.value}
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    @WithMockUser(OWNER_ADDRESS)
    fun mustReturn404NotFoundWhenUpdatingNonExistentMultiPaymentTemplate() {
        val newTemplateName = "newTemplateName"
        val newChainId = TestData.CHAIN_ID

        verify("404 is returned for non-existent multi-payment template") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.patch("/v1/multi-payment-template/${UUID.randomUUID()}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "template_name": "$newTemplateName",
                                "asset_type": "${AssetType.NATIVE.name}",
                                "token_address": null,
                                "chain_id": ${newChainId.value}
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    @WithMockUser(OWNER_ADDRESS)
    fun mustCorrectlyDeleteMultiPaymentTemplate() {
        val templateId = UUID.randomUUID()
        val template = MultiPaymentTemplate(
            id = templateId,
            items = WithItems(
                listOf(
                    MultiPaymentTemplateItem(
                        id = UUID.randomUUID(),
                        templateId = templateId,
                        walletAddress = WALLET_ADDRESS,
                        itemName = ITEM_NAME,
                        assetAmount = ASSET_AMOUNT,
                        createdAt = TestData.TIMESTAMP
                    )
                )
            ),
            templateName = TEMPLATE_NAME,
            tokenAddress = TOKEN_ADDRESS,
            chainId = CHAIN_ID,
            userId = OWNER_ID,
            createdAt = TestData.TIMESTAMP,
            updatedAt = null
        )

        suppose("multi-payment template exists in the database") {
            multiPaymentTemplateRepository.store(template)
        }

        suppose("request to delete multi-payment template is made") {
            mockMvc.perform(
                MockMvcRequestBuilders.delete("/v1/multi-payment-template/$templateId")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
        }

        verify("multi-payment template is deleted from database") {
            assertThat(multiPaymentTemplateRepository.getById(templateId)).withMessage()
                .isNull()
        }
    }

    @Test
    @WithMockUser(OTHER_OWNER_ADDRESS)
    fun mustReturn404NotFoundWhenDeletingNonOwnedMultiPaymentTemplate() {
        val templateId = UUID.randomUUID()
        val template = MultiPaymentTemplate(
            id = templateId,
            items = WithItems(
                listOf(
                    MultiPaymentTemplateItem(
                        id = UUID.randomUUID(),
                        templateId = templateId,
                        walletAddress = WALLET_ADDRESS,
                        itemName = ITEM_NAME,
                        assetAmount = ASSET_AMOUNT,
                        createdAt = TestData.TIMESTAMP
                    )
                )
            ),
            templateName = TEMPLATE_NAME,
            tokenAddress = TOKEN_ADDRESS,
            chainId = CHAIN_ID,
            userId = OWNER_ID,
            createdAt = TestData.TIMESTAMP,
            updatedAt = null
        )

        suppose("multi-payment template exists in the database") {
            multiPaymentTemplateRepository.store(template)
        }

        verify("404 is returned for non-owned multi-payment template") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.delete("/v1/multi-payment-template/$templateId")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    @WithMockUser(OWNER_ADDRESS)
    fun mustReturn404NotFoundWhenDeletingNonExistentMultiPaymentTemplate() {
        verify("404 is returned for non-existent multi-payment template") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.delete("/v1/multi-payment-template/${UUID.randomUUID()}")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    fun mustCorrectlyFetchMultiPaymentTemplateById() {
        val templateId = UUID.randomUUID()
        val template = MultiPaymentTemplate(
            id = templateId,
            items = WithItems(
                listOf(
                    MultiPaymentTemplateItem(
                        id = UUID.randomUUID(),
                        templateId = templateId,
                        walletAddress = WALLET_ADDRESS,
                        itemName = ITEM_NAME,
                        assetAmount = ASSET_AMOUNT,
                        createdAt = TestData.TIMESTAMP
                    )
                )
            ),
            templateName = TEMPLATE_NAME,
            tokenAddress = TOKEN_ADDRESS,
            chainId = CHAIN_ID,
            userId = OWNER_ID,
            createdAt = TestData.TIMESTAMP,
            updatedAt = null
        )

        suppose("multi-payment template exists in the database") {
            multiPaymentTemplateRepository.store(template)
        }

        val response = suppose("request to fetch multi-payment template is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/multi-payment-template/$templateId")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, MultiPaymentTemplateWithItemsResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(MultiPaymentTemplateWithItemsResponse(template))
        }
    }

    @Test
    fun mustReturn404NotFoundWhenNonExistentFetchingMultiPaymentTemplateById() {
        verify("404 is returned for non-existent multi-payment template") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/multi-payment-template/${UUID.randomUUID()}")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    fun mustCorrectlyFetchAllMultiPaymentTemplatesForWalletAddress() {
        val templateId = UUID.randomUUID()
        val template = MultiPaymentTemplate(
            id = templateId,
            items = WithItems(
                listOf(
                    MultiPaymentTemplateItem(
                        id = UUID.randomUUID(),
                        templateId = templateId,
                        walletAddress = WALLET_ADDRESS,
                        itemName = ITEM_NAME,
                        assetAmount = ASSET_AMOUNT,
                        createdAt = TestData.TIMESTAMP
                    )
                )
            ),
            templateName = TEMPLATE_NAME,
            tokenAddress = TOKEN_ADDRESS,
            chainId = CHAIN_ID,
            userId = OWNER_ID,
            createdAt = TestData.TIMESTAMP,
            updatedAt = null
        )

        suppose("multi-payment template exists in the database") {
            multiPaymentTemplateRepository.store(template)
        }

        val response = suppose("request to fetch all multi-payment templates by wallet address is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/multi-payment-template/by-wallet-address/$OWNER_ADDRESS")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, MultiPaymentTemplatesResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    MultiPaymentTemplatesResponse(
                        listOf(
                            MultiPaymentTemplateWithoutItemsResponse(template.withoutItems())
                        )
                    )
                )
        }
    }

    @Test
    @WithMockUser(OWNER_ADDRESS)
    fun mustCorrectlyCreateItemForMultiPaymentTemplate() {
        val templateId = UUID.randomUUID()
        val template = MultiPaymentTemplate(
            id = templateId,
            items = WithItems(
                listOf(
                    MultiPaymentTemplateItem(
                        id = UUID.randomUUID(),
                        templateId = templateId,
                        walletAddress = WALLET_ADDRESS,
                        itemName = ITEM_NAME,
                        assetAmount = ASSET_AMOUNT,
                        createdAt = TestData.TIMESTAMP
                    )
                )
            ),
            templateName = TEMPLATE_NAME,
            tokenAddress = TOKEN_ADDRESS,
            chainId = CHAIN_ID,
            userId = OWNER_ID,
            createdAt = TestData.TIMESTAMP,
            updatedAt = null
        )

        suppose("multi-payment template exists in the database") {
            multiPaymentTemplateRepository.store(template)
        }

        val newWalletAddress = WalletAddress("abc123")
        val newItemName = "newItemName"
        val newAmount = Balance(BigInteger.valueOf(123L))

        val response = suppose("request to create multi-payment template item is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/multi-payment-template/$templateId/items")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "wallet_address": "${newWalletAddress.rawValue}",
                                "item_name": "$newItemName",
                                "amount": "${newAmount.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, MultiPaymentTemplateWithItemsResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    MultiPaymentTemplateWithItemsResponse(template).copy(
                        items = listOf(
                            MultiPaymentTemplateItemResponse(template.items.value[0]),
                            MultiPaymentTemplateItemResponse(
                                id = response.items[1].id,
                                templateId = templateId,
                                walletAddress = newWalletAddress.rawValue,
                                itemName = newItemName,
                                amount = newAmount.rawValue,
                                createdAt = response.items[1].createdAt
                            )
                        ),
                        updatedAt = response.updatedAt
                    )
                )

            assertThat(response.updatedAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            assertThat(response.items[1].createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("multi-payment template item is correctly created in the database") {
            val storedTemplate = multiPaymentTemplateRepository.getById(response.id)!!

            assertThat(storedTemplate).withMessage()
                .isEqualTo(
                    template.copy(
                        items = WithItems(
                            listOf(
                                template.items.value[0],
                                MultiPaymentTemplateItem(
                                    id = response.items[1].id,
                                    templateId = templateId,
                                    walletAddress = newWalletAddress,
                                    itemName = newItemName,
                                    assetAmount = newAmount,
                                    createdAt = storedTemplate.items.value[1].createdAt
                                )
                            )
                        ),
                        updatedAt = storedTemplate.updatedAt
                    )
                )

            assertThat(storedTemplate.updatedAt?.value)
                .isCloseTo(response.updatedAt, WITHIN_TIME_TOLERANCE)
            assertThat(storedTemplate.updatedAt?.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            assertThat(storedTemplate.items.value[1].createdAt.value)
                .isCloseTo(response.items[1].createdAt, WITHIN_TIME_TOLERANCE)
            assertThat(storedTemplate.items.value[1].createdAt.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    @WithMockUser(OTHER_OWNER_ADDRESS)
    fun mustReturn404NotFoundWhenCreatingItemForNonOwnedMultiPaymentTemplate() {
        val templateId = UUID.randomUUID()
        val template = MultiPaymentTemplate(
            id = templateId,
            items = WithItems(
                listOf(
                    MultiPaymentTemplateItem(
                        id = UUID.randomUUID(),
                        templateId = templateId,
                        walletAddress = WALLET_ADDRESS,
                        itemName = ITEM_NAME,
                        assetAmount = ASSET_AMOUNT,
                        createdAt = TestData.TIMESTAMP
                    )
                )
            ),
            templateName = TEMPLATE_NAME,
            tokenAddress = TOKEN_ADDRESS,
            chainId = CHAIN_ID,
            userId = OWNER_ID,
            createdAt = TestData.TIMESTAMP,
            updatedAt = null
        )

        suppose("multi-payment template exists in the database") {
            multiPaymentTemplateRepository.store(template)
        }

        val newWalletAddress = WalletAddress("abc123")
        val newItemName = "newItemName"
        val newAmount = Balance(BigInteger.valueOf(123L))

        verify("404 is returned for non-owned multi-payment template") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/multi-payment-template/$templateId/items")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "wallet_address": "${newWalletAddress.rawValue}",
                                "item_name": "$newItemName",
                                "amount": "${newAmount.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    @WithMockUser(OWNER_ADDRESS)
    fun mustReturn404NotFoundWhenCreatingItemForNonExistentMultiPaymentTemplate() {
        val newWalletAddress = WalletAddress("abc123")
        val newItemName = "newItemName"
        val newAmount = Balance(BigInteger.valueOf(123L))

        verify("404 is returned for non-existent multi-payment template") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/multi-payment-template/${UUID.randomUUID()}/items")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "wallet_address": "${newWalletAddress.rawValue}",
                                "item_name": "$newItemName",
                                "amount": "${newAmount.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    @WithMockUser(OWNER_ADDRESS)
    fun mustCorrectlyUpdateItemForMultiPaymentTemplate() {
        val templateId = UUID.randomUUID()
        val template = MultiPaymentTemplate(
            id = templateId,
            items = WithItems(
                listOf(
                    MultiPaymentTemplateItem(
                        id = UUID.randomUUID(),
                        templateId = templateId,
                        walletAddress = WALLET_ADDRESS,
                        itemName = ITEM_NAME,
                        assetAmount = ASSET_AMOUNT,
                        createdAt = TestData.TIMESTAMP
                    )
                )
            ),
            templateName = TEMPLATE_NAME,
            tokenAddress = TOKEN_ADDRESS,
            chainId = CHAIN_ID,
            userId = OWNER_ID,
            createdAt = TestData.TIMESTAMP,
            updatedAt = null
        )

        suppose("multi-payment template exists in the database") {
            multiPaymentTemplateRepository.store(template)
        }

        val newWalletAddress = WalletAddress("abc123")
        val newItemName = "newItemName"
        val newAmount = Balance(BigInteger.valueOf(123L))

        val response = suppose("request to update multi-payment template item is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.patch(
                    "/v1/multi-payment-template/$templateId/items/${template.items.value[0].id}"
                )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "wallet_address": "${newWalletAddress.rawValue}",
                                "item_name": "$newItemName",
                                "amount": "${newAmount.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, MultiPaymentTemplateWithItemsResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    MultiPaymentTemplateWithItemsResponse(template).copy(
                        items = listOf(
                            MultiPaymentTemplateItemResponse(
                                id = template.items.value[0].id,
                                templateId = templateId,
                                walletAddress = newWalletAddress.rawValue,
                                itemName = newItemName,
                                amount = newAmount.rawValue,
                                createdAt = template.items.value[0].createdAt.value
                            )
                        ),
                        updatedAt = response.updatedAt
                    )
                )

            assertThat(response.updatedAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("multi-payment template item is correctly updated in the database") {
            val storedTemplate = multiPaymentTemplateRepository.getById(response.id)!!

            assertThat(storedTemplate).withMessage()
                .isEqualTo(
                    template.copy(
                        items = WithItems(
                            listOf(
                                MultiPaymentTemplateItem(
                                    id = template.items.value[0].id,
                                    templateId = templateId,
                                    walletAddress = newWalletAddress,
                                    itemName = newItemName,
                                    assetAmount = newAmount,
                                    createdAt = template.items.value[0].createdAt
                                )
                            )
                        ),
                        updatedAt = storedTemplate.updatedAt
                    )
                )

            assertThat(storedTemplate.updatedAt?.value)
                .isCloseTo(response.updatedAt, WITHIN_TIME_TOLERANCE)
            assertThat(storedTemplate.updatedAt?.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    @WithMockUser(OTHER_OWNER_ADDRESS)
    fun mustReturn404NotFoundWhenUpdatingItemForNonOwnedMultiPaymentTemplate() {
        val templateId = UUID.randomUUID()
        val template = MultiPaymentTemplate(
            id = templateId,
            items = WithItems(
                listOf(
                    MultiPaymentTemplateItem(
                        id = UUID.randomUUID(),
                        templateId = templateId,
                        walletAddress = WALLET_ADDRESS,
                        itemName = ITEM_NAME,
                        assetAmount = ASSET_AMOUNT,
                        createdAt = TestData.TIMESTAMP
                    )
                )
            ),
            templateName = TEMPLATE_NAME,
            tokenAddress = TOKEN_ADDRESS,
            chainId = CHAIN_ID,
            userId = OWNER_ID,
            createdAt = TestData.TIMESTAMP,
            updatedAt = null
        )

        suppose("multi-payment template exists in the database") {
            multiPaymentTemplateRepository.store(template)
        }

        val newWalletAddress = WalletAddress("abc123")
        val newItemName = "newItemName"
        val newAmount = Balance(BigInteger.valueOf(123L))

        verify("404 is returned for non-owned multi-payment template") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.patch(
                    "/v1/multi-payment-template/$templateId/items/${template.items.value[0].id}"
                )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "wallet_address": "${newWalletAddress.rawValue}",
                                "item_name": "$newItemName",
                                "amount": "${newAmount.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    @WithMockUser(OWNER_ADDRESS)
    fun mustReturn404NotFoundWhenUpdatingItemForNonExistentMultiPaymentTemplate() {
        val newWalletAddress = WalletAddress("abc123")
        val newItemName = "newItemName"
        val newAmount = Balance(BigInteger.valueOf(123L))

        verify("404 is returned for non-existent multi-payment template") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.patch(
                    "/v1/multi-payment-template/${UUID.randomUUID()}/items/${UUID.randomUUID()}"
                )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "wallet_address": "${newWalletAddress.rawValue}",
                                "item_name": "$newItemName",
                                "amount": "${newAmount.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    @WithMockUser(OWNER_ADDRESS)
    fun mustCorrectlyDeleteItemForMultiPaymentTemplate() {
        val templateId = UUID.randomUUID()
        val template = MultiPaymentTemplate(
            id = templateId,
            items = WithItems(
                listOf(
                    MultiPaymentTemplateItem(
                        id = UUID.randomUUID(),
                        templateId = templateId,
                        walletAddress = WALLET_ADDRESS,
                        itemName = ITEM_NAME,
                        assetAmount = ASSET_AMOUNT,
                        createdAt = TestData.TIMESTAMP
                    )
                )
            ),
            templateName = TEMPLATE_NAME,
            tokenAddress = TOKEN_ADDRESS,
            chainId = CHAIN_ID,
            userId = OWNER_ID,
            createdAt = TestData.TIMESTAMP,
            updatedAt = null
        )

        suppose("multi-payment template exists in the database") {
            multiPaymentTemplateRepository.store(template)
        }

        val response = suppose("request to delete multi-payment template item is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.delete(
                    "/v1/multi-payment-template/$templateId/items/${template.items.value[0].id}"
                )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, MultiPaymentTemplateWithItemsResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    MultiPaymentTemplateWithItemsResponse(template).copy(
                        items = emptyList(),
                        updatedAt = response.updatedAt
                    )
                )

            assertThat(response.updatedAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("multi-payment template item is correctly deleted from database") {
            val storedTemplate = multiPaymentTemplateRepository.getById(response.id)!!

            assertThat(storedTemplate).withMessage()
                .isEqualTo(
                    template.copy(
                        items = WithItems(emptyList()),
                        updatedAt = storedTemplate.updatedAt
                    )
                )

            assertThat(storedTemplate.updatedAt?.value)
                .isCloseTo(response.updatedAt, WITHIN_TIME_TOLERANCE)
            assertThat(storedTemplate.updatedAt?.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    @WithMockUser(OTHER_OWNER_ADDRESS)
    fun mustReturn404NotFoundWhenDeletingItemForNonOwnedMultiPaymentTemplate() {
        val templateId = UUID.randomUUID()
        val template = MultiPaymentTemplate(
            id = templateId,
            items = WithItems(
                listOf(
                    MultiPaymentTemplateItem(
                        id = UUID.randomUUID(),
                        templateId = templateId,
                        walletAddress = WALLET_ADDRESS,
                        itemName = ITEM_NAME,
                        assetAmount = ASSET_AMOUNT,
                        createdAt = TestData.TIMESTAMP
                    )
                )
            ),
            templateName = TEMPLATE_NAME,
            tokenAddress = TOKEN_ADDRESS,
            chainId = CHAIN_ID,
            userId = OWNER_ID,
            createdAt = TestData.TIMESTAMP,
            updatedAt = null
        )

        suppose("multi-payment template exists in the database") {
            multiPaymentTemplateRepository.store(template)
        }

        verify("404 is returned for non-owned multi-payment template") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.delete(
                    "/v1/multi-payment-template/$templateId/items/${template.items.value[0].id}"
                )
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    @WithMockUser(OWNER_ADDRESS)
    fun mustReturn404NotFoundWhenDeletingItemForNonExistentMultiPaymentTemplate() {
        verify("404 is returned for non-existent multi-payment template") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.delete(
                    "/v1/multi-payment-template/${UUID.randomUUID()}/items/${UUID.randomUUID()}"
                )
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }
}
