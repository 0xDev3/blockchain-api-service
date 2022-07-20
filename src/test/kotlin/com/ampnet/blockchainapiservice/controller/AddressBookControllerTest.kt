package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.JsonSchemaDocumentation
import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.model.request.CreateOrUpdateAddressBookEntryRequest
import com.ampnet.blockchainapiservice.model.response.AddressBookEntriesResponse
import com.ampnet.blockchainapiservice.model.response.AddressBookEntryResponse
import com.ampnet.blockchainapiservice.model.result.AddressBookEntry
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.service.AddressBookService
import com.ampnet.blockchainapiservice.util.BaseUrl
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.WalletAddress
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
        private val PROJECT = Project(
            id = UUID.randomUUID(),
            ownerId = UUID.randomUUID(),
            issuerContractAddress = ContractAddress("0"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = ChainId(1337L),
            customRpcUrl = "custom-rpc-url",
            createdAt = TestData.TIMESTAMP
        )
        private val ENTRY = AddressBookEntry(
            id = UUID.randomUUID(),
            alias = "alias",
            address = WalletAddress("a"),
            phoneNumber = "phone-number",
            email = "email",
            createdAt = TestData.TIMESTAMP,
            projectId = PROJECT.id
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
            given(service.createAddressBookEntry(request, PROJECT))
                .willReturn(ENTRY)
        }

        val controller = AddressBookController(service)

        verify("controller returns correct response") {
            val response = controller.createAddressBookEntry(PROJECT, request)

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
            given(service.updateAddressBookEntry(ENTRY.id, request, PROJECT))
                .willReturn(ENTRY)
        }

        val controller = AddressBookController(service)

        verify("controller returns correct response") {
            val response = controller.updateAddressBookEntry(ENTRY.id, PROJECT, request)

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
            controller.deleteAddressBookEntry(ENTRY.id, PROJECT)

            verifyMock(service)
                .deleteAddressBookEntryById(ENTRY.id, PROJECT)
            verifyNoMoreInteractions(service)
        }
    }

    @Test
    fun mustCorrectlyFetchAddressBookEntryById() {
        val service = mock<AddressBookService>()

        suppose("address book entry will be fetched by id") {
            given(service.getAddressBookEntryById(ENTRY.id, PROJECT))
                .willReturn(ENTRY)
        }

        val controller = AddressBookController(service)

        verify("controller returns correct response") {
            val response = controller.getAddressBookEntryById(ENTRY.id, PROJECT)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(ResponseEntity.ok(AddressBookEntryResponse(ENTRY)))
        }
    }

    @Test
    fun mustCorrectlyFetchAddressBookEntryByAlias() {
        val service = mock<AddressBookService>()

        suppose("address book entry will be fetched by alias") {
            given(service.getAddressBookEntryByAlias(ENTRY.alias, PROJECT))
                .willReturn(ENTRY)
        }

        val controller = AddressBookController(service)

        verify("controller returns correct response") {
            val response = controller.getAddressBookEntryByAlias(ENTRY.alias, PROJECT)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(ResponseEntity.ok(AddressBookEntryResponse(ENTRY)))
        }
    }

    @Test
    fun mustCorrectlyFetchAddressBookEntriesForProject() {
        val service = mock<AddressBookService>()

        suppose("address book entries will be fetched for project") {
            given(service.getAddressBookEntriesByProjectId(PROJECT.id))
                .willReturn(listOf(ENTRY))
        }

        val controller = AddressBookController(service)

        verify("controller returns correct response") {
            val response = controller.getAddressBookEntriesForProject(PROJECT)

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
