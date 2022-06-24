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
import com.ampnet.blockchainapiservice.generated.jooq.tables.ClientInfoTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.Erc20SendRequestTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.ProjectTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.UserIdentifierTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ApiKeyRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ClientInfoRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ProjectRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.UserIdentifierRecord
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.params.StoreErc20SendRequestParams
import com.ampnet.blockchainapiservice.model.response.Erc20SendRequestResponse
import com.ampnet.blockchainapiservice.model.response.Erc20SendRequestsResponse
import com.ampnet.blockchainapiservice.model.response.TransactionResponse
import com.ampnet.blockchainapiservice.model.result.Erc20SendRequest
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.repository.Erc20SendRequestRepository
import com.ampnet.blockchainapiservice.testcontainers.HardhatTestContainer
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.BaseUrl
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.DurationSeconds
import com.ampnet.blockchainapiservice.util.Status
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.web3j.tx.gas.DefaultGasProvider
import java.math.BigInteger
import java.util.UUID

class Erc20SendRequestControllerApiTest : ControllerTestBase() {

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
    private lateinit var erc20SendRequestRepository: Erc20SendRequestRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        dslContext.deleteFrom(Erc20SendRequestTable.ERC20_SEND_REQUEST).execute()
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
    fun mustCorrectlyCreateErc20SendRequest() {
        val tokenAddress = ContractAddress("cafebabe")
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress("b")
        val recipientAddress = WalletAddress("c")

        val response = suppose("request to create ERC20 send request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/send")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "amount": "${amount.rawValue}",
                                "sender_address": "${senderAddress.rawValue}",
                                "recipient_address": "${recipientAddress.rawValue}",
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

            objectMapper.readValue(response.response.contentAsString, Erc20SendRequestResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    Erc20SendRequestResponse(
                        id = response.id,
                        projectId = PROJECT_ID,
                        status = Status.PENDING,
                        chainId = PROJECT.chainId.value,
                        tokenAddress = tokenAddress.rawValue,
                        amount = amount.rawValue,
                        senderAddress = senderAddress.rawValue,
                        recipientAddress = recipientAddress.rawValue,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/send/${response.id}",
                        sendTx = TransactionResponse(
                            txHash = null,
                            from = senderAddress.rawValue,
                            to = tokenAddress.rawValue,
                            data = response.sendTx.data,
                            blockConfirmations = null,
                            timestamp = null
                        ),
                        createdAt = response.createdAt
                    )
                )

            assertThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("ERC20 send request is correctly stored in database") {
            val storedRequest = erc20SendRequestRepository.getById(response.id)

            assertThat(storedRequest).withMessage()
                .isEqualTo(
                    Erc20SendRequest(
                        id = response.id,
                        projectId = PROJECT_ID,
                        chainId = PROJECT.chainId,
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/send/${response.id}",
                        tokenAddress = tokenAddress,
                        tokenAmount = amount,
                        tokenSenderAddress = senderAddress,
                        tokenRecipientAddress = recipientAddress,
                        txHash = null,
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
    fun mustCorrectlyCreateErc20SendRequestWithRedirectUrl() {
        val tokenAddress = ContractAddress("cafebabe")
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress("b")
        val recipientAddress = WalletAddress("c")
        val redirectUrl = "https://custom-url/\${id}"

        val response = suppose("request to create ERC20 send request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/send")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "redirect_url": "$redirectUrl",
                                "token_address": "${tokenAddress.rawValue}",
                                "amount": "${amount.rawValue}",
                                "sender_address": "${senderAddress.rawValue}",
                                "recipient_address": "${recipientAddress.rawValue}",
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

            objectMapper.readValue(response.response.contentAsString, Erc20SendRequestResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    Erc20SendRequestResponse(
                        id = response.id,
                        projectId = PROJECT_ID,
                        status = Status.PENDING,
                        chainId = PROJECT.chainId.value,
                        tokenAddress = tokenAddress.rawValue,
                        amount = amount.rawValue,
                        senderAddress = senderAddress.rawValue,
                        recipientAddress = recipientAddress.rawValue,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        redirectUrl = "https://custom-url/${response.id}",
                        sendTx = TransactionResponse(
                            txHash = null,
                            from = senderAddress.rawValue,
                            to = tokenAddress.rawValue,
                            data = response.sendTx.data,
                            blockConfirmations = null,
                            timestamp = null
                        ),
                        createdAt = response.createdAt
                    )
                )

            assertThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("ERC20 send request is correctly stored in database") {
            val storedRequest = erc20SendRequestRepository.getById(response.id)

            assertThat(storedRequest).withMessage()
                .isEqualTo(
                    Erc20SendRequest(
                        id = response.id,
                        projectId = PROJECT_ID,
                        chainId = PROJECT.chainId,
                        redirectUrl = "https://custom-url/${response.id}",
                        tokenAddress = tokenAddress,
                        tokenAmount = amount,
                        tokenSenderAddress = senderAddress,
                        tokenRecipientAddress = recipientAddress,
                        txHash = null,
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
    fun mustReturn401UnauthorizedWhenCreatingErc20SendRequestWithInvalidApiKey() {
        val tokenAddress = ContractAddress("cafebabe")
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress("b")
        val recipientAddress = WalletAddress("c")

        verify("401 is returned for invalid API key") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/send")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, "invalid-api-key")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "amount": "${amount.rawValue}",
                                "sender_address": "${senderAddress.rawValue}",
                                "recipient_address": "${recipientAddress.rawValue}",
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
    fun mustCorrectlyFetchErc20SendRequest() {
        val mainAccount = accounts[0]

        val contract = suppose("simple ERC20 contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(mainAccount.address),
                listOf(BigInteger("10000")),
                mainAccount.address
            ).sendAndMine()
        }

        val tokenAddress = ContractAddress(contract.contractAddress)
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress(mainAccount.address)
        val recipientAddress = WalletAddress(accounts[1].address)

        val createResponse = suppose("request to create ERC20 send request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/send")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "amount": "${amount.rawValue}",
                                "sender_address": "${senderAddress.rawValue}",
                                "recipient_address": "${recipientAddress.rawValue}",
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

            objectMapper.readValue(createResponse.response.contentAsString, Erc20SendRequestResponse::class.java)
        }

        val txHash = suppose("some ERC20 transfer transaction is made without attached UUID") {
            contract.transferAndMine(recipientAddress, amount)
                ?.get()?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.waitAndMine()
        }

        suppose("transaction hash is attached to ERC20 send request") {
            erc20SendRequestRepository.setTxHash(createResponse.id, txHash)
        }

        val fetchResponse = suppose("request to fetch ERC20 send request is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/send/${createResponse.id}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, Erc20SendRequestResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(fetchResponse).withMessage()
                .isEqualTo(
                    Erc20SendRequestResponse(
                        id = createResponse.id,
                        projectId = PROJECT_ID,
                        status = Status.FAILED,
                        chainId = PROJECT.chainId.value,
                        tokenAddress = tokenAddress.rawValue,
                        amount = amount.rawValue,
                        senderAddress = senderAddress.rawValue,
                        recipientAddress = recipientAddress.rawValue,
                        arbitraryData = createResponse.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/send/${createResponse.id}",
                        sendTx = TransactionResponse(
                            txHash = txHash.value,
                            from = senderAddress.rawValue,
                            to = tokenAddress.rawValue,
                            data = createResponse.sendTx.data,
                            blockConfirmations = fetchResponse.sendTx.blockConfirmations,
                            timestamp = fetchResponse.sendTx.timestamp
                        ),
                        createdAt = fetchResponse.createdAt
                    )
                )

            assertThat(fetchResponse.sendTx.blockConfirmations)
                .isNotZero()
            assertThat(fetchResponse.sendTx.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            assertThat(fetchResponse.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchErc20SendRequestWhenCustomRpcUrlIsSpecified() {
        val mainAccount = accounts[0]

        val contract = suppose("simple ERC20 contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(mainAccount.address),
                listOf(BigInteger("10000")),
                mainAccount.address
            ).sendAndMine()
        }

        val tokenAddress = ContractAddress(contract.contractAddress)
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress(mainAccount.address)
        val recipientAddress = WalletAddress(accounts[1].address)

        val createResponse = suppose("request to create ERC20 send request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/send")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "amount": "${amount.rawValue}",
                                "sender_address": "${senderAddress.rawValue}",
                                "recipient_address": "${recipientAddress.rawValue}",
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

            objectMapper.readValue(createResponse.response.contentAsString, Erc20SendRequestResponse::class.java)
        }

        val txHash = suppose("some ERC20 transfer transaction is made without attached UUID") {
            contract.transferAndMine(recipientAddress, amount)
                ?.get()?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.waitAndMine()
        }

        suppose("transaction hash is attached to ERC20 send request") {
            erc20SendRequestRepository.setTxHash(createResponse.id, txHash)
        }

        val fetchResponse = suppose("request to fetch ERC20 send request is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/send/${createResponse.id}")
                    .header(
                        RpcUrlSpecResolver.RPC_URL_OVERRIDE_HEADER,
                        "http://localhost:${hardhatContainer.mappedPort}"
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, Erc20SendRequestResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(fetchResponse).withMessage()
                .isEqualTo(
                    Erc20SendRequestResponse(
                        id = createResponse.id,
                        projectId = PROJECT_ID,
                        status = Status.FAILED,
                        chainId = PROJECT.chainId.value,
                        tokenAddress = tokenAddress.rawValue,
                        amount = amount.rawValue,
                        senderAddress = senderAddress.rawValue,
                        recipientAddress = recipientAddress.rawValue,
                        arbitraryData = createResponse.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/send/${createResponse.id}",
                        sendTx = TransactionResponse(
                            txHash = txHash.value,
                            from = senderAddress.rawValue,
                            to = tokenAddress.rawValue,
                            data = createResponse.sendTx.data,
                            blockConfirmations = fetchResponse.sendTx.blockConfirmations,
                            timestamp = fetchResponse.sendTx.timestamp
                        ),
                        createdAt = fetchResponse.createdAt
                    )
                )

            assertThat(fetchResponse.sendTx.blockConfirmations)
                .isNotZero()
            assertThat(fetchResponse.sendTx.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            assertThat(fetchResponse.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustReturn404NotFoundForNonExistentErc20SendRequest() {
        verify("404 is returned for non-existent ERC20 send request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/send/${UUID.randomUUID()}")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    fun mustCorrectlyFetchErc20SendRequestsBySenderAddress() {
        val mainAccount = accounts[0]

        val contract = suppose("simple ERC20 contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(mainAccount.address),
                listOf(BigInteger("10000")),
                mainAccount.address
            ).sendAndMine()
        }

        val tokenAddress = ContractAddress(contract.contractAddress)
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress(mainAccount.address)
        val recipientAddress = WalletAddress(accounts[1].address)

        val createResponse = suppose("request to create ERC20 send request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/send")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "amount": "${amount.rawValue}",
                                "sender_address": "${senderAddress.rawValue}",
                                "recipient_address": "${recipientAddress.rawValue}",
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

            objectMapper.readValue(createResponse.response.contentAsString, Erc20SendRequestResponse::class.java)
        }

        val txHash = suppose("some ERC20 transfer transaction is made without attached UUID") {
            contract.transferAndMine(recipientAddress, amount)
                ?.get()?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.waitAndMine()
        }

        suppose("transaction hash is attached to ERC20 send request") {
            erc20SendRequestRepository.setTxHash(createResponse.id, txHash)
        }

        val fetchResponse = suppose("request to fetch ERC20 send requests by sender address is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/send/by-sender/${createResponse.senderAddress}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, Erc20SendRequestsResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(fetchResponse).withMessage()
                .isEqualTo(
                    Erc20SendRequestsResponse(
                        listOf(
                            Erc20SendRequestResponse(
                                id = createResponse.id,
                                projectId = PROJECT_ID,
                                status = Status.FAILED,
                                chainId = PROJECT.chainId.value,
                                tokenAddress = tokenAddress.rawValue,
                                amount = amount.rawValue,
                                senderAddress = senderAddress.rawValue,
                                recipientAddress = recipientAddress.rawValue,
                                arbitraryData = createResponse.arbitraryData,
                                screenConfig = ScreenConfig(
                                    beforeActionMessage = "before-action-message",
                                    afterActionMessage = "after-action-message"
                                ),
                                redirectUrl = PROJECT.baseRedirectUrl.value + "/send/${createResponse.id}",
                                sendTx = TransactionResponse(
                                    txHash = txHash.value,
                                    from = senderAddress.rawValue,
                                    to = tokenAddress.rawValue,
                                    data = createResponse.sendTx.data,
                                    blockConfirmations = fetchResponse.requests[0].sendTx.blockConfirmations,
                                    timestamp = fetchResponse.requests[0].sendTx.timestamp
                                ),
                                createdAt = createResponse.createdAt
                            )
                        )
                    )
                )

            assertThat(fetchResponse.requests[0].sendTx.blockConfirmations)
                .isNotZero()
            assertThat(fetchResponse.requests[0].sendTx.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            assertThat(fetchResponse.requests[0].createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchErc20SendRequestsBySenderAddressWhenCustomRpcUrlIsSpecified() {
        val mainAccount = accounts[0]

        val contract = suppose("simple ERC20 contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(mainAccount.address),
                listOf(BigInteger("10000")),
                mainAccount.address
            ).sendAndMine()
        }

        val chainId = Chain.HARDHAT_TESTNET.id
        val redirectUrl = "https://example.com/\${id}"
        val tokenAddress = ContractAddress(contract.contractAddress)
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress(mainAccount.address)
        val recipientAddress = WalletAddress(accounts[1].address)

        val createResponse = suppose("request to create ERC20 send request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/send")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "chain_id": ${chainId.value},
                                "redirect_url": "$redirectUrl",
                                "token_address": "${tokenAddress.rawValue}",
                                "amount": "${amount.rawValue}",
                                "sender_address": "${senderAddress.rawValue}",
                                "recipient_address": "${recipientAddress.rawValue}",
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

            objectMapper.readValue(createResponse.response.contentAsString, Erc20SendRequestResponse::class.java)
        }

        val txHash = suppose("some ERC20 transfer transaction is made without attached UUID") {
            contract.transferAndMine(recipientAddress, amount)
                ?.get()?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.waitAndMine()
        }

        suppose("transaction hash is attached to ERC20 send request") {
            erc20SendRequestRepository.setTxHash(createResponse.id, txHash)
        }

        val fetchResponse = suppose("request to fetch ERC20 send request by sender address is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/send/by-sender/${createResponse.senderAddress}")
                    .header(
                        RpcUrlSpecResolver.RPC_URL_OVERRIDE_HEADER,
                        "http://localhost:${hardhatContainer.mappedPort}"
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, Erc20SendRequestsResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(fetchResponse).withMessage()
                .isEqualTo(
                    Erc20SendRequestsResponse(
                        listOf(
                            Erc20SendRequestResponse(
                                id = createResponse.id,
                                projectId = PROJECT_ID,
                                status = Status.FAILED,
                                chainId = chainId.value,
                                tokenAddress = tokenAddress.rawValue,
                                amount = amount.rawValue,
                                senderAddress = senderAddress.rawValue,
                                recipientAddress = recipientAddress.rawValue,
                                arbitraryData = createResponse.arbitraryData,
                                screenConfig = ScreenConfig(
                                    beforeActionMessage = "before-action-message",
                                    afterActionMessage = "after-action-message"
                                ),
                                redirectUrl = redirectUrl.replace("\${id}", createResponse.id.toString()),
                                sendTx = TransactionResponse(
                                    txHash = txHash.value,
                                    from = senderAddress.rawValue,
                                    to = tokenAddress.rawValue,
                                    data = createResponse.sendTx.data,
                                    blockConfirmations = fetchResponse.requests[0].sendTx.blockConfirmations,
                                    timestamp = fetchResponse.requests[0].sendTx.timestamp
                                ),
                                createdAt = createResponse.createdAt
                            )
                        )
                    )
                )

            assertThat(fetchResponse.requests[0].sendTx.blockConfirmations)
                .isNotZero()
            assertThat(fetchResponse.requests[0].sendTx.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            assertThat(fetchResponse.requests[0].createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchErc20SendRequestsByRecipientAddress() {
        val mainAccount = accounts[0]

        val contract = suppose("simple ERC20 contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(mainAccount.address),
                listOf(BigInteger("10000")),
                mainAccount.address
            ).sendAndMine()
        }

        val tokenAddress = ContractAddress(contract.contractAddress)
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress(mainAccount.address)
        val recipientAddress = WalletAddress(accounts[1].address)

        val createResponse = suppose("request to create ERC20 send request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/send")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "amount": "${amount.rawValue}",
                                "sender_address": "${senderAddress.rawValue}",
                                "recipient_address": "${recipientAddress.rawValue}",
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

            objectMapper.readValue(createResponse.response.contentAsString, Erc20SendRequestResponse::class.java)
        }

        val txHash = suppose("some ERC20 transfer transaction is made without attached UUID") {
            contract.transferAndMine(recipientAddress, amount)
                ?.get()?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.waitAndMine()
        }

        suppose("transaction hash is attached to ERC20 send request") {
            erc20SendRequestRepository.setTxHash(createResponse.id, txHash)
        }

        val fetchResponse = suppose("request to fetch ERC20 send requests by recipient address is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/send/by-recipient/${createResponse.recipientAddress}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, Erc20SendRequestsResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(fetchResponse).withMessage()
                .isEqualTo(
                    Erc20SendRequestsResponse(
                        listOf(
                            Erc20SendRequestResponse(
                                id = createResponse.id,
                                projectId = PROJECT_ID,
                                status = Status.FAILED,
                                chainId = PROJECT.chainId.value,
                                tokenAddress = tokenAddress.rawValue,
                                amount = amount.rawValue,
                                senderAddress = senderAddress.rawValue,
                                recipientAddress = recipientAddress.rawValue,
                                arbitraryData = createResponse.arbitraryData,
                                screenConfig = ScreenConfig(
                                    beforeActionMessage = "before-action-message",
                                    afterActionMessage = "after-action-message"
                                ),
                                redirectUrl = PROJECT.baseRedirectUrl.value + "/send/${createResponse.id}",
                                sendTx = TransactionResponse(
                                    txHash = txHash.value,
                                    from = senderAddress.rawValue,
                                    to = tokenAddress.rawValue,
                                    data = createResponse.sendTx.data,
                                    blockConfirmations = fetchResponse.requests[0].sendTx.blockConfirmations,
                                    timestamp = fetchResponse.requests[0].sendTx.timestamp
                                ),
                                createdAt = createResponse.createdAt
                            )
                        )
                    )
                )

            assertThat(fetchResponse.requests[0].sendTx.blockConfirmations)
                .isNotZero()
            assertThat(fetchResponse.requests[0].sendTx.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            assertThat(fetchResponse.requests[0].createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchErc20SendRequestsByRecipientAddressWhenCustomRpcUrlIsSpecified() {
        val mainAccount = accounts[0]

        val contract = suppose("simple ERC20 contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(mainAccount.address),
                listOf(BigInteger("10000")),
                mainAccount.address
            ).sendAndMine()
        }

        val tokenAddress = ContractAddress(contract.contractAddress)
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress(mainAccount.address)
        val recipientAddress = WalletAddress(accounts[1].address)

        val createResponse = suppose("request to create ERC20 send request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/send")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "amount": "${amount.rawValue}",
                                "sender_address": "${senderAddress.rawValue}",
                                "recipient_address": "${recipientAddress.rawValue}",
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

            objectMapper.readValue(createResponse.response.contentAsString, Erc20SendRequestResponse::class.java)
        }

        val txHash = suppose("some ERC20 transfer transaction is made without attached UUID") {
            contract.transferAndMine(recipientAddress, amount)
                ?.get()?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.waitAndMine()
        }

        suppose("transaction hash is attached to ERC20 send request") {
            erc20SendRequestRepository.setTxHash(createResponse.id, txHash)
        }

        val fetchResponse = suppose("request to fetch ERC20 send request by recipient address is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/send/by-recipient/${createResponse.recipientAddress}")
                    .header(
                        RpcUrlSpecResolver.RPC_URL_OVERRIDE_HEADER,
                        "http://localhost:${hardhatContainer.mappedPort}"
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, Erc20SendRequestsResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(fetchResponse).withMessage()
                .isEqualTo(
                    Erc20SendRequestsResponse(
                        listOf(
                            Erc20SendRequestResponse(
                                id = createResponse.id,
                                projectId = PROJECT_ID,
                                status = Status.FAILED,
                                chainId = PROJECT.chainId.value,
                                tokenAddress = tokenAddress.rawValue,
                                amount = amount.rawValue,
                                senderAddress = senderAddress.rawValue,
                                recipientAddress = recipientAddress.rawValue,
                                arbitraryData = createResponse.arbitraryData,
                                screenConfig = ScreenConfig(
                                    beforeActionMessage = "before-action-message",
                                    afterActionMessage = "after-action-message"
                                ),
                                redirectUrl = PROJECT.baseRedirectUrl.value + "/send/${createResponse.id}",
                                sendTx = TransactionResponse(
                                    txHash = txHash.value,
                                    from = senderAddress.rawValue,
                                    to = tokenAddress.rawValue,
                                    data = createResponse.sendTx.data,
                                    blockConfirmations = fetchResponse.requests[0].sendTx.blockConfirmations,
                                    timestamp = fetchResponse.requests[0].sendTx.timestamp
                                ),
                                createdAt = fetchResponse.requests[0].createdAt
                            )
                        )
                    )
                )

            assertThat(fetchResponse.requests[0].sendTx.blockConfirmations)
                .isNotZero()
            assertThat(fetchResponse.requests[0].sendTx.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            assertThat(fetchResponse.requests[0].createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyAttachTransactionHash() {
        val id = UUID.randomUUID()

        suppose("some ERC20 send request without transaction hash exists in database") {
            erc20SendRequestRepository.store(
                StoreErc20SendRequestParams(
                    id = id,
                    projectId = PROJECT_ID,
                    chainId = Chain.HARDHAT_TESTNET.id,
                    redirectUrl = "https://example.com/$id",
                    tokenAddress = ContractAddress("a"),
                    tokenAmount = Balance(BigInteger.TEN),
                    tokenSenderAddress = WalletAddress("b"),
                    tokenRecipientAddress = WalletAddress("c"),
                    arbitraryData = TestData.EMPTY_JSON_OBJECT,
                    screenConfig = ScreenConfig(
                        beforeActionMessage = "before-action-message",
                        afterActionMessage = "after-action-message"
                    ),
                    createdAt = TestData.TIMESTAMP
                )
            )
        }

        val txHash = TransactionHash("tx-hash")

        suppose("request to attach transaction hash to ERC20 send request is made") {
            mockMvc.perform(
                MockMvcRequestBuilders.put("/v1/send/$id")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "tx_hash": "${txHash.value}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
        }

        verify("transaction hash is correctly attached to ERC20 send request") {
            val storedRequest = erc20SendRequestRepository.getById(id)

            assertThat(storedRequest?.txHash)
                .isEqualTo(txHash)
        }
    }

    @Test
    fun mustReturn400BadRequestWhenTransactionHashIsNotAttached() {
        val id = UUID.randomUUID()
        val txHash = TransactionHash("tx-hash")

        suppose("some ERC20 send request with transaction hash exists in database") {
            erc20SendRequestRepository.store(
                StoreErc20SendRequestParams(
                    id = id,
                    projectId = PROJECT_ID,
                    chainId = Chain.HARDHAT_TESTNET.id,
                    redirectUrl = "https://example.com/$id",
                    tokenAddress = ContractAddress("a"),
                    tokenAmount = Balance(BigInteger.TEN),
                    tokenSenderAddress = WalletAddress("b"),
                    tokenRecipientAddress = WalletAddress("c"),
                    arbitraryData = TestData.EMPTY_JSON_OBJECT,
                    screenConfig = ScreenConfig(
                        beforeActionMessage = "before-action-message",
                        afterActionMessage = "after-action-message"
                    ),
                    createdAt = TestData.TIMESTAMP
                )
            )
            erc20SendRequestRepository.setTxHash(id, txHash)
        }

        verify("400 is returned when attaching transaction hash") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.put("/v1/send/$id")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "tx_hash": "different-tx-hash"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.TX_HASH_ALREADY_SET)
        }

        verify("transaction hash is not changed in database") {
            val storedRequest = erc20SendRequestRepository.getById(id)

            assertThat(storedRequest?.txHash)
                .isEqualTo(txHash)
        }
    }
}
