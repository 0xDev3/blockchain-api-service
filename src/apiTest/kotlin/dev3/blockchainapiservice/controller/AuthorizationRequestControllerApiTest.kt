package dev3.blockchainapiservice.controller

import dev3.blockchainapiservice.ControllerTestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.config.CustomHeaders
import dev3.blockchainapiservice.exception.ErrorCode
import dev3.blockchainapiservice.generated.jooq.enums.UserIdentifierType
import dev3.blockchainapiservice.generated.jooq.tables.AuthorizationRequestTable
import dev3.blockchainapiservice.generated.jooq.tables.records.ApiKeyRecord
import dev3.blockchainapiservice.generated.jooq.tables.records.ProjectRecord
import dev3.blockchainapiservice.generated.jooq.tables.records.UserIdentifierRecord
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.model.params.StoreAuthorizationRequestParams
import dev3.blockchainapiservice.model.response.AuthorizationRequestResponse
import dev3.blockchainapiservice.model.response.AuthorizationRequestsResponse
import dev3.blockchainapiservice.model.result.AuthorizationRequest
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.repository.AuthorizationRequestRepository
import dev3.blockchainapiservice.util.BaseUrl
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.SignedMessage
import dev3.blockchainapiservice.util.Status
import dev3.blockchainapiservice.util.WalletAddress
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.util.UUID

class AuthorizationRequestControllerApiTest : ControllerTestBase() {

    companion object {
        private val PROJECT_ID = UUID.randomUUID()
        private val OWNER_ID = UUID.randomUUID()
        private val PROJECT = Project(
            id = PROJECT_ID,
            ownerId = OWNER_ID,
            issuerContractAddress = ContractAddress("0"),
            baseRedirectUrl = BaseUrl("https://example.com/"),
            chainId = TestData.CHAIN_ID,
            customRpcUrl = null,
            createdAt = TestData.TIMESTAMP
        )
        private const val API_KEY = "api-key"
    }

    @Autowired
    private lateinit var authorizationRequestRepository: AuthorizationRequestRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        postgresContainer.cleanAllDatabaseTables(dslContext)

        dslContext.executeInsert(
            UserIdentifierRecord(
                id = OWNER_ID,
                userIdentifier = "user-identifier",
                identifierType = UserIdentifierType.ETH_WALLET_ADDRESS,
                stripeClientId = null
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
            ApiKeyRecord(
                id = UUID.randomUUID(),
                projectId = PROJECT_ID,
                apiKey = API_KEY,
                createdAt = TestData.TIMESTAMP
            )
        )
    }

    @Test
    fun mustCorrectlyCreateAuthorizationRequest() {
        val walletAddress = WalletAddress("a")

        val response = suppose("request to create authorization request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/wallet-authorization")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "wallet_address": "${walletAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, AuthorizationRequestResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    AuthorizationRequestResponse(
                        id = response.id,
                        projectId = PROJECT_ID,
                        status = Status.PENDING,
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-authorization/${response.id}/action",
                        walletAddress = walletAddress.rawValue,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        messageToSign = "Authorization message ID to sign: ${response.id}",
                        signedMessage = null,
                        createdAt = response.createdAt
                    )
                )

            expectThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("authorization request is correctly stored in database") {
            val storedRequest = authorizationRequestRepository.getById(response.id)

            expectThat(storedRequest)
                .isEqualTo(
                    AuthorizationRequest(
                        id = response.id,
                        projectId = PROJECT_ID,
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-authorization/${response.id}/action",
                        messageToSignOverride = null,
                        storeIndefinitely = true,
                        requestedWalletAddress = walletAddress,
                        actualWalletAddress = null,
                        signedMessage = null,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        createdAt = storedRequest!!.createdAt
                    )
                )

            expectThat(storedRequest.createdAt.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyCreateAuthorizationRequestWithRedirectUrl() {
        val walletAddress = WalletAddress("a")
        val redirectUrl = "https://custom-url/\${id}"

        val response = suppose("request to create authorization request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/wallet-authorization")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "redirect_url": "$redirectUrl",
                                "wallet_address": "${walletAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, AuthorizationRequestResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    AuthorizationRequestResponse(
                        id = response.id,
                        projectId = PROJECT_ID,
                        status = Status.PENDING,
                        redirectUrl = "https://custom-url/${response.id}",
                        walletAddress = walletAddress.rawValue,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        messageToSign = "Authorization message ID to sign: ${response.id}",
                        signedMessage = null,
                        createdAt = response.createdAt
                    )
                )

            expectThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("authorization request is correctly stored in database") {
            val storedRequest = authorizationRequestRepository.getById(response.id)

            expectThat(storedRequest)
                .isEqualTo(
                    AuthorizationRequest(
                        id = response.id,
                        projectId = PROJECT_ID,
                        redirectUrl = "https://custom-url/${response.id}",
                        messageToSignOverride = null,
                        storeIndefinitely = true,
                        requestedWalletAddress = walletAddress,
                        actualWalletAddress = null,
                        signedMessage = null,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        createdAt = storedRequest!!.createdAt
                    )
                )

            expectThat(storedRequest.createdAt.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustReturn401UnauthorizedWhenCreatingAuthorizationRequestWithInvalidApiKey() {
        val walletAddress = WalletAddress("a")

        verify("401 is returned for invalid API key") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/wallet-authorization")
                    .header(CustomHeaders.API_KEY_HEADER, "invalid-api-key")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "wallet_address": "${walletAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                }
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
    fun mustCorrectlyFetchAuthorizationRequest() {
        val walletAddress = WalletAddress("0x865f603F42ca1231e5B5F90e15663b0FE19F0b21")

        val createResponse = suppose("request to create authorization request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/wallet-authorization")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "wallet_address": "${walletAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(createResponse.response.contentAsString, AuthorizationRequestResponse::class.java)
        }

        val id = UUID.fromString("7d86b0ac-a9a6-40fc-ac6d-2a29ca687f73")

        suppose("ID from pre-signed message is used in database") {
            dslContext.update(AuthorizationRequestTable)
                .set(AuthorizationRequestTable.ID, id)
                .set(AuthorizationRequestTable.REDIRECT_URL, "https://example.com/$id")
                .where(AuthorizationRequestTable.ID.eq(createResponse.id))
                .execute()
        }

        val signedMessage = SignedMessage(
            "0xcf6025b67228271aaed15b17d42d6258e3b47ad5dcd27088a81b2f36b9d7ff5d2d133a652297bc19bea94d10750076fbdc529a" +
                "b03540e2ea6be02903645a98531b"
        )

        suppose("signed message is attached to authorization request") {
            authorizationRequestRepository.setSignedMessage(id, walletAddress, signedMessage)
        }

        val fetchResponse = suppose("request to fetch authorization request is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/wallet-authorization/$id")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, AuthorizationRequestResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(fetchResponse)
                .isEqualTo(
                    AuthorizationRequestResponse(
                        id = id,
                        projectId = PROJECT_ID,
                        status = Status.SUCCESS,
                        redirectUrl = "https://example.com/$id",
                        walletAddress = walletAddress.rawValue,
                        arbitraryData = createResponse.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        messageToSign = "Authorization message ID to sign: $id",
                        signedMessage = signedMessage.value,
                        createdAt = fetchResponse.createdAt
                    )
                )

            expectThat(fetchResponse.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchAuthorizationRequestsByProjectId() {
        val walletAddress = WalletAddress("0x865f603F42ca1231e5B5F90e15663b0FE19F0b21")

        val createResponse = suppose("request to create authorization request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/wallet-authorization")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "wallet_address": "${walletAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(createResponse.response.contentAsString, AuthorizationRequestResponse::class.java)
        }

        val id = UUID.fromString("7d86b0ac-a9a6-40fc-ac6d-2a29ca687f73")

        suppose("ID from pre-signed message is used in database") {
            dslContext.update(AuthorizationRequestTable)
                .set(AuthorizationRequestTable.ID, id)
                .set(AuthorizationRequestTable.REDIRECT_URL, "https://example.com/$id")
                .where(AuthorizationRequestTable.ID.eq(createResponse.id))
                .execute()
        }

        val signedMessage = SignedMessage(
            "0xcf6025b67228271aaed15b17d42d6258e3b47ad5dcd27088a81b2f36b9d7ff5d2d133a652297bc19bea94d10750076fbdc529a" +
                "b03540e2ea6be02903645a98531b"
        )

        suppose("signed message is attached to authorization request") {
            authorizationRequestRepository.setSignedMessage(id, walletAddress, signedMessage)
        }

        val fetchResponse = suppose("request to fetch authorization requests by project ID is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/wallet-authorization/by-project/$PROJECT_ID")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, AuthorizationRequestsResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(fetchResponse)
                .isEqualTo(
                    AuthorizationRequestsResponse(
                        listOf(
                            AuthorizationRequestResponse(
                                id = id,
                                projectId = PROJECT_ID,
                                status = Status.SUCCESS,
                                redirectUrl = "https://example.com/$id",
                                walletAddress = walletAddress.rawValue,
                                arbitraryData = createResponse.arbitraryData,
                                screenConfig = ScreenConfig(
                                    beforeActionMessage = "before-action-message",
                                    afterActionMessage = "after-action-message"
                                ),
                                messageToSign = "Authorization message ID to sign: $id",
                                signedMessage = signedMessage.value,
                                createdAt = fetchResponse.requests[0].createdAt
                            )
                        )
                    )
                )

            expectThat(fetchResponse.requests[0].createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustReturn404NotFoundForNonExistentAuthorizationRequest() {
        verify("404 is returned for non-existent authorization request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/wallet-authorization/${UUID.randomUUID()}")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    fun mustCorrectlyAttachSignedMessage() {
        val id = UUID.randomUUID()

        suppose("some authorization request without signed message exists in database") {
            authorizationRequestRepository.store(
                StoreAuthorizationRequestParams(
                    id = id,
                    projectId = PROJECT_ID,
                    redirectUrl = "https://example.com/$id",
                    messageToSignOverride = null,
                    storeIndefinitely = true,
                    requestedWalletAddress = WalletAddress("b"),
                    arbitraryData = TestData.EMPTY_JSON_OBJECT,
                    screenConfig = ScreenConfig(
                        beforeActionMessage = "before-action-message",
                        afterActionMessage = "after-action-message"
                    ),
                    createdAt = TestData.TIMESTAMP
                )
            )
        }

        val walletAddress = WalletAddress("c")
        val signedMessage = SignedMessage("signed-message")

        suppose("request to attach signed message to authorization request is made") {
            mockMvc.perform(
                MockMvcRequestBuilders.put("/v1/wallet-authorization/$id")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "wallet_address": "${walletAddress.rawValue}",
                                "signed_message": "${signedMessage.value}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
        }

        verify("signed message is correctly attached to authorization request") {
            val storedRequest = authorizationRequestRepository.getById(id)

            expectThat(storedRequest?.actualWalletAddress)
                .isEqualTo(walletAddress)
            expectThat(storedRequest?.signedMessage)
                .isEqualTo(signedMessage)
        }
    }

    @Test
    fun mustReturn400BadRequestWhenSignedMessageIsNotAttached() {
        val id = UUID.randomUUID()
        val walletAddress = WalletAddress("c")
        val signedMessage = SignedMessage("signed-message")

        suppose("some authorization request with signed message exists in database") {
            authorizationRequestRepository.store(
                StoreAuthorizationRequestParams(
                    id = id,
                    projectId = PROJECT_ID,
                    redirectUrl = "https://example.com/$id",
                    messageToSignOverride = null,
                    storeIndefinitely = true,
                    requestedWalletAddress = WalletAddress("b"),
                    arbitraryData = TestData.EMPTY_JSON_OBJECT,
                    screenConfig = ScreenConfig(
                        beforeActionMessage = "before-action-message",
                        afterActionMessage = "after-action-message"
                    ),
                    createdAt = TestData.TIMESTAMP
                )
            )
            authorizationRequestRepository.setSignedMessage(id, walletAddress, signedMessage)
        }

        verify("400 is returned when attaching signed message") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.put("/v1/wallet-authorization/$id")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "wallet_address": "${WalletAddress("dead").rawValue}",
                                "signed_message": "different-signed-message"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.SIGNED_MESSAGE_ALREADY_SET)
        }

        verify("signed message is not changed in database") {
            val storedRequest = authorizationRequestRepository.getById(id)

            expectThat(storedRequest?.actualWalletAddress)
                .isEqualTo(walletAddress)
            expectThat(storedRequest?.signedMessage)
                .isEqualTo(signedMessage)
        }
    }
}
