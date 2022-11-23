package dev3.blockchainapiservice.controller

import dev3.blockchainapiservice.ControllerTestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.blockchain.SimpleLockManager
import dev3.blockchainapiservice.blockchain.properties.Chain
import dev3.blockchainapiservice.config.binding.ProjectApiKeyResolver
import dev3.blockchainapiservice.exception.ErrorCode
import dev3.blockchainapiservice.generated.jooq.enums.UserIdentifierType
import dev3.blockchainapiservice.generated.jooq.tables.records.ApiKeyRecord
import dev3.blockchainapiservice.generated.jooq.tables.records.ProjectRecord
import dev3.blockchainapiservice.generated.jooq.tables.records.UserIdentifierRecord
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.model.params.StoreErc20LockRequestParams
import dev3.blockchainapiservice.model.response.Erc20LockRequestResponse
import dev3.blockchainapiservice.model.response.Erc20LockRequestsResponse
import dev3.blockchainapiservice.model.response.TransactionResponse
import dev3.blockchainapiservice.model.result.Erc20LockRequest
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.repository.Erc20LockRequestRepository
import dev3.blockchainapiservice.testcontainers.HardhatTestContainer
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.BaseUrl
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.DurationSeconds
import dev3.blockchainapiservice.util.EthereumAddress
import dev3.blockchainapiservice.util.Status
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress
import dev3.blockchainapiservice.util.ZeroAddress
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

class Erc20LockRequestControllerApiTest : ControllerTestBase() {

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
    }

    private val accounts = HardhatTestContainer.ACCOUNTS

    @Autowired
    private lateinit var erc20LockRequestRepository: Erc20LockRequestRepository

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
    fun mustCorrectlyCreateErc20LockRequest() {
        val tokenAddress = ContractAddress("cafebabe")
        val amount = Balance(BigInteger.TEN)
        val lockDuration = DurationSeconds(BigInteger.valueOf(100L))
        val lockContractAddress = ContractAddress("b")
        val senderAddress = WalletAddress("c")

        val response = suppose("request to create ERC20 lock request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/lock")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
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
                        projectId = PROJECT_ID,
                        status = Status.PENDING,
                        chainId = PROJECT.chainId.value,
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
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-lock/${response.id}/action",
                        lockTx = TransactionResponse(
                            txHash = null,
                            from = senderAddress.rawValue,
                            to = tokenAddress.rawValue,
                            data = response.lockTx.data,
                            value = BigInteger.ZERO,
                            blockConfirmations = null,
                            timestamp = null
                        ),
                        createdAt = response.createdAt,
                        events = null
                    )
                )

            assertThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("ERC20 lock request is correctly stored in database") {
            val storedRequest = erc20LockRequestRepository.getById(response.id)

            assertThat(storedRequest).withMessage()
                .isEqualTo(
                    Erc20LockRequest(
                        id = response.id,
                        projectId = PROJECT_ID,
                        chainId = PROJECT.chainId,
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-lock/${response.id}/action",
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
                        ),
                        createdAt = storedRequest!!.createdAt
                    )
                )

            assertThat(storedRequest.createdAt.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyCreateErc20LockRequestWithRedirectUrl() {
        val tokenAddress = ContractAddress("cafebabe")
        val amount = Balance(BigInteger.TEN)
        val lockDuration = DurationSeconds(BigInteger.valueOf(100L))
        val lockContractAddress = ContractAddress("b")
        val senderAddress = WalletAddress("c")
        val redirectUrl = "https://custom-url/\${id}"

        val response = suppose("request to create ERC20 lock request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/lock")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
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
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, Erc20LockRequestResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    Erc20LockRequestResponse(
                        id = response.id,
                        projectId = PROJECT_ID,
                        status = Status.PENDING,
                        chainId = PROJECT.chainId.value,
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
                        redirectUrl = "https://custom-url/${response.id}",
                        lockTx = TransactionResponse(
                            txHash = null,
                            from = senderAddress.rawValue,
                            to = tokenAddress.rawValue,
                            data = response.lockTx.data,
                            value = BigInteger.ZERO,
                            blockConfirmations = null,
                            timestamp = null
                        ),
                        createdAt = response.createdAt,
                        events = null
                    )
                )

            assertThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("ERC20 lock request is correctly stored in database") {
            val storedRequest = erc20LockRequestRepository.getById(response.id)

            assertThat(storedRequest).withMessage()
                .isEqualTo(
                    Erc20LockRequest(
                        id = response.id,
                        projectId = PROJECT_ID,
                        chainId = PROJECT.chainId,
                        redirectUrl = "https://custom-url/${response.id}",
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
                        ),
                        createdAt = storedRequest!!.createdAt
                    )
                )

            assertThat(storedRequest.createdAt.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustReturn401UnauthorizedWhenCreatingErc20LockRequestWithInvalidApiKey() {
        val tokenAddress = ContractAddress("cafebabe")
        val amount = Balance(BigInteger.TEN)
        val lockDuration = DurationSeconds(BigInteger.valueOf(100L))
        val lockContractAddress = ContractAddress("b")
        val senderAddress = WalletAddress("c")

        verify("401 is returned for invalid API key") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/lock")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, "invalid-api-key")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
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
                .andExpect(MockMvcResultMatchers.status().isUnauthorized)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.NON_EXISTENT_API_KEY)
        }
    }

    @Test
    fun mustCorrectlyFetchErc20LockRequest() {
        val mainAccount = accounts[0]

        val lockContract = suppose("simple lock manager contract is deployed") {
            SimpleLockManager.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider()
            ).send()
        }

        val tokenAddress = ContractAddress("a")
        val amount = Balance(BigInteger.TEN)
        val lockDuration = DurationSeconds(BigInteger.valueOf(100L))
        val lockContractAddress = ContractAddress(lockContract.contractAddress)
        val senderAddress = WalletAddress(mainAccount.address)

        val createResponse = suppose("request to create ERC20 lock request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/lock")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
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

            objectMapper.readValue(createResponse.response.contentAsString, Erc20LockRequestResponse::class.java)
        }

        val txHash = suppose("some token lock transaction is made") {
            lockContract.lock(
                tokenAddress = tokenAddress,
                amount = amount,
                lockDuration = lockDuration,
                info = createResponse.id.toString(),
                unlockPrivilegeWallet = ZeroAddress
            )?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("transaction info is attached to ERC20 lock request") {
            erc20LockRequestRepository.setTxInfo(createResponse.id, txHash, senderAddress)
        }

        val fetchResponse = suppose("request to fetch ERC20 lock request is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/lock/${createResponse.id}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, Erc20LockRequestResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(fetchResponse).withMessage()
                .isEqualTo(
                    Erc20LockRequestResponse(
                        id = createResponse.id,
                        projectId = PROJECT_ID,
                        status = Status.SUCCESS,
                        chainId = PROJECT.chainId.value,
                        tokenAddress = tokenAddress.rawValue,
                        amount = amount.rawValue,
                        lockDurationInSeconds = lockDuration.rawValue,
                        unlocksAt = (fetchResponse.lockTx.timestamp!! + lockDuration.toDuration()),
                        lockContractAddress = lockContractAddress.rawValue,
                        senderAddress = senderAddress.rawValue,
                        arbitraryData = createResponse.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-lock/${createResponse.id}/action",
                        lockTx = TransactionResponse(
                            txHash = txHash.value,
                            from = senderAddress.rawValue,
                            to = lockContractAddress.rawValue,
                            data = createResponse.lockTx.data,
                            value = BigInteger.ZERO,
                            blockConfirmations = fetchResponse.lockTx.blockConfirmations,
                            timestamp = fetchResponse.lockTx.timestamp
                        ),
                        createdAt = fetchResponse.createdAt,
                        events = emptyList()
                    )
                )

            assertThat(fetchResponse.lockTx.blockConfirmations)
                .isNotZero()
            assertThat(fetchResponse.lockTx.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            assertThat(fetchResponse.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchErc20LockRequestWhenCustomRpcUrlIsSpecified() {
        val mainAccount = accounts[0]

        val lockContract = suppose("simple lock manager contract is deployed") {
            SimpleLockManager.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider()
            ).send()
        }

        val tokenAddress = ContractAddress("a")
        val amount = Balance(BigInteger.TEN)
        val lockDuration = DurationSeconds(BigInteger.valueOf(100L))
        val lockContractAddress = ContractAddress(lockContract.contractAddress)
        val senderAddress = WalletAddress(mainAccount.address)

        val (projectId, chainId, apiKey) = suppose("project with customRpcUrl is inserted into database") {
            insertProjectWithCustomRpcUrl()
        }

        val createResponse = suppose("request to create ERC20 lock request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/lock")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
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

            objectMapper.readValue(createResponse.response.contentAsString, Erc20LockRequestResponse::class.java)
        }

        val txHash = suppose("some token lock transaction is made") {
            lockContract.lock(
                tokenAddress = tokenAddress,
                amount = amount,
                lockDuration = lockDuration,
                info = createResponse.id.toString(),
                unlockPrivilegeWallet = ZeroAddress
            )?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("transaction info is attached to ERC20 lock request") {
            erc20LockRequestRepository.setTxInfo(createResponse.id, txHash, senderAddress)
        }

        val fetchResponse = suppose("request to fetch ERC20 lock request is made") {
            val fetchResponse = mockMvc.perform(MockMvcRequestBuilders.get("/v1/lock/${createResponse.id}"))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, Erc20LockRequestResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(fetchResponse).withMessage()
                .isEqualTo(
                    Erc20LockRequestResponse(
                        id = createResponse.id,
                        projectId = projectId,
                        status = Status.SUCCESS,
                        chainId = chainId.value,
                        tokenAddress = tokenAddress.rawValue,
                        amount = amount.rawValue,
                        lockDurationInSeconds = lockDuration.rawValue,
                        unlocksAt = (fetchResponse.lockTx.timestamp!! + lockDuration.toDuration()),
                        lockContractAddress = lockContractAddress.rawValue,
                        senderAddress = senderAddress.rawValue,
                        arbitraryData = createResponse.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        redirectUrl = PROJECT.baseRedirectUrl.value +
                            "/request-lock/${createResponse.id}/action",
                        lockTx = TransactionResponse(
                            txHash = txHash.value,
                            from = senderAddress.rawValue,
                            to = lockContractAddress.rawValue,
                            data = createResponse.lockTx.data,
                            value = BigInteger.ZERO,
                            blockConfirmations = fetchResponse.lockTx.blockConfirmations,
                            timestamp = fetchResponse.lockTx.timestamp
                        ),
                        createdAt = fetchResponse.createdAt,
                        events = emptyList()
                    )
                )

            assertThat(fetchResponse.lockTx.blockConfirmations)
                .isNotZero()
            assertThat(fetchResponse.lockTx.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            assertThat(fetchResponse.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchErc20LockRequestsByProjectId() {
        val mainAccount = accounts[0]

        val lockContract = suppose("simple lock manager contract is deployed") {
            SimpleLockManager.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider()
            ).send()
        }

        val tokenAddress = ContractAddress("a")
        val amount = Balance(BigInteger.TEN)
        val lockDuration = DurationSeconds(BigInteger.valueOf(100L))
        val lockContractAddress = ContractAddress(lockContract.contractAddress)
        val senderAddress = WalletAddress(mainAccount.address)

        val createResponse = suppose("request to create ERC20 lock request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/lock")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
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

            objectMapper.readValue(createResponse.response.contentAsString, Erc20LockRequestResponse::class.java)
        }

        val txHash = suppose("some token lock transaction is made") {
            lockContract.lock(
                tokenAddress = tokenAddress,
                amount = amount,
                lockDuration = lockDuration,
                info = createResponse.id.toString(),
                unlockPrivilegeWallet = ZeroAddress
            )?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("transaction info is attached to ERC20 lock request") {
            erc20LockRequestRepository.setTxInfo(createResponse.id, txHash, senderAddress)
        }

        val fetchResponse = suppose("request to fetch ERC20 lock requests by project ID is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/lock/by-project/${createResponse.projectId}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, Erc20LockRequestsResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(fetchResponse).withMessage()
                .isEqualTo(
                    Erc20LockRequestsResponse(
                        listOf(
                            Erc20LockRequestResponse(
                                id = createResponse.id,
                                projectId = PROJECT_ID,
                                status = Status.SUCCESS,
                                chainId = PROJECT.chainId.value,
                                tokenAddress = tokenAddress.rawValue,
                                amount = amount.rawValue,
                                lockDurationInSeconds = lockDuration.rawValue,
                                unlocksAt = (fetchResponse.requests[0].lockTx.timestamp!! + lockDuration.toDuration()),
                                lockContractAddress = lockContractAddress.rawValue,
                                senderAddress = senderAddress.rawValue,
                                arbitraryData = createResponse.arbitraryData,
                                screenConfig = ScreenConfig(
                                    beforeActionMessage = "before-action-message",
                                    afterActionMessage = "after-action-message"
                                ),
                                redirectUrl = PROJECT.baseRedirectUrl.value +
                                    "/request-lock/${createResponse.id}/action",
                                lockTx = TransactionResponse(
                                    txHash = txHash.value,
                                    from = senderAddress.rawValue,
                                    to = lockContractAddress.rawValue,
                                    data = createResponse.lockTx.data,
                                    value = BigInteger.ZERO,
                                    blockConfirmations = fetchResponse.requests[0].lockTx.blockConfirmations,
                                    timestamp = fetchResponse.requests[0].lockTx.timestamp
                                ),
                                createdAt = fetchResponse.requests[0].createdAt,
                                events = emptyList()
                            )
                        )
                    )
                )

            assertThat(fetchResponse.requests[0].lockTx.blockConfirmations)
                .isNotZero()
            assertThat(fetchResponse.requests[0].lockTx.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            assertThat(fetchResponse.requests[0].createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchErc20LockRequestsByProjectIdWhenCustomRpcUrlIsSpecified() {
        val mainAccount = accounts[0]

        val lockContract = suppose("simple lock manager contract is deployed") {
            SimpleLockManager.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider()
            ).send()
        }

        val tokenAddress = ContractAddress("a")
        val amount = Balance(BigInteger.TEN)
        val lockDuration = DurationSeconds(BigInteger.valueOf(100L))
        val lockContractAddress = ContractAddress(lockContract.contractAddress)
        val senderAddress = WalletAddress(mainAccount.address)

        val (projectId, chainId, apiKey) = suppose("project with customRpcUrl is inserted into database") {
            insertProjectWithCustomRpcUrl()
        }

        val createResponse = suppose("request to create ERC20 lock request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/lock")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
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

            objectMapper.readValue(createResponse.response.contentAsString, Erc20LockRequestResponse::class.java)
        }

        val txHash = suppose("some token lock transaction is made") {
            lockContract.lock(
                tokenAddress = tokenAddress,
                amount = amount,
                lockDuration = lockDuration,
                info = createResponse.id.toString(),
                unlockPrivilegeWallet = ZeroAddress
            )?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("transaction info is attached to ERC20 lock request") {
            erc20LockRequestRepository.setTxInfo(createResponse.id, txHash, senderAddress)
        }

        val fetchResponse = suppose("request to fetch ERC20 lock requests by project ID is made") {
            val fetchResponse = mockMvc.perform(MockMvcRequestBuilders.get("/v1/lock/by-project/$projectId"))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, Erc20LockRequestsResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(fetchResponse).withMessage()
                .isEqualTo(
                    Erc20LockRequestsResponse(
                        listOf(
                            Erc20LockRequestResponse(
                                id = createResponse.id,
                                projectId = projectId,
                                status = Status.SUCCESS,
                                chainId = chainId.value,
                                tokenAddress = tokenAddress.rawValue,
                                amount = amount.rawValue,
                                lockDurationInSeconds = lockDuration.rawValue,
                                unlocksAt = (fetchResponse.requests[0].lockTx.timestamp!! + lockDuration.toDuration()),
                                lockContractAddress = lockContractAddress.rawValue,
                                senderAddress = senderAddress.rawValue,
                                arbitraryData = createResponse.arbitraryData,
                                screenConfig = ScreenConfig(
                                    beforeActionMessage = "before-action-message",
                                    afterActionMessage = "after-action-message"
                                ),
                                redirectUrl = PROJECT.baseRedirectUrl.value +
                                    "/request-lock/${createResponse.id}/action",
                                lockTx = TransactionResponse(
                                    txHash = txHash.value,
                                    from = senderAddress.rawValue,
                                    to = lockContractAddress.rawValue,
                                    data = createResponse.lockTx.data,
                                    value = BigInteger.ZERO,
                                    blockConfirmations = fetchResponse.requests[0].lockTx.blockConfirmations,
                                    timestamp = fetchResponse.requests[0].lockTx.timestamp
                                ),
                                createdAt = fetchResponse.requests[0].createdAt,
                                events = emptyList()
                            )
                        )
                    )
                )

            assertThat(fetchResponse.requests[0].lockTx.blockConfirmations)
                .isNotZero()
            assertThat(fetchResponse.requests[0].lockTx.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            assertThat(fetchResponse.requests[0].createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustReturn404NotFoundForNonExistentErc20LockRequest() {
        verify("404 is returned for non-existent ERC20 lock request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/lock/${UUID.randomUUID()}")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    fun mustCorrectlyAttachTransactionInfo() {
        val id = UUID.randomUUID()
        val tokenSender = WalletAddress("c")

        suppose("some ERC20 lock request without transaction info exists in database") {
            erc20LockRequestRepository.store(
                StoreErc20LockRequestParams(
                    id = id,
                    projectId = PROJECT_ID,
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
                    ),
                    createdAt = TestData.TIMESTAMP
                )
            )
        }

        val txHash = TransactionHash("0x1")

        suppose("request to attach transaction info to ERC20 lock request is made") {
            mockMvc.perform(
                MockMvcRequestBuilders.put("/v1/lock/$id")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "tx_hash": "${txHash.value}",
                                "caller_address": "${tokenSender.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
        }

        verify("transaction info is correctly attached to ERC20 lock request") {
            val storedRequest = erc20LockRequestRepository.getById(id)

            assertThat(storedRequest?.txHash)
                .isEqualTo(txHash)
        }
    }

    @Test
    fun mustReturn400BadRequestWhenTransactionInfoIsNotAttached() {
        val id = UUID.randomUUID()
        val txHash = TransactionHash("0x1")
        val tokenSender = WalletAddress("c")

        suppose("some ERC20 lock request with transaction info exists in database") {
            erc20LockRequestRepository.store(
                StoreErc20LockRequestParams(
                    id = id,
                    projectId = PROJECT_ID,
                    chainId = Chain.HARDHAT_TESTNET.id,
                    redirectUrl = "https://example.com/$id",
                    tokenAddress = ContractAddress("a"),
                    tokenAmount = Balance(BigInteger.TEN),
                    lockDuration = DurationSeconds(BigInteger.valueOf(100L)),
                    lockContractAddress = ContractAddress("b"),
                    tokenSenderAddress = tokenSender,
                    arbitraryData = TestData.EMPTY_JSON_OBJECT,
                    screenConfig = ScreenConfig(
                        beforeActionMessage = "before-action-message",
                        afterActionMessage = "after-action-message"
                    ),
                    createdAt = TestData.TIMESTAMP
                )
            )
            erc20LockRequestRepository.setTxInfo(id, txHash, tokenSender)
        }

        verify("400 is returned when attaching transaction info") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.put("/v1/lock/$id")
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

        verify("transaction info is not changed in database") {
            val storedRequest = erc20LockRequestRepository.getById(id)

            assertThat(storedRequest?.txHash)
                .isEqualTo(txHash)
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

    private fun SimpleLockManager.lock(
        tokenAddress: ContractAddress,
        amount: Balance,
        lockDuration: DurationSeconds,
        info: String,
        unlockPrivilegeWallet: EthereumAddress
    ) = lock(tokenAddress.rawValue, amount.rawValue, lockDuration.rawValue, info, unlockPrivilegeWallet.rawValue).send()
}
