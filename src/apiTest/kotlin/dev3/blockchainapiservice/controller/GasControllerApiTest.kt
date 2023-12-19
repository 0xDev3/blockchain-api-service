package dev3.blockchainapiservice.controller

import dev3.blockchainapiservice.ControllerTestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.blockchain.ExampleContract
import dev3.blockchainapiservice.config.CustomHeaders
import dev3.blockchainapiservice.features.gas.model.response.EstimateGasCostResponse
import dev3.blockchainapiservice.features.gas.model.response.GasPriceResponse
import dev3.blockchainapiservice.generated.jooq.enums.UserIdentifierType
import dev3.blockchainapiservice.generated.jooq.tables.records.ApiKeyRecord
import dev3.blockchainapiservice.generated.jooq.tables.records.ContractMetadataRecord
import dev3.blockchainapiservice.generated.jooq.tables.records.ProjectRecord
import dev3.blockchainapiservice.generated.jooq.tables.records.UserIdentifierRecord
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.model.json.ArtifactJson
import dev3.blockchainapiservice.model.json.ManifestJson
import dev3.blockchainapiservice.model.params.StoreContractDeploymentRequestParams
import dev3.blockchainapiservice.model.result.ContractConstructor
import dev3.blockchainapiservice.model.result.ContractDecorator
import dev3.blockchainapiservice.model.result.ContractEvent
import dev3.blockchainapiservice.model.result.ContractFunction
import dev3.blockchainapiservice.model.result.ContractParameter
import dev3.blockchainapiservice.model.result.EventParameter
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.repository.ContractDeploymentRequestRepository
import dev3.blockchainapiservice.testcontainers.SharedTestContainers
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.BaseUrl
import dev3.blockchainapiservice.util.Constants
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.ContractBinaryData
import dev3.blockchainapiservice.util.ContractId
import dev3.blockchainapiservice.util.ContractTag
import dev3.blockchainapiservice.util.FunctionData
import dev3.blockchainapiservice.util.InterfaceId
import dev3.blockchainapiservice.util.WalletAddress
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.util.UUID

class GasControllerApiTest : ControllerTestBase() {

    companion object {
        private val PROJECT_ID = UUID.randomUUID()
        private val OWNER_ID = UUID.randomUUID()
        private val PROJECT = Project(
            id = PROJECT_ID,
            ownerId = OWNER_ID,
            issuerContractAddress = ContractAddress("0"),
            baseRedirectUrl = BaseUrl("https://example.com/"),
            chainId = TestData.CHAIN_ID,
            customRpcUrl = null,
            createdAt = TestData.TIMESTAMP
        )
        private const val API_KEY = "api-key"
        private val CONTRACT_DECORATOR_ID = ContractId("examples.exampleContract")
        private val DEPLOYED_CONTRACT = StoreContractDeploymentRequestParams(
            id = UUID.randomUUID(),
            alias = "contract-alias",
            contractData = ContractBinaryData("00"),
            contractId = CONTRACT_DECORATOR_ID,
            constructorParams = TestData.EMPTY_JSON_ARRAY,
            deployerAddress = null,
            initialEthAmount = Balance.ZERO,
            chainId = TestData.CHAIN_ID,
            redirectUrl = "redirect-url",
            projectId = PROJECT_ID,
            createdAt = TestData.TIMESTAMP,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig.EMPTY,
            imported = false,
            proxy = false,
            implementationContractAddress = null
        )
        private val CONTRACT_DECORATOR = ContractDecorator(
            id = CONTRACT_DECORATOR_ID,
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
            events = listOf(
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
    }

    @Suppress("unused")
    protected val manifestServiceContainer = SharedTestContainers.manifestServiceContainer

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
                userIdentifier = USER_IDENTIFIER,
                identifierType = UserIdentifierType.ETH_WALLET_ADDRESS,
                stripeClientId = null
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
    fun mustCorrectlyEstimateArbitraryCallGasCostViaDeployedContractId() {
        val callerAddress = WalletAddress("b")
        val functionData = FunctionData("0x13af4035000000000000000000000000000000000000000000000000000000000000000b")
        val ethAmount = Balance.ZERO

        val contractAddress = ContractAddress("cafebabe")

        suppose("some deployed contract exists in the database") {
            contractDeploymentRequestRepository.store(DEPLOYED_CONTRACT, Constants.NIL_UUID).apply {
                contractDeploymentRequestRepository.setContractAddress(DEPLOYED_CONTRACT.id, contractAddress)
            }
        }

        val response = suppose("request to estimate gas cost of arbitrary call request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/gas/estimate/arbitrary-call")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "deployed_contract_id": "${DEPLOYED_CONTRACT.id}",
                                "function_data": "${functionData.value}",
                                "eth_amount": "${ethAmount.rawValue}",
                                "caller_address": "${callerAddress.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, EstimateGasCostResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    EstimateGasCostResponse(
                        chainId = response.chainId,
                        gasEstimate = response.gasEstimate
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyEstimateArbitraryCallGasCostViaDeployedContractAlias() {
        val callerAddress = WalletAddress("b")
        val functionData = FunctionData("0x13af4035000000000000000000000000000000000000000000000000000000000000000b")
        val ethAmount = Balance.ZERO

        val contractAddress = ContractAddress("cafebabe")

        suppose("some deployed contract exists in the database") {
            contractDeploymentRequestRepository.store(DEPLOYED_CONTRACT, Constants.NIL_UUID).apply {
                contractDeploymentRequestRepository.setContractAddress(DEPLOYED_CONTRACT.id, contractAddress)
            }
        }

        val response = suppose("request to estimate gas cost of arbitrary call request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/gas/estimate/arbitrary-call")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "deployed_contract_alias": "${DEPLOYED_CONTRACT.alias}",
                                "function_data": "${functionData.value}",
                                "eth_amount": "${ethAmount.rawValue}",
                                "caller_address": "${callerAddress.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, EstimateGasCostResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    EstimateGasCostResponse(
                        chainId = response.chainId,
                        gasEstimate = response.gasEstimate
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyEstimateArbitraryCallGasCostViaDeployedContractAddress() {
        val callerAddress = WalletAddress("b")
        val functionData = FunctionData("0x13af4035000000000000000000000000000000000000000000000000000000000000000b")
        val ethAmount = Balance.ZERO

        val contractAddress = ContractAddress("cafebabe")

        val response = suppose("request to estimate gas cost of arbitrary call request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/gas/estimate/arbitrary-call")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "contract_address": "${contractAddress.rawValue}",
                                "function_data": "${functionData.value}",
                                "eth_amount": "${ethAmount.rawValue}",
                                "caller_address": "${callerAddress.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, EstimateGasCostResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    EstimateGasCostResponse(
                        chainId = response.chainId,
                        gasEstimate = response.gasEstimate
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyGetGasPrice() {
        val response = suppose("request to get gas price is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/gas/price")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, GasPriceResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    GasPriceResponse(
                        chainId = response.chainId,
                        gasPrice = response.gasPrice
                    )
                )
        }
    }
}
