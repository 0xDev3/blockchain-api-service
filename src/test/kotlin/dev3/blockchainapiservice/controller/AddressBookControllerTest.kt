package dev3.blockchainapiservice.controller

import dev3.blockchainapiservice.JsonSchemaDocumentation
import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.model.request.CreateOrUpdateAddressBookEntryRequest
import dev3.blockchainapiservice.model.response.AddressBookEntriesResponse
import dev3.blockchainapiservice.model.response.AddressBookEntryResponse
import dev3.blockchainapiservice.model.result.AddressBookEntry
import dev3.blockchainapiservice.model.result.UserWalletAddressIdentifier
import dev3.blockchainapiservice.service.AddressBookService
import dev3.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoMoreInteractions
import org.springframework.http.ResponseEntity
import java.util.UUID
import org.mockito.kotlin.verify as verifyMock

class AddressBookControllerTest : TestBase() {

    companion object {
        private val USER_IDENTIFIER = UserWalletAddressIdentifier(
            id = UUID.randomUUID(),
            stripeClientId = null,
            walletAddress = WalletAddress("cafebabe")
        )
        private val ENTRY = AddressBookEntry(
            id = UUID.randomUUID(),
            alias = "alias",
            address = WalletAddress("a"),
            phoneNumber = "phone-number",
            email = "email",
            createdAt = TestData.TIMESTAMP,
            userId = USER_IDENTIFIER.id
        )
    }

    @Test
    fun mustCorrectlyCreateAddressBookEntry() {
        val request = CreateOrUpdateAddressBookEntryRequest(
            alias = ENTRY.alias,
            address = ENTRY.address.rawValue,
            phoneNumber = ENTRY.phoneNumber,
            email = ENTRY.email
        )

        val service = mock<AddressBookService>()

        suppose("address book entry will be created") {
            given(service.createAddressBookEntry(request, USER_IDENTIFIER))
                .willReturn(ENTRY)
        }

        val controller = AddressBookController(service)

        verify("controller returns correct response") {
            val response = controller.createAddressBookEntry(USER_IDENTIFIER, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(ResponseEntity.ok(AddressBookEntryResponse(ENTRY)))
        }
    }

    @Test
    fun mustCorrectlyUpdateAddressBookEntry() {
        val request = CreateOrUpdateAddressBookEntryRequest(
            alias = ENTRY.alias,
            address = ENTRY.address.rawValue,
            phoneNumber = ENTRY.phoneNumber,
            email = ENTRY.email
        )

        val service = mock<AddressBookService>()

        suppose("address book entry will be updated") {
            given(service.updateAddressBookEntry(ENTRY.id, request, USER_IDENTIFIER))
                .willReturn(ENTRY)
        }

        val controller = AddressBookController(service)

        verify("controller returns correct response") {
            val response = controller.updateAddressBookEntry(ENTRY.id, USER_IDENTIFIER, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(ResponseEntity.ok(AddressBookEntryResponse(ENTRY)))
        }
    }

    @Test
    fun mustCorrectlyDeleteAddressBookEntry() {
        val service = mock<AddressBookService>()
        val controller = AddressBookController(service)

        verify("controller calls correct service method") {
            controller.deleteAddressBookEntry(ENTRY.id, USER_IDENTIFIER)

            verifyMock(service)
                .deleteAddressBookEntryById(ENTRY.id, USER_IDENTIFIER)
            verifyNoMoreInteractions(service)
        }
    }

    @Test
    fun mustCorrectlyFetchAddressBookEntryById() {
        val service = mock<AddressBookService>()

        suppose("address book entry will be fetched by id") {
            given(service.getAddressBookEntryById(ENTRY.id))
                .willReturn(ENTRY)
        }

        val controller = AddressBookController(service)

        verify("controller returns correct response") {
            val response = controller.getAddressBookEntryById(ENTRY.id)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(ResponseEntity.ok(AddressBookEntryResponse(ENTRY)))
        }
    }

    @Test
    fun mustCorrectlyFetchAddressBookEntryByAlias() {
        val service = mock<AddressBookService>()

        suppose("address book entry will be fetched by alias") {
            given(service.getAddressBookEntryByAlias(ENTRY.alias, USER_IDENTIFIER))
                .willReturn(ENTRY)
        }

        val controller = AddressBookController(service)

        verify("controller returns correct response") {
            val response = controller.getAddressBookEntryByAlias(ENTRY.alias, USER_IDENTIFIER)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(ResponseEntity.ok(AddressBookEntryResponse(ENTRY)))
        }
    }

    @Test
    fun mustCorrectlyFetchAddressBookEntriesForWalletAddress() {
        val service = mock<AddressBookService>()

        suppose("address book entries will be fetched for wallet address") {
            given(service.getAddressBookEntriesByWalletAddress(USER_IDENTIFIER.walletAddress))
                .willReturn(listOf(ENTRY))
        }

        val controller = AddressBookController(service)

        verify("controller returns correct response") {
            val response = controller.getAddressBookEntriesForWalletAddress(USER_IDENTIFIER.walletAddress.rawValue)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(
                        AddressBookEntriesResponse(
                            listOf(AddressBookEntryResponse(ENTRY))
                        )
                    )
                )
        }
    }
}
