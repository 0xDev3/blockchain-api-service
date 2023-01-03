package dev3.blockchainapiservice.controller

import dev3.blockchainapiservice.JsonSchemaDocumentation
import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.generated.jooq.id.MultiPaymentTemplateId
import dev3.blockchainapiservice.generated.jooq.id.MultiPaymentTemplateItemId
import dev3.blockchainapiservice.generated.jooq.id.UserId
import dev3.blockchainapiservice.model.request.CreateMultiPaymentTemplateRequest
import dev3.blockchainapiservice.model.request.MultiPaymentTemplateItemRequest
import dev3.blockchainapiservice.model.request.UpdateMultiPaymentTemplateRequest
import dev3.blockchainapiservice.model.response.MultiPaymentTemplateWithItemsResponse
import dev3.blockchainapiservice.model.response.MultiPaymentTemplateWithoutItemsResponse
import dev3.blockchainapiservice.model.response.MultiPaymentTemplatesResponse
import dev3.blockchainapiservice.model.result.MultiPaymentTemplate
import dev3.blockchainapiservice.model.result.MultiPaymentTemplateItem
import dev3.blockchainapiservice.model.result.UserWalletAddressIdentifier
import dev3.blockchainapiservice.model.result.WithItems
import dev3.blockchainapiservice.service.MultiPaymentTemplateService
import dev3.blockchainapiservice.util.AssetType
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.WalletAddress
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.http.ResponseEntity
import java.math.BigInteger
import java.util.UUID

class MultiPaymentTemplateControllerTest : TestBase() {

    companion object {
        private val USER_IDENTIFIER = UserWalletAddressIdentifier(
            id = UserId(UUID.randomUUID()),
            stripeClientId = null,
            walletAddress = WalletAddress("cafebabe")
        )
        private val TEMPLATE_ID = MultiPaymentTemplateId(UUID.randomUUID())
        private val ITEM = MultiPaymentTemplateItem(
            id = MultiPaymentTemplateItemId(UUID.randomUUID()),
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
    }

    @Test
    fun mustCorrectlyCreateMultiPaymentTemplate() {
        val request = CreateMultiPaymentTemplateRequest(
            templateName = TEMPLATE.templateName,
            assetType = AssetType.TOKEN,
            tokenAddress = TEMPLATE.tokenAddress?.rawValue,
            chainId = TEMPLATE.chainId.value,
            items = listOf(
                MultiPaymentTemplateItemRequest(
                    walletAddress = ITEM.walletAddress.rawValue,
                    itemName = ITEM.itemName,
                    amount = ITEM.assetAmount.rawValue
                )
            )
        )

        val service = mock<MultiPaymentTemplateService>()

        suppose("multi-payment template will be created") {
            call(service.createMultiPaymentTemplate(request, USER_IDENTIFIER))
                .willReturn(TEMPLATE)
        }

        val controller = MultiPaymentTemplateController(service)

        verify("controller returns correct response") {
            val response = controller.createMultiPaymentTemplate(USER_IDENTIFIER, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(MultiPaymentTemplateWithItemsResponse(TEMPLATE)))
        }
    }

    @Test
    fun mustCorrectlyUpdateMultiPaymentTemplate() {
        val request = UpdateMultiPaymentTemplateRequest(
            templateName = TEMPLATE.templateName,
            assetType = AssetType.TOKEN,
            tokenAddress = TEMPLATE.tokenAddress?.rawValue,
            chainId = TEMPLATE.chainId.value
        )

        val service = mock<MultiPaymentTemplateService>()

        suppose("multi-payment template will be updated") {
            call(service.updateMultiPaymentTemplate(TEMPLATE_ID, request, USER_IDENTIFIER))
                .willReturn(TEMPLATE)
        }

        val controller = MultiPaymentTemplateController(service)

        verify("controller returns correct response") {
            val response = controller.updateMultiPaymentTemplate(TEMPLATE_ID, USER_IDENTIFIER, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(MultiPaymentTemplateWithItemsResponse(TEMPLATE)))
        }
    }

    @Test
    fun mustCorrectlyDeleteMultiPaymentTemplate() {
        val service = mock<MultiPaymentTemplateService>()
        val controller = MultiPaymentTemplateController(service)

        verify("controller returns correct response") {
            controller.deleteMultiPaymentTemplate(TEMPLATE_ID, USER_IDENTIFIER)

            expectInteractions(service) {
                once.deleteMultiPaymentTemplateById(TEMPLATE_ID, USER_IDENTIFIER)
            }
        }
    }

    @Test
    fun mustCorrectlyFetchMultiPaymentTemplateById() {
        val service = mock<MultiPaymentTemplateService>()

        suppose("multi-payment template will be fetched") {
            call(service.getMultiPaymentTemplateById(TEMPLATE_ID))
                .willReturn(TEMPLATE)
        }

        val controller = MultiPaymentTemplateController(service)

        verify("controller returns correct response") {
            val response = controller.getMultiPaymentTemplateById(TEMPLATE_ID)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(MultiPaymentTemplateWithItemsResponse(TEMPLATE)))
        }
    }

    @Test
    fun mustCorrectlyFetchAllMultiPaymentTemplatesByWalletAddress() {
        val service = mock<MultiPaymentTemplateService>()

        suppose("multi-payment template will be fetched") {
            call(service.getAllMultiPaymentTemplatesByWalletAddress(USER_IDENTIFIER.walletAddress))
                .willReturn(listOf(TEMPLATE.withoutItems()))
        }

        val controller = MultiPaymentTemplateController(service)

        verify("controller returns correct response") {
            val response = controller.getAllMultiPaymentTemplatesByWalletAddress(USER_IDENTIFIER.walletAddress.rawValue)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        MultiPaymentTemplatesResponse(
                            listOf(
                                MultiPaymentTemplateWithoutItemsResponse(TEMPLATE.withoutItems())
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyAddItemToMultiPaymentTemplate() {
        val request = MultiPaymentTemplateItemRequest(
            walletAddress = ITEM.walletAddress.rawValue,
            itemName = ITEM.itemName,
            amount = ITEM.assetAmount.rawValue
        )

        val service = mock<MultiPaymentTemplateService>()

        suppose("multi-payment template item will be created") {
            call(service.addItemToMultiPaymentTemplate(TEMPLATE_ID, request, USER_IDENTIFIER))
                .willReturn(TEMPLATE)
        }

        val controller = MultiPaymentTemplateController(service)

        verify("controller returns correct response") {
            val response = controller.addItemToMultiPaymentTemplate(TEMPLATE_ID, USER_IDENTIFIER, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(MultiPaymentTemplateWithItemsResponse(TEMPLATE)))
        }
    }

    @Test
    fun mustCorrectlyUpdateMultiPaymentTemplateItem() {
        val request = MultiPaymentTemplateItemRequest(
            walletAddress = ITEM.walletAddress.rawValue,
            itemName = ITEM.itemName,
            amount = ITEM.assetAmount.rawValue
        )

        val service = mock<MultiPaymentTemplateService>()

        suppose("multi-payment template item will be updated") {
            call(service.updateMultiPaymentTemplateItem(TEMPLATE_ID, ITEM.id, request, USER_IDENTIFIER))
                .willReturn(TEMPLATE)
        }

        val controller = MultiPaymentTemplateController(service)

        verify("controller returns correct response") {
            val response = controller.updateMultiPaymentTemplateItem(TEMPLATE_ID, ITEM.id, USER_IDENTIFIER, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(MultiPaymentTemplateWithItemsResponse(TEMPLATE)))
        }
    }

    @Test
    fun mustCorrectlyDeleteMultiPaymentTemplateItem() {
        val service = mock<MultiPaymentTemplateService>()

        suppose("multi-payment template item will be deleted") {
            call(service.deleteMultiPaymentTemplateItem(TEMPLATE_ID, ITEM.id, USER_IDENTIFIER))
                .willReturn(TEMPLATE)
        }

        val controller = MultiPaymentTemplateController(service)

        verify("controller returns correct response") {
            val response = controller.deleteMultiPaymentTemplateItem(TEMPLATE_ID, ITEM.id, USER_IDENTIFIER)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(MultiPaymentTemplateWithItemsResponse(TEMPLATE)))
        }
    }
}
