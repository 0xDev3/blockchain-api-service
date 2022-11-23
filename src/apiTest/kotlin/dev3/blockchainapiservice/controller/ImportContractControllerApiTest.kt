package dev3.blockchainapiservice.controller

import dev3.blockchainapiservice.ControllerTestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.blockchain.DummyProxy
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
import dev3.blockchainapiservice.model.json.ArtifactJson
import dev3.blockchainapiservice.model.json.FunctionDecorator
import dev3.blockchainapiservice.model.json.InterfaceManifestJson
import dev3.blockchainapiservice.model.json.ManifestJson
import dev3.blockchainapiservice.model.json.ReturnTypeDecorator
import dev3.blockchainapiservice.model.json.TypeDecorator
import dev3.blockchainapiservice.model.response.ContractDecoratorResponse
import dev3.blockchainapiservice.model.response.ContractDeploymentRequestResponse
import dev3.blockchainapiservice.model.response.ContractInterfaceManifestResponse
import dev3.blockchainapiservice.model.response.ContractInterfaceManifestsResponse
import dev3.blockchainapiservice.model.response.EventArgumentResponse
import dev3.blockchainapiservice.model.response.EventArgumentResponseType
import dev3.blockchainapiservice.model.response.EventInfoResponse
import dev3.blockchainapiservice.model.response.ImportPreviewResponse
import dev3.blockchainapiservice.model.response.TransactionResponse
import dev3.blockchainapiservice.model.result.ContractConstructor
import dev3.blockchainapiservice.model.result.ContractDecorator
import dev3.blockchainapiservice.model.result.ContractDeploymentRequest
import dev3.blockchainapiservice.model.result.ContractEvent
import dev3.blockchainapiservice.model.result.ContractFunction
import dev3.blockchainapiservice.model.result.ContractParameter
import dev3.blockchainapiservice.model.result.EventParameter
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.repository.ContractDecoratorRepository
import dev3.blockchainapiservice.repository.ContractDeploymentRequestRepository
import dev3.blockchainapiservice.repository.ContractInterfacesRepository
import dev3.blockchainapiservice.repository.ContractMetadataRepository
import dev3.blockchainapiservice.repository.ImportedContractDecoratorRepository
import dev3.blockchainapiservice.service.ContractImportServiceImpl.Companion.TypeAndValue
import dev3.blockchainapiservice.testcontainers.HardhatTestContainer
import dev3.blockchainapiservice.testcontainers.SharedTestContainers
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.BaseUrl
import dev3.blockchainapiservice.util.Constants
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.ContractBinaryData
import dev3.blockchainapiservice.util.ContractId
import dev3.blockchainapiservice.util.ContractTag
import dev3.blockchainapiservice.util.InterfaceId
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
            implements = listOf(InterfaceId("traits.example"), InterfaceId("traits.exampleOwnable")),
            constructors = listOf(
                ContractConstructor(
                    inputs = listOf(
                        ContractParameter(
                            name = "Owner address",
                            description = "Contract owner address",
                            solidityName = "owner",
                            solidityType = "address",
                            recommendedTypes = listOf(),
                            parameters = null,
                            hints = null
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
                    solidityName = "getOwner",
                    signature = "getOwner()",
                    inputs = listOf(),
                    outputs = listOf(
                        ContractParameter(
                            name = "Owner address",
                            description = "Contract owner address",
                            solidityName = "",
                            solidityType = "address",
                            recommendedTypes = listOf(),
                            parameters = null,
                            hints = null
                        )
                    ),
                    emittableEvents = emptyList(),
                    readOnly = true
                )
            ),
            listOf(
                ContractEvent(
                    name = "Example event",
                    description = "Example event",
                    solidityName = "ExampleEvent",
                    signature = "ExampleEvent(tuple(address),tuple(address))",
                    inputs = listOf(
                        EventParameter(
                            name = "Non-indexed struct",
                            description = "Non-indexed struct",
                            indexed = false,
                            solidityName = "nonIndexedStruct",
                            solidityType = "tuple",
                            recommendedTypes = emptyList(),
                            parameters = listOf(
                                ContractParameter(
                                    name = "Owner address",
                                    description = "Contract owner address",
                                    solidityName = "owner",
                                    solidityType = "address",
                                    recommendedTypes = emptyList(),
                                    parameters = null,
                                    hints = null
                                )
                            ),
                            hints = null
                        ),
                        EventParameter(
                            name = "Indexed struct",
                            description = "Indexed struct",
                            indexed = true,
                            solidityName = "indexedStruct",
                            solidityType = "tuple",
                            recommendedTypes = emptyList(),
                            parameters = listOf(
                                ContractParameter(
                                    name = "Owner address",
                                    description = "Contract owner address",
                                    solidityName = "owner",
                                    solidityType = "address",
                                    recommendedTypes = emptyList(),
                                    parameters = null,
                                    hints = null
                                )
                            ),
                            hints = null
                        )
                    )
                )
            ),
            manifest = ManifestJson.EMPTY,
            artifact = ArtifactJson.EMPTY
        )
        private val CONTRACT_INTERFACE = InterfaceManifestJson(
            name = "Example Interface",
            description = "Example smart contract interface",
            tags = setOf("interface-tag"),
            eventDecorators = emptyList(),
            functionDecorators = listOf(
                FunctionDecorator(
                    signature = "setOwner(address)",
                    name = "Set owner",
                    description = "Set contract owner",
                    parameterDecorators = listOf(
                        TypeDecorator(
                            name = "New owner",
                            description = "New owner of the contract",
                            recommendedTypes = emptyList(),
                            parameters = emptyList(),
                            hints = emptyList()
                        )
                    ),
                    returnDecorators = emptyList(),
                    emittableEvents = emptyList(),
                    readOnly = false
                ),
                FunctionDecorator(
                    signature = "getOwner()",
                    name = "Get owner",
                    description = "Get current contract owner",
                    parameterDecorators = emptyList(),
                    returnDecorators = listOf(
                        ReturnTypeDecorator(
                            name = "Current owner",
                            description = "Current owner of the contract",
                            solidityType = "address",
                            recommendedTypes = emptyList(),
                            parameters = emptyList(),
                            hints = emptyList()
                        )
                    ),
                    emittableEvents = emptyList(),
                    readOnly = true
                )
            )
        )
        private val INCOMPATIBLE_INTERFACE = InterfaceManifestJson(
            name = "Incomaptible Interface",
            description = "Incomaptible smart contract interface",
            tags = setOf("interface-tag"),
            eventDecorators = emptyList(),
            functionDecorators = listOf(
                FunctionDecorator(
                    signature = "nonExistent()",
                    name = "",
                    description = "",
                    parameterDecorators = emptyList(),
                    returnDecorators = emptyList(),
                    emittableEvents = emptyList(),
                    readOnly = false
                )
            )
        )
        private val EVENTS_1 = listOf(
            EventInfoResponse(
                signature = "ExampleEvent(tuple(address),tuple(address))",
                arguments = listOf(
                    EventArgumentResponse(
                        name = "nonIndexedStruct",
                        type = EventArgumentResponseType.VALUE,
                        value = listOf(WalletAddress("a").rawValue),
                        hash = null
                    ),
                    EventArgumentResponse(
                        name = "indexedStruct",
                        type = EventArgumentResponseType.HASH,
                        value = null,
                        hash = "0xc65a7bb8d6351c1cf70c95a316cc6a92839c986682d98bc35f958f4883f9d2a8"
                    )
                )
            )
        )
        private val EVENTS_2 = listOf(
            EventInfoResponse(
                signature = "ExampleEvent(tuple(address),tuple(address))",
                arguments = listOf(
                    EventArgumentResponse(
                        name = "nonIndexedStruct",
                        type = EventArgumentResponseType.VALUE,
                        value = listOf("0x35e13c4870077f4610b74f23e887cbb10e21c19f"),
                        hash = null
                    ),
                    EventArgumentResponse(
                        name = "indexedStruct",
                        type = EventArgumentResponseType.HASH,
                        value = null,
                        hash = "0xe412d7b15343cf0762057bcdfc6e0e1196c887a6e48273443abb85a65433a9e2"
                    )
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
    private lateinit var contractMetadataRepository: ContractMetadataRepository

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
    fun mustCorrectlyPreviewSmartContractImport() {
        val interfaceId = InterfaceId("example.ownable")

        suppose("some contract interface is in the repository") {
            contractInterfacesRepository.store(interfaceId, CONTRACT_INTERFACE)
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

        val chainId = Chain.HARDHAT_TESTNET.id
        val contractAddress = ContractAddress(contract.contractAddress)

        val response = suppose("request to preview smart contract import is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get(
                    "/v1/import-smart-contract/preview/${chainId.value}/contract/${contractAddress.rawValue}"
                )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ImportPreviewResponse::class.java)
        }

        val importedContractId = ContractId("imported-${contractAddress.rawValue}-${chainId.value}")

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    ImportPreviewResponse(
                        manifest = response.manifest,
                        artifact = response.artifact,
                        decorator = ContractDecoratorResponse(
                            id = importedContractId.value,
                            name = "Imported Contract",
                            description = "Imported smart contract.",
                            binary = response.decorator.binary,
                            tags = listOf("interface-tag"),
                            implements = listOf(interfaceId.value),
                            constructors = emptyList(),
                            functions = listOf(
                                ContractFunction(
                                    name = "Set owner",
                                    description = "Set contract owner",
                                    solidityName = "setOwner",
                                    signature = "setOwner(address)",
                                    inputs = listOf(
                                        ContractParameter(
                                            name = "New owner",
                                            description = "New owner of the contract",
                                            solidityName = "param1",
                                            solidityType = "address",
                                            recommendedTypes = emptyList(),
                                            parameters = emptyList(),
                                            hints = emptyList()
                                        )
                                    ),
                                    outputs = emptyList(),
                                    emittableEvents = emptyList(),
                                    readOnly = false
                                ),
                                ContractFunction(
                                    name = "Get owner",
                                    description = "Get current contract owner",
                                    solidityName = "getOwner",
                                    signature = "getOwner()",
                                    inputs = emptyList(),
                                    outputs = listOf(
                                        ContractParameter(
                                            name = "Current owner",
                                            description = "Current owner of the contract",
                                            solidityName = "",
                                            solidityType = "address",
                                            recommendedTypes = emptyList(),
                                            parameters = emptyList(),
                                            hints = emptyList()
                                        )
                                    ),
                                    emittableEvents = emptyList(),
                                    readOnly = true
                                )
                            ),
                            events = emptyList()
                        )
                    )
                )
        }

        verify("imported contract decorator is not stored in database") {
            val importedContractDecorator = importedContractDecoratorRepository.getByContractIdAndProjectId(
                contractId = importedContractId,
                projectId = Constants.NIL_UUID
            )

            assertThat(importedContractDecorator).withMessage()
                .isEqualTo(null)
        }
    }

    @Test
    fun mustCorrectlyImportAlreadyDeployedSmartContract() {
        val alias = "alias"
        val ownerAddress = WalletAddress("a")
        val mainAccount = accounts[0]
        val deployerAddress = WalletAddress(mainAccount.address)
        val initialEthAmount = Balance.ZERO

        suppose("some contract decorator exists in the database") {
            contractDecoratorRepository.store(CONTRACT_DECORATOR)
        }

        val paramsJson =
            """
                [
                    {
                        "type": "address",
                        "value": "${ownerAddress.rawValue}"
                    }
                ]
            """.trimIndent()

        val createResponse = suppose("request to create contract deployment request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/deploy")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "alias": "$alias",
                                "contract_id": "${CONTRACT_DECORATOR.id.value}",
                                "constructor_params": $paramsJson,
                                "deployer_address": "${deployerAddress.rawValue}",
                                "initial_eth_amount": "${initialEthAmount.rawValue}",
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

            objectMapper.readValue(
                createResponse.response.contentAsString,
                ContractDeploymentRequestResponse::class.java
            )
        }

        val contract = suppose("contract is deployed") {
            ExampleContract.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                ownerAddress.rawValue
            ).send()
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        val receipt = contract.transactionReceipt.get()
        val txHash = TransactionHash(receipt.transactionHash)
        val contractAddress = ContractAddress(receipt.contractAddress)

        suppose("transaction info is attached to contract deployment request") {
            contractDeploymentRequestRepository.setTxInfo(createResponse.id, txHash, deployerAddress)
            contractDeploymentRequestRepository.setContractAddress(createResponse.id, contractAddress)
        }

        val newAlias = "new-alias"
        val importResponse = suppose("request to import smart contract is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/import-smart-contract")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "alias": "$newAlias",
                                "contract_address": "${contractAddress.rawValue}",
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

        verify("correct response is returned") {
            assertThat(importResponse).withMessage()
                .isEqualTo(
                    ContractDeploymentRequestResponse(
                        id = importResponse.id,
                        alias = newAlias,
                        name = CONTRACT_DECORATOR.name,
                        description = CONTRACT_DECORATOR.description,
                        status = Status.SUCCESS,
                        contractId = CONTRACT_DECORATOR.id.value,
                        contractDeploymentData = createResponse.contractDeploymentData,
                        constructorParams = createResponse.constructorParams,
                        contractTags = CONTRACT_DECORATOR.tags.map { it.value },
                        contractImplements = CONTRACT_DECORATOR.implements.map { it.value },
                        initialEthAmount = BigInteger.ZERO,
                        chainId = PROJECT.chainId.value,
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-deploy/${importResponse.id}/action",
                        projectId = PROJECT_ID,
                        createdAt = importResponse.createdAt,
                        arbitraryData = importResponse.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        contractAddress = ContractAddress(contract.contractAddress).rawValue,
                        deployerAddress = deployerAddress.rawValue,
                        deployTx = createResponse.deployTx.copy(
                            txHash = txHash.value,
                            blockConfirmations = importResponse.deployTx.blockConfirmations,
                            timestamp = importResponse.deployTx.timestamp
                        ),
                        imported = false,
                        proxy = false,
                        implementationContractAddress = null,
                        events = EVENTS_1
                    )
                )

            assertThat(importResponse.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("contract deployment request is correctly stored in database") {
            val storedRequest = contractDeploymentRequestRepository.getById(importResponse.id)

            assertThat(storedRequest).withMessage()
                .isEqualTo(
                    ContractDeploymentRequest(
                        id = importResponse.id,
                        alias = newAlias,
                        name = CONTRACT_DECORATOR.name,
                        description = CONTRACT_DECORATOR.description,
                        contractId = CONTRACT_DECORATOR.id,
                        contractData = ContractBinaryData(createResponse.contractDeploymentData),
                        constructorParams = createResponse.constructorParams,
                        contractTags = CONTRACT_DECORATOR.tags,
                        contractImplements = CONTRACT_DECORATOR.implements,
                        initialEthAmount = Balance.ZERO,
                        chainId = PROJECT.chainId,
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-deploy/${importResponse.id}/action",
                        projectId = PROJECT_ID,
                        createdAt = storedRequest!!.createdAt,
                        arbitraryData = importResponse.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        contractAddress = contractAddress,
                        deployerAddress = deployerAddress,
                        txHash = txHash,
                        imported = false,
                        proxy = false,
                        implementationContractAddress = null
                    )
                )

            assertThat(storedRequest.createdAt.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
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
                        imported = true,
                        proxy = false,
                        implementationContractAddress = null,
                        events = EVENTS_2
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
                        imported = true,
                        proxy = false,
                        implementationContractAddress = null
                    )
                )

            assertThat(storedRequest.createdAt.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyImportSmartContractWhenContractDecoratorIsNotSpecified() {
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
                        imported = true,
                        proxy = false,
                        implementationContractAddress = null,
                        events = response.events
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
                        imported = true,
                        proxy = false,
                        implementationContractAddress = null
                    )
                )

            assertThat(storedRequest.createdAt.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("imported contract decorator is correctly stored in database") {
            val importedContractDecorator = importedContractDecoratorRepository.getByContractIdAndProjectId(
                contractId = importedContractId,
                projectId = PROJECT_ID
            )!!

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
                                signature = "setOwner(address)",
                                inputs = listOf(
                                    ContractParameter(
                                        name = "param1",
                                        description = "",
                                        solidityName = "param1",
                                        solidityType = "address",
                                        recommendedTypes = emptyList(),
                                        parameters = null,
                                        hints = null
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
                                signature = "getOwner()",
                                inputs = emptyList(),
                                outputs = emptyList(),
                                emittableEvents = emptyList(),
                                readOnly = false
                            )
                        ),
                        manifest = importedContractDecorator.manifest,
                        artifact = importedContractDecorator.artifact
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyImportProxySmartContractWhenContractDecoratorIsNotSpecified() {
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

        val proxy = suppose("proxy contract is deployed") {
            DummyProxy.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                contract.contractAddress
            ).send()
        }

        val alias = "alias"

        val response = suppose("request to import proxy smart contract is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/import-smart-contract")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "alias": "$alias",
                                "contract_address": "${proxy.contractAddress}",
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
                value = ContractAddress(contract.contractAddress).rawValue.removePrefix("0x").padStart(64, '0')
                    .chunked(2).map { it.toUByte(16).toByte() }
            )
        )
        val importedContractId = ContractId("imported-${proxy.contractAddress}-${PROJECT.chainId.value}")

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
                        contractAddress = ContractAddress(proxy.contractAddress).rawValue,
                        deployerAddress = deployerAddress,
                        deployTx = TransactionResponse(
                            txHash = TransactionHash(proxy.transactionReceipt.get().transactionHash).value,
                            from = deployerAddress,
                            to = ZeroAddress.rawValue,
                            data = response.deployTx.data,
                            value = BigInteger.ZERO,
                            blockConfirmations = response.deployTx.blockConfirmations,
                            timestamp = response.deployTx.timestamp
                        ),
                        imported = true,
                        proxy = true,
                        implementationContractAddress = ContractAddress(contract.contractAddress).rawValue,
                        events = emptyList()
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
                        contractAddress = ContractAddress(proxy.contractAddress),
                        deployerAddress = WalletAddress(deployerAddress),
                        txHash = TransactionHash(proxy.transactionReceipt.get().transactionHash),
                        imported = true,
                        proxy = true,
                        implementationContractAddress = ContractAddress(contract.contractAddress)
                    )
                )

            assertThat(storedRequest.createdAt.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("imported contract decorator is correctly stored in database") {
            val importedContractDecorator = importedContractDecoratorRepository.getByContractIdAndProjectId(
                contractId = importedContractId,
                projectId = PROJECT_ID
            )!!

            assertThat(importedContractDecorator).withMessage()
                .isEqualTo(
                    CONTRACT_DECORATOR.copy(
                        id = importedContractId,
                        name = "Imported Contract",
                        description = "Imported smart contract.",
                        binary = importedContractDecorator.binary,
                        tags = emptyList(),
                        implements = emptyList(),
                        constructors = emptyList(),
                        events = emptyList(),
                        functions = listOf(
                            ContractFunction(
                                name = "implementation",
                                description = "",
                                solidityName = "implementation",
                                signature = "implementation()",
                                inputs = emptyList(),
                                outputs = emptyList(),
                                emittableEvents = emptyList(),
                                readOnly = false
                            ),
                            ContractFunction(
                                name = "setOwner",
                                description = "",
                                solidityName = "setOwner",
                                signature = "setOwner(address)",
                                inputs = listOf(
                                    ContractParameter(
                                        name = "param1",
                                        description = "",
                                        solidityName = "param1",
                                        solidityType = "address",
                                        recommendedTypes = emptyList(),
                                        parameters = null,
                                        hints = null
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
                                signature = "getOwner()",
                                inputs = emptyList(),
                                outputs = emptyList(),
                                emittableEvents = emptyList(),
                                readOnly = false
                            )
                        ),
                        manifest = importedContractDecorator.manifest,
                        artifact = importedContractDecorator.artifact
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

        val interfaceId = InterfaceId("example.ownable")

        suppose("some contract interface is in the repository") {
            contractInterfacesRepository.store(interfaceId, CONTRACT_INTERFACE)
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
                            ContractInterfaceManifestResponse(interfaceId, CONTRACT_INTERFACE)
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyAddInterfacesToImportedSmartContractWhenContractDecoratorIsNotSpecified() {
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

        val interfaceId = InterfaceId("example.ownable")

        suppose("some contract interface is in the repository") {
            contractInterfacesRepository.store(interfaceId, CONTRACT_INTERFACE)
        }

        val interfacesResponse = suppose("interface is added to imported smart contract") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.patch("/v1/import-smart-contract/${importResponse.id}/add-interfaces")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "interfaces": ["${interfaceId.value}"]
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
            assertThat(interfacesResponse).withMessage()
                .isEqualTo(
                    ContractDeploymentRequestResponse(
                        id = interfacesResponse.id,
                        alias = alias,
                        name = "Imported Contract",
                        description = "Imported smart contract.",
                        status = Status.SUCCESS,
                        contractId = importedContractId.value,
                        contractDeploymentData = interfacesResponse.contractDeploymentData,
                        constructorParams = objectMapper.valueToTree(constructorParams),
                        contractTags = emptyList(),
                        contractImplements = listOf(interfaceId.value),
                        initialEthAmount = BigInteger.ZERO,
                        chainId = PROJECT.chainId.value,
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-deploy/${interfacesResponse.id}/action",
                        projectId = PROJECT_ID,
                        createdAt = interfacesResponse.createdAt,
                        arbitraryData = interfacesResponse.arbitraryData,
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
                            data = interfacesResponse.deployTx.data,
                            value = BigInteger.ZERO,
                            blockConfirmations = interfacesResponse.deployTx.blockConfirmations,
                            timestamp = interfacesResponse.deployTx.timestamp
                        ),
                        imported = true,
                        proxy = false,
                        implementationContractAddress = null,
                        events = interfacesResponse.events
                    )
                )
        }
    }

    @Test
    fun mustReturn400BadRequestWhenAddingNonExistentContractInterface() {
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

        val interfaceId = InterfaceId("non-existent")

        verify("400 is returned for non-existent smart contract interface") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.patch("/v1/import-smart-contract/${importResponse.id}/add-interfaces")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "interfaces": ["${interfaceId.value}"]
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.CONTRACT_INTERFACE_NOT_FOUND)
        }
    }

    @Test
    fun mustReturn400BadRequestWhenAddingIncompatibleContractInterface() {
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

        val interfaceId = InterfaceId("example.incompatible")

        suppose("some incompatible contract interface is in the repository") {
            contractInterfacesRepository.store(interfaceId, INCOMPATIBLE_INTERFACE)
        }

        verify("400 is returned for incompatible smart contract interface") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.patch("/v1/import-smart-contract/${importResponse.id}/add-interfaces")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "interfaces": ["${interfaceId.value}"]
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.CONTRACT_DECORATOR_INCOMPATIBLE)
        }
    }

    @Test
    fun mustCorrectlyRemoveInterfacesFromImportedSmartContractWhenContractDecoratorIsNotSpecified() {
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

        val interfaceId = InterfaceId("example.ownable")

        suppose("some contract interface is in the repository") {
            contractInterfacesRepository.store(interfaceId, CONTRACT_INTERFACE)
        }

        suppose("some smart contract interface is added to imported smart contract") {
            importedContractDecoratorRepository.updateInterfaces(
                contractId = ContractId(importResponse.contractId),
                projectId = importResponse.projectId,
                interfaces = listOf(interfaceId),
                manifest = importedContractDecoratorRepository.getManifestJsonByContractIdAndProjectId(
                    contractId = ContractId(importResponse.contractId),
                    projectId = importResponse.projectId
                )!!.copy(implements = setOf(interfaceId.value))
            )

            contractMetadataRepository.updateInterfaces(
                contractId = ContractId(importResponse.contractId),
                projectId = importResponse.projectId,
                interfaces = listOf(interfaceId)
            )
        }

        val interfacesResponse = suppose("interface is removed from imported smart contract") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.patch("/v1/import-smart-contract/${importResponse.id}/remove-interfaces")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "interfaces": ["${interfaceId.value}"]
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
            assertThat(interfacesResponse).withMessage()
                .isEqualTo(
                    ContractDeploymentRequestResponse(
                        id = interfacesResponse.id,
                        alias = alias,
                        name = "Imported Contract",
                        description = "Imported smart contract.",
                        status = Status.SUCCESS,
                        contractId = importedContractId.value,
                        contractDeploymentData = interfacesResponse.contractDeploymentData,
                        constructorParams = objectMapper.valueToTree(constructorParams),
                        contractTags = emptyList(),
                        contractImplements = emptyList(),
                        initialEthAmount = BigInteger.ZERO,
                        chainId = PROJECT.chainId.value,
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-deploy/${interfacesResponse.id}/action",
                        projectId = PROJECT_ID,
                        createdAt = interfacesResponse.createdAt,
                        arbitraryData = interfacesResponse.arbitraryData,
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
                            data = interfacesResponse.deployTx.data,
                            value = BigInteger.ZERO,
                            blockConfirmations = interfacesResponse.deployTx.blockConfirmations,
                            timestamp = interfacesResponse.deployTx.timestamp
                        ),
                        imported = true,
                        proxy = false,
                        implementationContractAddress = null,
                        events = interfacesResponse.events
                    )
                )
        }
    }

    @Test
    fun mustCorrectlySetInterfacesForImportedSmartContractWhenContractDecoratorIsNotSpecified() {
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

        val interfaceId = InterfaceId("example.ownable")

        suppose("some contract interface is in the repository") {
            contractInterfacesRepository.store(interfaceId, CONTRACT_INTERFACE)
        }

        val interfacesResponse = suppose("interface is added to imported smart contract") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.patch("/v1/import-smart-contract/${importResponse.id}/set-interfaces")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "interfaces": ["${interfaceId.value}"]
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
            assertThat(interfacesResponse).withMessage()
                .isEqualTo(
                    ContractDeploymentRequestResponse(
                        id = interfacesResponse.id,
                        alias = alias,
                        name = "Imported Contract",
                        description = "Imported smart contract.",
                        status = Status.SUCCESS,
                        contractId = importedContractId.value,
                        contractDeploymentData = interfacesResponse.contractDeploymentData,
                        constructorParams = objectMapper.valueToTree(constructorParams),
                        contractTags = emptyList(),
                        contractImplements = listOf(interfaceId.value),
                        initialEthAmount = BigInteger.ZERO,
                        chainId = PROJECT.chainId.value,
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-deploy/${interfacesResponse.id}/action",
                        projectId = PROJECT_ID,
                        createdAt = interfacesResponse.createdAt,
                        arbitraryData = interfacesResponse.arbitraryData,
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
                            data = interfacesResponse.deployTx.data,
                            value = BigInteger.ZERO,
                            blockConfirmations = interfacesResponse.deployTx.blockConfirmations,
                            timestamp = interfacesResponse.deployTx.timestamp
                        ),
                        imported = true,
                        proxy = false,
                        implementationContractAddress = null,
                        events = interfacesResponse.events
                    )
                )
        }
    }

    @Test
    fun mustReturn400BadRequestWhenSettingNonExistentContractInterface() {
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

        val interfaceId = InterfaceId("non-existent")

        verify("400 is returned for non-existent smart contract interface") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.patch("/v1/import-smart-contract/${importResponse.id}/set-interfaces")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "interfaces": ["${interfaceId.value}"]
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.CONTRACT_INTERFACE_NOT_FOUND)
        }
    }

    @Test
    fun mustReturn400BadRequestWhenSettingIncompatibleContractInterface() {
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

        val interfaceId = InterfaceId("example.incompatible")

        suppose("some incompatible contract interface is in the repository") {
            contractInterfacesRepository.store(interfaceId, INCOMPATIBLE_INTERFACE)
        }

        verify("400 is returned for incompatible smart contract interface") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.patch("/v1/import-smart-contract/${importResponse.id}/set-interfaces")
                    .header(ProjectApiKeyResolver.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "interfaces": ["${interfaceId.value}"]
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.CONTRACT_DECORATOR_INCOMPATIBLE)
        }
    }
}
