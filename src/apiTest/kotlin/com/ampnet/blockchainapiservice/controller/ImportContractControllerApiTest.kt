package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.ControllerTestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.blockchain.ExampleContract
import com.ampnet.blockchainapiservice.blockchain.properties.Chain
import com.ampnet.blockchainapiservice.config.binding.ProjectApiKeyResolver
import com.ampnet.blockchainapiservice.exception.ErrorCode
import com.ampnet.blockchainapiservice.generated.jooq.enums.UserIdentifierType
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ApiKeyRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ContractMetadataRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ProjectRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.UserIdentifierRecord
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.json.FunctionDecorator
import com.ampnet.blockchainapiservice.model.json.InterfaceManifestJson
import com.ampnet.blockchainapiservice.model.json.TypeDecorator
import com.ampnet.blockchainapiservice.model.response.ContractDeploymentRequestResponse
import com.ampnet.blockchainapiservice.model.response.ContractInterfaceManifestResponse
import com.ampnet.blockchainapiservice.model.response.ContractInterfaceManifestsResponse
import com.ampnet.blockchainapiservice.model.response.TransactionResponse
import com.ampnet.blockchainapiservice.model.result.ContractConstructor
import com.ampnet.blockchainapiservice.model.result.ContractDecorator
import com.ampnet.blockchainapiservice.model.result.ContractDeploymentRequest
import com.ampnet.blockchainapiservice.model.result.ContractFunction
import com.ampnet.blockchainapiservice.model.result.ContractParameter
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.repository.ContractDecoratorRepository
import com.ampnet.blockchainapiservice.repository.ContractDeploymentRequestRepository
import com.ampnet.blockchainapiservice.repository.ContractInterfacesRepository
import com.ampnet.blockchainapiservice.repository.ImportedContractDecoratorRepository
import com.ampnet.blockchainapiservice.service.ContractImportServiceImpl.Companion.TypeAndValue
import com.ampnet.blockchainapiservice.testcontainers.HardhatTestContainer
import com.ampnet.blockchainapiservice.testcontainers.SharedTestContainers
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.BaseUrl
import com.ampnet.blockchainapiservice.util.Constants
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.ContractBinaryData
import com.ampnet.blockchainapiservice.util.ContractId
import com.ampnet.blockchainapiservice.util.ContractTag
import com.ampnet.blockchainapiservice.util.ContractTrait
import com.ampnet.blockchainapiservice.util.Status
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.ampnet.blockchainapiservice.util.ZeroAddress
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

class ImportContractControllerApiTest : ControllerTestBase() {

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
        private val CONTRACT_DECORATOR = ContractDecorator(
            id = ContractId("examples.exampleContract"),
            name = "name",
            description = "description",
            binary = ContractBinaryData(ExampleContract.BINARY),
            tags = listOf(ContractTag("example"), ContractTag("simple")),
            implements = listOf(ContractTrait("traits.example"), ContractTrait("traits.exampleOwnable")),
            constructors = listOf(
                ContractConstructor(
                    inputs = listOf(
                        ContractParameter(
                            name = "Owner address",
                            description = "Contract owner address",
                            solidityName = "owner",
                            solidityType = "address",
                            recommendedTypes = listOf(),
                            parameters = null
                        )
                    ),
                    description = "Main constructor",
                    payable = true
                )
            ),
            functions = listOf(
                ContractFunction(
                    name = "Get contract owner",
                    description = "Fetches contract owner",
                    solidityName = "getOWner",
                    inputs = listOf(),
                    outputs = listOf(
                        ContractParameter(
                            name = "Owner address",
                            description = "Contract owner address",
                            solidityName = "",
                            solidityType = "address",
                            recommendedTypes = listOf(),
                            parameters = null
                        )
                    ),
                    emittableEvents = emptyList(),
                    readOnly = true
                )
            ),
            events = listOf()
        )
        private val CONTRACT_INTERFACE = InterfaceManifestJson(
            name = "Example Interface",
            description = "Example smart contract interface",
            eventDecorators = emptyList(),
            functionDecorators = listOf(
                FunctionDecorator(
                    signature = "setOwner(address)",
                    name = "Set owner",
                    description = "Set contract owner",
                    parameterDecorators = listOf(
                        TypeDecorator(
                            name = "New owner",
                            description = "New owner of the cotnract",
                            recommendedTypes = emptyList(),
                            parameters = emptyList()
                        )
                    ),
                    returnDecorators = emptyList(),
                    emittableEvents = emptyList()
                ),
                FunctionDecorator(
                    signature = "getOwner()",
                    name = "Get owner",
                    description = "Get current contract owner",
                    parameterDecorators = emptyList(),
                    returnDecorators = listOf(
                        TypeDecorator(
                            name = "Current owner",
                            description = "Current owner of the cotnract",
                            recommendedTypes = emptyList(),
                            parameters = emptyList()
                        )
                    ),
                    emittableEvents = emptyList()
                )
            )
        )
    }

    private val accounts = HardhatTestContainer.ACCOUNTS

    @Suppress("unused")
    protected val manifestServiceContainer = SharedTestContainers.manifestServiceContainer

    @Autowired
    private lateinit var contractDeploymentRequestRepository: ContractDeploymentRequestRepository

    @Autowired
    private lateinit var contractDecoratorRepository: ContractDecoratorRepository

    @Autowired
    private lateinit var importedContractDecoratorRepository: ImportedContractDecoratorRepository

    @Autowired
    private lateinit var contractInterfacesRepository: ContractInterfacesRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        postgresContainer.cleanAllDatabaseTables(dslContext)

        dslContext.executeInsert(
            ContractMetadataRecord(
                id = UUID.randomUUID(),
                name = CONTRACT_DECORATOR.name,
                description = CONTRACT_DECORATOR.description,
                contractId = CONTRACT_DECORATOR.id,
                contractTags = CONTRACT_DECORATOR.tags.map { it.value }.toTypedArray(),
                contractImplements = CONTRACT_DECORATOR.implements.map { it.value }.toTypedArray(),
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

        contractDecoratorRepository.delete(CONTRACT_DECORATOR.id)
    }

    @Test
    fun mustCorrectlyImportSmartContractForSomeExistingContractDecorator() {
        suppose("some contract decorator exists in the database") {
            contractDecoratorRepository.store(CONTRACT_DECORATOR)
        }

        val mainAccount = accounts[0]
        val ownerAddress = WalletAddress(HardhatTestContainer.ACCOUNT_ADDRESS_10)

        val contract = suppose("simple ERC20 contract is deployed") {
            ExampleContract.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                ownerAddress.rawValue
            ).send()
        }

        val alias = "alias"

        val response = suppose("request to import smart contract is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/import-smart-contract")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "alias": "$alias",
                                "contract_id": "${CONTRACT_DECORATOR.id.value}",
                                "contract_address": "${contract.contractAddress}",
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

            objectMapper.readValue(response.response.contentAsString, ContractDeploymentRequestResponse::class.java)
        }

        val deployerAddress = WalletAddress(mainAccount.address).rawValue
        val constructorParams = listOf(TypeAndValue(type = "address", value = ownerAddress.rawValue))

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    ContractDeploymentRequestResponse(
                        id = response.id,
                        alias = alias,
                        name = CONTRACT_DECORATOR.name,
                        description = CONTRACT_DECORATOR.description,
                        status = Status.SUCCESS,
                        contractId = CONTRACT_DECORATOR.id.value,
                        contractDeploymentData = response.contractDeploymentData,
                        constructorParams = objectMapper.valueToTree(constructorParams),
                        contractTags = CONTRACT_DECORATOR.tags.map { it.value },
                        contractImplements = CONTRACT_DECORATOR.implements.map { it.value },
                        initialEthAmount = BigInteger.ZERO,
                        chainId = PROJECT.chainId.value,
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-deploy/${response.id}/action",
                        projectId = PROJECT_ID,
                        createdAt = response.createdAt,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        contractAddress = ContractAddress(contract.contractAddress).rawValue,
                        deployerAddress = deployerAddress,
                        deployTx = TransactionResponse(
                            txHash = TransactionHash(contract.transactionReceipt.get().transactionHash).value,
                            from = deployerAddress,
                            to = ZeroAddress.rawValue,
                            data = response.deployTx.data,
                            value = BigInteger.ZERO,
                            blockConfirmations = response.deployTx.blockConfirmations,
                            timestamp = response.deployTx.timestamp
                        ),
                        imported = true
                    )
                )

            assertThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("contract deployment request is correctly stored in database") {
            val storedRequest = contractDeploymentRequestRepository.getById(response.id)

            assertThat(storedRequest).withMessage()
                .isEqualTo(
                    ContractDeploymentRequest(
                        id = response.id,
                        alias = alias,
                        name = CONTRACT_DECORATOR.name,
                        description = CONTRACT_DECORATOR.description,
                        contractId = CONTRACT_DECORATOR.id,
                        contractData = ContractBinaryData(response.contractDeploymentData),
                        constructorParams = objectMapper.valueToTree(constructorParams),
                        contractTags = CONTRACT_DECORATOR.tags,
                        contractImplements = CONTRACT_DECORATOR.implements,
                        initialEthAmount = Balance.ZERO,
                        chainId = PROJECT.chainId,
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-deploy/${response.id}/action",
                        projectId = PROJECT_ID,
                        createdAt = storedRequest!!.createdAt,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        contractAddress = ContractAddress(contract.contractAddress),
                        deployerAddress = WalletAddress(deployerAddress),
                        txHash = TransactionHash(contract.transactionReceipt.get().transactionHash),
                        imported = true
                    )
                )

            assertThat(storedRequest.createdAt.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyImportSmartContractWhenContractDecoratorIsNotSpecified() {
        suppose("some contract decorator exists in the database") {
            contractDecoratorRepository.store(CONTRACT_DECORATOR)
        }

        val mainAccount = accounts[0]
        val ownerAddress = WalletAddress(HardhatTestContainer.ACCOUNT_ADDRESS_10)

        val contract = suppose("simple ERC20 contract is deployed") {
            ExampleContract.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                ownerAddress.rawValue
            ).send()
        }

        val alias = "alias"

        val response = suppose("request to import smart contract is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/import-smart-contract")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "alias": "$alias",
                                "contract_address": "${contract.contractAddress}",
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

            objectMapper.readValue(response.response.contentAsString, ContractDeploymentRequestResponse::class.java)
        }

        val deployerAddress = WalletAddress(mainAccount.address).rawValue
        val constructorParams = listOf(
            TypeAndValue(
                type = "bytes32",
                value = ownerAddress.rawValue.removePrefix("0x").padStart(64, '0')
                    .chunked(2).map { it.toUByte(16).toByte() }
            )
        )
        val importedContractId = ContractId("imported-${contract.contractAddress}-${PROJECT.chainId.value}")

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    ContractDeploymentRequestResponse(
                        id = response.id,
                        alias = alias,
                        name = "Imported Contract",
                        description = "Imported smart contract.",
                        status = Status.SUCCESS,
                        contractId = importedContractId.value,
                        contractDeploymentData = response.contractDeploymentData,
                        constructorParams = objectMapper.valueToTree(constructorParams),
                        contractTags = emptyList(),
                        contractImplements = emptyList(),
                        initialEthAmount = BigInteger.ZERO,
                        chainId = PROJECT.chainId.value,
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-deploy/${response.id}/action",
                        projectId = PROJECT_ID,
                        createdAt = response.createdAt,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        contractAddress = ContractAddress(contract.contractAddress).rawValue,
                        deployerAddress = deployerAddress,
                        deployTx = TransactionResponse(
                            txHash = TransactionHash(contract.transactionReceipt.get().transactionHash).value,
                            from = deployerAddress,
                            to = ZeroAddress.rawValue,
                            data = response.deployTx.data,
                            value = BigInteger.ZERO,
                            blockConfirmations = response.deployTx.blockConfirmations,
                            timestamp = response.deployTx.timestamp
                        ),
                        imported = true
                    )
                )

            assertThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("contract deployment request is correctly stored in database") {
            val storedRequest = contractDeploymentRequestRepository.getById(response.id)

            assertThat(storedRequest).withMessage()
                .isEqualTo(
                    ContractDeploymentRequest(
                        id = response.id,
                        alias = alias,
                        name = "Imported Contract",
                        description = "Imported smart contract.",
                        contractId = importedContractId,
                        contractData = ContractBinaryData(response.contractDeploymentData),
                        constructorParams = objectMapper.valueToTree(constructorParams),
                        contractTags = emptyList(),
                        contractImplements = emptyList(),
                        initialEthAmount = Balance.ZERO,
                        chainId = PROJECT.chainId,
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-deploy/${response.id}/action",
                        projectId = PROJECT_ID,
                        createdAt = storedRequest!!.createdAt,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        contractAddress = ContractAddress(contract.contractAddress),
                        deployerAddress = WalletAddress(deployerAddress),
                        txHash = TransactionHash(contract.transactionReceipt.get().transactionHash),
                        imported = true
                    )
                )

            assertThat(storedRequest.createdAt.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("imported contract decorator is correctly stored in database") {
            val importedContractDecorator = importedContractDecoratorRepository.getByContractIdAndProjectId(
                contractId = importedContractId,
                projectId = PROJECT_ID
            )

            assertThat(importedContractDecorator).withMessage()
                .isEqualTo(
                    CONTRACT_DECORATOR.copy(
                        id = importedContractId,
                        name = "Imported Contract",
                        description = "Imported smart contract.",
                        tags = emptyList(),
                        implements = emptyList(),
                        constructors = emptyList(),
                        events = emptyList(),
                        functions = listOf(
                            ContractFunction(
                                name = "setOwner",
                                description = "",
                                solidityName = "setOwner",
                                inputs = listOf(
                                    ContractParameter(
                                        name = "param1",
                                        description = "",
                                        solidityName = "param1",
                                        solidityType = "address",
                                        recommendedTypes = emptyList(),
                                        parameters = null
                                    )
                                ),
                                outputs = emptyList(),
                                emittableEvents = emptyList(),
                                readOnly = false
                            ),
                            ContractFunction(
                                name = "getOwner",
                                description = "",
                                solidityName = "getOwner",
                                inputs = emptyList(),
                                outputs = emptyList(),
                                emittableEvents = emptyList(),
                                readOnly = false
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustReturn404NotFoundWhenImportingSmartContractForNonExistentContractDecorator() {
        verify("404 is returned for non-existent contract decorator") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/import-smart-contract")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "alias": "alias",
                                "contract_id": "${CONTRACT_DECORATOR.id.value}",
                                "contract_address": "${ContractAddress("abc").rawValue}",
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
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    fun mustReturn404NotFoundWhenImportingNonExistentSmartContract() {
        suppose("some contract decorator exists in the database") {
            contractDecoratorRepository.store(CONTRACT_DECORATOR)
        }

        verify("404 is returned for non-existent smart contract") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/import-smart-contract")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "alias": "alias",
                                "contract_id": "${CONTRACT_DECORATOR.id.value}",
                                "contract_address": "${ContractAddress("abc").rawValue}",
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
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.CONTRACT_NOT_FOUND)
        }
    }

    @Test
    fun mustReturn400BadRequestWhenContractBinaryMismatchesRequestedContractDecoratorBinary() {
        suppose("some contract decorator exists in the database") {
            contractDecoratorRepository.store(CONTRACT_DECORATOR.copy(binary = ContractBinaryData("abc")))
        }

        val mainAccount = accounts[0]
        val ownerAddress = WalletAddress(HardhatTestContainer.ACCOUNT_ADDRESS_10)

        val contract = suppose("simple ERC20 contract is deployed") {
            ExampleContract.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                ownerAddress.rawValue
            ).send()
        }

        verify("400 is returned for mismatching smart contract binary") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/import-smart-contract")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "alias": "alias",
                                "contract_id": "${CONTRACT_DECORATOR.id.value}",
                                "contract_address": "${contract.contractAddress}",
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

            verifyResponseErrorCode(response, ErrorCode.CONTRACT_BINARY_MISMATCH)
        }
    }

    @Test
    fun mustCorrectlySuggestInterfacesForImportedSmartContractWhenContractDecoratorIsNotSpecified() {
        suppose("some contract decorator exists in the database") {
            contractDecoratorRepository.store(CONTRACT_DECORATOR)
        }

        val mainAccount = accounts[0]
        val ownerAddress = WalletAddress(HardhatTestContainer.ACCOUNT_ADDRESS_10)

        val contract = suppose("simple ERC20 contract is deployed") {
            ExampleContract.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                ownerAddress.rawValue
            ).send()
        }

        val alias = "alias"

        val importResponse = suppose("request to import smart contract is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/import-smart-contract")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "alias": "$alias",
                                "contract_address": "${contract.contractAddress}",
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

            objectMapper.readValue(response.response.contentAsString, ContractDeploymentRequestResponse::class.java)
        }

        val contractId = ContractId("example.ownable")

        suppose("some contract interface is in the repository") {
            contractInterfacesRepository.store(contractId, CONTRACT_INTERFACE)
        }

        val suggestedInterfacesResponse = suppose("suggested imported smart contract interfaces are fetched") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/import-smart-contract/${importResponse.id}/suggested-interfaces")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ContractInterfaceManifestsResponse::class.java)
        }

        verify("correct interface manifests are returned") {
            assertThat(suggestedInterfacesResponse).withMessage()
                .isEqualTo(
                    ContractInterfaceManifestsResponse(
                        listOf(
                            ContractInterfaceManifestResponse(contractId, CONTRACT_INTERFACE)
                        )
                    )
                )
        }
    }
}
