package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.ControllerTestBase
import com.ampnet.blockchainapiservice.blockchain.SimpleERC20
import com.ampnet.blockchainapiservice.blockchain.properties.Chain
import com.ampnet.blockchainapiservice.config.binding.RpcUrlSpecResolver
import com.ampnet.blockchainapiservice.exception.ErrorCode
import com.ampnet.blockchainapiservice.generated.jooq.tables.ClientInfoTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.Erc20BalanceRequestTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ClientInfoRecord
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.params.StoreErc20BalanceRequestParams
import com.ampnet.blockchainapiservice.model.response.BalanceResponse
import com.ampnet.blockchainapiservice.model.response.Erc20BalanceRequestResponse
import com.ampnet.blockchainapiservice.model.result.Erc20BalanceRequest
import com.ampnet.blockchainapiservice.repository.Erc20BalanceRequestRepository
import com.ampnet.blockchainapiservice.testcontainers.HardhatTestContainer
import com.ampnet.blockchainapiservice.util.Balance
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
import org.web3j.tx.gas.DefaultGasProvider
import java.math.BigInteger
import java.util.UUID

class Erc20BalanceRequestControllerApiTest : ControllerTestBase() {

    private val accounts = HardhatTestContainer.accounts

    @Autowired
    private lateinit var erc20BalanceRequestRepository: Erc20BalanceRequestRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        dslContext.deleteFrom(ClientInfoTable.CLIENT_INFO).execute()
        dslContext.deleteFrom(Erc20BalanceRequestTable.ERC20_BALANCE_REQUEST).execute()
    }

    @Test
    fun mustCorrectlyCreateErc20BalanceRequestViaClientId() {
        val clientId = "client-id"
        val chainId = Chain.HARDHAT_TESTNET.id
        val redirectUrl = "https://example.com/\${id}"
        val tokenAddress = ContractAddress("cafebabe")

        suppose("some client info exists in database") {
            dslContext.insertInto(ClientInfoTable.CLIENT_INFO)
                .set(
                    ClientInfoRecord(
                        clientId = "client-id",
                        chainId = chainId.value,
                        sendRedirectUrl = null,
                        balanceRedirectUrl = redirectUrl,
                        tokenAddress = tokenAddress.rawValue
                    )
                )
                .execute()
        }

        val blockNumber = BlockNumber(BigInteger.TEN)
        val walletAddress = WalletAddress("a")

        val response = suppose("request to create ERC20 balance request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/balance")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "client_id": "$clientId",
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
                        status = Status.PENDING,
                        chainId = chainId.value,
                        redirectUrl = redirectUrl.replace("\${id}", response.id.toString()),
                        tokenAddress = tokenAddress.rawValue,
                        blockNumber = blockNumber.value,
                        walletAddress = walletAddress.rawValue,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        balance = null,
                        messageToSign = "Verification message ID to sign: ${response.id}",
                        signedMessage = null
                    )
                )
        }

        verify("ERC20 balance request is correctly stored in database") {
            val storedRequest = erc20BalanceRequestRepository.getById(response.id)

            assertThat(storedRequest).withMessage()
                .isEqualTo(
                    Erc20BalanceRequest(
                        id = response.id,
                        chainId = chainId,
                        redirectUrl = redirectUrl.replace("\${id}", response.id.toString()),
                        tokenAddress = tokenAddress,
                        blockNumber = blockNumber,
                        requestedWalletAddress = walletAddress,
                        actualWalletAddress = null,
                        signedMessage = null,
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
    fun mustCorrectlyCreateErc20BalanceRequestViaChainIdRedirectUrlAndTokenAddress() {
        val chainId = Chain.HARDHAT_TESTNET.id
        val redirectUrl = "https://example.com/\${id}"
        val tokenAddress = ContractAddress("cafebabe")
        val blockNumber = BlockNumber(BigInteger.TEN)
        val walletAddress = WalletAddress("a")

        val response = suppose("request to create ERC20 balance request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/balance")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "chain_id": ${chainId.value},
                                "redirect_url": "$redirectUrl",
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
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, Erc20BalanceRequestResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    Erc20BalanceRequestResponse(
                        id = response.id,
                        status = Status.PENDING,
                        chainId = chainId.value,
                        redirectUrl = redirectUrl.replace("\${id}", response.id.toString()),
                        tokenAddress = tokenAddress.rawValue,
                        blockNumber = blockNumber.value,
                        walletAddress = walletAddress.rawValue,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        balance = null,
                        messageToSign = "Verification message ID to sign: ${response.id}",
                        signedMessage = null
                    )
                )
        }

        verify("ERC20 balance request is correctly stored in database") {
            val storedRequest = erc20BalanceRequestRepository.getById(response.id)

            assertThat(storedRequest).withMessage()
                .isEqualTo(
                    Erc20BalanceRequest(
                        id = response.id,
                        chainId = chainId,
                        redirectUrl = redirectUrl.replace("\${id}", response.id.toString()),
                        tokenAddress = tokenAddress,
                        blockNumber = blockNumber,
                        requestedWalletAddress = walletAddress,
                        actualWalletAddress = null,
                        signedMessage = null,
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
        val blockNumber = BlockNumber(BigInteger.TEN)
        val walletAddress = WalletAddress("a")

        verify("400 is returned for non-existent clientId") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/balance")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "client_id": "non-existent-client-id",
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

            verifyResponseErrorCode(response, ErrorCode.NON_EXISTENT_CLIENT_ID)
        }
    }

    @Test
    fun mustReturn400BadRequestForMissingChainId() {
        val redirectUrl = "https://example.com"
        val tokenAddress = ContractAddress("a")
        val blockNumber = BlockNumber(BigInteger.TEN)
        val walletAddress = WalletAddress("a")

        verify("400 is returned for non-existent clientId") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/balance")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "redirect_url": "$redirectUrl",
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
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.INCOMPLETE_REQUEST)
        }
    }

    @Test
    fun mustReturn400BadRequestForMissingRedirectUrl() {
        val chainId = Chain.HARDHAT_TESTNET.id
        val tokenAddress = ContractAddress("a")
        val blockNumber = BlockNumber(BigInteger.TEN)
        val walletAddress = WalletAddress("a")

        verify("400 is returned for non-existent clientId") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/balance")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "chain_id": ${chainId.value},
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
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.INCOMPLETE_REQUEST)
        }
    }

    @Test
    fun mustReturn400BadRequestForMissingTokenAddress() {
        val redirectUrl = "https://example.com"
        val chainId = Chain.HARDHAT_TESTNET.id
        val blockNumber = BlockNumber(BigInteger.TEN)
        val walletAddress = WalletAddress("a")

        verify("400 is returned for non-existent clientId") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/balance")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "chain_id": ${chainId.value},
                                "redirect_url": "$redirectUrl",
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

            verifyResponseErrorCode(response, ErrorCode.INCOMPLETE_REQUEST)
        }
    }

    @Test
    fun mustCorrectlyFetchErc20BalanceRequest() {
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

        val chainId = Chain.HARDHAT_TESTNET.id
        val redirectUrl = "https://example.com/\${id}"
        val tokenAddress = ContractAddress(contract.contractAddress)
        val blockNumber = hardhatContainer.blockNumber()

        val createResponse = suppose("request to create ERC20 balance request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/balance")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "chain_id": ${chainId.value},
                                "redirect_url": "$redirectUrl",
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
                MockMvcRequestBuilders.get("/balance/$id")
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
                        status = Status.SUCCESS,
                        chainId = chainId.value,
                        redirectUrl = redirectUrl.replace("\${id}", id.toString()),
                        tokenAddress = tokenAddress.rawValue,
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
                        signedMessage = signedMessage.value
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchErc20BalanceRequestWhenCustomRpcUrlIsSpecified() {
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

        val chainId = Chain.HARDHAT_TESTNET.id
        val redirectUrl = "https://example.com/\${id}"
        val tokenAddress = ContractAddress(contract.contractAddress)
        val blockNumber = hardhatContainer.blockNumber()

        val createResponse = suppose("request to create ERC20 balance request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/balance")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "chain_id": ${chainId.value},
                                "redirect_url": "$redirectUrl",
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
                MockMvcRequestBuilders.get("/balance/$id")
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
                        status = Status.SUCCESS,
                        chainId = chainId.value,
                        redirectUrl = redirectUrl.replace("\${id}", id.toString()),
                        tokenAddress = tokenAddress.rawValue,
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
                        signedMessage = signedMessage.value
                    )
                )
        }
    }

    @Test
    fun mustReturn404NotFoundForNonExistentErc20BalanceRequest() {
        verify("404 is returned for non-existent ERC20 balance request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/balance/${UUID.randomUUID()}")
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
                    chainId = Chain.HARDHAT_TESTNET.id,
                    redirectUrl = "https://example.com/$id",
                    tokenAddress = ContractAddress("a"),
                    blockNumber = BlockNumber(BigInteger.TEN),
                    requestedWalletAddress = WalletAddress("b"),
                    arbitraryData = null,
                    screenConfig = ScreenConfig(
                        beforeActionMessage = "before-action-message",
                        afterActionMessage = "after-action-message"
                    )
                )
            )
        }

        val walletAddress = WalletAddress("c")
        val signedMessage = SignedMessage("signed-message")

        suppose("request to attach signed message to ERC20 balance request is made") {
            mockMvc.perform(
                MockMvcRequestBuilders.put("/balance/$id")
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
                    chainId = Chain.HARDHAT_TESTNET.id,
                    redirectUrl = "https://example.com/$id",
                    tokenAddress = ContractAddress("a"),
                    blockNumber = BlockNumber(BigInteger.TEN),
                    requestedWalletAddress = WalletAddress("b"),
                    arbitraryData = null,
                    screenConfig = ScreenConfig(
                        beforeActionMessage = "before-action-message",
                        afterActionMessage = "after-action-message"
                    )
                )
            )
            erc20BalanceRequestRepository.setSignedMessage(id, walletAddress, signedMessage)
        }

        verify("400 is returned when attaching signed message") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.put("/balance/$id")
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
