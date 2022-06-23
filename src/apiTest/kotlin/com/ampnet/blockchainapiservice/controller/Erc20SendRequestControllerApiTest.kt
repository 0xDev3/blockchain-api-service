package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.ControllerTestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.blockchain.SimpleERC20
import com.ampnet.blockchainapiservice.blockchain.properties.Chain
import com.ampnet.blockchainapiservice.config.binding.RpcUrlSpecResolver
import com.ampnet.blockchainapiservice.exception.ErrorCode
import com.ampnet.blockchainapiservice.generated.jooq.tables.ClientInfoTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.Erc20SendRequestTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ClientInfoRecord
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.params.StoreErc20SendRequestParams
import com.ampnet.blockchainapiservice.model.response.Erc20SendRequestResponse
import com.ampnet.blockchainapiservice.model.response.Erc20SendRequestsResponse
import com.ampnet.blockchainapiservice.model.response.TransactionResponse
import com.ampnet.blockchainapiservice.model.result.Erc20SendRequest
import com.ampnet.blockchainapiservice.repository.Erc20SendRequestRepository
import com.ampnet.blockchainapiservice.testcontainers.HardhatTestContainer
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ContractAddress
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

    private val accounts = HardhatTestContainer.accounts

    @Autowired
    private lateinit var erc20SendRequestRepository: Erc20SendRequestRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        dslContext.deleteFrom(ClientInfoTable.CLIENT_INFO).execute()
        dslContext.deleteFrom(Erc20SendRequestTable.ERC20_SEND_REQUEST).execute()
    }

    @Test
    fun mustCorrectlyCreateErc20SendRequestViaClientId() {
        val clientId = "client-id"
        val chainId = Chain.HARDHAT_TESTNET.id
        val redirectUrl = "https://example.com/\${id}"
        val tokenAddress = ContractAddress("cafebabe")

        suppose("some client info exists in database") {
            dslContext.insertInto(ClientInfoTable.CLIENT_INFO)
                .set(
                    ClientInfoRecord(
                        clientId = "client-id",
                        chainId = chainId,
                        sendRedirectUrl = redirectUrl,
                        balanceRedirectUrl = null,
                        lockRedirectUrl = null,
                        tokenAddress = tokenAddress
                    )
                )
                .execute()
        }

        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress("b")
        val recipientAddress = WalletAddress("c")

        val response = suppose("request to create ERC20 send request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "client_id": "$clientId",
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
                        status = Status.PENDING,
                        chainId = chainId.value,
                        tokenAddress = tokenAddress.rawValue,
                        amount = amount.rawValue,
                        senderAddress = senderAddress.rawValue,
                        recipientAddress = recipientAddress.rawValue,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        redirectUrl = redirectUrl.replace("\${id}", response.id.toString()),
                        sendTx = TransactionResponse(
                            txHash = null,
                            from = senderAddress.rawValue,
                            to = tokenAddress.rawValue,
                            data = response.sendTx.data,
                            blockConfirmations = null,
                            timestamp = null
                        )
                    )
                )
        }

        verify("ERC20 send request is correctly stored in database") {
            val storedRequest = erc20SendRequestRepository.getById(response.id)

            assertThat(storedRequest).withMessage()
                .isEqualTo(
                    Erc20SendRequest(
                        id = response.id,
                        chainId = chainId,
                        redirectUrl = redirectUrl.replace("\${id}", response.id.toString()),
                        tokenAddress = tokenAddress,
                        tokenAmount = amount,
                        tokenSenderAddress = senderAddress,
                        tokenRecipientAddress = recipientAddress,
                        txHash = null,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyCreateErc20SendRequestViaChainIdRedirectUrlAndTokenAddress() {
        val chainId = Chain.HARDHAT_TESTNET.id
        val redirectUrl = "https://example.com/\${id}"
        val tokenAddress = ContractAddress("a")
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress("b")
        val recipientAddress = WalletAddress("c")

        val response = suppose("request to create ERC20 send request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/send")
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

            objectMapper.readValue(response.response.contentAsString, Erc20SendRequestResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    Erc20SendRequestResponse(
                        id = response.id,
                        status = Status.PENDING,
                        chainId = chainId.value,
                        tokenAddress = tokenAddress.rawValue,
                        amount = amount.rawValue,
                        senderAddress = senderAddress.rawValue,
                        recipientAddress = recipientAddress.rawValue,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        redirectUrl = redirectUrl.replace("\${id}", response.id.toString()),
                        sendTx = TransactionResponse(
                            txHash = null,
                            from = senderAddress.rawValue,
                            to = tokenAddress.rawValue,
                            data = response.sendTx.data,
                            blockConfirmations = null,
                            timestamp = null
                        )
                    )
                )
        }

        verify("ERC20 send request is correctly stored in database") {
            val storedRequest = erc20SendRequestRepository.getById(response.id)

            assertThat(storedRequest).withMessage()
                .isEqualTo(
                    Erc20SendRequest(
                        id = response.id,
                        chainId = chainId,
                        redirectUrl = redirectUrl.replace("\${id}", response.id.toString()),
                        tokenAddress = tokenAddress,
                        tokenAmount = amount,
                        tokenSenderAddress = senderAddress,
                        tokenRecipientAddress = recipientAddress,
                        txHash = null,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        )
                    )
                )
        }
    }

    @Test
    fun mustReturn400BadRequestForNonExistentClientId() {
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress("b")
        val recipientAddress = WalletAddress("c")

        verify("400 is returned for non-existent clientId") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "client_id": "non-existent-client-id",
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
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.NON_EXISTENT_CLIENT_ID)
        }
    }

    @Test
    fun mustReturn400BadRequestForMissingChainId() {
        val redirectUrl = "https://example.com"
        val tokenAddress = ContractAddress("a")
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress("b")
        val recipientAddress = WalletAddress("c")

        verify("400 is returned for missing chainId") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/send")
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
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.INCOMPLETE_REQUEST)
        }
    }

    @Test
    fun mustReturn400BadRequestForMissingRedirectUrl() {
        val chainId = Chain.HARDHAT_TESTNET.id
        val tokenAddress = ContractAddress("a")
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress("b")
        val recipientAddress = WalletAddress("c")

        verify("400 is returned for missing redirectUrl") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "chain_id": ${chainId.value},
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
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.INCOMPLETE_REQUEST)
        }
    }

    @Test
    fun mustReturn400BadRequestForMissingTokenAddress() {
        val redirectUrl = "https://example.com"
        val chainId = Chain.HARDHAT_TESTNET.id
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress("b")
        val recipientAddress = WalletAddress("c")

        verify("400 is returned for missing redirectUrl") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "chain_id": ${chainId.value},
                                "redirect_url": "$redirectUrl",
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
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.INCOMPLETE_REQUEST)
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

        val chainId = Chain.HARDHAT_TESTNET.id
        val redirectUrl = "https://example.com/\${id}"
        val tokenAddress = ContractAddress(contract.contractAddress)
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress(mainAccount.address)
        val recipientAddress = WalletAddress(accounts[1].address)

        val createResponse = suppose("request to create ERC20 send request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/send")
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
                            blockConfirmations = fetchResponse.sendTx.blockConfirmations,
                            timestamp = fetchResponse.sendTx.timestamp
                        )
                    )
                )

            assertThat(fetchResponse.sendTx.blockConfirmations)
                .isNotZero()
            assertThat(fetchResponse.sendTx.timestamp)
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

        val chainId = Chain.HARDHAT_TESTNET.id
        val redirectUrl = "https://example.com/\${id}"
        val tokenAddress = ContractAddress(contract.contractAddress)
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress(mainAccount.address)
        val recipientAddress = WalletAddress(accounts[1].address)

        val createResponse = suppose("request to create ERC20 send request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/send")
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
                            blockConfirmations = fetchResponse.sendTx.blockConfirmations,
                            timestamp = fetchResponse.sendTx.timestamp
                        )
                    )
                )

            assertThat(fetchResponse.sendTx.blockConfirmations)
                .isNotZero()
            assertThat(fetchResponse.sendTx.timestamp)
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

        val chainId = Chain.HARDHAT_TESTNET.id
        val redirectUrl = "https://example.com/\${id}"
        val tokenAddress = ContractAddress(contract.contractAddress)
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress(mainAccount.address)
        val recipientAddress = WalletAddress(accounts[1].address)

        val createResponse = suppose("request to create ERC20 send request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/send")
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
                                )
                            )
                        )
                    )
                )

            assertThat(fetchResponse.requests[0].sendTx.blockConfirmations)
                .isNotZero()
            assertThat(fetchResponse.requests[0].sendTx.timestamp)
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
                                )
                            )
                        )
                    )
                )

            assertThat(fetchResponse.requests[0].sendTx.blockConfirmations)
                .isNotZero()
            assertThat(fetchResponse.requests[0].sendTx.timestamp)
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

        val chainId = Chain.HARDHAT_TESTNET.id
        val redirectUrl = "https://example.com/\${id}"
        val tokenAddress = ContractAddress(contract.contractAddress)
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress(mainAccount.address)
        val recipientAddress = WalletAddress(accounts[1].address)

        val createResponse = suppose("request to create ERC20 send request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/send")
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
                                )
                            )
                        )
                    )
                )

            assertThat(fetchResponse.requests[0].sendTx.blockConfirmations)
                .isNotZero()
            assertThat(fetchResponse.requests[0].sendTx.timestamp)
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

        val chainId = Chain.HARDHAT_TESTNET.id
        val redirectUrl = "https://example.com/\${id}"
        val tokenAddress = ContractAddress(contract.contractAddress)
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress(mainAccount.address)
        val recipientAddress = WalletAddress(accounts[1].address)

        val createResponse = suppose("request to create ERC20 send request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/send")
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
                                )
                            )
                        )
                    )
                )

            assertThat(fetchResponse.requests[0].sendTx.blockConfirmations)
                .isNotZero()
            assertThat(fetchResponse.requests[0].sendTx.timestamp)
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
                    )
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
                    )
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
