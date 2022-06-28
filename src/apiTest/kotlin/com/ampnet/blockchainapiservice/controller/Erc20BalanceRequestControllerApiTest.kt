package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.ControllerTestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.blockchain.SimpleERC20
import com.ampnet.blockchainapiservice.blockchain.properties.Chain
import com.ampnet.blockchainapiservice.config.binding.ProjectApiKeyResolver
import com.ampnet.blockchainapiservice.config.binding.RpcUrlSpecResolver
import com.ampnet.blockchainapiservice.exception.ErrorCode
import com.ampnet.blockchainapiservice.generated.jooq.enums.UserIdentifierType
import com.ampnet.blockchainapiservice.generated.jooq.tables.ApiKeyTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.Erc20BalanceRequestTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.ProjectTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.UserIdentifierTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ApiKeyRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ProjectRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.UserIdentifierRecord
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.params.StoreErc20BalanceRequestParams
import com.ampnet.blockchainapiservice.model.response.BalanceResponse
import com.ampnet.blockchainapiservice.model.response.Erc20BalanceRequestResponse
import com.ampnet.blockchainapiservice.model.response.Erc20BalanceRequestsResponse
import com.ampnet.blockchainapiservice.model.result.Erc20BalanceRequest
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.repository.Erc20BalanceRequestRepository
import com.ampnet.blockchainapiservice.testcontainers.HardhatTestContainer
import com.ampnet.blockchainapiservice.util.AssetType
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.BaseUrl
import com.ampnet.blockchainapiservice.util.BlockNumber
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.SignedMessage
import com.ampnet.blockchainapiservice.util.Status
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.web3j.tx.Transfer
import org.web3j.tx.gas.DefaultGasProvider
import org.web3j.utils.Convert
import java.math.BigInteger
import java.util.UUID

class Erc20BalanceRequestControllerApiTest : ControllerTestBase() {

    companion object {
        private val PROJECT_ID = UUID.randomUUID()
        private val OWNER_ID = UUID.randomUUID()
        private val PROJECT = Project(
            id = PROJECT_ID,
            ownerId = OWNER_ID,
            issuerContractAddress = ContractAddress("0"),
            baseRedirectUrl = BaseUrl("https://example.com/"),
            chainId = Chain.HARDHAT_TESTNET.id,
            customRpcUrl = "custom-rpc-url",
            createdAt = TestData.TIMESTAMP
        )
        private const val API_KEY = "api-key"
    }

    private val accounts = HardhatTestContainer.accounts

    @Autowired
    private lateinit var erc20BalanceRequestRepository: Erc20BalanceRequestRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        dslContext.deleteFrom(Erc20BalanceRequestTable.ERC20_BALANCE_REQUEST).execute()
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
            ApiKeyRecord(
                id = UUID.randomUUID(),
                projectId = PROJECT_ID,
                apiKey = API_KEY,
                createdAt = TestData.TIMESTAMP
            )
        )
    }

    @Test
    fun mustCorrectlyCreateErc20BalanceRequestForSomeToken() {
        val tokenAddress = ContractAddress("cafebabe")
        val blockNumber = BlockNumber(BigInteger.TEN)
        val walletAddress = WalletAddress("a")

        val response = suppose("request to create ERC20 balance request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/balance")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "asset_type" : "TOKEN",
                                "block_number": "${blockNumber.value}",
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

            objectMapper.readValue(response.response.contentAsString, Erc20BalanceRequestResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    Erc20BalanceRequestResponse(
                        id = response.id,
                        projectId = PROJECT_ID,
                        status = Status.PENDING,
                        chainId = PROJECT.chainId.value,
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-balance/${response.id}/action",
                        tokenAddress = tokenAddress.rawValue,
                        assetType = AssetType.TOKEN,
                        blockNumber = blockNumber.value,
                        walletAddress = walletAddress.rawValue,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        balance = null,
                        messageToSign = "Verification message ID to sign: ${response.id}",
                        signedMessage = null,
                        createdAt = response.createdAt
                    )
                )

            assertThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("ERC20 balance request is correctly stored in database") {
            val storedRequest = erc20BalanceRequestRepository.getById(response.id)

            assertThat(storedRequest).withMessage()
                .isEqualTo(
                    Erc20BalanceRequest(
                        id = response.id,
                        projectId = PROJECT_ID,
                        chainId = PROJECT.chainId,
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-balance/${response.id}/action",
                        tokenAddress = tokenAddress,
                        blockNumber = blockNumber,
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

            assertThat(storedRequest.createdAt.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyCreateErc20BalanceRequestForSomeTokenWithRedirectUrl() {
        val tokenAddress = ContractAddress("cafebabe")
        val blockNumber = BlockNumber(BigInteger.TEN)
        val walletAddress = WalletAddress("a")
        val redirectUrl = "https://custom-url/\${id}"

        val response = suppose("request to create ERC20 balance request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/balance")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "redirect_url": "$redirectUrl",
                                "token_address" : "${tokenAddress.rawValue}",
                                "asset_type" : "TOKEN",
                                "block_number": "${blockNumber.value}",
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

            objectMapper.readValue(response.response.contentAsString, Erc20BalanceRequestResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    Erc20BalanceRequestResponse(
                        id = response.id,
                        projectId = PROJECT_ID,
                        status = Status.PENDING,
                        chainId = PROJECT.chainId.value,
                        redirectUrl = "https://custom-url/${response.id}",
                        tokenAddress = tokenAddress.rawValue,
                        assetType = AssetType.TOKEN,
                        blockNumber = blockNumber.value,
                        walletAddress = walletAddress.rawValue,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        balance = null,
                        messageToSign = "Verification message ID to sign: ${response.id}",
                        signedMessage = null,
                        createdAt = response.createdAt
                    )
                )

            assertThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("ERC20 balance request is correctly stored in database") {
            val storedRequest = erc20BalanceRequestRepository.getById(response.id)

            assertThat(storedRequest).withMessage()
                .isEqualTo(
                    Erc20BalanceRequest(
                        id = response.id,
                        projectId = PROJECT_ID,
                        chainId = PROJECT.chainId,
                        redirectUrl = "https://custom-url/${response.id}",
                        tokenAddress = tokenAddress,
                        blockNumber = blockNumber,
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

            assertThat(storedRequest.createdAt.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyCreateErc20BalanceRequestForNativeAsset() {
        val blockNumber = BlockNumber(BigInteger.TEN)
        val walletAddress = WalletAddress("a")

        val response = suppose("request to create ERC20 balance request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/balance")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "asset_type" : "NATIVE",
                                "block_number": "${blockNumber.value}",
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

            objectMapper.readValue(response.response.contentAsString, Erc20BalanceRequestResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    Erc20BalanceRequestResponse(
                        id = response.id,
                        projectId = PROJECT_ID,
                        status = Status.PENDING,
                        chainId = PROJECT.chainId.value,
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-balance/${response.id}/action",
                        tokenAddress = null,
                        assetType = AssetType.NATIVE,
                        blockNumber = blockNumber.value,
                        walletAddress = walletAddress.rawValue,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        balance = null,
                        messageToSign = "Verification message ID to sign: ${response.id}",
                        signedMessage = null,
                        createdAt = response.createdAt
                    )
                )

            assertThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("ERC20 balance request is correctly stored in database") {
            val storedRequest = erc20BalanceRequestRepository.getById(response.id)

            assertThat(storedRequest).withMessage()
                .isEqualTo(
                    Erc20BalanceRequest(
                        id = response.id,
                        projectId = PROJECT_ID,
                        chainId = PROJECT.chainId,
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-balance/${response.id}/action",
                        tokenAddress = null,
                        blockNumber = blockNumber,
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

            assertThat(storedRequest.createdAt.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyCreateErc20BalanceRequestForNativeAssetWithRedirectUrl() {
        val blockNumber = BlockNumber(BigInteger.TEN)
        val walletAddress = WalletAddress("a")
        val redirectUrl = "https://custom-url/\${id}"

        val response = suppose("request to create ERC20 balance request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/balance")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "redirect_url": "$redirectUrl",
                                "asset_type" : "NATIVE",
                                "block_number": "${blockNumber.value}",
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

            objectMapper.readValue(response.response.contentAsString, Erc20BalanceRequestResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    Erc20BalanceRequestResponse(
                        id = response.id,
                        projectId = PROJECT_ID,
                        status = Status.PENDING,
                        chainId = PROJECT.chainId.value,
                        redirectUrl = "https://custom-url/${response.id}",
                        tokenAddress = null,
                        assetType = AssetType.NATIVE,
                        blockNumber = blockNumber.value,
                        walletAddress = walletAddress.rawValue,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        balance = null,
                        messageToSign = "Verification message ID to sign: ${response.id}",
                        signedMessage = null,
                        createdAt = response.createdAt
                    )
                )

            assertThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("ERC20 balance request is correctly stored in database") {
            val storedRequest = erc20BalanceRequestRepository.getById(response.id)

            assertThat(storedRequest).withMessage()
                .isEqualTo(
                    Erc20BalanceRequest(
                        id = response.id,
                        projectId = PROJECT_ID,
                        chainId = PROJECT.chainId,
                        redirectUrl = "https://custom-url/${response.id}",
                        tokenAddress = null,
                        blockNumber = blockNumber,
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

            assertThat(storedRequest.createdAt.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustReturn400BadRequestWhenTokenAddressIsMissingForTokenAssetType() {
        val blockNumber = BlockNumber(BigInteger.TEN)
        val walletAddress = WalletAddress("a")

        verify("400 is returned for missing token address") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/balance")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "asset_type": "TOKEN",
                                "asset_type" : "TOKEN",
                                "block_number": "${blockNumber.value}",
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
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.MISSING_TOKEN_ADDRESS)
        }
    }

    @Test
    fun mustReturn400BadRequestWhenTokenAddressIsSpecifiedForNativeAssetType() {
        val tokenAddress = ContractAddress("cafebabe")
        val blockNumber = BlockNumber(BigInteger.TEN)
        val walletAddress = WalletAddress("a")

        verify("400 is returned for non-allowed token address") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/balance")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "asset_type": "NATIVE",
                                "block_number": "${blockNumber.value}",
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
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.TOKEN_ADDRESS_NOT_ALLOWED)
        }
    }

    @Test
    fun mustReturn401UnauthorizedWhenCreatingErc20BalanceRequestWithInvalidApiKey() {
        val tokenAddress = ContractAddress("cafebabe")
        val blockNumber = BlockNumber(BigInteger.TEN)
        val walletAddress = WalletAddress("a")

        verify("401 is returned for invalid API key") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/balance")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, "invalid-api-key")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "block_number": "${blockNumber.value}",
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
    fun mustCorrectlyFetchErc20BalanceRequestForSomeToken() {
        val mainAccount = accounts[0]
        val walletAddress = WalletAddress("0x865f603F42ca1231e5B5F90e15663b0FE19F0b21")
        val erc20balance = Balance(BigInteger("10000"))

        val contract = suppose("simple ERC20 contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(walletAddress.rawValue),
                listOf(erc20balance.rawValue),
                mainAccount.address
            ).sendAndMine()
        }

        val tokenAddress = ContractAddress(contract.contractAddress)
        val blockNumber = hardhatContainer.blockNumber()

        val createResponse = suppose("request to create ERC20 balance request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/balance")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "asset_type" : "TOKEN",
                                "block_number": "${blockNumber.value}",
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

            objectMapper.readValue(createResponse.response.contentAsString, Erc20BalanceRequestResponse::class.java)
        }

        val id = UUID.fromString("7d86b0ac-a9a6-40fc-ac6d-2a29ca687f73")

        suppose("ID from pre-signed message is used in database") {
            dslContext.update(Erc20BalanceRequestTable.ERC20_BALANCE_REQUEST)
                .set(Erc20BalanceRequestTable.ERC20_BALANCE_REQUEST.ID, id)
                .set(Erc20BalanceRequestTable.ERC20_BALANCE_REQUEST.REDIRECT_URL, "https://example.com/$id")
                .where(Erc20BalanceRequestTable.ERC20_BALANCE_REQUEST.ID.eq(createResponse.id))
                .execute()
        }

        val signedMessage = SignedMessage(
            "0xfc90c8aa9f2164234b8826144d8ecfc287b5d7c168d0e9d284baf76dbef55c4c5761cf46e34b7cdb72cc97f1fb1c19f315ee7a" +
                "430dd6111fa6c693b41c96c5501c"
        )

        suppose("signed message is attached to ERC20 balance request") {
            erc20BalanceRequestRepository.setSignedMessage(id, walletAddress, signedMessage)
        }

        val fetchResponse = suppose("request to fetch ERC20 balance request is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/balance/$id")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, Erc20BalanceRequestResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(fetchResponse).withMessage()
                .isEqualTo(
                    Erc20BalanceRequestResponse(
                        id = id,
                        projectId = PROJECT_ID,
                        status = Status.SUCCESS,
                        chainId = PROJECT.chainId.value,
                        redirectUrl = "https://example.com/$id",
                        tokenAddress = tokenAddress.rawValue,
                        assetType = AssetType.TOKEN,
                        blockNumber = blockNumber.value,
                        walletAddress = walletAddress.rawValue,
                        arbitraryData = createResponse.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        balance = BalanceResponse(
                            wallet = walletAddress.rawValue,
                            blockNumber = blockNumber.value,
                            timestamp = fetchResponse.balance!!.timestamp,
                            amount = erc20balance.rawValue
                        ),
                        messageToSign = "Verification message ID to sign: $id",
                        signedMessage = signedMessage.value,
                        createdAt = fetchResponse.createdAt
                    )
                )

            assertThat(fetchResponse.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchErc20BalanceRequestForSomeTokenWhenCustomRpcUrlIsSpecified() {
        val mainAccount = accounts[0]
        val walletAddress = WalletAddress("0x865f603F42ca1231e5B5F90e15663b0FE19F0b21")
        val erc20balance = Balance(BigInteger("10000"))

        val contract = suppose("simple ERC20 contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(walletAddress.rawValue),
                listOf(erc20balance.rawValue),
                mainAccount.address
            ).sendAndMine()
        }

        val tokenAddress = ContractAddress(contract.contractAddress)
        val blockNumber = hardhatContainer.blockNumber()

        val createResponse = suppose("request to create ERC20 balance request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/balance")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "asset_type" : "TOKEN",
                                "block_number": "${blockNumber.value}",
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

            objectMapper.readValue(createResponse.response.contentAsString, Erc20BalanceRequestResponse::class.java)
        }

        val id = UUID.fromString("7d86b0ac-a9a6-40fc-ac6d-2a29ca687f73")

        suppose("ID from pre-signed message is used in database") {
            dslContext.update(Erc20BalanceRequestTable.ERC20_BALANCE_REQUEST)
                .set(Erc20BalanceRequestTable.ERC20_BALANCE_REQUEST.ID, id)
                .set(Erc20BalanceRequestTable.ERC20_BALANCE_REQUEST.REDIRECT_URL, "https://example.com/$id")
                .where(Erc20BalanceRequestTable.ERC20_BALANCE_REQUEST.ID.eq(createResponse.id))
                .execute()
        }

        val signedMessage = SignedMessage(
            "0xfc90c8aa9f2164234b8826144d8ecfc287b5d7c168d0e9d284baf76dbef55c4c5761cf46e34b7cdb72cc97f1fb1c19f315ee7a" +
                "430dd6111fa6c693b41c96c5501c"
        )

        suppose("signed message is attached to ERC20 balance request") {
            erc20BalanceRequestRepository.setSignedMessage(id, walletAddress, signedMessage)
        }

        val fetchResponse = suppose("request to fetch ERC20 balance request is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/balance/$id")
                    .header(
                        RpcUrlSpecResolver.RPC_URL_OVERRIDE_HEADER,
                        "http://localhost:${hardhatContainer.mappedPort}"
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, Erc20BalanceRequestResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(fetchResponse).withMessage()
                .isEqualTo(
                    Erc20BalanceRequestResponse(
                        id = id,
                        projectId = PROJECT_ID,
                        status = Status.SUCCESS,
                        chainId = PROJECT.chainId.value,
                        redirectUrl = "https://example.com/$id",
                        tokenAddress = tokenAddress.rawValue,
                        assetType = AssetType.TOKEN,
                        blockNumber = blockNumber.value,
                        walletAddress = walletAddress.rawValue,
                        arbitraryData = createResponse.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        balance = BalanceResponse(
                            wallet = walletAddress.rawValue,
                            blockNumber = blockNumber.value,
                            timestamp = fetchResponse.balance!!.timestamp,
                            amount = erc20balance.rawValue
                        ),
                        messageToSign = "Verification message ID to sign: $id",
                        signedMessage = signedMessage.value,
                        createdAt = fetchResponse.createdAt
                    )
                )

            assertThat(fetchResponse.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchErc20BalanceRequestForNativeAsset() {
        val mainAccount = accounts[0]
        val walletAddress = WalletAddress("0x865f603F42ca1231e5B5F90e15663b0FE19F0b21")
        val amount = Balance(BigInteger("10000"))

        suppose("some regular transfer transaction is made") {
            Transfer.sendFunds(
                hardhatContainer.web3j,
                mainAccount,
                walletAddress.rawValue,
                amount.rawValue.toBigDecimal(),
                Convert.Unit.WEI
            ).sendAndMine()
        }

        val blockNumber = hardhatContainer.blockNumber()

        val createResponse = suppose("request to create ERC20 balance request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/balance")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "asset_type" : "NATIVE",
                                "block_number": "${blockNumber.value}",
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

            objectMapper.readValue(createResponse.response.contentAsString, Erc20BalanceRequestResponse::class.java)
        }

        val id = UUID.fromString("7d86b0ac-a9a6-40fc-ac6d-2a29ca687f73")

        suppose("ID from pre-signed message is used in database") {
            dslContext.update(Erc20BalanceRequestTable.ERC20_BALANCE_REQUEST)
                .set(Erc20BalanceRequestTable.ERC20_BALANCE_REQUEST.ID, id)
                .set(Erc20BalanceRequestTable.ERC20_BALANCE_REQUEST.REDIRECT_URL, "https://example.com/$id")
                .where(Erc20BalanceRequestTable.ERC20_BALANCE_REQUEST.ID.eq(createResponse.id))
                .execute()
        }

        val signedMessage = SignedMessage(
            "0xfc90c8aa9f2164234b8826144d8ecfc287b5d7c168d0e9d284baf76dbef55c4c5761cf46e34b7cdb72cc97f1fb1c19f315ee7a" +
                "430dd6111fa6c693b41c96c5501c"
        )

        suppose("signed message is attached to ERC20 balance request") {
            erc20BalanceRequestRepository.setSignedMessage(id, walletAddress, signedMessage)
        }

        val fetchResponse = suppose("request to fetch ERC20 balance request is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/balance/$id")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, Erc20BalanceRequestResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(fetchResponse).withMessage()
                .isEqualTo(
                    Erc20BalanceRequestResponse(
                        id = id,
                        projectId = PROJECT_ID,
                        status = Status.SUCCESS,
                        chainId = PROJECT.chainId.value,
                        redirectUrl = "https://example.com/$id",
                        tokenAddress = null,
                        assetType = AssetType.NATIVE,
                        blockNumber = blockNumber.value,
                        walletAddress = walletAddress.rawValue,
                        arbitraryData = createResponse.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        balance = BalanceResponse(
                            wallet = walletAddress.rawValue,
                            blockNumber = blockNumber.value,
                            timestamp = fetchResponse.balance!!.timestamp,
                            amount = amount.rawValue
                        ),
                        messageToSign = "Verification message ID to sign: $id",
                        signedMessage = signedMessage.value,
                        createdAt = fetchResponse.createdAt
                    )
                )

            assertThat(fetchResponse.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchErc20BalanceRequestForNativeAssetWhenCustomRpcUrlIsSpecified() {
        val mainAccount = accounts[0]
        val walletAddress = WalletAddress("0x865f603F42ca1231e5B5F90e15663b0FE19F0b21")
        val amount = Balance(BigInteger("10000"))

        suppose("some regular transfer transaction is made") {
            Transfer.sendFunds(
                hardhatContainer.web3j,
                mainAccount,
                walletAddress.rawValue,
                amount.rawValue.toBigDecimal(),
                Convert.Unit.WEI
            ).sendAndMine()
        }

        val blockNumber = hardhatContainer.blockNumber()

        val createResponse = suppose("request to create ERC20 balance request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/balance")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "asset_type" : "NATIVE",
                                "block_number": "${blockNumber.value}",
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

            objectMapper.readValue(createResponse.response.contentAsString, Erc20BalanceRequestResponse::class.java)
        }

        val id = UUID.fromString("7d86b0ac-a9a6-40fc-ac6d-2a29ca687f73")

        suppose("ID from pre-signed message is used in database") {
            dslContext.update(Erc20BalanceRequestTable.ERC20_BALANCE_REQUEST)
                .set(Erc20BalanceRequestTable.ERC20_BALANCE_REQUEST.ID, id)
                .set(Erc20BalanceRequestTable.ERC20_BALANCE_REQUEST.REDIRECT_URL, "https://example.com/$id")
                .where(Erc20BalanceRequestTable.ERC20_BALANCE_REQUEST.ID.eq(createResponse.id))
                .execute()
        }

        val signedMessage = SignedMessage(
            "0xfc90c8aa9f2164234b8826144d8ecfc287b5d7c168d0e9d284baf76dbef55c4c5761cf46e34b7cdb72cc97f1fb1c19f315ee7a" +
                "430dd6111fa6c693b41c96c5501c"
        )

        suppose("signed message is attached to ERC20 balance request") {
            erc20BalanceRequestRepository.setSignedMessage(id, walletAddress, signedMessage)
        }

        val fetchResponse = suppose("request to fetch ERC20 balance request is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/balance/$id")
                    .header(
                        RpcUrlSpecResolver.RPC_URL_OVERRIDE_HEADER,
                        "http://localhost:${hardhatContainer.mappedPort}"
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, Erc20BalanceRequestResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(fetchResponse).withMessage()
                .isEqualTo(
                    Erc20BalanceRequestResponse(
                        id = id,
                        projectId = PROJECT_ID,
                        status = Status.SUCCESS,
                        chainId = PROJECT.chainId.value,
                        redirectUrl = "https://example.com/$id",
                        tokenAddress = null,
                        assetType = AssetType.NATIVE,
                        blockNumber = blockNumber.value,
                        walletAddress = walletAddress.rawValue,
                        arbitraryData = createResponse.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        balance = BalanceResponse(
                            wallet = walletAddress.rawValue,
                            blockNumber = blockNumber.value,
                            timestamp = fetchResponse.balance!!.timestamp,
                            amount = amount.rawValue
                        ),
                        messageToSign = "Verification message ID to sign: $id",
                        signedMessage = signedMessage.value,
                        createdAt = fetchResponse.createdAt
                    )
                )

            assertThat(fetchResponse.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchErc20BalanceRequestsByProjectId() {
        val mainAccount = accounts[0]
        val walletAddress = WalletAddress("0x865f603F42ca1231e5B5F90e15663b0FE19F0b21")
        val erc20balance = Balance(BigInteger("10000"))

        val contract = suppose("simple ERC20 contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(walletAddress.rawValue),
                listOf(erc20balance.rawValue),
                mainAccount.address
            ).sendAndMine()
        }

        val tokenAddress = ContractAddress(contract.contractAddress)
        val blockNumber = hardhatContainer.blockNumber()

        val createResponse = suppose("request to create ERC20 balance request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/balance")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "asset_type" : "TOKEN",
                                "block_number": "${blockNumber.value}",
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

            objectMapper.readValue(createResponse.response.contentAsString, Erc20BalanceRequestResponse::class.java)
        }

        val id = UUID.fromString("7d86b0ac-a9a6-40fc-ac6d-2a29ca687f73")

        suppose("ID from pre-signed message is used in database") {
            dslContext.update(Erc20BalanceRequestTable.ERC20_BALANCE_REQUEST)
                .set(Erc20BalanceRequestTable.ERC20_BALANCE_REQUEST.ID, id)
                .set(Erc20BalanceRequestTable.ERC20_BALANCE_REQUEST.REDIRECT_URL, "https://example.com/$id")
                .where(Erc20BalanceRequestTable.ERC20_BALANCE_REQUEST.ID.eq(createResponse.id))
                .execute()
        }

        val signedMessage = SignedMessage(
            "0xfc90c8aa9f2164234b8826144d8ecfc287b5d7c168d0e9d284baf76dbef55c4c5761cf46e34b7cdb72cc97f1fb1c19f315ee7a" +
                "430dd6111fa6c693b41c96c5501c"
        )

        suppose("signed message is attached to ERC20 balance request") {
            erc20BalanceRequestRepository.setSignedMessage(id, walletAddress, signedMessage)
        }

        val fetchResponse = suppose("request to fetch ERC20 balance requests by project ID is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/balance/by-project/$PROJECT_ID")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, Erc20BalanceRequestsResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(fetchResponse).withMessage()
                .isEqualTo(
                    Erc20BalanceRequestsResponse(
                        listOf(
                            Erc20BalanceRequestResponse(
                                id = id,
                                projectId = PROJECT_ID,
                                status = Status.SUCCESS,
                                chainId = PROJECT.chainId.value,
                                redirectUrl = "https://example.com/$id",
                                tokenAddress = tokenAddress.rawValue,
                                assetType = AssetType.TOKEN,
                                blockNumber = blockNumber.value,
                                walletAddress = walletAddress.rawValue,
                                arbitraryData = createResponse.arbitraryData,
                                screenConfig = ScreenConfig(
                                    beforeActionMessage = "before-action-message",
                                    afterActionMessage = "after-action-message"
                                ),
                                balance = BalanceResponse(
                                    wallet = walletAddress.rawValue,
                                    blockNumber = blockNumber.value,
                                    timestamp = fetchResponse.requests[0].balance!!.timestamp,
                                    amount = erc20balance.rawValue
                                ),
                                messageToSign = "Verification message ID to sign: $id",
                                signedMessage = signedMessage.value,
                                createdAt = fetchResponse.requests[0].createdAt
                            )
                        )
                    )
                )

            assertThat(fetchResponse.requests[0].createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchErc20BalanceRequestsByProjectIdWhenCustomRpcUrlIsSpecified() {
        val mainAccount = accounts[0]
        val walletAddress = WalletAddress("0x865f603F42ca1231e5B5F90e15663b0FE19F0b21")
        val erc20balance = Balance(BigInteger("10000"))

        val contract = suppose("simple ERC20 contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(walletAddress.rawValue),
                listOf(erc20balance.rawValue),
                mainAccount.address
            ).sendAndMine()
        }

        val tokenAddress = ContractAddress(contract.contractAddress)
        val blockNumber = hardhatContainer.blockNumber()

        val createResponse = suppose("request to create ERC20 balance request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/balance")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "asset_type" : "TOKEN",
                                "block_number": "${blockNumber.value}",
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

            objectMapper.readValue(createResponse.response.contentAsString, Erc20BalanceRequestResponse::class.java)
        }

        val id = UUID.fromString("7d86b0ac-a9a6-40fc-ac6d-2a29ca687f73")

        suppose("ID from pre-signed message is used in database") {
            dslContext.update(Erc20BalanceRequestTable.ERC20_BALANCE_REQUEST)
                .set(Erc20BalanceRequestTable.ERC20_BALANCE_REQUEST.ID, id)
                .set(Erc20BalanceRequestTable.ERC20_BALANCE_REQUEST.REDIRECT_URL, "https://example.com/$id")
                .where(Erc20BalanceRequestTable.ERC20_BALANCE_REQUEST.ID.eq(createResponse.id))
                .execute()
        }

        val signedMessage = SignedMessage(
            "0xfc90c8aa9f2164234b8826144d8ecfc287b5d7c168d0e9d284baf76dbef55c4c5761cf46e34b7cdb72cc97f1fb1c19f315ee7a" +
                "430dd6111fa6c693b41c96c5501c"
        )

        suppose("signed message is attached to ERC20 balance request") {
            erc20BalanceRequestRepository.setSignedMessage(id, walletAddress, signedMessage)
        }

        val fetchResponse = suppose("request to fetch ERC20 balance requests by project ID is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/balance/by-project/$PROJECT_ID")
                    .header(
                        RpcUrlSpecResolver.RPC_URL_OVERRIDE_HEADER,
                        "http://localhost:${hardhatContainer.mappedPort}"
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, Erc20BalanceRequestsResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(fetchResponse).withMessage()
                .isEqualTo(
                    Erc20BalanceRequestsResponse(
                        listOf(
                            Erc20BalanceRequestResponse(
                                id = id,
                                projectId = PROJECT_ID,
                                status = Status.SUCCESS,
                                chainId = PROJECT.chainId.value,
                                redirectUrl = "https://example.com/$id",
                                tokenAddress = tokenAddress.rawValue,
                                assetType = AssetType.TOKEN,
                                blockNumber = blockNumber.value,
                                walletAddress = walletAddress.rawValue,
                                arbitraryData = createResponse.arbitraryData,
                                screenConfig = ScreenConfig(
                                    beforeActionMessage = "before-action-message",
                                    afterActionMessage = "after-action-message"
                                ),
                                balance = BalanceResponse(
                                    wallet = walletAddress.rawValue,
                                    blockNumber = blockNumber.value,
                                    timestamp = fetchResponse.requests[0].balance!!.timestamp,
                                    amount = erc20balance.rawValue
                                ),
                                messageToSign = "Verification message ID to sign: $id",
                                signedMessage = signedMessage.value,
                                createdAt = fetchResponse.requests[0].createdAt
                            )
                        )
                    )
                )

            assertThat(fetchResponse.requests[0].createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustReturn404NotFoundForNonExistentErc20BalanceRequest() {
        verify("404 is returned for non-existent ERC20 balance request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/balance/${UUID.randomUUID()}")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    fun mustCorrectlyAttachSignedMessage() {
        val id = UUID.randomUUID()

        suppose("some ERC20 balance request without signed message exists in database") {
            erc20BalanceRequestRepository.store(
                StoreErc20BalanceRequestParams(
                    id = id,
                    projectId = PROJECT_ID,
                    chainId = Chain.HARDHAT_TESTNET.id,
                    redirectUrl = "https://example.com/$id",
                    tokenAddress = ContractAddress("a"),
                    blockNumber = BlockNumber(BigInteger.TEN),
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

        suppose("request to attach signed message to ERC20 balance request is made") {
            mockMvc.perform(
                MockMvcRequestBuilders.put("/v1/balance/$id")
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

        verify("signed message is correctly attached to ERC20 balance request") {
            val storedRequest = erc20BalanceRequestRepository.getById(id)

            assertThat(storedRequest?.actualWalletAddress)
                .isEqualTo(walletAddress)
            assertThat(storedRequest?.signedMessage)
                .isEqualTo(signedMessage)
        }
    }

    @Test
    fun mustReturn400BadRequestWhenSignedMessageIsNotAttached() {
        val id = UUID.randomUUID()
        val walletAddress = WalletAddress("c")
        val signedMessage = SignedMessage("signed-message")

        suppose("some ERC20 balance request with signed message exists in database") {
            erc20BalanceRequestRepository.store(
                StoreErc20BalanceRequestParams(
                    id = id,
                    projectId = PROJECT_ID,
                    chainId = Chain.HARDHAT_TESTNET.id,
                    redirectUrl = "https://example.com/$id",
                    tokenAddress = ContractAddress("a"),
                    blockNumber = BlockNumber(BigInteger.TEN),
                    requestedWalletAddress = WalletAddress("b"),
                    arbitraryData = TestData.EMPTY_JSON_OBJECT,
                    screenConfig = ScreenConfig(
                        beforeActionMessage = "before-action-message",
                        afterActionMessage = "after-action-message"
                    ),
                    createdAt = TestData.TIMESTAMP
                )
            )
            erc20BalanceRequestRepository.setSignedMessage(id, walletAddress, signedMessage)
        }

        verify("400 is returned when attaching signed message") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.put("/v1/balance/$id")
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
            val storedRequest = erc20BalanceRequestRepository.getById(id)

            assertThat(storedRequest?.actualWalletAddress)
                .isEqualTo(walletAddress)
            assertThat(storedRequest?.signedMessage)
                .isEqualTo(signedMessage)
        }
    }
}
