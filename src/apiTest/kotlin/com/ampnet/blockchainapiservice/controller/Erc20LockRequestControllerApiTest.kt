package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.ControllerTestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.blockchain.SimpleERC20
import com.ampnet.blockchainapiservice.blockchain.properties.Chain
import com.ampnet.blockchainapiservice.config.binding.RpcUrlSpecResolver
import com.ampnet.blockchainapiservice.exception.ErrorCode
import com.ampnet.blockchainapiservice.generated.jooq.tables.ClientInfoTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.Erc20LockRequestTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ClientInfoRecord
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.params.StoreErc20LockRequestParams
import com.ampnet.blockchainapiservice.model.response.Erc20LockRequestResponse
import com.ampnet.blockchainapiservice.model.response.TransactionResponse
import com.ampnet.blockchainapiservice.model.result.Erc20LockRequest
import com.ampnet.blockchainapiservice.repository.Erc20LockRequestRepository
import com.ampnet.blockchainapiservice.testcontainers.HardhatTestContainer
import com.ampnet.blockchainapiservice.util.Balance
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

class Erc20LockRequestControllerApiTest : ControllerTestBase() { // TODO write docs

    private val accounts = HardhatTestContainer.accounts

    @Autowired
    private lateinit var erc20LockRequestRepository: Erc20LockRequestRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        dslContext.deleteFrom(ClientInfoTable.CLIENT_INFO).execute()
        dslContext.deleteFrom(Erc20LockRequestTable.ERC20_LOCK_REQUEST).execute()
    }

    @Test
    fun mustCorrectlyCreateErc20LockRequestViaClientId() {
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
                        sendRedirectUrl = null,
                        balanceRedirectUrl = null,
                        lockRedirectUrl = redirectUrl,
                        tokenAddress = tokenAddress
                    )
                )
                .execute()
        }

        val amount = Balance(BigInteger.TEN)
        val lockDuration = DurationSeconds(BigInteger.valueOf(100L))
        val lockContractAddress = ContractAddress("b")
        val senderAddress = WalletAddress("c")

        val response = suppose("request to create ERC20 lock request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/lock")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "client_id": "$clientId",
                                "amount": "${amount.rawValue}",
                                "lock_duration_in_seconds": "${lockDuration.rawValue}",
                                "lock_contract_address": "${lockContractAddress.rawValue}",
                                "sender_address": "${senderAddress.rawValue}",
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

            objectMapper.readValue(response.response.contentAsString, Erc20LockRequestResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    Erc20LockRequestResponse(
                        id = response.id,
                        status = Status.PENDING,
                        chainId = chainId.value,
                        tokenAddress = tokenAddress.rawValue,
                        amount = amount.rawValue,
                        lockDurationInSeconds = lockDuration.rawValue,
                        unlocksAt = null,
                        lockContractAddress = lockContractAddress.rawValue,
                        senderAddress = senderAddress.rawValue,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        redirectUrl = redirectUrl.replace("\${id}", response.id.toString()),
                        lockTx = TransactionResponse(
                            txHash = null,
                            from = senderAddress.rawValue,
                            to = tokenAddress.rawValue,
                            data = response.lockTx.data,
                            blockConfirmations = null,
                            timestamp = null
                        )
                    )
                )
        }

        verify("ERC20 lock request is correctly stored in database") {
            val storedRequest = erc20LockRequestRepository.getById(response.id)

            assertThat(storedRequest).withMessage()
                .isEqualTo(
                    Erc20LockRequest(
                        id = response.id,
                        chainId = chainId,
                        redirectUrl = redirectUrl.replace("\${id}", response.id.toString()),
                        tokenAddress = tokenAddress,
                        tokenAmount = amount,
                        lockDuration = lockDuration,
                        lockContractAddress = lockContractAddress,
                        tokenSenderAddress = senderAddress,
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
    fun mustCorrectlyCreateErc20LockRequestViaChainIdRedirectUrlAndTokenAddress() {
        val chainId = Chain.HARDHAT_TESTNET.id
        val redirectUrl = "https://example.com/\${id}"
        val tokenAddress = ContractAddress("a")
        val amount = Balance(BigInteger.TEN)
        val lockDuration = DurationSeconds(BigInteger.valueOf(100L))
        val lockContractAddress = ContractAddress("b")
        val senderAddress = WalletAddress("c")

        val response = suppose("request to create ERC20 lock request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/lock")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "chain_id": ${chainId.value},
                                "redirect_url": "$redirectUrl",
                                "token_address": "${tokenAddress.rawValue}",
                                "amount": "${amount.rawValue}",
                                "lock_duration_in_seconds": "${lockDuration.rawValue}",
                                "lock_contract_address": "${lockContractAddress.rawValue}",
                                "sender_address": "${senderAddress.rawValue}",
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

            objectMapper.readValue(response.response.contentAsString, Erc20LockRequestResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    Erc20LockRequestResponse(
                        id = response.id,
                        status = Status.PENDING,
                        chainId = chainId.value,
                        tokenAddress = tokenAddress.rawValue,
                        amount = amount.rawValue,
                        lockDurationInSeconds = lockDuration.rawValue,
                        unlocksAt = null,
                        lockContractAddress = lockContractAddress.rawValue,
                        senderAddress = senderAddress.rawValue,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        redirectUrl = redirectUrl.replace("\${id}", response.id.toString()),
                        lockTx = TransactionResponse(
                            txHash = null,
                            from = senderAddress.rawValue,
                            to = tokenAddress.rawValue,
                            data = response.lockTx.data,
                            blockConfirmations = null,
                            timestamp = null
                        )
                    )
                )
        }

        verify("ERC20 lock request is correctly stored in database") {
            val storedRequest = erc20LockRequestRepository.getById(response.id)

            assertThat(storedRequest).withMessage()
                .isEqualTo(
                    Erc20LockRequest(
                        id = response.id,
                        chainId = chainId,
                        redirectUrl = redirectUrl.replace("\${id}", response.id.toString()),
                        tokenAddress = tokenAddress,
                        tokenAmount = amount,
                        lockDuration = lockDuration,
                        lockContractAddress = lockContractAddress,
                        tokenSenderAddress = senderAddress,
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
        val lockDuration = DurationSeconds(BigInteger.valueOf(100L))
        val lockContractAddress = ContractAddress("b")
        val senderAddress = WalletAddress("c")

        verify("400 is returned for non-existent clientId") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/lock")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "client_id": "non-existent-client-id",
                                "amount": "${amount.rawValue}",
                                "lock_duration_in_seconds": "${lockDuration.rawValue}",
                                "lock_contract_address": "${lockContractAddress.rawValue}",
                                "sender_address": "${senderAddress.rawValue}",
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
        val lockDuration = DurationSeconds(BigInteger.valueOf(100L))
        val lockContractAddress = ContractAddress("b")
        val senderAddress = WalletAddress("c")

        verify("400 is returned for missing chainId") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/lock")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "redirect_url": "$redirectUrl",
                                "token_address": "${tokenAddress.rawValue}",
                                "amount": "${amount.rawValue}",
                                "lock_duration_in_seconds": "${lockDuration.rawValue}",
                                "lock_contract_address": "${lockContractAddress.rawValue}",
                                "sender_address": "${senderAddress.rawValue}",
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
        val lockDuration = DurationSeconds(BigInteger.valueOf(100L))
        val lockContractAddress = ContractAddress("b")
        val senderAddress = WalletAddress("c")

        verify("400 is returned for missing redirectUrl") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/lock")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "chain_id": ${chainId.value},
                                "token_address": "${tokenAddress.rawValue}",
                                "amount": "${amount.rawValue}",
                                "lock_duration_in_seconds": "${lockDuration.rawValue}",
                                "lock_contract_address": "${lockContractAddress.rawValue}",
                                "sender_address": "${senderAddress.rawValue}",
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
        val lockDuration = DurationSeconds(BigInteger.valueOf(100L))
        val lockContractAddress = ContractAddress("b")
        val senderAddress = WalletAddress("c")

        verify("400 is returned for missing redirectUrl") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/lock")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "chain_id": ${chainId.value},
                                "redirect_url": "$redirectUrl",
                                "amount": "${amount.rawValue}",
                                "lock_duration_in_seconds": "${lockDuration.rawValue}",
                                "lock_contract_address": "${lockContractAddress.rawValue}",
                                "sender_address": "${senderAddress.rawValue}",
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

    // TODO implement after actual lock contract is known
    // @Test
//    fun mustCorrectlyFetchErc20LockRequest() {
//        val mainAccount = accounts[0]
//
//        val contract = suppose("simple ERC20 contract is deployed") {
//            SimpleERC20.deploy(
//                hardhatContainer.web3j,
//                mainAccount,
//                DefaultGasProvider(),
//                listOf(mainAccount.address),
//                listOf(BigInteger("10000")),
//                mainAccount.address
//            ).sendAndMine()
//        }
//
//        val chainId = Chain.HARDHAT_TESTNET.id
//        val redirectUrl = "https://example.com/\${id}"
//        val tokenAddress = ContractAddress(contract.contractAddress)
//        val amount = Balance(BigInteger.TEN)
//        val lockDuration = DurationSeconds(BigInteger.valueOf(100L))
//        val lockContractAddress = ContractAddress("dead") // TODO deploy lock contract
//        val senderAddress = WalletAddress(mainAccount.address)
//
//        val createResponse = suppose("request to create ERC20 lock request is made") {
//            val createResponse = mockMvc.perform(
//                MockMvcRequestBuilders.post("/lock")
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .content(
//                        """
//                            {
//                                "chain_id": ${chainId.value},
//                                "redirect_url": "$redirectUrl",
//                                "token_address": "${tokenAddress.rawValue}",
//                                "amount": "${amount.rawValue}",
//                                "lock_duration_seconds": "${lockDuration.rawValue}",
//                                "lock_contract_address": "${lockContractAddress.rawValue}",
//                                "sender_address": "${senderAddress.rawValue}",
//                                "arbitrary_data": {
//                                    "test": true
//                                },
//                                "screen_config": {
//                                    "before_action_message": "before-action-message",
//                                    "after_action_message": "after-action-message"
//                                }
//                            }
//                        """.trimIndent()
//                    )
//            )
//                .andExpect(MockMvcResultMatchers.status().isOk)
//                .andReturn()
//
//            objectMapper.readValue(createResponse.response.contentAsString, Erc20LockRequestResponse::class.java)
//        }
//
//        val txHash = suppose("some ERC20 transfer transaction is made with missing transaction data") {
//            contract.transferAndMine(lockContractAddress.rawValue, amount.rawValue)
//                ?.get()?.transactionHash?.let { TransactionHash(it) }!!
//        }
//
//        suppose("transaction will have at least one block confirmation") {
//            hardhatContainer.waitAndMine()
//        }
//
//        suppose("transaction hash is attached to ERC20 lock request") {
//            erc20LockRequestRepository.setTxHash(createResponse.id, txHash)
//        }
//
//        val fetchResponse = suppose("request to fetch ERC20 lock request is made") {
//            val fetchResponse = mockMvc.perform(
//                MockMvcRequestBuilders.get("/lock/${createResponse.id}")
//            )
//                .andExpect(MockMvcResultMatchers.status().isOk)
//                .andReturn()
//
//            objectMapper.readValue(fetchResponse.response.contentAsString, Erc20LockRequestResponse::class.java)
//        }
//
//        verify("correct response is returned") {
//            assertThat(fetchResponse).withMessage()
//                .isEqualTo(
//                    Erc20LockRequestResponse(
//                        id = createResponse.id,
//                        status = Status.FAILED,
//                        chainId = chainId.value,
//                        tokenAddress = tokenAddress.rawValue,
//                        amount = amount.rawValue,
//                        lockContractAddress = lockContractAddress.rawValue,
//                        senderAddress = senderAddress.rawValue,
//                        arbitraryData = createResponse.arbitraryData,
//                        screenConfig = ScreenConfig(
//                            beforeActionMessage = "before-action-message",
//                            afterActionMessage = "after-action-message"
//                        ),
//                        redirectUrl = redirectUrl.replace("\${id}", createResponse.id.toString()),
//                        lockTx = TransactionResponse(
//                            txHash = txHash.value,
//                            from = senderAddress.rawValue,
//                            to = tokenAddress.rawValue,
//                            data = createResponse.lockTx.data,
//                            blockConfirmations = fetchResponse.lockTx.blockConfirmations
//                        )
//                    )
//                )
//
//            assertThat(fetchResponse.lockTx.blockConfirmations)
//                .isNotZero()
//        }
//    }

    // TODO implement after actual lock contract is known
    // @Test
//    fun mustCorrectlyFetchErc20LockRequestWhenCustomRpcUrlIsSpecified() {
//        val mainAccount = accounts[0]
//
//        val contract = suppose("simple ERC20 contract is deployed") {
//            SimpleERC20.deploy(
//                hardhatContainer.web3j,
//                mainAccount,
//                DefaultGasProvider(),
//                listOf(mainAccount.address),
//                listOf(BigInteger("10000")),
//                mainAccount.address
//            ).sendAndMine()
//        }
//
//        val chainId = Chain.HARDHAT_TESTNET.id
//        val redirectUrl = "https://example.com/\${id}"
//        val tokenAddress = ContractAddress(contract.contractAddress)
//        val amount = Balance(BigInteger.TEN)
//        val lockDuration = DurationSeconds(BigInteger.valueOf(100L))
//        val lockContractAddress = ContractAddress("dead") // TODO deploy lock contract
//        val senderAddress = WalletAddress(mainAccount.address)
//
//        val createResponse = suppose("request to create ERC20 lock request is made") {
//            val createResponse = mockMvc.perform(
//                MockMvcRequestBuilders.post("/lock")
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .content(
//                        """
//                            {
//                                "chain_id": ${chainId.value},
//                                "redirect_url": "$redirectUrl",
//                                "token_address": "${tokenAddress.rawValue}",
//                                "amount": "${amount.rawValue}",
//                                "lock_duration_seconds": "${lockDuration.rawValue}",
//                                "lock_contract_address": "${lockContractAddress.rawValue}",
//                                "sender_address": "${senderAddress.rawValue}",
//                                "arbitrary_data": {
//                                    "test": true
//                                },
//                                "screen_config": {
//                                    "before_action_message": "before-action-message",
//                                    "after_action_message": "after-action-message"
//                                }
//                            }
//                        """.trimIndent()
//                    )
//            )
//                .andExpect(MockMvcResultMatchers.status().isOk)
//                .andReturn()
//
//            objectMapper.readValue(createResponse.response.contentAsString, Erc20LockRequestResponse::class.java)
//        }
//
//        val txHash = suppose("some ERC20 transfer transaction is made with missing transaction data") {
//            contract.transferAndMine(lockContractAddress.rawValue, amount.rawValue)
//                ?.get()?.transactionHash?.let { TransactionHash(it) }!!
//        }
//
//        suppose("transaction will have at least one block confirmation") {
//            hardhatContainer.waitAndMine()
//        }
//
//        suppose("transaction hash is attached to ERC20 lock request") {
//            erc20LockRequestRepository.setTxHash(createResponse.id, txHash)
//        }
//
//        val fetchResponse = suppose("request to fetch ERC20 lock request is made") {
//            val fetchResponse = mockMvc.perform(
//                MockMvcRequestBuilders.get("/lock/${createResponse.id}")
//                    .header(
//                        RpcUrlSpecResolver.RPC_URL_OVERRIDE_HEADER,
//                        "http://localhost:${hardhatContainer.mappedPort}"
//                    )
//            )
//                .andExpect(MockMvcResultMatchers.status().isOk)
//                .andReturn()
//
//            objectMapper.readValue(fetchResponse.response.contentAsString, Erc20LockRequestResponse::class.java)
//        }
//
//        verify("correct response is returned") {
//            assertThat(fetchResponse).withMessage()
//                .isEqualTo(
//                    Erc20LockRequestResponse(
//                        id = createResponse.id,
//                        status = Status.FAILED,
//                        chainId = chainId.value,
//                        tokenAddress = tokenAddress.rawValue,
//                        amount = amount.rawValue,
//                        lockContractAddress = lockContractAddress.rawValue,
//                        senderAddress = senderAddress.rawValue,
//                        arbitraryData = createResponse.arbitraryData,
//                        screenConfig = ScreenConfig(
//                            beforeActionMessage = "before-action-message",
//                            afterActionMessage = "after-action-message"
//                        ),
//                        redirectUrl = redirectUrl.replace("\${id}", createResponse.id.toString()),
//                        lockTx = TransactionResponse(
//                            txHash = txHash.value,
//                            from = senderAddress.rawValue,
//                            to = tokenAddress.rawValue,
//                            data = createResponse.lockTx.data,
//                            blockConfirmations = fetchResponse.lockTx.blockConfirmations
//                        )
//                    )
//                )
//
//            assertThat(fetchResponse.lockTx.blockConfirmations)
//                .isNotZero()
//        }
//    }

    @Test
    fun mustReturn404NotFoundForNonExistentErc20LockRequest() {
        verify("404 is returned for non-existent ERC20 lock request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/lock/${UUID.randomUUID()}")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    fun mustCorrectlyAttachTransactionHash() {
        val id = UUID.randomUUID()

        suppose("some ERC20 lock request without transaction hash exists in database") {
            erc20LockRequestRepository.store(
                StoreErc20LockRequestParams(
                    id = id,
                    chainId = Chain.HARDHAT_TESTNET.id,
                    redirectUrl = "https://example.com/$id",
                    tokenAddress = ContractAddress("a"),
                    tokenAmount = Balance(BigInteger.TEN),
                    lockDuration = DurationSeconds(BigInteger.valueOf(100L)),
                    lockContractAddress = ContractAddress("b"),
                    tokenSenderAddress = WalletAddress("c"),
                    arbitraryData = TestData.EMPTY_JSON_OBJECT,
                    screenConfig = ScreenConfig(
                        beforeActionMessage = "before-action-message",
                        afterActionMessage = "after-action-message"
                    )
                )
            )
        }

        val txHash = TransactionHash("tx-hash")

        suppose("request to attach transaction hash to ERC20 lock request is made") {
            mockMvc.perform(
                MockMvcRequestBuilders.put("/lock/$id")
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

        verify("transaction hash is correctly attached to ERC20 lock request") {
            val storedRequest = erc20LockRequestRepository.getById(id)

            assertThat(storedRequest?.txHash)
                .isEqualTo(txHash)
        }
    }

    @Test
    fun mustReturn400BadRequestWhenTransactionHashIsNotAttached() {
        val id = UUID.randomUUID()
        val txHash = TransactionHash("tx-hash")

        suppose("some ERC20 lock request with transaction hash exists in database") {
            erc20LockRequestRepository.store(
                StoreErc20LockRequestParams(
                    id = id,
                    chainId = Chain.HARDHAT_TESTNET.id,
                    redirectUrl = "https://example.com/$id",
                    tokenAddress = ContractAddress("a"),
                    tokenAmount = Balance(BigInteger.TEN),
                    lockDuration = DurationSeconds(BigInteger.valueOf(100L)),
                    lockContractAddress = ContractAddress("b"),
                    tokenSenderAddress = WalletAddress("c"),
                    arbitraryData = TestData.EMPTY_JSON_OBJECT,
                    screenConfig = ScreenConfig(
                        beforeActionMessage = "before-action-message",
                        afterActionMessage = "after-action-message"
                    )
                )
            )
            erc20LockRequestRepository.setTxHash(id, txHash)
        }

        verify("400 is returned when attaching transaction hash") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.put("/lock/$id")
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
            val storedRequest = erc20LockRequestRepository.getById(id)

            assertThat(storedRequest?.txHash)
                .isEqualTo(txHash)
        }
    }
}
