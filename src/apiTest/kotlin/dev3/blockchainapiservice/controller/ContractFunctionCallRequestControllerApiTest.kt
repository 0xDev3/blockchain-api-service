package dev3.blockchainapiservice.controller

import dev3.blockchainapiservice.ControllerTestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.blockchain.ExampleContract
import dev3.blockchainapiservice.blockchain.properties.Chain
import dev3.blockchainapiservice.config.binding.ProjectApiKeyResolver
import dev3.blockchainapiservice.exception.ErrorCode
import dev3.blockchainapiservice.generated.jooq.enums.UserIdentifierType
import dev3.blockchainapiservice.generated.jooq.tables.records.ApiKeyRecord
import dev3.blockchainapiservice.generated.jooq.tables.records.ContractMetadataRecord
import dev3.blockchainapiservice.generated.jooq.tables.records.ProjectRecord
import dev3.blockchainapiservice.generated.jooq.tables.records.UserIdentifierRecord
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.model.params.StoreContractDeploymentRequestParams
import dev3.blockchainapiservice.model.params.StoreContractFunctionCallRequestParams
import dev3.blockchainapiservice.model.response.ContractFunctionCallRequestResponse
import dev3.blockchainapiservice.model.response.ContractFunctionCallRequestsResponse
import dev3.blockchainapiservice.model.response.TransactionResponse
import dev3.blockchainapiservice.model.result.ContractFunctionCallRequest
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.repository.ContractDeploymentRequestRepository
import dev3.blockchainapiservice.repository.ContractFunctionCallRequestRepository
import dev3.blockchainapiservice.testcontainers.HardhatTestContainer
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.BaseUrl
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.Constants
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.ContractBinaryData
import dev3.blockchainapiservice.util.ContractId
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
import org.web3j.tx.gas.DefaultGasProvider
import java.math.BigInteger
import java.util.UUID

class ContractFunctionCallRequestControllerApiTest : ControllerTestBase() {

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
        private val CONTRACT_DECORATOR_ID = ContractId("decorator.id")
        private val DEPLOYED_CONTRACT = StoreContractDeploymentRequestParams(
            id = UUID.randomUUID(),
            alias = "contract-alias",
            contractData = ContractBinaryData("00"),
            contractId = CONTRACT_DECORATOR_ID,
            constructorParams = TestData.EMPTY_JSON_ARRAY,
            deployerAddress = null,
            initialEthAmount = Balance.ZERO,
            chainId = Chain.HARDHAT_TESTNET.id,
            redirectUrl = "redirect-url",
            projectId = PROJECT_ID,
            createdAt = TestData.TIMESTAMP,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig.EMPTY,
            imported = false
        )
    }

    private val accounts = HardhatTestContainer.ACCOUNTS

    @Autowired
    private lateinit var contractFunctionCallRequestRepository: ContractFunctionCallRequestRepository

    @Autowired
    private lateinit var contractDeploymentRequestRepository: ContractDeploymentRequestRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        postgresContainer.cleanAllDatabaseTables(dslContext)

        dslContext.executeInsert(
            ContractMetadataRecord(
                id = UUID.randomUUID(),
                contractId = CONTRACT_DECORATOR_ID,
                contractTags = emptyArray(),
                contractImplements = emptyArray(),
                name = null,
                description = null,
                projectId = Constants.NIL_UUID
            )
        )

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
    fun mustCorrectlyCreateContractFunctionCallRequestViaDeployedContractId() {
        val callerAddress = WalletAddress("b")
        val functionName = "setOwner"
        val ethAmount = Balance.ZERO

        val contractAddress = ContractAddress("cafebabe")
        val storedContract = suppose("some deployed contract exists in the database") {
            contractDeploymentRequestRepository.store(DEPLOYED_CONTRACT, Constants.NIL_UUID).apply {
                contractDeploymentRequestRepository.setContractAddress(DEPLOYED_CONTRACT.id, contractAddress)
            }
        }

        val paramsJson =
            """
                [
                    {
                        "type": "address",
                        "value": "${callerAddress.rawValue}"
                    }
                ]
            """.trimIndent()

        val response = suppose("request to create contract function call request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/function-call")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "deployed_contract_id": "${DEPLOYED_CONTRACT.id}",
                                "function_name": "$functionName",
                                "function_params": $paramsJson,
                                "eth_amount": "${ethAmount.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                },
                                "caller_address": "${callerAddress.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ContractFunctionCallRequestResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    ContractFunctionCallRequestResponse(
                        id = response.id,
                        status = Status.PENDING,
                        deployedContractId = storedContract.id,
                        contractAddress = contractAddress.rawValue,
                        functionName = functionName,
                        functionParams = objectMapper.readTree(paramsJson),
                        functionCallData = response.functionCallData,
                        ethAmount = ethAmount.rawValue,
                        chainId = PROJECT.chainId.value,
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-function-call/${response.id}/action",
                        projectId = PROJECT_ID,
                        createdAt = response.createdAt,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        callerAddress = callerAddress.rawValue,
                        functionCallTx = TransactionResponse(
                            txHash = null,
                            from = callerAddress.rawValue,
                            to = contractAddress.rawValue,
                            data = response.functionCallTx.data,
                            value = ethAmount.rawValue,
                            blockConfirmations = null,
                            timestamp = null
                        )
                    )
                )

            assertThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("contract function call request is correctly stored in database") {
            val storedRequest = contractFunctionCallRequestRepository.getById(response.id)

            assertThat(storedRequest).withMessage()
                .isEqualTo(
                    ContractFunctionCallRequest(
                        id = response.id,
                        deployedContractId = storedContract.id,
                        contractAddress = contractAddress,
                        functionName = functionName,
                        functionParams = objectMapper.readTree(paramsJson),
                        ethAmount = ethAmount,
                        chainId = PROJECT.chainId,
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-function-call/${response.id}/action",
                        projectId = PROJECT_ID,
                        createdAt = storedRequest!!.createdAt,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        callerAddress = callerAddress,
                        txHash = null
                    )
                )

            assertThat(storedRequest.createdAt.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyCreateContractFunctionCallRequestViaDeployedContractAlias() {
        val callerAddress = WalletAddress("b")
        val functionName = "setOwner"
        val ethAmount = Balance.ZERO

        val contractAddress = ContractAddress("cafebabe")
        val storedContract = suppose("some deployed contract exists in the database") {
            contractDeploymentRequestRepository.store(DEPLOYED_CONTRACT, Constants.NIL_UUID).apply {
                contractDeploymentRequestRepository.setContractAddress(DEPLOYED_CONTRACT.id, contractAddress)
            }
        }

        val paramsJson =
            """
                [
                    {
                        "type": "address",
                        "value": "${callerAddress.rawValue}"
                    }
                ]
            """.trimIndent()

        val response = suppose("request to create contract function call request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/function-call")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "deployed_contract_alias": "${DEPLOYED_CONTRACT.alias}",
                                "function_name": "$functionName",
                                "function_params": $paramsJson,
                                "eth_amount": "${ethAmount.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                },
                                "caller_address": "${callerAddress.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ContractFunctionCallRequestResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    ContractFunctionCallRequestResponse(
                        id = response.id,
                        status = Status.PENDING,
                        deployedContractId = storedContract.id,
                        contractAddress = contractAddress.rawValue,
                        functionName = functionName,
                        functionParams = objectMapper.readTree(paramsJson),
                        functionCallData = response.functionCallData,
                        ethAmount = ethAmount.rawValue,
                        chainId = PROJECT.chainId.value,
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-function-call/${response.id}/action",
                        projectId = PROJECT_ID,
                        createdAt = response.createdAt,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        callerAddress = callerAddress.rawValue,
                        functionCallTx = TransactionResponse(
                            txHash = null,
                            from = callerAddress.rawValue,
                            to = contractAddress.rawValue,
                            data = response.functionCallTx.data,
                            value = ethAmount.rawValue,
                            blockConfirmations = null,
                            timestamp = null
                        )
                    )
                )

            assertThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("contract function call request is correctly stored in database") {
            val storedRequest = contractFunctionCallRequestRepository.getById(response.id)

            assertThat(storedRequest).withMessage()
                .isEqualTo(
                    ContractFunctionCallRequest(
                        id = response.id,
                        deployedContractId = storedContract.id,
                        contractAddress = contractAddress,
                        functionName = functionName,
                        functionParams = objectMapper.readTree(paramsJson),
                        ethAmount = ethAmount,
                        chainId = PROJECT.chainId,
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-function-call/${response.id}/action",
                        projectId = PROJECT_ID,
                        createdAt = storedRequest!!.createdAt,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        callerAddress = callerAddress,
                        txHash = null
                    )
                )

            assertThat(storedRequest.createdAt.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyCreateContractFunctionCallRequestViaDeployedContractAddress() {
        val callerAddress = WalletAddress("b")
        val functionName = "setOwner"
        val ethAmount = Balance.ZERO

        val contractAddress = ContractAddress("cafebabe")

        val paramsJson =
            """
                [
                    {
                        "type": "address",
                        "value": "${callerAddress.rawValue}"
                    }
                ]
            """.trimIndent()

        val response = suppose("request to create contract function call request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/function-call")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "contract_address": "${contractAddress.rawValue}",
                                "function_name": "$functionName",
                                "function_params": $paramsJson,
                                "eth_amount": "${ethAmount.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                },
                                "caller_address": "${callerAddress.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ContractFunctionCallRequestResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    ContractFunctionCallRequestResponse(
                        id = response.id,
                        status = Status.PENDING,
                        deployedContractId = null,
                        contractAddress = contractAddress.rawValue,
                        functionName = functionName,
                        functionParams = objectMapper.readTree(paramsJson),
                        functionCallData = response.functionCallData,
                        ethAmount = ethAmount.rawValue,
                        chainId = PROJECT.chainId.value,
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-function-call/${response.id}/action",
                        projectId = PROJECT_ID,
                        createdAt = response.createdAt,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        callerAddress = callerAddress.rawValue,
                        functionCallTx = TransactionResponse(
                            txHash = null,
                            from = callerAddress.rawValue,
                            to = contractAddress.rawValue,
                            data = response.functionCallTx.data,
                            value = ethAmount.rawValue,
                            blockConfirmations = null,
                            timestamp = null
                        )
                    )
                )

            assertThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("contract function call request is correctly stored in database") {
            val storedRequest = contractFunctionCallRequestRepository.getById(response.id)

            assertThat(storedRequest).withMessage()
                .isEqualTo(
                    ContractFunctionCallRequest(
                        id = response.id,
                        deployedContractId = null,
                        contractAddress = contractAddress,
                        functionName = functionName,
                        functionParams = objectMapper.readTree(paramsJson),
                        ethAmount = ethAmount,
                        chainId = PROJECT.chainId,
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-function-call/${response.id}/action",
                        projectId = PROJECT_ID,
                        createdAt = storedRequest!!.createdAt,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        callerAddress = callerAddress,
                        txHash = null
                    )
                )

            assertThat(storedRequest.createdAt.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyCreateContractFunctionCallRequestWithRedirectUrl() {
        val callerAddress = WalletAddress("b")
        val functionName = "setOwner"
        val ethAmount = Balance.ZERO
        val redirectUrl = "https://custom-url/\${id}"

        val contractAddress = ContractAddress("cafebabe")
        val storedContract = suppose("some deployed contract exists in the database") {
            contractDeploymentRequestRepository.store(DEPLOYED_CONTRACT, Constants.NIL_UUID).apply {
                contractDeploymentRequestRepository.setContractAddress(DEPLOYED_CONTRACT.id, contractAddress)
            }
        }

        val paramsJson =
            """
                [
                    {
                        "type": "address",
                        "value": "${callerAddress.rawValue}"
                    }
                ]
            """.trimIndent()

        val response = suppose("request to create contract function call request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/function-call")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "deployed_contract_id": "${DEPLOYED_CONTRACT.id}",
                                "function_name": "$functionName",
                                "function_params": $paramsJson,
                                "eth_amount": "${ethAmount.rawValue}",
                                "redirect_url": "$redirectUrl",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                },
                                "caller_address": "${callerAddress.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ContractFunctionCallRequestResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    ContractFunctionCallRequestResponse(
                        id = response.id,
                        status = Status.PENDING,
                        deployedContractId = storedContract.id,
                        contractAddress = contractAddress.rawValue,
                        functionName = functionName,
                        functionParams = objectMapper.readTree(paramsJson),
                        functionCallData = response.functionCallData,
                        ethAmount = ethAmount.rawValue,
                        chainId = PROJECT.chainId.value,
                        redirectUrl = "https://custom-url/${response.id}",
                        projectId = PROJECT_ID,
                        createdAt = response.createdAt,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        callerAddress = callerAddress.rawValue,
                        functionCallTx = TransactionResponse(
                            txHash = null,
                            from = callerAddress.rawValue,
                            to = contractAddress.rawValue,
                            data = response.functionCallTx.data,
                            value = ethAmount.rawValue,
                            blockConfirmations = null,
                            timestamp = null
                        )
                    )
                )

            assertThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("contract function call request is correctly stored in database") {
            val storedRequest = contractFunctionCallRequestRepository.getById(response.id)

            assertThat(storedRequest).withMessage()
                .isEqualTo(
                    ContractFunctionCallRequest(
                        id = response.id,
                        deployedContractId = storedContract.id,
                        contractAddress = contractAddress,
                        functionName = functionName,
                        functionParams = objectMapper.readTree(paramsJson),
                        ethAmount = ethAmount,
                        chainId = PROJECT.chainId,
                        redirectUrl = "https://custom-url/${response.id}",
                        projectId = PROJECT_ID,
                        createdAt = storedRequest!!.createdAt,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        callerAddress = callerAddress,
                        txHash = null
                    )
                )

            assertThat(storedRequest.createdAt.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustReturn400BadRequestWhenCreatingContractFunctionCallRequestWithAllContractIdentifiers() {
        val callerAddress = WalletAddress("b")
        val contractAddress = ContractAddress("cafebabe")

        suppose("some deployed contract exists in the database") {
            contractDeploymentRequestRepository.store(DEPLOYED_CONTRACT, Constants.NIL_UUID)
            contractDeploymentRequestRepository.setContractAddress(DEPLOYED_CONTRACT.id, contractAddress)
        }

        verify("400 is returned when creating contract function call request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/function-call")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "deployed_contract_id": "${DEPLOYED_CONTRACT.id}",
                                "deployed_contract_alias": "${DEPLOYED_CONTRACT.alias}",
                                "contract_address": "${contractAddress.rawValue}",
                                "function_name": "setOwner",
                                "function_params": [],
                                "eth_amount": "0",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                },
                                "caller_address": "${callerAddress.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.INVALID_REQUEST_BODY)
        }
    }

    @Test
    fun mustReturn400BadRequestWhenCreatingContractFunctionCallRequestWithNoContractIdentifiers() {
        val callerAddress = WalletAddress("b")

        suppose("some deployed contract exists in the database") {
            contractDeploymentRequestRepository.store(DEPLOYED_CONTRACT, Constants.NIL_UUID)
        }

        verify("400 is returned when creating contract function call request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/function-call")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "function_name": "setOwner",
                                "function_params": [],
                                "eth_amount": "0",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                },
                                "caller_address": "${callerAddress.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.INVALID_REQUEST_BODY)
        }
    }

    @Test
    fun mustReturn404NotFoundWhenCreatingContractFunctionCallRequestForNonExistentContractId() {
        val callerAddress = WalletAddress("b")

        verify("404 is returned when creating contract function call request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/function-call")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "deployed_contract_id": "${UUID.randomUUID()}",
                                "function_name": "setOwner",
                                "function_params": [],
                                "eth_amount": "0",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                },
                                "caller_address": "${callerAddress.rawValue}"
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
    fun mustReturn404NotFoundWhenCreatingContractFunctionCallRequestForNonExistentContractAlias() {
        val callerAddress = WalletAddress("b")

        verify("404 is returned when creating contract function call request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/function-call")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "deployed_contract_alias": "non-existent-alias",
                                "function_name": "setOwner",
                                "function_params": [],
                                "eth_amount": "0",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                },
                                "caller_address": "${callerAddress.rawValue}"
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
    fun mustReturn400BadRequestWhenCreatingContractFunctionCallRequestViaDeployedContractIdForNonDeployedContract() {
        val callerAddress = WalletAddress("b")

        suppose("some non-deployed contract exists in the database") {
            contractDeploymentRequestRepository.store(DEPLOYED_CONTRACT, Constants.NIL_UUID)
        }

        verify("400 is returned when creating contract function call request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/function-call")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "deployed_contract_id": "${DEPLOYED_CONTRACT.id}",
                                "function_name": "setOwner",
                                "function_params": [],
                                "eth_amount": "0",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                },
                                "caller_address": "${callerAddress.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.CONTRACT_NOT_DEPLOYED)
        }
    }

    @Test
    fun mustReturn400BadRequestWhenCreatingContractFunctionCallRequestViaDeployedContractAliasForNonDeployedContract() {
        val callerAddress = WalletAddress("b")

        suppose("some non-deployed contract exists in the database") {
            contractDeploymentRequestRepository.store(DEPLOYED_CONTRACT, Constants.NIL_UUID)
        }

        verify("400 is returned when creating contract function call request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/function-call")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "deployed_contract_alias": "${DEPLOYED_CONTRACT.alias}",
                                "function_name": "setOwner",
                                "function_params": [],
                                "eth_amount": "0",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                },
                                "caller_address": "${callerAddress.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.CONTRACT_NOT_DEPLOYED)
        }
    }

    @Test
    fun mustReturn401UnauthorizedWhenCreatingContractFunctionCallRequestWithInvalidApiKey() {
        val callerAddress = WalletAddress("b")

        verify("401 is returned when creating contract function call request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/function-call")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, "invalid-api-key")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "deployed_contract_id": "${UUID.randomUUID()}",
                                "function_name": "setOwner",
                                "function_params": [],
                                "eth_amount": "0",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                },
                                "caller_address": "${callerAddress.rawValue}"
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
    fun mustCorrectlyFetchContractFunctionCallRequest() {
        val ownerAddress = WalletAddress("a")
        val mainAccount = accounts[0]

        val contract = suppose("contract is deployed") {
            ExampleContract.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                ownerAddress.rawValue
            ).send()
        }

        val callerAddress = WalletAddress(mainAccount.address)
        val contractAddress = ContractAddress(contract.contractAddress)
        val functionName = "setOwner"
        val ethAmount = Balance.ZERO

        val paramsJson =
            """
                [
                    {
                        "type": "address",
                        "value": "${callerAddress.rawValue}"
                    }
                ]
            """.trimIndent()

        val createResponse = suppose("request to create contract function call request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/function-call")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "contract_address": "${contractAddress.rawValue}",
                                "function_name": "$functionName",
                                "function_params": $paramsJson,
                                "eth_amount": "${ethAmount.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                },
                                "caller_address": "${callerAddress.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(
                createResponse.response.contentAsString,
                ContractFunctionCallRequestResponse::class.java
            )
        }

        val txHash = suppose("function is called") {
            contract.setOwner(callerAddress.rawValue).send()?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("transaction info is attached to contract function call request") {
            contractFunctionCallRequestRepository.setTxInfo(createResponse.id, txHash, callerAddress)
        }

        val fetchResponse = suppose("request to fetch contract function call request is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/function-call/${createResponse.id}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(
                fetchResponse.response.contentAsString,
                ContractFunctionCallRequestResponse::class.java
            )
        }

        verify("correct response is returned") {
            assertThat(fetchResponse).withMessage()
                .isEqualTo(
                    ContractFunctionCallRequestResponse(
                        id = createResponse.id,
                        status = Status.SUCCESS,
                        deployedContractId = null,
                        contractAddress = contractAddress.rawValue,
                        functionName = functionName,
                        functionParams = objectMapper.readTree(paramsJson),
                        functionCallData = createResponse.functionCallData,
                        ethAmount = ethAmount.rawValue,
                        chainId = PROJECT.chainId.value,
                        redirectUrl = PROJECT.baseRedirectUrl.value +
                            "/request-function-call/${createResponse.id}/action",
                        projectId = PROJECT_ID,
                        createdAt = fetchResponse.createdAt,
                        arbitraryData = createResponse.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        callerAddress = callerAddress.rawValue,
                        functionCallTx = TransactionResponse(
                            txHash = txHash.value,
                            from = callerAddress.rawValue,
                            to = contractAddress.rawValue,
                            data = createResponse.functionCallTx.data,
                            value = ethAmount.rawValue,
                            blockConfirmations = fetchResponse.functionCallTx.blockConfirmations,
                            timestamp = fetchResponse.functionCallTx.timestamp
                        )
                    )
                )

            assertThat(fetchResponse.functionCallTx.blockConfirmations)
                .isNotZero()
            assertThat(fetchResponse.functionCallTx.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            assertThat(fetchResponse.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchContractFunctionCallRequestWhenCustomRpcUrlIsSpecified() {
        val ownerAddress = WalletAddress("a")
        val mainAccount = accounts[0]

        val contract = suppose("contract is deployed") {
            ExampleContract.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                ownerAddress.rawValue
            ).send()
        }

        val callerAddress = WalletAddress(mainAccount.address)
        val contractAddress = ContractAddress(contract.contractAddress)
        val functionName = "setOwner"
        val ethAmount = Balance.ZERO

        val (projectId, chainId, apiKey) = suppose("project with customRpcUrl is inserted into database") {
            insertProjectWithCustomRpcUrl()
        }

        val paramsJson =
            """
                [
                    {
                        "type": "address",
                        "value": "${callerAddress.rawValue}"
                    }
                ]
            """.trimIndent()

        val createResponse = suppose("request to create contract function call request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/function-call")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "contract_address": "${contractAddress.rawValue}",
                                "function_name": "$functionName",
                                "function_params": $paramsJson,
                                "eth_amount": "${ethAmount.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                },
                                "caller_address": "${callerAddress.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(
                createResponse.response.contentAsString,
                ContractFunctionCallRequestResponse::class.java
            )
        }

        val txHash = suppose("function is called") {
            contract.setOwner(callerAddress.rawValue).send()?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("transaction info is attached to contract function call request") {
            contractFunctionCallRequestRepository.setTxInfo(createResponse.id, txHash, callerAddress)
        }

        val fetchResponse = suppose("request to fetch contract function call request is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/function-call/${createResponse.id}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(
                fetchResponse.response.contentAsString,
                ContractFunctionCallRequestResponse::class.java
            )
        }

        verify("correct response is returned") {
            assertThat(fetchResponse).withMessage()
                .isEqualTo(
                    ContractFunctionCallRequestResponse(
                        id = createResponse.id,
                        status = Status.SUCCESS,
                        deployedContractId = null,
                        contractAddress = contractAddress.rawValue,
                        functionName = functionName,
                        functionParams = objectMapper.readTree(paramsJson),
                        functionCallData = createResponse.functionCallData,
                        ethAmount = ethAmount.rawValue,
                        chainId = chainId.value,
                        redirectUrl = PROJECT.baseRedirectUrl.value +
                            "/request-function-call/${createResponse.id}/action",
                        projectId = projectId,
                        createdAt = fetchResponse.createdAt,
                        arbitraryData = createResponse.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        callerAddress = callerAddress.rawValue,
                        functionCallTx = TransactionResponse(
                            txHash = txHash.value,
                            from = callerAddress.rawValue,
                            to = contractAddress.rawValue,
                            data = createResponse.functionCallTx.data,
                            value = ethAmount.rawValue,
                            blockConfirmations = fetchResponse.functionCallTx.blockConfirmations,
                            timestamp = fetchResponse.functionCallTx.timestamp
                        )
                    )
                )

            assertThat(fetchResponse.functionCallTx.blockConfirmations)
                .isNotZero()
            assertThat(fetchResponse.functionCallTx.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            assertThat(fetchResponse.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustReturn404NotFoundForNonExistentContractFunctionCallRequest() {
        verify("404 is returned for non-existent contract function call request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/function-call/${UUID.randomUUID()}")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    fun mustCorrectlyFetchContractFunctionCallRequestsByProjectIdAndFilters() {
        val ownerAddress = WalletAddress("a")
        val mainAccount = accounts[0]

        val contract = suppose("contract is deployed") {
            ExampleContract.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                ownerAddress.rawValue
            ).send()
        }

        val callerAddress = WalletAddress(mainAccount.address)
        val contractAddress = ContractAddress(contract.contractAddress)
        val functionName = "setOwner"
        val ethAmount = Balance.ZERO

        val storedContract = suppose("some deployed contract exists in the database") {
            contractDeploymentRequestRepository.store(DEPLOYED_CONTRACT, Constants.NIL_UUID).apply {
                contractDeploymentRequestRepository.setContractAddress(DEPLOYED_CONTRACT.id, contractAddress)
            }
        }

        val paramsJson =
            """
                [
                    {
                        "type": "address",
                        "value": "${callerAddress.rawValue}"
                    }
                ]
            """.trimIndent()

        val createResponse = suppose("request to create contract function call request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/function-call")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "deployed_contract_id": "${storedContract.id}",
                                "function_name": "$functionName",
                                "function_params": $paramsJson,
                                "eth_amount": "${ethAmount.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                },
                                "caller_address": "${callerAddress.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(
                createResponse.response.contentAsString,
                ContractFunctionCallRequestResponse::class.java
            )
        }

        val txHash = suppose("function is called") {
            contract.setOwner(callerAddress.rawValue).send()?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("transaction info is attached to contract function call request") {
            contractFunctionCallRequestRepository.setTxInfo(createResponse.id, txHash, callerAddress)
        }

        val fetchResponse = suppose("request to fetch contract function call requests by project ID is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get(
                    "/v1/function-call/by-project/${createResponse.projectId}?deployedContractId=${storedContract.id}" +
                        "&contractAddress=${contractAddress.rawValue}"
                )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(
                fetchResponse.response.contentAsString,
                ContractFunctionCallRequestsResponse::class.java
            )
        }

        verify("correct response is returned") {
            assertThat(fetchResponse).withMessage()
                .isEqualTo(
                    ContractFunctionCallRequestsResponse(
                        listOf(
                            ContractFunctionCallRequestResponse(
                                id = createResponse.id,
                                status = Status.SUCCESS,
                                deployedContractId = storedContract.id,
                                contractAddress = contractAddress.rawValue,
                                functionName = functionName,
                                functionParams = objectMapper.readTree(paramsJson),
                                functionCallData = createResponse.functionCallData,
                                ethAmount = ethAmount.rawValue,
                                chainId = PROJECT.chainId.value,
                                redirectUrl = PROJECT.baseRedirectUrl.value +
                                    "/request-function-call/${createResponse.id}/action",
                                projectId = PROJECT_ID,
                                createdAt = fetchResponse.requests[0].createdAt,
                                arbitraryData = createResponse.arbitraryData,
                                screenConfig = ScreenConfig(
                                    beforeActionMessage = "before-action-message",
                                    afterActionMessage = "after-action-message"
                                ),
                                callerAddress = callerAddress.rawValue,
                                functionCallTx = TransactionResponse(
                                    txHash = txHash.value,
                                    from = callerAddress.rawValue,
                                    to = contractAddress.rawValue,
                                    data = createResponse.functionCallTx.data,
                                    value = ethAmount.rawValue,
                                    blockConfirmations = fetchResponse.requests[0].functionCallTx.blockConfirmations,
                                    timestamp = fetchResponse.requests[0].functionCallTx.timestamp
                                )
                            )
                        )
                    )
                )

            assertThat(fetchResponse.requests[0].functionCallTx.blockConfirmations)
                .isNotZero()
            assertThat(fetchResponse.requests[0].functionCallTx.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            assertThat(fetchResponse.requests[0].createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchContractFunctionCallRequestsByProjectIdAndFiltersWhenCustomRpcUrlIsSpecified() {
        val ownerAddress = WalletAddress("a")
        val mainAccount = accounts[0]

        val contract = suppose("contract is deployed") {
            ExampleContract.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                ownerAddress.rawValue
            ).send()
        }

        val callerAddress = WalletAddress(mainAccount.address)
        val contractAddress = ContractAddress(contract.contractAddress)
        val functionName = "setOwner"
        val ethAmount = Balance.ZERO

        val (projectId, chainId, apiKey) = suppose("project with customRpcUrl is inserted into database") {
            insertProjectWithCustomRpcUrl()
        }

        val storedContract = suppose("some deployed contract exists in the database") {
            contractDeploymentRequestRepository.store(DEPLOYED_CONTRACT, Constants.NIL_UUID).apply {
                contractDeploymentRequestRepository.setContractAddress(DEPLOYED_CONTRACT.id, contractAddress)
            }
        }

        val paramsJson =
            """
                [
                    {
                        "type": "address",
                        "value": "${callerAddress.rawValue}"
                    }
                ]
            """.trimIndent()

        val createResponse = suppose("request to create contract function call request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/function-call")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "deployed_contract_id": "${storedContract.id}",
                                "function_name": "$functionName",
                                "function_params": $paramsJson,
                                "eth_amount": "${ethAmount.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                },
                                "caller_address": "${callerAddress.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(
                createResponse.response.contentAsString,
                ContractFunctionCallRequestResponse::class.java
            )
        }

        val txHash = suppose("function is called") {
            contract.setOwner(callerAddress.rawValue).send()?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("transaction info is attached to contract function call request") {
            contractFunctionCallRequestRepository.setTxInfo(createResponse.id, txHash, callerAddress)
        }

        val fetchResponse = suppose("request to fetch contract function call requests by project ID is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get(
                    "/v1/function-call/by-project/${createResponse.projectId}?deployedContractId=${storedContract.id}" +
                        "&contractAddress=${contractAddress.rawValue}"
                )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(
                fetchResponse.response.contentAsString,
                ContractFunctionCallRequestsResponse::class.java
            )
        }

        verify("correct response is returned") {
            assertThat(fetchResponse).withMessage()
                .isEqualTo(
                    ContractFunctionCallRequestsResponse(
                        listOf(
                            ContractFunctionCallRequestResponse(
                                id = createResponse.id,
                                status = Status.SUCCESS,
                                deployedContractId = storedContract.id,
                                contractAddress = contractAddress.rawValue,
                                functionName = functionName,
                                functionParams = objectMapper.readTree(paramsJson),
                                functionCallData = createResponse.functionCallData,
                                ethAmount = ethAmount.rawValue,
                                chainId = chainId.value,
                                redirectUrl = PROJECT.baseRedirectUrl.value +
                                    "/request-function-call/${createResponse.id}/action",
                                projectId = projectId,
                                createdAt = fetchResponse.requests[0].createdAt,
                                arbitraryData = createResponse.arbitraryData,
                                screenConfig = ScreenConfig(
                                    beforeActionMessage = "before-action-message",
                                    afterActionMessage = "after-action-message"
                                ),
                                callerAddress = callerAddress.rawValue,
                                functionCallTx = TransactionResponse(
                                    txHash = txHash.value,
                                    from = callerAddress.rawValue,
                                    to = contractAddress.rawValue,
                                    data = createResponse.functionCallTx.data,
                                    value = ethAmount.rawValue,
                                    blockConfirmations = fetchResponse.requests[0].functionCallTx.blockConfirmations,
                                    timestamp = fetchResponse.requests[0].functionCallTx.timestamp
                                )
                            )
                        )
                    )
                )

            assertThat(fetchResponse.requests[0].functionCallTx.blockConfirmations)
                .isNotZero()
            assertThat(fetchResponse.requests[0].functionCallTx.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            assertThat(fetchResponse.requests[0].createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyAttachTransactionInfo() {
        val id = UUID.randomUUID()
        val callerAddress = WalletAddress("c")

        suppose("some contract function call request without transaction info exists in database") {
            contractFunctionCallRequestRepository.store(
                StoreContractFunctionCallRequestParams(
                    id = id,
                    deployedContractId = null,
                    contractAddress = ContractAddress("a"),
                    functionName = "test",
                    functionParams = TestData.EMPTY_JSON_ARRAY,
                    ethAmount = Balance(BigInteger.TEN),
                    chainId = Chain.HARDHAT_TESTNET.id,
                    redirectUrl = "https://example.com/$id",
                    projectId = PROJECT_ID,
                    createdAt = TestData.TIMESTAMP,
                    arbitraryData = TestData.EMPTY_JSON_OBJECT,
                    screenConfig = ScreenConfig(
                        beforeActionMessage = "before-action-message",
                        afterActionMessage = "after-action-message"
                    ),
                    callerAddress = WalletAddress("b")
                )
            )
        }

        val txHash = TransactionHash("0x1")

        suppose("request to attach transaction info to contract deployment request is made") {
            mockMvc.perform(
                MockMvcRequestBuilders.put("/v1/function-call/$id")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "tx_hash": "${txHash.value}",
                                "caller_address": "${callerAddress.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
        }

        verify("transaction info is correctly attached to contract deployment request") {
            val storedRequest = contractFunctionCallRequestRepository.getById(id)

            assertThat(storedRequest?.txHash)
                .isEqualTo(txHash)
        }
    }

    @Test
    fun mustReturn400BadRequestWhenTransactionInfoIsNotAttached() {
        val id = UUID.randomUUID()
        val txHash = TransactionHash("0x1")
        val callerAddress = WalletAddress("c")

        suppose("some contract function call request with transaction info exists in database") {
            contractFunctionCallRequestRepository.store(
                StoreContractFunctionCallRequestParams(
                    id = id,
                    deployedContractId = null,
                    contractAddress = ContractAddress("a"),
                    functionName = "test",
                    functionParams = TestData.EMPTY_JSON_ARRAY,
                    ethAmount = Balance(BigInteger.TEN),
                    chainId = Chain.HARDHAT_TESTNET.id,
                    redirectUrl = "https://example.com/$id",
                    projectId = PROJECT_ID,
                    createdAt = TestData.TIMESTAMP,
                    arbitraryData = TestData.EMPTY_JSON_OBJECT,
                    screenConfig = ScreenConfig(
                        beforeActionMessage = "before-action-message",
                        afterActionMessage = "after-action-message"
                    ),
                    callerAddress = WalletAddress("b")
                )
            )
            contractFunctionCallRequestRepository.setTxInfo(id, txHash, callerAddress)
        }

        verify("400 is returned when attaching transaction info") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.put("/v1/function-call/$id")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "tx_hash": "0x2",
                                "caller_address": "${callerAddress.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.TX_INFO_ALREADY_SET)
        }

        verify("transaction info is not changed in database") {
            val storedRequest = contractFunctionCallRequestRepository.getById(id)

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
}
