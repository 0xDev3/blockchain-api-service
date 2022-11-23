package dev3.blockchainapiservice.controller

import dev3.blockchainapiservice.ControllerTestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.blockchain.SimpleDisperse
import dev3.blockchainapiservice.blockchain.SimpleERC20
import dev3.blockchainapiservice.blockchain.properties.Chain
import dev3.blockchainapiservice.config.binding.ProjectApiKeyResolver
import dev3.blockchainapiservice.exception.ErrorCode
import dev3.blockchainapiservice.generated.jooq.enums.UserIdentifierType
import dev3.blockchainapiservice.generated.jooq.tables.records.ApiKeyRecord
import dev3.blockchainapiservice.generated.jooq.tables.records.ProjectRecord
import dev3.blockchainapiservice.generated.jooq.tables.records.UserIdentifierRecord
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.model.params.StoreAssetMultiSendRequestParams
import dev3.blockchainapiservice.model.response.AssetMultiSendRequestResponse
import dev3.blockchainapiservice.model.response.AssetMultiSendRequestsResponse
import dev3.blockchainapiservice.model.response.EventArgumentResponse
import dev3.blockchainapiservice.model.response.EventArgumentResponseType
import dev3.blockchainapiservice.model.response.EventInfoResponse
import dev3.blockchainapiservice.model.response.MultiSendItemResponse
import dev3.blockchainapiservice.model.response.TransactionResponse
import dev3.blockchainapiservice.model.result.AssetMultiSendRequest
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.repository.AssetMultiSendRequestRepository
import dev3.blockchainapiservice.testcontainers.HardhatTestContainer
import dev3.blockchainapiservice.util.AssetType
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.BaseUrl
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.Status
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.tx.gas.DefaultGasProvider
import java.math.BigInteger
import java.util.UUID

class AssetMultiSendRequestControllerApiTest : ControllerTestBase() {

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
        private val APPROVE_EVENTS = listOf(
            EventInfoResponse(
                signature = "Approval(address,address,uint256)",
                arguments = listOf(
                    EventArgumentResponse(
                        name = "owner",
                        type = EventArgumentResponseType.VALUE,
                        value = HardhatTestContainer.ACCOUNT_ADDRESS_1,
                        hash = null
                    ),
                    EventArgumentResponse(
                        name = "spender",
                        type = EventArgumentResponseType.VALUE,
                        value = "{disperseContractAddress}",
                        hash = null
                    ),
                    EventArgumentResponse(
                        name = "value",
                        type = EventArgumentResponseType.VALUE,
                        value = "10",
                        hash = null
                    )
                )
            )
        )
        private val DISPERSE_EVENTS = listOf(
            EventInfoResponse(
                signature = "Transfer(address,address,uint256)",
                arguments = listOf(
                    EventArgumentResponse(
                        name = "from",
                        type = EventArgumentResponseType.VALUE,
                        value = HardhatTestContainer.ACCOUNT_ADDRESS_1,
                        hash = null
                    ),
                    EventArgumentResponse(
                        name = "to",
                        type = EventArgumentResponseType.VALUE,
                        value = "{disperseContractAddress}",
                        hash = null
                    ),
                    EventArgumentResponse(
                        name = "value",
                        type = EventArgumentResponseType.VALUE,
                        value = "10",
                        hash = null
                    )
                )
            ),
            EventInfoResponse(
                signature = "Transfer(address,address,uint256)",
                arguments = listOf(
                    EventArgumentResponse(
                        name = "from",
                        type = EventArgumentResponseType.VALUE,
                        value = "{disperseContractAddress}",
                        hash = null
                    ),
                    EventArgumentResponse(
                        name = "to",
                        type = EventArgumentResponseType.VALUE,
                        value = HardhatTestContainer.ACCOUNT_ADDRESS_2,
                        hash = null
                    ),
                    EventArgumentResponse(
                        name = "value",
                        type = EventArgumentResponseType.VALUE,
                        value = "10",
                        hash = null
                    )
                )
            )
        )
    }

    private val accounts = HardhatTestContainer.ACCOUNTS

    @Autowired
    private lateinit var assetMultiSendRequestRepository: AssetMultiSendRequestRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        postgresContainer.cleanAllDatabaseTables(dslContext)

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
            ApiKeyRecord(
                id = UUID.randomUUID(),
                projectId = PROJECT_ID,
                apiKey = API_KEY,
                createdAt = TestData.TIMESTAMP
            )
        )
    }

    @Test
    fun mustCorrectlyCreateAssetMultiSendRequestForSomeToken() {
        val tokenAddress = ContractAddress("cafebabe")
        val disperseContractAddress = ContractAddress("abe")
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress("b")
        val recipientAddress = WalletAddress("c")

        val response = suppose("request to create asset multi-send request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/multi-send")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "disperse_contract_address": "${disperseContractAddress.rawValue}",
                                "asset_type": "TOKEN",
                                "items": [
                                    {
                                        "wallet_address": "${recipientAddress.rawValue}",
                                        "amount": "${amount.rawValue}",
                                        "item_name": "Example"
                                    }
                                ],
                                "sender_address": "${senderAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "approve_screen_config": {
                                    "before_action_message": "approve-before-action-message",
                                    "after_action_message": "approve-after-action-message"
                                },
                                "disperse_screen_config": {
                                    "before_action_message": "disperse-before-action-message",
                                    "after_action_message": "disperse-after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, AssetMultiSendRequestResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    AssetMultiSendRequestResponse(
                        id = response.id,
                        projectId = PROJECT_ID,
                        approveStatus = Status.PENDING,
                        disperseStatus = null,
                        chainId = PROJECT.chainId.value,
                        tokenAddress = tokenAddress.rawValue,
                        disperseContractAddress = disperseContractAddress.rawValue,
                        assetType = AssetType.TOKEN,
                        items = listOf(
                            MultiSendItemResponse(
                                walletAddress = recipientAddress.rawValue,
                                amount = amount.rawValue,
                                itemName = "Example"
                            )
                        ),
                        senderAddress = senderAddress.rawValue,
                        arbitraryData = response.arbitraryData,
                        approveScreenConfig = ScreenConfig(
                            beforeActionMessage = "approve-before-action-message",
                            afterActionMessage = "approve-after-action-message"
                        ),
                        disperseScreenConfig = ScreenConfig(
                            beforeActionMessage = "disperse-before-action-message",
                            afterActionMessage = "disperse-after-action-message"
                        ),
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-multi-send/${response.id}/action",
                        approveTx = TransactionResponse(
                            txHash = null,
                            from = senderAddress.rawValue,
                            to = tokenAddress.rawValue,
                            data = response.approveTx!!.data,
                            value = BigInteger.ZERO,
                            blockConfirmations = null,
                            timestamp = null
                        ),
                        disperseTx = null,
                        createdAt = response.createdAt,
                        approveEvents = null,
                        disperseEvents = null
                    )
                )

            assertThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("asset multi-send request is correctly stored in database") {
            val storedRequest = assetMultiSendRequestRepository.getById(response.id)

            assertThat(storedRequest).withMessage()
                .isEqualTo(
                    AssetMultiSendRequest(
                        id = response.id,
                        projectId = PROJECT_ID,
                        chainId = PROJECT.chainId,
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-multi-send/${response.id}/action",
                        tokenAddress = tokenAddress,
                        disperseContractAddress = disperseContractAddress,
                        assetAmounts = listOf(amount),
                        assetRecipientAddresses = listOf(recipientAddress),
                        itemNames = listOf("Example"),
                        assetSenderAddress = senderAddress,
                        approveTxHash = null,
                        disperseTxHash = null,
                        arbitraryData = response.arbitraryData,
                        approveScreenConfig = ScreenConfig(
                            beforeActionMessage = "approve-before-action-message",
                            afterActionMessage = "approve-after-action-message"
                        ),
                        disperseScreenConfig = ScreenConfig(
                            beforeActionMessage = "disperse-before-action-message",
                            afterActionMessage = "disperse-after-action-message"
                        ),
                        createdAt = storedRequest!!.createdAt
                    )
                )

            assertThat(storedRequest.createdAt.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyCreateAssetMultiSendRequestForSomeTokenWithRedirectUrl() {
        val tokenAddress = ContractAddress("cafebabe")
        val disperseContractAddress = ContractAddress("abe")
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress("b")
        val recipientAddress = WalletAddress("c")
        val redirectUrl = "https://custom-url/\${id}"

        val response = suppose("request to create asset multi-send request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/multi-send")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "redirect_url": "$redirectUrl",
                                "token_address": "${tokenAddress.rawValue}",
                                "disperse_contract_address": "${disperseContractAddress.rawValue}",
                                "asset_type": "TOKEN",
                                "items": [
                                    {
                                        "wallet_address": "${recipientAddress.rawValue}",
                                        "amount": "${amount.rawValue}",
                                        "item_name": "Example"
                                    }
                                ],
                                "sender_address": "${senderAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "approve_screen_config": {
                                    "before_action_message": "approve-before-action-message",
                                    "after_action_message": "approve-after-action-message"
                                },
                                "disperse_screen_config": {
                                    "before_action_message": "disperse-before-action-message",
                                    "after_action_message": "disperse-after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, AssetMultiSendRequestResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    AssetMultiSendRequestResponse(
                        id = response.id,
                        projectId = PROJECT_ID,
                        approveStatus = Status.PENDING,
                        disperseStatus = null,
                        chainId = PROJECT.chainId.value,
                        tokenAddress = tokenAddress.rawValue,
                        disperseContractAddress = disperseContractAddress.rawValue,
                        assetType = AssetType.TOKEN,
                        items = listOf(
                            MultiSendItemResponse(
                                walletAddress = recipientAddress.rawValue,
                                amount = amount.rawValue,
                                itemName = "Example"
                            )
                        ),
                        senderAddress = senderAddress.rawValue,
                        arbitraryData = response.arbitraryData,
                        approveScreenConfig = ScreenConfig(
                            beforeActionMessage = "approve-before-action-message",
                            afterActionMessage = "approve-after-action-message"
                        ),
                        disperseScreenConfig = ScreenConfig(
                            beforeActionMessage = "disperse-before-action-message",
                            afterActionMessage = "disperse-after-action-message"
                        ),
                        redirectUrl = "https://custom-url/${response.id}",
                        approveTx = TransactionResponse(
                            txHash = null,
                            from = senderAddress.rawValue,
                            to = tokenAddress.rawValue,
                            data = response.approveTx!!.data,
                            value = BigInteger.ZERO,
                            blockConfirmations = null,
                            timestamp = null
                        ),
                        disperseTx = null,
                        createdAt = response.createdAt,
                        approveEvents = null,
                        disperseEvents = null
                    )
                )

            assertThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("asset multi-send request is correctly stored in database") {
            val storedRequest = assetMultiSendRequestRepository.getById(response.id)

            assertThat(storedRequest).withMessage()
                .isEqualTo(
                    AssetMultiSendRequest(
                        id = response.id,
                        projectId = PROJECT_ID,
                        chainId = PROJECT.chainId,
                        redirectUrl = "https://custom-url/${response.id}",
                        tokenAddress = tokenAddress,
                        disperseContractAddress = disperseContractAddress,
                        assetAmounts = listOf(amount),
                        assetRecipientAddresses = listOf(recipientAddress),
                        itemNames = listOf("Example"),
                        assetSenderAddress = senderAddress,
                        approveTxHash = null,
                        disperseTxHash = null,
                        arbitraryData = response.arbitraryData,
                        approveScreenConfig = ScreenConfig(
                            beforeActionMessage = "approve-before-action-message",
                            afterActionMessage = "approve-after-action-message"
                        ),
                        disperseScreenConfig = ScreenConfig(
                            beforeActionMessage = "disperse-before-action-message",
                            afterActionMessage = "disperse-after-action-message"
                        ),
                        createdAt = storedRequest!!.createdAt
                    )
                )

            assertThat(storedRequest.createdAt.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyCreateAssetMultiSendRequestForNativeAsset() {
        val disperseContractAddress = ContractAddress("abe")
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress("b")
        val recipientAddress = WalletAddress("c")

        val response = suppose("request to create asset multi-send request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/multi-send")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "disperse_contract_address": "${disperseContractAddress.rawValue}",
                                "asset_type": "NATIVE",
                                "items": [
                                    {
                                        "wallet_address": "${recipientAddress.rawValue}",
                                        "amount": "${amount.rawValue}",
                                        "item_name": "Example"
                                    }
                                ],
                                "sender_address": "${senderAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "approve_screen_config": {
                                    "before_action_message": "approve-before-action-message",
                                    "after_action_message": "approve-after-action-message"
                                },
                                "disperse_screen_config": {
                                    "before_action_message": "disperse-before-action-message",
                                    "after_action_message": "disperse-after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, AssetMultiSendRequestResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    AssetMultiSendRequestResponse(
                        id = response.id,
                        projectId = PROJECT_ID,
                        approveStatus = null,
                        disperseStatus = Status.PENDING,
                        chainId = PROJECT.chainId.value,
                        tokenAddress = null,
                        disperseContractAddress = disperseContractAddress.rawValue,
                        assetType = AssetType.NATIVE,
                        items = listOf(
                            MultiSendItemResponse(
                                walletAddress = recipientAddress.rawValue,
                                amount = amount.rawValue,
                                itemName = "Example"
                            )
                        ),
                        senderAddress = senderAddress.rawValue,
                        arbitraryData = response.arbitraryData,
                        approveScreenConfig = ScreenConfig(
                            beforeActionMessage = "approve-before-action-message",
                            afterActionMessage = "approve-after-action-message"
                        ),
                        disperseScreenConfig = ScreenConfig(
                            beforeActionMessage = "disperse-before-action-message",
                            afterActionMessage = "disperse-after-action-message"
                        ),
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-multi-send/${response.id}/action",
                        approveTx = null,
                        disperseTx = TransactionResponse(
                            txHash = null,
                            from = senderAddress.rawValue,
                            to = disperseContractAddress.rawValue,
                            data = response.disperseTx!!.data,
                            value = amount.rawValue,
                            blockConfirmations = null,
                            timestamp = null
                        ),
                        createdAt = response.createdAt,
                        approveEvents = null,
                        disperseEvents = null
                    )
                )

            assertThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("asset multi-send request is correctly stored in database") {
            val storedRequest = assetMultiSendRequestRepository.getById(response.id)

            assertThat(storedRequest).withMessage()
                .isEqualTo(
                    AssetMultiSendRequest(
                        id = response.id,
                        projectId = PROJECT_ID,
                        chainId = PROJECT.chainId,
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-multi-send/${response.id}/action",
                        tokenAddress = null,
                        disperseContractAddress = disperseContractAddress,
                        assetAmounts = listOf(amount),
                        assetRecipientAddresses = listOf(recipientAddress),
                        itemNames = listOf("Example"),
                        assetSenderAddress = senderAddress,
                        approveTxHash = null,
                        disperseTxHash = null,
                        arbitraryData = response.arbitraryData,
                        approveScreenConfig = ScreenConfig(
                            beforeActionMessage = "approve-before-action-message",
                            afterActionMessage = "approve-after-action-message"
                        ),
                        disperseScreenConfig = ScreenConfig(
                            beforeActionMessage = "disperse-before-action-message",
                            afterActionMessage = "disperse-after-action-message"
                        ),
                        createdAt = storedRequest!!.createdAt
                    )
                )

            assertThat(storedRequest.createdAt.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyCreateAssetMultiSendRequestForNativeAssetWithRedirectUrl() {
        val disperseContractAddress = ContractAddress("abe")
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress("b")
        val recipientAddress = WalletAddress("c")
        val redirectUrl = "https://custom-url/\${id}"

        val response = suppose("request to create asset multi-send request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/multi-send")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "redirect_url": "$redirectUrl",
                                "disperse_contract_address": "${disperseContractAddress.rawValue}",
                                "asset_type": "NATIVE",
                                "items": [
                                    {
                                        "wallet_address": "${recipientAddress.rawValue}",
                                        "amount": "${amount.rawValue}",
                                        "item_name": "Example"
                                    }
                                ],
                                "sender_address": "${senderAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "approve_screen_config": {
                                    "before_action_message": "approve-before-action-message",
                                    "after_action_message": "approve-after-action-message"
                                },
                                "disperse_screen_config": {
                                    "before_action_message": "disperse-before-action-message",
                                    "after_action_message": "disperse-after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, AssetMultiSendRequestResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    AssetMultiSendRequestResponse(
                        id = response.id,
                        projectId = PROJECT_ID,
                        approveStatus = null,
                        disperseStatus = Status.PENDING,
                        chainId = PROJECT.chainId.value,
                        tokenAddress = null,
                        disperseContractAddress = disperseContractAddress.rawValue,
                        assetType = AssetType.NATIVE,
                        items = listOf(
                            MultiSendItemResponse(
                                walletAddress = recipientAddress.rawValue,
                                amount = amount.rawValue,
                                itemName = "Example"
                            )
                        ),
                        senderAddress = senderAddress.rawValue,
                        arbitraryData = response.arbitraryData,
                        approveScreenConfig = ScreenConfig(
                            beforeActionMessage = "approve-before-action-message",
                            afterActionMessage = "approve-after-action-message"
                        ),
                        disperseScreenConfig = ScreenConfig(
                            beforeActionMessage = "disperse-before-action-message",
                            afterActionMessage = "disperse-after-action-message"
                        ),
                        redirectUrl = "https://custom-url/${response.id}",
                        approveTx = null,
                        disperseTx = TransactionResponse(
                            txHash = null,
                            from = senderAddress.rawValue,
                            to = disperseContractAddress.rawValue,
                            data = response.disperseTx!!.data,
                            value = amount.rawValue,
                            blockConfirmations = null,
                            timestamp = null
                        ),
                        createdAt = response.createdAt,
                        approveEvents = null,
                        disperseEvents = null
                    )
                )

            assertThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("asset multi-send request is correctly stored in database") {
            val storedRequest = assetMultiSendRequestRepository.getById(response.id)

            assertThat(storedRequest).withMessage()
                .isEqualTo(
                    AssetMultiSendRequest(
                        id = response.id,
                        projectId = PROJECT_ID,
                        chainId = PROJECT.chainId,
                        redirectUrl = "https://custom-url/${response.id}",
                        tokenAddress = null,
                        disperseContractAddress = disperseContractAddress,
                        assetAmounts = listOf(amount),
                        assetRecipientAddresses = listOf(recipientAddress),
                        itemNames = listOf("Example"),
                        assetSenderAddress = senderAddress,
                        approveTxHash = null,
                        disperseTxHash = null,
                        arbitraryData = response.arbitraryData,
                        approveScreenConfig = ScreenConfig(
                            beforeActionMessage = "approve-before-action-message",
                            afterActionMessage = "approve-after-action-message"
                        ),
                        disperseScreenConfig = ScreenConfig(
                            beforeActionMessage = "disperse-before-action-message",
                            afterActionMessage = "disperse-after-action-message"
                        ),
                        createdAt = storedRequest!!.createdAt
                    )
                )

            assertThat(storedRequest.createdAt.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustReturn400BadRequestWhenTokenAddressIsMissingForTokenAssetType() {
        val disperseContractAddress = ContractAddress("abe")
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress("b")
        val recipientAddress = WalletAddress("c")

        verify("400 is returned for missing token address") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/multi-send")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "disperse_contract_address": "${disperseContractAddress.rawValue}",
                                "asset_type": "TOKEN",
                                "items": [
                                    {
                                        "wallet_address": "${recipientAddress.rawValue}",
                                        "amount": "${amount.rawValue}",
                                        "item_name": "Example"
                                    }
                                ],
                                "sender_address": "${senderAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "approve_screen_config": {
                                    "before_action_message": "approve-before-action-message",
                                    "after_action_message": "approve-after-action-message"
                                },
                                "disperse_screen_config": {
                                    "before_action_message": "disperse-before-action-message",
                                    "after_action_message": "disperse-after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.MISSING_TOKEN_ADDRESS)
        }
    }

    @Test
    fun mustReturn400BadRequestWhenTokenAddressIsSpecifiedForNativeAssetType() {
        val disperseContractAddress = ContractAddress("abe")
        val tokenAddress = ContractAddress("cafebabe")
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress("b")
        val recipientAddress = WalletAddress("c")

        verify("400 is returned for non-allowed token address") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/multi-send")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "disperse_contract_address": "${disperseContractAddress.rawValue}",
                                "asset_type": "NATIVE",
                                "items": [
                                    {
                                        "wallet_address": "${recipientAddress.rawValue}",
                                        "amount": "${amount.rawValue}",
                                        "item_name": "Example"
                                    }
                                ],
                                "sender_address": "${senderAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "approve_screen_config": {
                                    "before_action_message": "approve-before-action-message",
                                    "after_action_message": "approve-after-action-message"
                                },
                                "disperse_screen_config": {
                                    "before_action_message": "disperse-before-action-message",
                                    "after_action_message": "disperse-after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.TOKEN_ADDRESS_NOT_ALLOWED)
        }
    }

    @Test
    fun mustReturn401UnauthorizedWhenCreatingAssetMultiSendRequestWithInvalidApiKey() {
        val tokenAddress = ContractAddress("cafebabe")
        val disperseContractAddress = ContractAddress("abe")
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress("b")
        val recipientAddress = WalletAddress("c")

        verify("401 is returned for invalid API key") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/multi-send")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, "invalid-api-key")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "disperse_contract_address": "${disperseContractAddress.rawValue}",
                                "asset_type": "TOKEN",
                                "items": [
                                    {
                                        "wallet_address": "${recipientAddress.rawValue}",
                                        "amount": "${amount.rawValue}",
                                        "item_name": "Example"
                                    }
                                ],
                                "sender_address": "${senderAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "approve_screen_config": {
                                    "before_action_message": "approve-before-action-message",
                                    "after_action_message": "approve-after-action-message"
                                },
                                "disperse_screen_config": {
                                    "before_action_message": "disperse-before-action-message",
                                    "after_action_message": "disperse-after-action-message"
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
    fun mustCorrectlyFetchAssetMultiSendRequestForSomeToken() {
        val mainAccount = accounts[0]

        val tokenContract = suppose("simple asset contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(mainAccount.address),
                listOf(BigInteger("10000")),
                mainAccount.address
            ).send()
        }

        val disperseContract = suppose("simple disperse contract is deployed") {
            SimpleDisperse.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider()
            ).send()
        }

        val tokenAddress = ContractAddress(tokenContract.contractAddress)
        val disperseContractAddress = ContractAddress(disperseContract.contractAddress)
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress(mainAccount.address)
        val recipientAddress = WalletAddress(accounts[1].address)

        val createResponse = suppose("request to create asset multi-send request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/multi-send")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "disperse_contract_address": "${disperseContractAddress.rawValue}",
                                "asset_type": "TOKEN",
                                "items": [
                                    {
                                        "wallet_address": "${recipientAddress.rawValue}",
                                        "amount": "${amount.rawValue}",
                                        "item_name": "Example"
                                    }
                                ],
                                "sender_address": "${senderAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "approve_screen_config": {
                                    "before_action_message": "approve-before-action-message",
                                    "after_action_message": "approve-after-action-message"
                                },
                                "disperse_screen_config": {
                                    "before_action_message": "disperse-before-action-message",
                                    "after_action_message": "disperse-after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(createResponse.response.contentAsString, AssetMultiSendRequestResponse::class.java)
        }

        val approveTxHash = suppose("some asset approve transaction is made") {
            tokenContract.approve(disperseContractAddress.rawValue, amount.rawValue).send()
                ?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("approve transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("approve transaction info is attached to asset multi-send request") {
            assetMultiSendRequestRepository.setApproveTxInfo(createResponse.id, approveTxHash, senderAddress)
        }

        val disperseTxHash = suppose("some disperse transaction is made") {
            disperseContract.disperseToken(
                tokenAddress.rawValue,
                listOf(recipientAddress.rawValue),
                listOf(amount.rawValue)
            ).send()
                ?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("disperse transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("disperse transaction info is attached to asset multi-send request") {
            assetMultiSendRequestRepository.setDisperseTxInfo(createResponse.id, disperseTxHash, senderAddress)
        }

        val fetchResponse = suppose("request to fetch asset multi-send request is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/multi-send/${createResponse.id}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, AssetMultiSendRequestResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(fetchResponse).withMessage()
                .isEqualTo(
                    AssetMultiSendRequestResponse(
                        id = fetchResponse.id,
                        projectId = PROJECT_ID,
                        approveStatus = Status.SUCCESS,
                        disperseStatus = Status.SUCCESS,
                        chainId = PROJECT.chainId.value,
                        tokenAddress = tokenAddress.rawValue,
                        disperseContractAddress = disperseContractAddress.rawValue,
                        assetType = AssetType.TOKEN,
                        items = listOf(
                            MultiSendItemResponse(
                                walletAddress = recipientAddress.rawValue,
                                amount = amount.rawValue,
                                itemName = "Example"
                            )
                        ),
                        senderAddress = senderAddress.rawValue,
                        arbitraryData = fetchResponse.arbitraryData,
                        approveScreenConfig = ScreenConfig(
                            beforeActionMessage = "approve-before-action-message",
                            afterActionMessage = "approve-after-action-message"
                        ),
                        disperseScreenConfig = ScreenConfig(
                            beforeActionMessage = "disperse-before-action-message",
                            afterActionMessage = "disperse-after-action-message"
                        ),
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-multi-send/${fetchResponse.id}/action",
                        approveTx = TransactionResponse(
                            txHash = approveTxHash.value,
                            from = senderAddress.rawValue,
                            to = tokenAddress.rawValue,
                            data = fetchResponse.approveTx!!.data,
                            value = BigInteger.ZERO,
                            blockConfirmations = fetchResponse.approveTx!!.blockConfirmations,
                            timestamp = fetchResponse.approveTx!!.timestamp
                        ),
                        disperseTx = TransactionResponse(
                            txHash = disperseTxHash.value,
                            from = senderAddress.rawValue,
                            to = disperseContractAddress.rawValue,
                            data = fetchResponse.disperseTx!!.data,
                            value = BigInteger.ZERO,
                            blockConfirmations = fetchResponse.disperseTx!!.blockConfirmations,
                            timestamp = fetchResponse.disperseTx!!.timestamp
                        ),
                        createdAt = fetchResponse.createdAt,
                        approveEvents = APPROVE_EVENTS.withDisperseContractAddress(disperseContractAddress),
                        disperseEvents = DISPERSE_EVENTS.withDisperseContractAddress(disperseContractAddress)
                    )
                )

            assertThat(fetchResponse.approveTx!!.blockConfirmations)
                .isNotZero()
            assertThat(fetchResponse.disperseTx!!.blockConfirmations)
                .isNotZero()
            assertThat(fetchResponse.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            assertThat(fetchResponse.approveTx!!.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            assertThat(fetchResponse.disperseTx!!.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("recipient has some balance of distributed token") {
            assertThat(tokenContract.balanceOf(recipientAddress.rawValue).send()).withMessage()
                .isEqualTo(amount.rawValue)
        }
    }

    @Test
    fun mustCorrectlyFetchAssetMultiSendRequestForSomeTokenWhenCustomRpcUrlIsSpecified() {
        val mainAccount = accounts[0]

        val tokenContract = suppose("simple asset contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(mainAccount.address),
                listOf(BigInteger("10000")),
                mainAccount.address
            ).send()
        }

        val disperseContract = suppose("simple disperse contract is deployed") {
            SimpleDisperse.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider()
            ).send()
        }

        val tokenAddress = ContractAddress(tokenContract.contractAddress)
        val disperseContractAddress = ContractAddress(disperseContract.contractAddress)
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress(mainAccount.address)
        val recipientAddress = WalletAddress(accounts[1].address)

        val (projectId, chainId, apiKey) = suppose("project with customRpcUrl is inserted into database") {
            insertProjectWithCustomRpcUrl()
        }

        val createResponse = suppose("request to create asset multi-send request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/multi-send")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "disperse_contract_address": "${disperseContractAddress.rawValue}",
                                "asset_type": "TOKEN",
                                "items": [
                                    {
                                        "wallet_address": "${recipientAddress.rawValue}",
                                        "amount": "${amount.rawValue}",
                                        "item_name": "Example"
                                    }
                                ],
                                "sender_address": "${senderAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "approve_screen_config": {
                                    "before_action_message": "approve-before-action-message",
                                    "after_action_message": "approve-after-action-message"
                                },
                                "disperse_screen_config": {
                                    "before_action_message": "disperse-before-action-message",
                                    "after_action_message": "disperse-after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(createResponse.response.contentAsString, AssetMultiSendRequestResponse::class.java)
        }

        val approveTxHash = suppose("some asset approve transaction is made") {
            tokenContract.approve(disperseContractAddress.rawValue, amount.rawValue).send()
                ?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("approve transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("approve transaction info is attached to asset multi-send request") {
            assetMultiSendRequestRepository.setApproveTxInfo(createResponse.id, approveTxHash, senderAddress)
        }

        val disperseTxHash = suppose("some disperse transaction is made") {
            disperseContract.disperseToken(
                tokenAddress.rawValue,
                listOf(recipientAddress.rawValue),
                listOf(amount.rawValue)
            ).send()
                ?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("disperse transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("disperse transaction info is attached to asset multi-send request") {
            assetMultiSendRequestRepository.setDisperseTxInfo(createResponse.id, disperseTxHash, senderAddress)
        }

        val fetchResponse = suppose("request to fetch asset multi-send request is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/multi-send/${createResponse.id}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, AssetMultiSendRequestResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(fetchResponse).withMessage()
                .isEqualTo(
                    AssetMultiSendRequestResponse(
                        id = fetchResponse.id,
                        projectId = projectId,
                        approveStatus = Status.SUCCESS,
                        disperseStatus = Status.SUCCESS,
                        chainId = chainId.value,
                        tokenAddress = tokenAddress.rawValue,
                        disperseContractAddress = disperseContractAddress.rawValue,
                        assetType = AssetType.TOKEN,
                        items = listOf(
                            MultiSendItemResponse(
                                walletAddress = recipientAddress.rawValue,
                                amount = amount.rawValue,
                                itemName = "Example"
                            )
                        ),
                        senderAddress = senderAddress.rawValue,
                        arbitraryData = fetchResponse.arbitraryData,
                        approveScreenConfig = ScreenConfig(
                            beforeActionMessage = "approve-before-action-message",
                            afterActionMessage = "approve-after-action-message"
                        ),
                        disperseScreenConfig = ScreenConfig(
                            beforeActionMessage = "disperse-before-action-message",
                            afterActionMessage = "disperse-after-action-message"
                        ),
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-multi-send/${fetchResponse.id}/action",
                        approveTx = TransactionResponse(
                            txHash = approveTxHash.value,
                            from = senderAddress.rawValue,
                            to = tokenAddress.rawValue,
                            data = fetchResponse.approveTx!!.data,
                            value = BigInteger.ZERO,
                            blockConfirmations = fetchResponse.approveTx!!.blockConfirmations,
                            timestamp = fetchResponse.approveTx!!.timestamp
                        ),
                        disperseTx = TransactionResponse(
                            txHash = disperseTxHash.value,
                            from = senderAddress.rawValue,
                            to = disperseContractAddress.rawValue,
                            data = fetchResponse.disperseTx!!.data,
                            value = BigInteger.ZERO,
                            blockConfirmations = fetchResponse.disperseTx!!.blockConfirmations,
                            timestamp = fetchResponse.disperseTx!!.timestamp
                        ),
                        createdAt = fetchResponse.createdAt,
                        approveEvents = APPROVE_EVENTS.withDisperseContractAddress(disperseContractAddress),
                        disperseEvents = DISPERSE_EVENTS.withDisperseContractAddress(disperseContractAddress)
                    )
                )

            assertThat(fetchResponse.approveTx!!.blockConfirmations)
                .isNotZero()
            assertThat(fetchResponse.disperseTx!!.blockConfirmations)
                .isNotZero()
            assertThat(fetchResponse.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            assertThat(fetchResponse.approveTx!!.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            assertThat(fetchResponse.disperseTx!!.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("recipient has some balance of distributed token") {
            assertThat(tokenContract.balanceOf(recipientAddress.rawValue).send()).withMessage()
                .isEqualTo(amount.rawValue)
        }
    }

    @Test
    fun mustCorrectlyFetchAssetMultiSendRequestForNativeAsset() {
        val mainAccount = accounts[0]

        val disperseContract = suppose("simple disperse contract is deployed") {
            SimpleDisperse.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider()
            ).send()
        }

        val disperseContractAddress = ContractAddress(disperseContract.contractAddress)
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress(mainAccount.address)
        val recipientAddress = WalletAddress(accounts[1].address)

        val initialEthBalance = hardhatContainer.web3j.ethGetBalance(
            recipientAddress.rawValue,
            DefaultBlockParameterName.LATEST
        ).send().balance

        val createResponse = suppose("request to create asset multi-send request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/multi-send")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "disperse_contract_address": "${disperseContractAddress.rawValue}",
                                "asset_type": "NATIVE",
                                "items": [
                                    {
                                        "wallet_address": "${recipientAddress.rawValue}",
                                        "amount": "${amount.rawValue}",
                                        "item_name": "Example"
                                    }
                                ],
                                "sender_address": "${senderAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "approve_screen_config": {
                                    "before_action_message": "approve-before-action-message",
                                    "after_action_message": "approve-after-action-message"
                                },
                                "disperse_screen_config": {
                                    "before_action_message": "disperse-before-action-message",
                                    "after_action_message": "disperse-after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(createResponse.response.contentAsString, AssetMultiSendRequestResponse::class.java)
        }

        val disperseTxHash = suppose("some disperse transaction is made") {
            disperseContract.disperseEther(
                listOf(recipientAddress.rawValue),
                listOf(amount.rawValue),
                amount.rawValue
            ).send()
                ?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("disperse transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("disperse transaction info is attached to asset multi-send request") {
            assetMultiSendRequestRepository.setDisperseTxInfo(createResponse.id, disperseTxHash, senderAddress)
        }

        val fetchResponse = suppose("request to fetch asset multi-send request is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/multi-send/${createResponse.id}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, AssetMultiSendRequestResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(fetchResponse).withMessage()
                .isEqualTo(
                    AssetMultiSendRequestResponse(
                        id = fetchResponse.id,
                        projectId = PROJECT_ID,
                        approveStatus = null,
                        disperseStatus = Status.SUCCESS,
                        chainId = PROJECT.chainId.value,
                        tokenAddress = null,
                        disperseContractAddress = disperseContractAddress.rawValue,
                        assetType = AssetType.NATIVE,
                        items = listOf(
                            MultiSendItemResponse(
                                walletAddress = recipientAddress.rawValue,
                                amount = amount.rawValue,
                                itemName = "Example"
                            )
                        ),
                        senderAddress = senderAddress.rawValue,
                        arbitraryData = fetchResponse.arbitraryData,
                        approveScreenConfig = ScreenConfig(
                            beforeActionMessage = "approve-before-action-message",
                            afterActionMessage = "approve-after-action-message"
                        ),
                        disperseScreenConfig = ScreenConfig(
                            beforeActionMessage = "disperse-before-action-message",
                            afterActionMessage = "disperse-after-action-message"
                        ),
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-multi-send/${fetchResponse.id}/action",
                        approveTx = null,
                        disperseTx = TransactionResponse(
                            txHash = disperseTxHash.value,
                            from = senderAddress.rawValue,
                            to = disperseContractAddress.rawValue,
                            data = fetchResponse.disperseTx!!.data,
                            value = amount.rawValue,
                            blockConfirmations = fetchResponse.disperseTx!!.blockConfirmations,
                            timestamp = fetchResponse.disperseTx!!.timestamp
                        ),
                        createdAt = fetchResponse.createdAt,
                        approveEvents = null,
                        disperseEvents = emptyList()
                    )
                )

            assertThat(fetchResponse.disperseTx!!.blockConfirmations)
                .isNotZero()
            assertThat(fetchResponse.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            assertThat(fetchResponse.disperseTx!!.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("recipient has some balance of distributed native asset") {
            val balance = hardhatContainer.web3j.ethGetBalance(
                recipientAddress.rawValue,
                DefaultBlockParameterName.LATEST
            ).send().balance

            assertThat(balance).withMessage()
                .isEqualTo(initialEthBalance + amount.rawValue)
        }
    }

    @Test
    fun mustCorrectlyFetchAssetMultiSendRequestForNativeAssetWhenCustomRpcUrlIsSpecified() {
        val mainAccount = accounts[0]

        val disperseContract = suppose("simple disperse contract is deployed") {
            SimpleDisperse.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider()
            ).send()
        }

        val disperseContractAddress = ContractAddress(disperseContract.contractAddress)
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress(mainAccount.address)
        val recipientAddress = WalletAddress(accounts[1].address)

        val initialEthBalance = hardhatContainer.web3j.ethGetBalance(
            recipientAddress.rawValue,
            DefaultBlockParameterName.LATEST
        ).send().balance

        val (projectId, chainId, apiKey) = suppose("project with customRpcUrl is inserted into database") {
            insertProjectWithCustomRpcUrl()
        }

        val createResponse = suppose("request to create asset multi-send request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/multi-send")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "disperse_contract_address": "${disperseContractAddress.rawValue}",
                                "asset_type": "NATIVE",
                                "items": [
                                    {
                                        "wallet_address": "${recipientAddress.rawValue}",
                                        "amount": "${amount.rawValue}",
                                        "item_name": "Example"
                                    }
                                ],
                                "sender_address": "${senderAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "approve_screen_config": {
                                    "before_action_message": "approve-before-action-message",
                                    "after_action_message": "approve-after-action-message"
                                },
                                "disperse_screen_config": {
                                    "before_action_message": "disperse-before-action-message",
                                    "after_action_message": "disperse-after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(createResponse.response.contentAsString, AssetMultiSendRequestResponse::class.java)
        }

        val disperseTxHash = suppose("some disperse transaction is made") {
            disperseContract.disperseEther(
                listOf(recipientAddress.rawValue),
                listOf(amount.rawValue),
                amount.rawValue
            ).send()
                ?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("disperse transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("disperse transaction info is attached to asset multi-send request") {
            assetMultiSendRequestRepository.setDisperseTxInfo(createResponse.id, disperseTxHash, senderAddress)
        }

        val fetchResponse = suppose("request to fetch asset multi-send request is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/multi-send/${createResponse.id}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, AssetMultiSendRequestResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(fetchResponse).withMessage()
                .isEqualTo(
                    AssetMultiSendRequestResponse(
                        id = fetchResponse.id,
                        projectId = projectId,
                        approveStatus = null,
                        disperseStatus = Status.SUCCESS,
                        chainId = chainId.value,
                        tokenAddress = null,
                        disperseContractAddress = disperseContractAddress.rawValue,
                        assetType = AssetType.NATIVE,
                        items = listOf(
                            MultiSendItemResponse(
                                walletAddress = recipientAddress.rawValue,
                                amount = amount.rawValue,
                                itemName = "Example"
                            )
                        ),
                        senderAddress = senderAddress.rawValue,
                        arbitraryData = fetchResponse.arbitraryData,
                        approveScreenConfig = ScreenConfig(
                            beforeActionMessage = "approve-before-action-message",
                            afterActionMessage = "approve-after-action-message"
                        ),
                        disperseScreenConfig = ScreenConfig(
                            beforeActionMessage = "disperse-before-action-message",
                            afterActionMessage = "disperse-after-action-message"
                        ),
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-multi-send/${fetchResponse.id}/action",
                        approveTx = null,
                        disperseTx = TransactionResponse(
                            txHash = disperseTxHash.value,
                            from = senderAddress.rawValue,
                            to = disperseContractAddress.rawValue,
                            data = fetchResponse.disperseTx!!.data,
                            value = amount.rawValue,
                            blockConfirmations = fetchResponse.disperseTx!!.blockConfirmations,
                            timestamp = fetchResponse.disperseTx!!.timestamp
                        ),
                        createdAt = fetchResponse.createdAt,
                        approveEvents = null,
                        disperseEvents = emptyList()
                    )
                )

            assertThat(fetchResponse.disperseTx!!.blockConfirmations)
                .isNotZero()
            assertThat(fetchResponse.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            assertThat(fetchResponse.disperseTx!!.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("recipient has some balance of distributed native asset") {
            val balance = hardhatContainer.web3j.ethGetBalance(
                recipientAddress.rawValue,
                DefaultBlockParameterName.LATEST
            ).send().balance

            assertThat(balance).withMessage()
                .isEqualTo(initialEthBalance + amount.rawValue)
        }
    }

    @Test
    fun mustReturn404NotFoundForNonExistentAssetMultiSendRequest() {
        verify("404 is returned for non-existent asset multi-send request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/multi-send/${UUID.randomUUID()}")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    fun mustCorrectlyFetchAssetMultiSendRequestsByProjectId() {
        val mainAccount = accounts[0]

        val tokenContract = suppose("simple asset contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(mainAccount.address),
                listOf(BigInteger("10000")),
                mainAccount.address
            ).send()
        }

        val disperseContract = suppose("simple disperse contract is deployed") {
            SimpleDisperse.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider()
            ).send()
        }

        val tokenAddress = ContractAddress(tokenContract.contractAddress)
        val disperseContractAddress = ContractAddress(disperseContract.contractAddress)
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress(mainAccount.address)
        val recipientAddress = WalletAddress(accounts[1].address)

        val createResponse = suppose("request to create asset multi-send request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/multi-send")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "disperse_contract_address": "${disperseContractAddress.rawValue}",
                                "asset_type": "TOKEN",
                                "items": [
                                    {
                                        "wallet_address": "${recipientAddress.rawValue}",
                                        "amount": "${amount.rawValue}",
                                        "item_name": "Example"
                                    }
                                ],
                                "sender_address": "${senderAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "approve_screen_config": {
                                    "before_action_message": "approve-before-action-message",
                                    "after_action_message": "approve-after-action-message"
                                },
                                "disperse_screen_config": {
                                    "before_action_message": "disperse-before-action-message",
                                    "after_action_message": "disperse-after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(createResponse.response.contentAsString, AssetMultiSendRequestResponse::class.java)
        }

        val approveTxHash = suppose("some asset approve transaction is made") {
            tokenContract.approve(disperseContractAddress.rawValue, amount.rawValue).send()
                ?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("approve transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("approve transaction info is attached to asset multi-send request") {
            assetMultiSendRequestRepository.setApproveTxInfo(createResponse.id, approveTxHash, senderAddress)
        }

        val disperseTxHash = suppose("some disperse transaction is made") {
            disperseContract.disperseToken(
                tokenAddress.rawValue,
                listOf(recipientAddress.rawValue),
                listOf(amount.rawValue)
            ).send()
                ?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("disperse transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("disperse transaction info is attached to asset multi-send request") {
            assetMultiSendRequestRepository.setDisperseTxInfo(createResponse.id, disperseTxHash, senderAddress)
        }

        val fetchResponse = suppose("request to fetch asset multi-send requests is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/multi-send/by-project/${createResponse.projectId}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, AssetMultiSendRequestsResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(fetchResponse).withMessage()
                .isEqualTo(
                    AssetMultiSendRequestsResponse(
                        listOf(
                            AssetMultiSendRequestResponse(
                                id = fetchResponse.requests[0].id,
                                projectId = PROJECT_ID,
                                approveStatus = Status.SUCCESS,
                                disperseStatus = Status.SUCCESS,
                                chainId = PROJECT.chainId.value,
                                tokenAddress = tokenAddress.rawValue,
                                disperseContractAddress = disperseContractAddress.rawValue,
                                assetType = AssetType.TOKEN,
                                items = listOf(
                                    MultiSendItemResponse(
                                        walletAddress = recipientAddress.rawValue,
                                        amount = amount.rawValue,
                                        itemName = "Example"
                                    )
                                ),
                                senderAddress = senderAddress.rawValue,
                                arbitraryData = fetchResponse.requests[0].arbitraryData,
                                approveScreenConfig = ScreenConfig(
                                    beforeActionMessage = "approve-before-action-message",
                                    afterActionMessage = "approve-after-action-message"
                                ),
                                disperseScreenConfig = ScreenConfig(
                                    beforeActionMessage = "disperse-before-action-message",
                                    afterActionMessage = "disperse-after-action-message"
                                ),
                                redirectUrl = PROJECT.baseRedirectUrl.value +
                                    "/request-multi-send/${fetchResponse.requests[0].id}/action",
                                approveTx = TransactionResponse(
                                    txHash = approveTxHash.value,
                                    from = senderAddress.rawValue,
                                    to = tokenAddress.rawValue,
                                    data = fetchResponse.requests[0].approveTx!!.data,
                                    value = BigInteger.ZERO,
                                    blockConfirmations = fetchResponse.requests[0].approveTx!!.blockConfirmations,
                                    timestamp = fetchResponse.requests[0].approveTx!!.timestamp
                                ),
                                disperseTx = TransactionResponse(
                                    txHash = disperseTxHash.value,
                                    from = senderAddress.rawValue,
                                    to = disperseContractAddress.rawValue,
                                    data = fetchResponse.requests[0].disperseTx!!.data,
                                    value = BigInteger.ZERO,
                                    blockConfirmations = fetchResponse.requests[0].disperseTx!!.blockConfirmations,
                                    timestamp = fetchResponse.requests[0].disperseTx!!.timestamp
                                ),
                                createdAt = fetchResponse.requests[0].createdAt,
                                approveEvents = APPROVE_EVENTS.withDisperseContractAddress(disperseContractAddress),
                                disperseEvents = DISPERSE_EVENTS.withDisperseContractAddress(disperseContractAddress)
                            )
                        )
                    )
                )

            assertThat(fetchResponse.requests[0].approveTx!!.blockConfirmations)
                .isNotZero()
            assertThat(fetchResponse.requests[0].disperseTx!!.blockConfirmations)
                .isNotZero()
            assertThat(fetchResponse.requests[0].createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            assertThat(fetchResponse.requests[0].approveTx!!.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            assertThat(fetchResponse.requests[0].disperseTx!!.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchAssetMultiSendRequestsByProjectIdWhenCustomRpcUrlIsSpecified() {
        val mainAccount = accounts[0]

        val tokenContract = suppose("simple asset contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(mainAccount.address),
                listOf(BigInteger("10000")),
                mainAccount.address
            ).send()
        }

        val disperseContract = suppose("simple disperse contract is deployed") {
            SimpleDisperse.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider()
            ).send()
        }

        val tokenAddress = ContractAddress(tokenContract.contractAddress)
        val disperseContractAddress = ContractAddress(disperseContract.contractAddress)
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress(mainAccount.address)
        val recipientAddress = WalletAddress(accounts[1].address)

        val (projectId, chainId, apiKey) = suppose("project with customRpcUrl is inserted into database") {
            insertProjectWithCustomRpcUrl()
        }

        val createResponse = suppose("request to create asset multi-send request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/multi-send")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "disperse_contract_address": "${disperseContractAddress.rawValue}",
                                "asset_type": "TOKEN",
                                "items": [
                                    {
                                        "wallet_address": "${recipientAddress.rawValue}",
                                        "amount": "${amount.rawValue}",
                                        "item_name": "Example"
                                    }
                                ],
                                "sender_address": "${senderAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "approve_screen_config": {
                                    "before_action_message": "approve-before-action-message",
                                    "after_action_message": "approve-after-action-message"
                                },
                                "disperse_screen_config": {
                                    "before_action_message": "disperse-before-action-message",
                                    "after_action_message": "disperse-after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(createResponse.response.contentAsString, AssetMultiSendRequestResponse::class.java)
        }

        val approveTxHash = suppose("some asset approve transaction is made") {
            tokenContract.approve(disperseContractAddress.rawValue, amount.rawValue).send()
                ?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("approve transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("approve transaction info is attached to asset multi-send request") {
            assetMultiSendRequestRepository.setApproveTxInfo(createResponse.id, approveTxHash, senderAddress)
        }

        val disperseTxHash = suppose("some disperse transaction is made") {
            disperseContract.disperseToken(
                tokenAddress.rawValue,
                listOf(recipientAddress.rawValue),
                listOf(amount.rawValue)
            ).send()
                ?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("disperse transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("disperse transaction info is attached to asset multi-send request") {
            assetMultiSendRequestRepository.setDisperseTxInfo(createResponse.id, disperseTxHash, senderAddress)
        }

        val fetchResponse = suppose("request to fetch asset multi-send requests is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/multi-send/by-project/${createResponse.projectId}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, AssetMultiSendRequestsResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(fetchResponse).withMessage()
                .isEqualTo(
                    AssetMultiSendRequestsResponse(
                        listOf(
                            AssetMultiSendRequestResponse(
                                id = fetchResponse.requests[0].id,
                                projectId = projectId,
                                approveStatus = Status.SUCCESS,
                                disperseStatus = Status.SUCCESS,
                                chainId = chainId.value,
                                tokenAddress = tokenAddress.rawValue,
                                disperseContractAddress = disperseContractAddress.rawValue,
                                assetType = AssetType.TOKEN,
                                items = listOf(
                                    MultiSendItemResponse(
                                        walletAddress = recipientAddress.rawValue,
                                        amount = amount.rawValue,
                                        itemName = "Example"
                                    )
                                ),
                                senderAddress = senderAddress.rawValue,
                                arbitraryData = fetchResponse.requests[0].arbitraryData,
                                approveScreenConfig = ScreenConfig(
                                    beforeActionMessage = "approve-before-action-message",
                                    afterActionMessage = "approve-after-action-message"
                                ),
                                disperseScreenConfig = ScreenConfig(
                                    beforeActionMessage = "disperse-before-action-message",
                                    afterActionMessage = "disperse-after-action-message"
                                ),
                                redirectUrl = PROJECT.baseRedirectUrl.value +
                                    "/request-multi-send/${fetchResponse.requests[0].id}/action",
                                approveTx = TransactionResponse(
                                    txHash = approveTxHash.value,
                                    from = senderAddress.rawValue,
                                    to = tokenAddress.rawValue,
                                    data = fetchResponse.requests[0].approveTx!!.data,
                                    value = BigInteger.ZERO,
                                    blockConfirmations = fetchResponse.requests[0].approveTx!!.blockConfirmations,
                                    timestamp = fetchResponse.requests[0].approveTx!!.timestamp
                                ),
                                disperseTx = TransactionResponse(
                                    txHash = disperseTxHash.value,
                                    from = senderAddress.rawValue,
                                    to = disperseContractAddress.rawValue,
                                    data = fetchResponse.requests[0].disperseTx!!.data,
                                    value = BigInteger.ZERO,
                                    blockConfirmations = fetchResponse.requests[0].disperseTx!!.blockConfirmations,
                                    timestamp = fetchResponse.requests[0].disperseTx!!.timestamp
                                ),
                                createdAt = fetchResponse.requests[0].createdAt,
                                approveEvents = APPROVE_EVENTS.withDisperseContractAddress(disperseContractAddress),
                                disperseEvents = DISPERSE_EVENTS.withDisperseContractAddress(disperseContractAddress)
                            )
                        )
                    )
                )

            assertThat(fetchResponse.requests[0].approveTx!!.blockConfirmations)
                .isNotZero()
            assertThat(fetchResponse.requests[0].disperseTx!!.blockConfirmations)
                .isNotZero()
            assertThat(fetchResponse.requests[0].createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            assertThat(fetchResponse.requests[0].approveTx!!.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            assertThat(fetchResponse.requests[0].disperseTx!!.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchAssetMultiSendRequestsBySenderAddress() {
        val mainAccount = accounts[0]

        val tokenContract = suppose("simple asset contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(mainAccount.address),
                listOf(BigInteger("10000")),
                mainAccount.address
            ).send()
        }

        val disperseContract = suppose("simple disperse contract is deployed") {
            SimpleDisperse.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider()
            ).send()
        }

        val tokenAddress = ContractAddress(tokenContract.contractAddress)
        val disperseContractAddress = ContractAddress(disperseContract.contractAddress)
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress(mainAccount.address)
        val recipientAddress = WalletAddress(accounts[1].address)

        val createResponse = suppose("request to create asset multi-send request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/multi-send")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "disperse_contract_address": "${disperseContractAddress.rawValue}",
                                "asset_type": "TOKEN",
                                "items": [
                                    {
                                        "wallet_address": "${recipientAddress.rawValue}",
                                        "amount": "${amount.rawValue}",
                                        "item_name": "Example"
                                    }
                                ],
                                "sender_address": "${senderAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "approve_screen_config": {
                                    "before_action_message": "approve-before-action-message",
                                    "after_action_message": "approve-after-action-message"
                                },
                                "disperse_screen_config": {
                                    "before_action_message": "disperse-before-action-message",
                                    "after_action_message": "disperse-after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(createResponse.response.contentAsString, AssetMultiSendRequestResponse::class.java)
        }

        val approveTxHash = suppose("some asset approve transaction is made") {
            tokenContract.approve(disperseContractAddress.rawValue, amount.rawValue).send()
                ?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("approve transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("approve transaction info is attached to asset multi-send request") {
            assetMultiSendRequestRepository.setApproveTxInfo(createResponse.id, approveTxHash, senderAddress)
        }

        val disperseTxHash = suppose("some disperse transaction is made") {
            disperseContract.disperseToken(
                tokenAddress.rawValue,
                listOf(recipientAddress.rawValue),
                listOf(amount.rawValue)
            ).send()
                ?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("disperse transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("disperse transaction info is attached to asset multi-send request") {
            assetMultiSendRequestRepository.setDisperseTxInfo(createResponse.id, disperseTxHash, senderAddress)
        }

        val fetchResponse = suppose("request to fetch asset multi-send requests is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/multi-send/by-sender/${WalletAddress(mainAccount.address).rawValue}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, AssetMultiSendRequestsResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(fetchResponse).withMessage()
                .isEqualTo(
                    AssetMultiSendRequestsResponse(
                        listOf(
                            AssetMultiSendRequestResponse(
                                id = fetchResponse.requests[0].id,
                                projectId = PROJECT_ID,
                                approveStatus = Status.SUCCESS,
                                disperseStatus = Status.SUCCESS,
                                chainId = PROJECT.chainId.value,
                                tokenAddress = tokenAddress.rawValue,
                                disperseContractAddress = disperseContractAddress.rawValue,
                                assetType = AssetType.TOKEN,
                                items = listOf(
                                    MultiSendItemResponse(
                                        walletAddress = recipientAddress.rawValue,
                                        amount = amount.rawValue,
                                        itemName = "Example"
                                    )
                                ),
                                senderAddress = senderAddress.rawValue,
                                arbitraryData = fetchResponse.requests[0].arbitraryData,
                                approveScreenConfig = ScreenConfig(
                                    beforeActionMessage = "approve-before-action-message",
                                    afterActionMessage = "approve-after-action-message"
                                ),
                                disperseScreenConfig = ScreenConfig(
                                    beforeActionMessage = "disperse-before-action-message",
                                    afterActionMessage = "disperse-after-action-message"
                                ),
                                redirectUrl = PROJECT.baseRedirectUrl.value +
                                    "/request-multi-send/${fetchResponse.requests[0].id}/action",
                                approveTx = TransactionResponse(
                                    txHash = approveTxHash.value,
                                    from = senderAddress.rawValue,
                                    to = tokenAddress.rawValue,
                                    data = fetchResponse.requests[0].approveTx!!.data,
                                    value = BigInteger.ZERO,
                                    blockConfirmations = fetchResponse.requests[0].approveTx!!.blockConfirmations,
                                    timestamp = fetchResponse.requests[0].approveTx!!.timestamp
                                ),
                                disperseTx = TransactionResponse(
                                    txHash = disperseTxHash.value,
                                    from = senderAddress.rawValue,
                                    to = disperseContractAddress.rawValue,
                                    data = fetchResponse.requests[0].disperseTx!!.data,
                                    value = BigInteger.ZERO,
                                    blockConfirmations = fetchResponse.requests[0].disperseTx!!.blockConfirmations,
                                    timestamp = fetchResponse.requests[0].disperseTx!!.timestamp
                                ),
                                createdAt = fetchResponse.requests[0].createdAt,
                                approveEvents = APPROVE_EVENTS.withDisperseContractAddress(disperseContractAddress),
                                disperseEvents = DISPERSE_EVENTS.withDisperseContractAddress(disperseContractAddress)
                            )
                        )
                    )
                )

            assertThat(fetchResponse.requests[0].approveTx!!.blockConfirmations)
                .isNotZero()
            assertThat(fetchResponse.requests[0].disperseTx!!.blockConfirmations)
                .isNotZero()
            assertThat(fetchResponse.requests[0].createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            assertThat(fetchResponse.requests[0].approveTx!!.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            assertThat(fetchResponse.requests[0].disperseTx!!.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchAssetMultiSendRequestsBySenderAddressWhenCustomRpcUrlIsSpecified() {
        val mainAccount = accounts[0]

        val tokenContract = suppose("simple asset contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(mainAccount.address),
                listOf(BigInteger("10000")),
                mainAccount.address
            ).send()
        }

        val disperseContract = suppose("simple disperse contract is deployed") {
            SimpleDisperse.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider()
            ).send()
        }

        val tokenAddress = ContractAddress(tokenContract.contractAddress)
        val disperseContractAddress = ContractAddress(disperseContract.contractAddress)
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress(mainAccount.address)
        val recipientAddress = WalletAddress(accounts[1].address)

        val (projectId, chainId, apiKey) = suppose("project with customRpcUrl is inserted into database") {
            insertProjectWithCustomRpcUrl()
        }

        val createResponse = suppose("request to create asset multi-send request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/multi-send")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "disperse_contract_address": "${disperseContractAddress.rawValue}",
                                "asset_type": "TOKEN",
                                "items": [
                                    {
                                        "wallet_address": "${recipientAddress.rawValue}",
                                        "amount": "${amount.rawValue}",
                                        "item_name": "Example"
                                    }
                                ],
                                "sender_address": "${senderAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "approve_screen_config": {
                                    "before_action_message": "approve-before-action-message",
                                    "after_action_message": "approve-after-action-message"
                                },
                                "disperse_screen_config": {
                                    "before_action_message": "disperse-before-action-message",
                                    "after_action_message": "disperse-after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(createResponse.response.contentAsString, AssetMultiSendRequestResponse::class.java)
        }

        val approveTxHash = suppose("some asset approve transaction is made") {
            tokenContract.approve(disperseContractAddress.rawValue, amount.rawValue).send()
                ?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("approve transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("approve transaction info is attached to asset multi-send request") {
            assetMultiSendRequestRepository.setApproveTxInfo(createResponse.id, approveTxHash, senderAddress)
        }

        val disperseTxHash = suppose("some disperse transaction is made") {
            disperseContract.disperseToken(
                tokenAddress.rawValue,
                listOf(recipientAddress.rawValue),
                listOf(amount.rawValue)
            ).send()
                ?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("disperse transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("disperse transaction info is attached to asset multi-send request") {
            assetMultiSendRequestRepository.setDisperseTxInfo(createResponse.id, disperseTxHash, senderAddress)
        }

        val fetchResponse = suppose("request to fetch asset multi-send requests is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/multi-send/by-sender/${WalletAddress(mainAccount.address).rawValue}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, AssetMultiSendRequestsResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(fetchResponse).withMessage()
                .isEqualTo(
                    AssetMultiSendRequestsResponse(
                        listOf(
                            AssetMultiSendRequestResponse(
                                id = fetchResponse.requests[0].id,
                                projectId = projectId,
                                approveStatus = Status.SUCCESS,
                                disperseStatus = Status.SUCCESS,
                                chainId = chainId.value,
                                tokenAddress = tokenAddress.rawValue,
                                disperseContractAddress = disperseContractAddress.rawValue,
                                assetType = AssetType.TOKEN,
                                items = listOf(
                                    MultiSendItemResponse(
                                        walletAddress = recipientAddress.rawValue,
                                        amount = amount.rawValue,
                                        itemName = "Example"
                                    )
                                ),
                                senderAddress = senderAddress.rawValue,
                                arbitraryData = fetchResponse.requests[0].arbitraryData,
                                approveScreenConfig = ScreenConfig(
                                    beforeActionMessage = "approve-before-action-message",
                                    afterActionMessage = "approve-after-action-message"
                                ),
                                disperseScreenConfig = ScreenConfig(
                                    beforeActionMessage = "disperse-before-action-message",
                                    afterActionMessage = "disperse-after-action-message"
                                ),
                                redirectUrl = PROJECT.baseRedirectUrl.value +
                                    "/request-multi-send/${fetchResponse.requests[0].id}/action",
                                approveTx = TransactionResponse(
                                    txHash = approveTxHash.value,
                                    from = senderAddress.rawValue,
                                    to = tokenAddress.rawValue,
                                    data = fetchResponse.requests[0].approveTx!!.data,
                                    value = BigInteger.ZERO,
                                    blockConfirmations = fetchResponse.requests[0].approveTx!!.blockConfirmations,
                                    timestamp = fetchResponse.requests[0].approveTx!!.timestamp
                                ),
                                disperseTx = TransactionResponse(
                                    txHash = disperseTxHash.value,
                                    from = senderAddress.rawValue,
                                    to = disperseContractAddress.rawValue,
                                    data = fetchResponse.requests[0].disperseTx!!.data,
                                    value = BigInteger.ZERO,
                                    blockConfirmations = fetchResponse.requests[0].disperseTx!!.blockConfirmations,
                                    timestamp = fetchResponse.requests[0].disperseTx!!.timestamp
                                ),
                                createdAt = fetchResponse.requests[0].createdAt,
                                approveEvents = APPROVE_EVENTS.withDisperseContractAddress(disperseContractAddress),
                                disperseEvents = DISPERSE_EVENTS.withDisperseContractAddress(disperseContractAddress)
                            )
                        )
                    )
                )

            assertThat(fetchResponse.requests[0].approveTx!!.blockConfirmations)
                .isNotZero()
            assertThat(fetchResponse.requests[0].disperseTx!!.blockConfirmations)
                .isNotZero()
            assertThat(fetchResponse.requests[0].createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            assertThat(fetchResponse.requests[0].approveTx!!.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            assertThat(fetchResponse.requests[0].disperseTx!!.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyAttachApproveTransactionInfo() {
        val id = UUID.randomUUID()
        val tokenSender = WalletAddress("d")

        suppose("some asset multi-send request without approve transaction info exists in database") {
            assetMultiSendRequestRepository.store(
                StoreAssetMultiSendRequestParams(
                    id = id,
                    projectId = PROJECT_ID,
                    chainId = Chain.HARDHAT_TESTNET.id,
                    redirectUrl = "https://example.com/$id",
                    tokenAddress = ContractAddress("a"),
                    disperseContractAddress = ContractAddress("b"),
                    assetAmounts = listOf(Balance(BigInteger.TEN)),
                    assetRecipientAddresses = listOf(WalletAddress("c")),
                    itemNames = listOf("Example"),
                    assetSenderAddress = tokenSender,
                    arbitraryData = TestData.EMPTY_JSON_OBJECT,
                    approveScreenConfig = ScreenConfig(
                        beforeActionMessage = "approve-before-action-message",
                        afterActionMessage = "approve-after-action-message"
                    ),
                    disperseScreenConfig = ScreenConfig(
                        beforeActionMessage = "disperse-before-action-message",
                        afterActionMessage = "disperse-after-action-message"
                    ),
                    createdAt = TestData.TIMESTAMP
                )
            )
        }

        val approveTxHash = TransactionHash("0x1")

        suppose("request to attach approve transaction info to asset multi-send request is made") {
            mockMvc.perform(
                MockMvcRequestBuilders.put("/v1/multi-send/$id/approve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "tx_hash": "${approveTxHash.value}",
                                "caller_address": "${tokenSender.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
        }

        verify("approve transaction info is correctly attached to asset multi-send request") {
            val storedRequest = assetMultiSendRequestRepository.getById(id)

            assertThat(storedRequest?.approveTxHash)
                .isEqualTo(approveTxHash)
        }
    }

    @Test
    fun mustReturn400BadRequestWhenApproveTransactionInfoIsNotAttached() {
        val id = UUID.randomUUID()
        val approveTxHash = TransactionHash("0x1")
        val tokenSender = WalletAddress("b")

        suppose("some asset multi-send request with approve transaction info exists in database") {
            assetMultiSendRequestRepository.store(
                StoreAssetMultiSendRequestParams(
                    id = id,
                    projectId = PROJECT_ID,
                    chainId = Chain.HARDHAT_TESTNET.id,
                    redirectUrl = "https://example.com/$id",
                    tokenAddress = ContractAddress("a"),
                    disperseContractAddress = ContractAddress("b"),
                    assetAmounts = listOf(Balance(BigInteger.TEN)),
                    assetRecipientAddresses = listOf(WalletAddress("c")),
                    itemNames = listOf("Example"),
                    assetSenderAddress = tokenSender,
                    arbitraryData = TestData.EMPTY_JSON_OBJECT,
                    approveScreenConfig = ScreenConfig(
                        beforeActionMessage = "approve-before-action-message",
                        afterActionMessage = "approve-after-action-message"
                    ),
                    disperseScreenConfig = ScreenConfig(
                        beforeActionMessage = "disperse-before-action-message",
                        afterActionMessage = "disperse-after-action-message"
                    ),
                    createdAt = TestData.TIMESTAMP
                )
            )
            assetMultiSendRequestRepository.setApproveTxInfo(id, approveTxHash, tokenSender)
        }

        verify("400 is returned when attaching approve transaction info") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.put("/v1/multi-send/$id/approve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "tx_hash": "0x2",
                                "caller_address": "${tokenSender.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.TX_INFO_ALREADY_SET)
        }

        verify("approve transaction info is not changed in database") {
            val storedRequest = assetMultiSendRequestRepository.getById(id)

            assertThat(storedRequest?.approveTxHash)
                .isEqualTo(approveTxHash)
        }
    }

    @Test
    fun mustCorrectlyAttachDisperseTransactionInfo() {
        val id = UUID.randomUUID()
        val tokenSender = WalletAddress("d")

        suppose("some asset multi-send request without disperse transaction info exists in database") {
            assetMultiSendRequestRepository.store(
                StoreAssetMultiSendRequestParams(
                    id = id,
                    projectId = PROJECT_ID,
                    chainId = Chain.HARDHAT_TESTNET.id,
                    redirectUrl = "https://example.com/$id",
                    tokenAddress = null,
                    disperseContractAddress = ContractAddress("b"),
                    assetAmounts = listOf(Balance(BigInteger.TEN)),
                    assetRecipientAddresses = listOf(WalletAddress("c")),
                    itemNames = listOf("Example"),
                    assetSenderAddress = tokenSender,
                    arbitraryData = TestData.EMPTY_JSON_OBJECT,
                    approveScreenConfig = ScreenConfig(
                        beforeActionMessage = "approve-before-action-message",
                        afterActionMessage = "approve-after-action-message"
                    ),
                    disperseScreenConfig = ScreenConfig(
                        beforeActionMessage = "disperse-before-action-message",
                        afterActionMessage = "disperse-after-action-message"
                    ),
                    createdAt = TestData.TIMESTAMP
                )
            )
        }

        val disperseTxHash = TransactionHash("0x1")

        suppose("request to attach disperse transaction info to asset multi-send request is made") {
            mockMvc.perform(
                MockMvcRequestBuilders.put("/v1/multi-send/$id/disperse")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "tx_hash": "${disperseTxHash.value}",
                                "caller_address": "${tokenSender.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
        }

        verify("disperse transaction info is correctly attached to asset multi-send request") {
            val storedRequest = assetMultiSendRequestRepository.getById(id)

            assertThat(storedRequest?.disperseTxHash)
                .isEqualTo(disperseTxHash)
        }
    }

    @Test
    fun mustReturn400BadRequestWhenDisperseTransactionInfoIsNotAttached() {
        val id = UUID.randomUUID()
        val disperseTxHash = TransactionHash("0x1")
        val tokenSender = WalletAddress("b")

        suppose("some asset multi-send request with disperse transaction info exists in database") {
            assetMultiSendRequestRepository.store(
                StoreAssetMultiSendRequestParams(
                    id = id,
                    projectId = PROJECT_ID,
                    chainId = Chain.HARDHAT_TESTNET.id,
                    redirectUrl = "https://example.com/$id",
                    tokenAddress = null,
                    disperseContractAddress = ContractAddress("b"),
                    assetAmounts = listOf(Balance(BigInteger.TEN)),
                    assetRecipientAddresses = listOf(WalletAddress("c")),
                    itemNames = listOf("Example"),
                    assetSenderAddress = tokenSender,
                    arbitraryData = TestData.EMPTY_JSON_OBJECT,
                    approveScreenConfig = ScreenConfig(
                        beforeActionMessage = "approve-before-action-message",
                        afterActionMessage = "approve-after-action-message"
                    ),
                    disperseScreenConfig = ScreenConfig(
                        beforeActionMessage = "disperse-before-action-message",
                        afterActionMessage = "disperse-after-action-message"
                    ),
                    createdAt = TestData.TIMESTAMP
                )
            )
            assetMultiSendRequestRepository.setDisperseTxInfo(id, disperseTxHash, tokenSender)
        }

        verify("400 is returned when attaching disperse transaction info") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.put("/v1/multi-send/$id/disperse")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "tx_hash": "0x2",
                                "caller_address": "${tokenSender.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.TX_INFO_ALREADY_SET)
        }

        verify("disperse transaction info is not changed in database") {
            val storedRequest = assetMultiSendRequestRepository.getById(id)

            assertThat(storedRequest?.disperseTxHash)
                .isEqualTo(disperseTxHash)
        }
    }

    private fun insertProjectWithCustomRpcUrl(): Triple<UUID, ChainId, String> {
        val projectId = UUID.randomUUID()
        val chainId = ChainId(1337L)

        dslContext.executeInsert(
            ProjectRecord(
                id = projectId,
                ownerId = PROJECT.ownerId,
                issuerContractAddress = ContractAddress("1"),
                baseRedirectUrl = PROJECT.baseRedirectUrl,
                chainId = chainId,
                customRpcUrl = "http://localhost:${hardhatContainer.mappedPort}",
                createdAt = PROJECT.createdAt
            )
        )

        val apiKey = "another-api-key"

        dslContext.executeInsert(
            ApiKeyRecord(
                id = UUID.randomUUID(),
                projectId = projectId,
                apiKey = apiKey,
                createdAt = TestData.TIMESTAMP
            )
        )

        return Triple(projectId, chainId, apiKey)
    }

    private fun List<EventInfoResponse>.withDisperseContractAddress(disperseContractAddress: ContractAddress) =
        map { event ->
            event.copy(
                arguments = event.arguments.map { arg ->
                    arg.copy(
                        value = arg.value?.toString()
                            ?.replace("{disperseContractAddress}", disperseContractAddress.rawValue)
                    )
                }
            )
        }
}
