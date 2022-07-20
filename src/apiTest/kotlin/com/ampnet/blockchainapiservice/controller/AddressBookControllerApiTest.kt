package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.ControllerTestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.blockchain.properties.Chain
import com.ampnet.blockchainapiservice.config.binding.ProjectApiKeyResolver
import com.ampnet.blockchainapiservice.exception.ErrorCode
import com.ampnet.blockchainapiservice.generated.jooq.enums.UserIdentifierType
import com.ampnet.blockchainapiservice.generated.jooq.tables.AddressBookTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.ApiKeyTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.ProjectTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.UserIdentifierTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ApiKeyRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ProjectRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.UserIdentifierRecord
import com.ampnet.blockchainapiservice.model.response.AddressBookEntriesResponse
import com.ampnet.blockchainapiservice.model.response.AddressBookEntryResponse
import com.ampnet.blockchainapiservice.model.result.AddressBookEntry
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.repository.AddressBookRepository
import com.ampnet.blockchainapiservice.util.BaseUrl
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.UtcDateTime
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.util.UUID

class AddressBookControllerApiTest : ControllerTestBase() {

    companion object {
        private val PROJECT_ID = UUID.randomUUID()
        private val OWNER_ID = UUID.randomUUID()
        private val PROJECT = Project(
            id = PROJECT_ID,
            ownerId = OWNER_ID,
            issuerContractAddress = ContractAddress("0"),
            baseRedirectUrl = BaseUrl("https://example.com/"),
            chainId = Chain.HARDHAT_TESTNET.id,
            customRpcUrl = null,
            createdAt = TestData.TIMESTAMP
        )
        private const val API_KEY = "api-key"
        private val OTHER_PROJECT = Project(
            id = UUID.randomUUID(),
            ownerId = OWNER_ID,
            issuerContractAddress = ContractAddress("1"),
            baseRedirectUrl = BaseUrl("https://example.com/"),
            chainId = Chain.HARDHAT_TESTNET.id,
            customRpcUrl = null,
            createdAt = TestData.TIMESTAMP
        )
        private const val OTHER_API_KEY = "other-api-key"
    }

    @Autowired
    private lateinit var addressBookRepository: AddressBookRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        dslContext.deleteFrom(AddressBookTable.ADDRESS_BOOK).execute()
        dslContext.deleteFrom(ApiKeyTable.API_KEY).execute()
        dslContext.deleteFrom(ProjectTable.PROJECT).execute()
        dslContext.deleteFrom(UserIdentifierTable.USER_IDENTIFIER).execute()

        dslContext.executeInsert(
            UserIdentifierRecord(
                id = OWNER_ID,
                userIdentifier = "user-identifier",
                identifierType = UserIdentifierType.ETH_WALLET_ADDRESS
            )
        )

        dslContext.executeInsert(
            ProjectRecord(
                id = PROJECT.id,
                ownerId = PROJECT.ownerId,
                issuerContractAddress = PROJECT.issuerContractAddress,
                baseRedirectUrl = PROJECT.baseRedirectUrl,
                chainId = PROJECT.chainId,
                customRpcUrl = PROJECT.customRpcUrl,
                createdAt = PROJECT.createdAt
            )
        )

        dslContext.executeInsert(
            ProjectRecord(
                id = OTHER_PROJECT.id,
                ownerId = OTHER_PROJECT.ownerId,
                issuerContractAddress = OTHER_PROJECT.issuerContractAddress,
                baseRedirectUrl = OTHER_PROJECT.baseRedirectUrl,
                chainId = OTHER_PROJECT.chainId,
                customRpcUrl = OTHER_PROJECT.customRpcUrl,
                createdAt = OTHER_PROJECT.createdAt
            )
        )

        dslContext.executeInsert(
            ApiKeyRecord(
                id = UUID.randomUUID(),
                projectId = PROJECT_ID,
                apiKey = API_KEY,
                createdAt = TestData.TIMESTAMP
            )
        )

        dslContext.executeInsert(
            ApiKeyRecord(
                id = UUID.randomUUID(),
                projectId = OTHER_PROJECT.id,
                apiKey = OTHER_API_KEY,
                createdAt = TestData.TIMESTAMP
            )
        )
    }

    @Test
    fun mustCorrectlyCreateAddressBookEntry() {
        val alias = "alias"
        val address = WalletAddress("abc")
        val phoneNumber = "phone-number"
        val email = "email"

        val response = suppose("request to create address book entry is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/address-book")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "alias": "$alias",
                                "address": "${address.rawValue}",
                                "phone_number": "$phoneNumber",
                                "email": "$email"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, AddressBookEntryResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    AddressBookEntryResponse(
                        id = response.id,
                        alias = alias,
                        address = address.rawValue,
                        phoneNumber = phoneNumber,
                        email = email,
                        createdAt = response.createdAt,
                        projectId = PROJECT_ID
                    )
                )

            assertThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("address book entry is correctly stored into the database") {
            val storedEntry = addressBookRepository.getById(response.id)!!

            assertThat(storedEntry).withMessage()
                .isEqualTo(
                    AddressBookEntry(
                        id = response.id,
                        alias = alias,
                        address = address,
                        phoneNumber = phoneNumber,
                        email = email,
                        createdAt = UtcDateTime(response.createdAt),
                        projectId = PROJECT_ID
                    )
                )

            assertThat(storedEntry.createdAt.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustReturn401UnauthorizedWhenCreatingAddressBookEntryWithInvalidApiKey() {
        val alias = "alias"
        val address = WalletAddress("abc")
        val phoneNumber = "phone-number"
        val email = "email"

        verify("401 is returned for invalid API key") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/address-book")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, "invalid-api-key")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "alias": "$alias",
                                "address": "${address.rawValue}",
                                "phone_number": "$phoneNumber",
                                "email": "$email"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isUnauthorized)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.NON_EXISTENT_API_KEY)
        }
    }

    @Test
    fun mustCorrectlyUpdateAddressBookEntry() {
        val entry = AddressBookEntry(
            id = UUID.randomUUID(),
            alias = "alias",
            address = WalletAddress("abc"),
            phoneNumber = "phone-number",
            email = "email",
            createdAt = TestData.TIMESTAMP,
            projectId = PROJECT_ID
        )

        suppose("some address book entry exists in the database") {
            addressBookRepository.store(entry)
        }

        val newAlias = "alias"
        val newAddress = WalletAddress("abc")
        val newPhoneNumber = "phone-number"
        val newEmail = "email"

        val response = suppose("request to update address book entry is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.patch("/v1/address-book/${entry.id}")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "alias": "$newAlias",
                                "address": "${newAddress.rawValue}",
                                "phone_number": "$newPhoneNumber",
                                "email": "$newEmail"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, AddressBookEntryResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    AddressBookEntryResponse(
                        id = entry.id,
                        alias = newAlias,
                        address = newAddress.rawValue,
                        phoneNumber = newPhoneNumber,
                        email = newEmail,
                        createdAt = entry.createdAt.value,
                        projectId = entry.projectId
                    )
                )
        }

        verify("address book entry is correctly updated in the database") {
            val storedEntry = addressBookRepository.getById(response.id)!!

            assertThat(storedEntry).withMessage()
                .isEqualTo(
                    AddressBookEntry(
                        id = entry.id,
                        alias = newAlias,
                        address = newAddress,
                        phoneNumber = newPhoneNumber,
                        email = newEmail,
                        createdAt = entry.createdAt,
                        projectId = entry.projectId
                    )
                )
        }
    }

    @Test
    fun mustReturn401UnauthorizedWhenUpdatingAddressBookEntryWithInvalidApiKey() {
        val alias = "alias"
        val address = WalletAddress("abc")
        val phoneNumber = "phone-number"
        val email = "email"

        verify("401 is returned for invalid API key") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.patch("/v1/address-book/${UUID.randomUUID()}")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, "invalid-api-key")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "alias": "$alias",
                                "address": "${address.rawValue}",
                                "phone_number": "$phoneNumber",
                                "email": "$email"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isUnauthorized)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.NON_EXISTENT_API_KEY)
        }
    }

    @Test
    fun mustReturn404NotFoundWhenUpdatingNonOwnedAddressBookEntry() {
        val entry = AddressBookEntry(
            id = UUID.randomUUID(),
            alias = "alias",
            address = WalletAddress("abc"),
            phoneNumber = "phone-number",
            email = "email",
            createdAt = TestData.TIMESTAMP,
            projectId = OTHER_PROJECT.id
        )

        suppose("some address book entry exists in the database") {
            addressBookRepository.store(entry)
        }

        val newAlias = "alias"
        val newAddress = WalletAddress("abc")
        val newPhoneNumber = "phone-number"
        val newEmail = "email"

        verify("404 is returned for non-owned address book entry") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.patch("/v1/address-book/${entry.id}")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "alias": "$newAlias",
                                "address": "${newAddress.rawValue}",
                                "phone_number": "$newPhoneNumber",
                                "email": "$newEmail"
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
    fun mustReturn404NotFoundWhenUpdatingNonExistentAddressBookEntry() {
        val alias = "alias"
        val address = WalletAddress("abc")
        val phoneNumber = "phone-number"
        val email = "email"

        verify("404 is returned for non-existent address book entry") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.patch("/v1/address-book/${UUID.randomUUID()}")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "alias": "$alias",
                                "address": "${address.rawValue}",
                                "phone_number": "$phoneNumber",
                                "email": "$email"
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
    fun mustCorrectlyDeleteAddressBookEntry() {
        val entry = AddressBookEntry(
            id = UUID.randomUUID(),
            alias = "alias",
            address = WalletAddress("abc"),
            phoneNumber = "phone-number",
            email = "email",
            createdAt = TestData.TIMESTAMP,
            projectId = PROJECT_ID
        )

        suppose("some address book entry exists in the database") {
            addressBookRepository.store(entry)
        }

        suppose("request to delete address book entry is made") {
            mockMvc.perform(
                MockMvcRequestBuilders.delete("/v1/address-book/${entry.id}")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
        }

        verify("address book entry is deleted from database") {
            assertThat(addressBookRepository.getById(entry.id)).withMessage()
                .isNull()
        }
    }

    @Test
    fun mustReturn401UnauthorizedWhenDeletingAddressBookEntryWithInvalidApiKey() {
        verify("401 is returned for invalid API key") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.delete("/v1/address-book/${UUID.randomUUID()}")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, "invalid-api-key")
            )
                .andExpect(MockMvcResultMatchers.status().isUnauthorized)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.NON_EXISTENT_API_KEY)
        }
    }

    @Test
    fun mustReturn404NotFoundWhenDeletingNonOwnedAddressBookEntry() {
        val entry = AddressBookEntry(
            id = UUID.randomUUID(),
            alias = "alias",
            address = WalletAddress("abc"),
            phoneNumber = "phone-number",
            email = "email",
            createdAt = TestData.TIMESTAMP,
            projectId = OTHER_PROJECT.id
        )

        suppose("some address book entry exists in the database") {
            addressBookRepository.store(entry)
        }

        verify("404 is returned for non-owned address book entry") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.delete("/v1/address-book/${entry.id}")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    fun mustReturn404NotFoundWhenDeletingNonExistentAddressBookEntry() {
        verify("404 is returned for non-existent address book entry") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.delete("/v1/address-book/${UUID.randomUUID()}")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    fun mustCorrectlyFetchAddressBookEntryById() {
        val entry = AddressBookEntry(
            id = UUID.randomUUID(),
            alias = "alias",
            address = WalletAddress("abc"),
            phoneNumber = "phone-number",
            email = "email",
            createdAt = TestData.TIMESTAMP,
            projectId = PROJECT_ID
        )

        suppose("some address book entry exists in the database") {
            addressBookRepository.store(entry)
        }

        val response = suppose("request to fetch address book entry by id is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/address-book/${entry.id}")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, AddressBookEntryResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    AddressBookEntryResponse(
                        id = entry.id,
                        alias = entry.alias,
                        address = entry.address.rawValue,
                        phoneNumber = entry.phoneNumber,
                        email = entry.email,
                        createdAt = entry.createdAt.value,
                        projectId = entry.projectId
                    )
                )
        }
    }

    @Test
    fun mustReturn404NotFoundWhenFetchingNonOwnedAddressBookEntryById() {
        val entry = AddressBookEntry(
            id = UUID.randomUUID(),
            alias = "alias",
            address = WalletAddress("abc"),
            phoneNumber = "phone-number",
            email = "email",
            createdAt = TestData.TIMESTAMP,
            projectId = OTHER_PROJECT.id
        )

        suppose("some address book entry exists in the database") {
            addressBookRepository.store(entry)
        }

        verify("404 is returned for non-owned address book entry") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/address-book/${entry.id}")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    fun mustReturn404NotFoundWhenFetchingNonExistentAddressBookEntryById() {
        verify("404 is returned for non-existent address book entry") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/address-book/${UUID.randomUUID()}")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    fun mustReturn401UnauthorizedWhenFetchingAddressBookEntryWithInvalidApiKeyById() {
        verify("401 is returned for invalid API key") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/address-book/${UUID.randomUUID()}")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, "invalid-api-key")
            )
                .andExpect(MockMvcResultMatchers.status().isUnauthorized)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.NON_EXISTENT_API_KEY)
        }
    }

    @Test
    fun mustCorrectlyFetchAddressBookEntryByAlias() {
        val entry = AddressBookEntry(
            id = UUID.randomUUID(),
            alias = "alias",
            address = WalletAddress("abc"),
            phoneNumber = "phone-number",
            email = "email",
            createdAt = TestData.TIMESTAMP,
            projectId = PROJECT_ID
        )

        suppose("some address book entry exists in the database") {
            addressBookRepository.store(entry)
        }

        val response = suppose("request to fetch address book entry by alias is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/address-book/by-alias/${entry.alias}")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, AddressBookEntryResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    AddressBookEntryResponse(
                        id = entry.id,
                        alias = entry.alias,
                        address = entry.address.rawValue,
                        phoneNumber = entry.phoneNumber,
                        email = entry.email,
                        createdAt = entry.createdAt.value,
                        projectId = entry.projectId
                    )
                )
        }
    }

    @Test
    fun mustReturn404NotFoundWhenFetchingNonExistentAddressBookEntryByAlias() {
        verify("404 is returned for non-existent address book entry") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/address-book/by-alias/non-existent-alias")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    fun mustReturn401UnauthorizedWhenFetchingAddressBookEntryWithInvalidApiKeyByAlias() {
        verify("401 is returned for invalid API key") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/address-book/by-alias/alias")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, "invalid-api-key")
            )
                .andExpect(MockMvcResultMatchers.status().isUnauthorized)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.NON_EXISTENT_API_KEY)
        }
    }

    @Test
    fun mustCorrectlyFetchAddressBookEntriesForProject() {
        val entry = AddressBookEntry(
            id = UUID.randomUUID(),
            alias = "alias",
            address = WalletAddress("abc"),
            phoneNumber = "phone-number",
            email = "email",
            createdAt = TestData.TIMESTAMP,
            projectId = PROJECT_ID
        )

        suppose("some address book entry exists in the database") {
            addressBookRepository.store(entry)
        }

        val response = suppose("request to fetch address book entries for project is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/address-book")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, AddressBookEntriesResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    AddressBookEntriesResponse(
                        listOf(
                            AddressBookEntryResponse(
                                id = entry.id,
                                alias = entry.alias,
                                address = entry.address.rawValue,
                                phoneNumber = entry.phoneNumber,
                                email = entry.email,
                                createdAt = entry.createdAt.value,
                                projectId = entry.projectId
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustReturn401UnauthorizedWhenFetchingAddressBookEntriesWithInvalidApiKey() {
        verify("401 is returned for invalid API key") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/address-book")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, "invalid-api-key")
            )
                .andExpect(MockMvcResultMatchers.status().isUnauthorized)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.NON_EXISTENT_API_KEY)
        }
    }
}
