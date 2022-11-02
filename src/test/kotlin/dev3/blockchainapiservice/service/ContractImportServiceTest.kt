package dev3.blockchainapiservice.service

import com.fasterxml.jackson.databind.JsonNode
import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.blockchain.BlockchainService
import dev3.blockchainapiservice.blockchain.properties.ChainSpec
import dev3.blockchainapiservice.config.JsonConfig
import dev3.blockchainapiservice.exception.ContractDecoratorBinaryMismatchException
import dev3.blockchainapiservice.exception.ContractNotFoundException
import dev3.blockchainapiservice.exception.ResourceNotFoundException
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.model.json.AbiInputOutput
import dev3.blockchainapiservice.model.json.AbiObject
import dev3.blockchainapiservice.model.json.ArtifactJson
import dev3.blockchainapiservice.model.json.ConstructorDecorator
import dev3.blockchainapiservice.model.json.DecompiledContractJson
import dev3.blockchainapiservice.model.json.ManifestJson
import dev3.blockchainapiservice.model.json.TypeDecorator
import dev3.blockchainapiservice.model.params.ImportContractParams
import dev3.blockchainapiservice.model.params.StoreContractDeploymentRequestParams
import dev3.blockchainapiservice.model.result.ContractDecorator
import dev3.blockchainapiservice.model.result.ContractDeploymentTransactionInfo
import dev3.blockchainapiservice.model.result.ContractMetadata
import dev3.blockchainapiservice.model.result.ContractParameter
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.repository.ContractDecoratorRepository
import dev3.blockchainapiservice.repository.ContractDeploymentRequestRepository
import dev3.blockchainapiservice.repository.ContractMetadataRepository
import dev3.blockchainapiservice.repository.ImportedContractDecoratorRepository
import dev3.blockchainapiservice.service.ContractImportServiceImpl.Companion.TypeAndValue
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.BaseUrl
import dev3.blockchainapiservice.util.BlockNumber
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.Constants
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.ContractBinaryData
import dev3.blockchainapiservice.util.ContractId
import dev3.blockchainapiservice.util.FunctionArgument
import dev3.blockchainapiservice.util.FunctionData
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.Tuple
import dev3.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoMoreInteractions
import java.math.BigInteger
import java.util.UUID
import org.mockito.kotlin.verify as verifyMock

class ContractImportServiceTest : TestBase() {

    companion object {
        private val objectMapper = JsonConfig().objectMapper()
        private val PROJECT = Project(
            id = UUID.randomUUID(),
            ownerId = UUID.randomUUID(),
            issuerContractAddress = ContractAddress("a"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = ChainId(1337L),
            customRpcUrl = "custom-rpc-url",
            createdAt = TestData.TIMESTAMP
        )
        private val CONTRACT_ID = ContractId("imported")
        private val PARAMS = ImportContractParams(
            alias = "alias",
            contractId = CONTRACT_ID,
            contractAddress = ContractAddress("abc"),
            redirectUrl = null,
            arbitraryData = null,
            screenConfig = ScreenConfig.EMPTY
        )
        private val CONSTRUCTOR_PARAMS = listOf(FunctionArgument(WalletAddress("cafebabe")))
        private val ENCODED_CONSTRUCTOR_CALL = EthereumFunctionEncoderService().encodeConstructor(CONSTRUCTOR_PARAMS)
        private val CONSTRUCTOR_PARAMS_JSON = objectMapper.valueToTree<JsonNode>(
            listOf(TypeAndValue(type = "address", value = WalletAddress("cafebabe").rawValue))
        )
        private val CONSTRUCTOR_BYTES_32_JSON = objectMapper.valueToTree<JsonNode>(
            listOf(
                TypeAndValue(
                    type = "bytes32",
                    value = ENCODED_CONSTRUCTOR_CALL.withoutPrefix.chunked(2).map { it.toUByte(16).toByte() }
                )
            )
        )
        private val CONSTRUCTOR_BYTECODE = "123456"
        private val CONTRACT_BYTECODE = "abcdef1234567890abcdef"
        private val ARTIFACT_JSON = ArtifactJson(
            contractName = "imported",
            sourceName = "imported.sol",
            abi = listOf(
                AbiObject(
                    anonymous = false,
                    inputs = listOf(
                        AbiInputOutput(
                            components = null,
                            internalType = "address",
                            name = "someAddress",
                            type = "address",
                            indexed = false
                        )
                    ),
                    outputs = null,
                    stateMutability = null,
                    name = "",
                    type = "constructor"
                )
            ),
            bytecode = "$CONSTRUCTOR_BYTECODE$CONTRACT_BYTECODE",
            deployedBytecode = CONTRACT_BYTECODE,
            linkReferences = null,
            deployedLinkReferences = null
        )
        private val MANIFEST_JSON = ManifestJson(
            name = "imported",
            description = "imported",
            tags = emptySet(),
            implements = emptySet(),
            eventDecorators = emptyList(),
            constructorDecorators = listOf(
                ConstructorDecorator(
                    signature = "constructor(address)",
                    description = "",
                    parameterDecorators = listOf(
                        TypeDecorator(
                            name = "",
                            description = "",
                            recommendedTypes = emptyList(),
                            parameters = null
                        )
                    )
                )
            ),
            functionDecorators = emptyList()
        )
        private val CONTRACT_DECORATOR = ContractDecorator(
            id = CONTRACT_ID,
            artifact = ARTIFACT_JSON,
            manifest = MANIFEST_JSON,
            interfacesProvider = null
        )
        private val CONTRACT_DEPLOYMENT_TRANSACTION_INFO = ContractDeploymentTransactionInfo(
            hash = TransactionHash("tx-hash"),
            from = WalletAddress("123"),
            deployedContractAddress = PARAMS.contractAddress,
            data = FunctionData("${ARTIFACT_JSON.bytecode}${ENCODED_CONSTRUCTOR_CALL.withoutPrefix}"),
            value = Balance.ZERO,
            binary = ContractBinaryData(ARTIFACT_JSON.deployedBytecode),
            blockNumber = BlockNumber(BigInteger.ONE)
        )
    }

    @Test
    fun mustCorrectlyImportContractForSomeExistingContractDecorator() {
        val contractDecoratorRepository = mock<ContractDecoratorRepository>()

        suppose("contract decorator will be returned") {
            given(contractDecoratorRepository.getById(CONTRACT_ID))
                .willReturn(CONTRACT_DECORATOR)
        }

        val contractMetadataRepository = mock<ContractMetadataRepository>()

        suppose("contract metadata exists") {
            given(contractMetadataRepository.exists(CONTRACT_ID, Constants.NIL_UUID))
                .willReturn(true)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(PROJECT.chainId, PROJECT.customRpcUrl)

        suppose("contract deployment transaction will be found on blockchain") {
            given(blockchainService.findContractDeploymentTransaction(chainSpec, PARAMS.contractAddress))
                .willReturn(CONTRACT_DEPLOYMENT_TRANSACTION_INFO)
        }

        val id = UUID.randomUUID()
        val uuidProvider = mock<UuidProvider>()

        suppose("some UUID will be returned") {
            given(uuidProvider.getUuid())
                .willReturn(id)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some UTC date-time will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        val service = ContractImportServiceImpl(
            abiDecoderService = EthereumAbiDecoderService(),
            contractDecompilerService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepository,
            contractDecoratorRepository = contractDecoratorRepository,
            importedContractDecoratorRepository = mock(),
            blockchainService = blockchainService,
            uuidProvider = uuidProvider,
            utcDateTimeProvider = utcDateTimeProvider,
            objectMapper = objectMapper
        )

        verify("contract is successfully imported") {
            assertThat(service.importContract(PARAMS, PROJECT)).withMessage()
                .isEqualTo(id)

            verifyMock(contractDeploymentRequestRepository)
                .store(
                    params = StoreContractDeploymentRequestParams(
                        id = id,
                        alias = PARAMS.alias,
                        contractId = CONTRACT_ID,
                        contractData = ContractBinaryData(CONTRACT_DEPLOYMENT_TRANSACTION_INFO.data.value),
                        constructorParams = CONSTRUCTOR_PARAMS_JSON,
                        deployerAddress = CONTRACT_DEPLOYMENT_TRANSACTION_INFO.from,
                        initialEthAmount = CONTRACT_DEPLOYMENT_TRANSACTION_INFO.value,
                        chainId = PROJECT.chainId,
                        redirectUrl = "${PROJECT.baseRedirectUrl.value}/request-deploy/$id/action",
                        projectId = PROJECT.id,
                        createdAt = TestData.TIMESTAMP,
                        arbitraryData = PARAMS.arbitraryData,
                        screenConfig = PARAMS.screenConfig,
                        imported = true
                    ),
                    metadataProjectId = Constants.NIL_UUID
                )
            verifyMock(contractDeploymentRequestRepository)
                .setContractAddress(id, PARAMS.contractAddress)
            verifyMock(contractDeploymentRequestRepository)
                .setTxInfo(id, CONTRACT_DEPLOYMENT_TRANSACTION_INFO.hash, CONTRACT_DEPLOYMENT_TRANSACTION_INFO.from)
            verifyNoMoreInteractions(contractDeploymentRequestRepository)
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenImportingContractForNonExistentContractDecorator() {
        val contractDecoratorRepository = mock<ContractDecoratorRepository>()

        suppose("null will be returned for contract decorator") {
            given(contractDecoratorRepository.getById(CONTRACT_ID))
                .willReturn(null)
        }

        val service = ContractImportServiceImpl(
            abiDecoderService = EthereumAbiDecoderService(),
            contractDecompilerService = mock(),
            contractDeploymentRequestRepository = mock(),
            contractMetadataRepository = mock(),
            contractDecoratorRepository = contractDecoratorRepository,
            importedContractDecoratorRepository = mock(),
            blockchainService = mock(),
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            objectMapper = objectMapper
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.importContract(PARAMS, PROJECT)
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenImportingContractForNonExistentContractMetadata() {
        val contractDecoratorRepository = mock<ContractDecoratorRepository>()

        suppose("contract decorator will be returned") {
            given(contractDecoratorRepository.getById(CONTRACT_ID))
                .willReturn(CONTRACT_DECORATOR)
        }

        val contractMetadataRepository = mock<ContractMetadataRepository>()

        suppose("contract metadata does not exist") {
            given(contractMetadataRepository.exists(CONTRACT_ID, Constants.NIL_UUID))
                .willReturn(false)
        }

        val service = ContractImportServiceImpl(
            abiDecoderService = EthereumAbiDecoderService(),
            contractDecompilerService = mock(),
            contractDeploymentRequestRepository = mock(),
            contractMetadataRepository = contractMetadataRepository,
            contractDecoratorRepository = contractDecoratorRepository,
            importedContractDecoratorRepository = mock(),
            blockchainService = mock(),
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            objectMapper = objectMapper
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.importContract(PARAMS, PROJECT)
            }
        }
    }

    @Test
    fun mustThrowContractNotFoundExceptionWhenContractCannotBeFoundOnBlockchain() {
        val contractDecoratorRepository = mock<ContractDecoratorRepository>()

        suppose("contract decorator will be returned") {
            given(contractDecoratorRepository.getById(CONTRACT_ID))
                .willReturn(CONTRACT_DECORATOR)
        }

        val contractMetadataRepository = mock<ContractMetadataRepository>()

        suppose("contract metadata exists") {
            given(contractMetadataRepository.exists(CONTRACT_ID, Constants.NIL_UUID))
                .willReturn(true)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(PROJECT.chainId, PROJECT.customRpcUrl)

        suppose("contract deployment transaction will not be found on blockchain") {
            given(blockchainService.findContractDeploymentTransaction(chainSpec, PARAMS.contractAddress))
                .willReturn(null)
        }

        val service = ContractImportServiceImpl(
            abiDecoderService = EthereumAbiDecoderService(),
            contractDecompilerService = mock(),
            contractDeploymentRequestRepository = mock(),
            contractMetadataRepository = contractMetadataRepository,
            contractDecoratorRepository = contractDecoratorRepository,
            importedContractDecoratorRepository = mock(),
            blockchainService = blockchainService,
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            objectMapper = objectMapper
        )

        verify("ContractNotFoundException is thrown") {
            assertThrows<ContractNotFoundException>(message) {
                service.importContract(PARAMS, PROJECT)
            }
        }
    }

    @Test
    fun mustThrowContractDecoratorBinaryMismatchExceptionWhenContractDecoratorBinaryMismatchesImportedContractBinary() {
        val contractDecoratorRepository = mock<ContractDecoratorRepository>()

        suppose("contract decorator will be returned") {
            given(contractDecoratorRepository.getById(CONTRACT_ID))
                .willReturn(CONTRACT_DECORATOR)
        }

        val contractMetadataRepository = mock<ContractMetadataRepository>()

        suppose("contract metadata exists") {
            given(contractMetadataRepository.exists(CONTRACT_ID, Constants.NIL_UUID))
                .willReturn(true)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(PROJECT.chainId, PROJECT.customRpcUrl)

        suppose("contract deployment transaction will be found on blockchain") {
            given(blockchainService.findContractDeploymentTransaction(chainSpec, PARAMS.contractAddress))
                .willReturn(CONTRACT_DEPLOYMENT_TRANSACTION_INFO.copy(data = FunctionData("ffff")))
        }

        val service = ContractImportServiceImpl(
            abiDecoderService = EthereumAbiDecoderService(),
            contractDecompilerService = mock(),
            contractDeploymentRequestRepository = mock(),
            contractMetadataRepository = contractMetadataRepository,
            contractDecoratorRepository = contractDecoratorRepository,
            importedContractDecoratorRepository = mock(),
            blockchainService = blockchainService,
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            objectMapper = objectMapper
        )

        verify("ContractDecoratorBinaryMismatchException is thrown") {
            assertThrows<ContractDecoratorBinaryMismatchException>(message) {
                service.importContract(PARAMS, PROJECT)
            }
        }
    }

    @Test
    fun mustCorrectlyImportContractAndCreateNewImportedContractDecorator() {
        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(PROJECT.chainId, PROJECT.customRpcUrl)

        suppose("contract deployment transaction will be found on blockchain") {
            given(blockchainService.findContractDeploymentTransaction(chainSpec, PARAMS.contractAddress))
                .willReturn(CONTRACT_DEPLOYMENT_TRANSACTION_INFO)
        }

        val contractDecompilerService = mock<ContractDecompilerService>()

        suppose("contract will be decompiled") {
            given(contractDecompilerService.decompile(CONTRACT_DEPLOYMENT_TRANSACTION_INFO.binary))
                .willReturn(
                    DecompiledContractJson(
                        manifest = MANIFEST_JSON,
                        artifact = ARTIFACT_JSON,
                        infoMarkdown = "infoMd"
                    )
                )
        }

        val importedContractDecoratorRepository = mock<ImportedContractDecoratorRepository>()
        val contractId = ContractId("imported-${PARAMS.contractAddress.rawValue}-${PROJECT.chainId.value}")

        suppose("imported contract decorator does not exist in the database") {
            given(importedContractDecoratorRepository.getByContractIdAndProjectId(contractId, PROJECT.id))
                .willReturn(null)
        }

        val contractDecorator = CONTRACT_DECORATOR.copy(id = contractId)
        val contractDecoratorId = UUID.randomUUID()
        val adjustedArtifactJson = ARTIFACT_JSON.copy(
            bytecode = "$CONSTRUCTOR_BYTECODE$CONTRACT_BYTECODE",
            deployedBytecode = CONTRACT_BYTECODE
        )

        suppose("imported contract decorator will be stored into the database") {
            given(
                importedContractDecoratorRepository.store(
                    id = contractDecoratorId,
                    projectId = PROJECT.id,
                    contractId = contractId,
                    manifestJson = MANIFEST_JSON,
                    artifactJson = adjustedArtifactJson,
                    infoMarkdown = "infoMd",
                    importedAt = TestData.TIMESTAMP
                )
            )
                .willReturn(contractDecorator)
        }

        val contractMetadataId = UUID.randomUUID()
        val deployedContractId = UUID.randomUUID()
        val uuidProvider = mock<UuidProvider>()

        suppose("some UUID will be returned") {
            given(uuidProvider.getUuid())
                .willReturn(contractDecoratorId, contractMetadataId, deployedContractId)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some UTC date-time will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val contractMetadataRepository = mock<ContractMetadataRepository>()
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        val service = ContractImportServiceImpl(
            abiDecoderService = EthereumAbiDecoderService(),
            contractDecompilerService = contractDecompilerService,
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepository,
            contractDecoratorRepository = mock(),
            importedContractDecoratorRepository = importedContractDecoratorRepository,
            blockchainService = blockchainService,
            uuidProvider = uuidProvider,
            utcDateTimeProvider = utcDateTimeProvider,
            objectMapper = objectMapper
        )

        verify("contract is successfully imported") {
            assertThat(service.importContract(PARAMS.copy(contractId = null), PROJECT)).withMessage()
                .isEqualTo(deployedContractId)

            verifyMock(contractMetadataRepository)
                .createOrUpdate(
                    ContractMetadata(
                        id = contractMetadataId,
                        name = contractDecorator.name,
                        description = contractDecorator.description,
                        contractId = contractId,
                        contractTags = contractDecorator.tags,
                        contractImplements = contractDecorator.implements,
                        projectId = PROJECT.id
                    )
                )
            verifyNoMoreInteractions(contractMetadataRepository)

            verifyMock(contractDeploymentRequestRepository)
                .store(
                    params = StoreContractDeploymentRequestParams(
                        id = deployedContractId,
                        alias = PARAMS.alias,
                        contractId = contractId,
                        contractData = ContractBinaryData(CONTRACT_DEPLOYMENT_TRANSACTION_INFO.data.value),
                        constructorParams = CONSTRUCTOR_BYTES_32_JSON,
                        deployerAddress = CONTRACT_DEPLOYMENT_TRANSACTION_INFO.from,
                        initialEthAmount = CONTRACT_DEPLOYMENT_TRANSACTION_INFO.value,
                        chainId = PROJECT.chainId,
                        redirectUrl = "${PROJECT.baseRedirectUrl.value}/request-deploy/$deployedContractId/action",
                        projectId = PROJECT.id,
                        createdAt = TestData.TIMESTAMP,
                        arbitraryData = PARAMS.arbitraryData,
                        screenConfig = PARAMS.screenConfig,
                        imported = true
                    ),
                    metadataProjectId = PROJECT.id
                )
            verifyMock(contractDeploymentRequestRepository)
                .setContractAddress(deployedContractId, PARAMS.contractAddress)
            verifyMock(contractDeploymentRequestRepository)
                .setTxInfo(
                    id = deployedContractId,
                    txHash = CONTRACT_DEPLOYMENT_TRANSACTION_INFO.hash,
                    deployer = CONTRACT_DEPLOYMENT_TRANSACTION_INFO.from
                )
            verifyNoMoreInteractions(contractDeploymentRequestRepository)
        }
    }

    @Test
    fun mustCorrectlyImportContractAndReuseExistingImportedContractDecorator() {
        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(PROJECT.chainId, PROJECT.customRpcUrl)

        suppose("contract deployment transaction will be found on blockchain") {
            given(blockchainService.findContractDeploymentTransaction(chainSpec, PARAMS.contractAddress))
                .willReturn(CONTRACT_DEPLOYMENT_TRANSACTION_INFO)
        }

        val contractDecompilerService = mock<ContractDecompilerService>()

        suppose("contract will be decompiled") {
            given(contractDecompilerService.decompile(CONTRACT_DEPLOYMENT_TRANSACTION_INFO.binary))
                .willReturn(
                    DecompiledContractJson(
                        manifest = MANIFEST_JSON,
                        artifact = ARTIFACT_JSON,
                        infoMarkdown = "infoMd"
                    )
                )
        }

        val importedContractDecoratorRepository = mock<ImportedContractDecoratorRepository>()
        val contractId = ContractId("imported-${PARAMS.contractAddress.rawValue}-${PROJECT.chainId.value}")
        val contractDecorator = CONTRACT_DECORATOR.copy(id = contractId)

        suppose("imported contract decorator exists in the database") {
            given(importedContractDecoratorRepository.getByContractIdAndProjectId(contractId, PROJECT.id))
                .willReturn(contractDecorator)
        }

        val contractMetadataId = UUID.randomUUID()
        val deployedContractId = UUID.randomUUID()
        val uuidProvider = mock<UuidProvider>()

        suppose("some UUID will be returned") {
            given(uuidProvider.getUuid())
                .willReturn(contractMetadataId, deployedContractId)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some UTC date-time will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val contractMetadataRepository = mock<ContractMetadataRepository>()
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        val service = ContractImportServiceImpl(
            abiDecoderService = EthereumAbiDecoderService(),
            contractDecompilerService = contractDecompilerService,
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepository,
            contractDecoratorRepository = mock(),
            importedContractDecoratorRepository = importedContractDecoratorRepository,
            blockchainService = blockchainService,
            uuidProvider = uuidProvider,
            utcDateTimeProvider = utcDateTimeProvider,
            objectMapper = objectMapper
        )

        verify("contract is successfully imported") {
            assertThat(service.importContract(PARAMS.copy(contractId = null), PROJECT)).withMessage()
                .isEqualTo(deployedContractId)

            verifyMock(contractMetadataRepository)
                .createOrUpdate(
                    ContractMetadata(
                        id = contractMetadataId,
                        name = contractDecorator.name,
                        description = contractDecorator.description,
                        contractId = contractId,
                        contractTags = contractDecorator.tags,
                        contractImplements = contractDecorator.implements,
                        projectId = PROJECT.id
                    )
                )
            verifyNoMoreInteractions(contractMetadataRepository)

            verifyMock(contractDeploymentRequestRepository)
                .store(
                    params = StoreContractDeploymentRequestParams(
                        id = deployedContractId,
                        alias = PARAMS.alias,
                        contractId = contractId,
                        contractData = ContractBinaryData(CONTRACT_DEPLOYMENT_TRANSACTION_INFO.data.value),
                        constructorParams = CONSTRUCTOR_BYTES_32_JSON,
                        deployerAddress = CONTRACT_DEPLOYMENT_TRANSACTION_INFO.from,
                        initialEthAmount = CONTRACT_DEPLOYMENT_TRANSACTION_INFO.value,
                        chainId = PROJECT.chainId,
                        redirectUrl = "${PROJECT.baseRedirectUrl.value}/request-deploy/$deployedContractId/action",
                        projectId = PROJECT.id,
                        createdAt = TestData.TIMESTAMP,
                        arbitraryData = PARAMS.arbitraryData,
                        screenConfig = PARAMS.screenConfig,
                        imported = true
                    ),
                    metadataProjectId = PROJECT.id
                )
            verifyMock(contractDeploymentRequestRepository)
                .setContractAddress(deployedContractId, PARAMS.contractAddress)
            verifyMock(contractDeploymentRequestRepository)
                .setTxInfo(
                    id = deployedContractId,
                    txHash = CONTRACT_DEPLOYMENT_TRANSACTION_INFO.hash,
                    deployer = CONTRACT_DEPLOYMENT_TRANSACTION_INFO.from
                )
            verifyNoMoreInteractions(contractDeploymentRequestRepository)
        }
    }

    @Test
    fun mustCorrectlyCreateInputArgsForSimpleTypes() {
        val service = ContractImportServiceImpl(
            abiDecoderService = mock(),
            contractDecompilerService = mock(),
            contractDeploymentRequestRepository = mock(),
            contractMetadataRepository = mock(),
            contractDecoratorRepository = mock(),
            importedContractDecoratorRepository = mock(),
            blockchainService = mock(),
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            objectMapper = mock()
        )

        verify("input args are correctly created for simple types") {
            val result = service.inputArgs(
                listOf(
                    param("uint"),
                    param("string"),
                    param("bytes"),
                    param("address")
                ),
                listOf(
                    BigInteger.ONE,
                    "test",
                    listOf("1", "2", "3"),
                    WalletAddress("123").rawValue
                )
            )

            assertThat(result).withMessage()
                .isEqualTo(
                    listOf(
                        TypeAndValue("uint", BigInteger.ONE),
                        TypeAndValue("string", "test"),
                        TypeAndValue("bytes", listOf("1", "2", "3")),
                        TypeAndValue("address", WalletAddress("123").rawValue)
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyCreateInputArgsForArrayTypes() {
        val service = ContractImportServiceImpl(
            abiDecoderService = mock(),
            contractDecompilerService = mock(),
            contractDeploymentRequestRepository = mock(),
            contractMetadataRepository = mock(),
            contractDecoratorRepository = mock(),
            importedContractDecoratorRepository = mock(),
            blockchainService = mock(),
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            objectMapper = mock()
        )

        verify("input args are correctly created for array types") {
            val result = service.inputArgs(
                listOf(
                    param("uint[]"),
                    param("string[][][]"),
                    param("bytes[]"),
                    param("address[]")
                ),
                listOf(
                    listOf(BigInteger.ONE, BigInteger.TWO),
                    listOf(
                        listOf(
                            listOf("test1", "test2"),
                            listOf("test3")
                        ),
                        listOf(
                            listOf("test4"),
                            listOf("test5", "test6", "test7")
                        )
                    ),
                    listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5"),
                        listOf("6", "7", "8", "9")
                    ),
                    listOf(WalletAddress("123").rawValue)
                )
            )

            assertThat(result).withMessage()
                .isEqualTo(
                    listOf(
                        TypeAndValue("uint[]", listOf(BigInteger.ONE, BigInteger.TWO)),
                        TypeAndValue(
                            "string[][][]",
                            listOf(
                                listOf(
                                    listOf("test1", "test2"),
                                    listOf("test3")
                                ),
                                listOf(
                                    listOf("test4"),
                                    listOf("test5", "test6", "test7")
                                )
                            ),
                        ),
                        TypeAndValue(
                            "bytes[]",
                            listOf(
                                listOf("1", "2", "3"),
                                listOf("4", "5"),
                                listOf("6", "7", "8", "9")
                            )
                        ),
                        TypeAndValue("address[]", listOf(WalletAddress("123").rawValue))
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyCreateInputArgsForTupleType() {
        val service = ContractImportServiceImpl(
            abiDecoderService = mock(),
            contractDecompilerService = mock(),
            contractDeploymentRequestRepository = mock(),
            contractMetadataRepository = mock(),
            contractDecoratorRepository = mock(),
            importedContractDecoratorRepository = mock(),
            blockchainService = mock(),
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            objectMapper = mock()
        )

        verify("input args are correctly created for tuple type") {
            val result = service.inputArgs(
                listOf(
                    param(
                        "tuple",
                        listOf(
                            param("uint"),
                            param("string"),
                            param("bytes"),
                            param("address")
                        )
                    )
                ),
                listOf(
                    tupleOf(
                        BigInteger.ONE,
                        "test",
                        listOf("1", "2", "3"),
                        WalletAddress("123").rawValue
                    )
                )
            )

            assertThat(result).withMessage()
                .isEqualTo(
                    listOf(
                        TypeAndValue(
                            type = "tuple",
                            value = listOf(
                                TypeAndValue("uint", BigInteger.ONE),
                                TypeAndValue("string", "test"),
                                TypeAndValue("bytes", listOf("1", "2", "3")),
                                TypeAndValue("address", WalletAddress("123").rawValue)
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyCreateInputArgsForNestedTupleTypeArray() {
        val service = ContractImportServiceImpl(
            abiDecoderService = mock(),
            contractDecompilerService = mock(),
            contractDeploymentRequestRepository = mock(),
            contractMetadataRepository = mock(),
            contractDecoratorRepository = mock(),
            importedContractDecoratorRepository = mock(),
            blockchainService = mock(),
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            objectMapper = mock()
        )

        verify("input args are correctly created for nested tuple array") {
            val result = service.inputArgs(
                listOf(
                    param(
                        "tuple[][]",
                        listOf(
                            param("uint"),
                            param("string"),
                            param("bytes"),
                            param("address")
                        )
                    )
                ),
                listOf(
                    listOf(
                        listOf(
                            tupleOf(
                                BigInteger.ZERO,
                                "test0",
                                listOf("1", "2", "3"),
                                WalletAddress("123").rawValue
                            ),
                            tupleOf(
                                BigInteger.ONE,
                                "test1",
                                listOf("4"),
                                WalletAddress("456").rawValue
                            ),
                            tupleOf(
                                BigInteger.TWO,
                                "test2",
                                listOf("5", "6"),
                                WalletAddress("789").rawValue
                            )
                        ),
                        listOf(
                            tupleOf(
                                BigInteger.TEN,
                                "test10",
                                listOf("10", "11"),
                                WalletAddress("abc").rawValue
                            ),
                            tupleOf(
                                BigInteger.ZERO,
                                "test0",
                                emptyList<String>(),
                                WalletAddress("def").rawValue
                            )
                        )
                    )
                )
            )

            assertThat(result).withMessage()
                .isEqualTo(
                    listOf(
                        TypeAndValue(
                            type = "tuple[][]",
                            value = listOf(
                                listOf(
                                    listOf(
                                        TypeAndValue("uint", BigInteger.ZERO),
                                        TypeAndValue("string", "test0"),
                                        TypeAndValue("bytes", listOf("1", "2", "3")),
                                        TypeAndValue("address", WalletAddress("123").rawValue)
                                    ),
                                    listOf(
                                        TypeAndValue("uint", BigInteger.ONE),
                                        TypeAndValue("string", "test1"),
                                        TypeAndValue("bytes", listOf("4")),
                                        TypeAndValue("address", WalletAddress("456").rawValue)
                                    ),
                                    listOf(
                                        TypeAndValue("uint", BigInteger.TWO),
                                        TypeAndValue("string", "test2"),
                                        TypeAndValue("bytes", listOf("5", "6")),
                                        TypeAndValue("address", WalletAddress("789").rawValue)
                                    )
                                ),
                                listOf(
                                    listOf(
                                        TypeAndValue("uint", BigInteger.TEN),
                                        TypeAndValue("string", "test10"),
                                        TypeAndValue("bytes", listOf("10", "11")),
                                        TypeAndValue("address", WalletAddress("abc").rawValue)
                                    ),
                                    listOf(
                                        TypeAndValue("uint", BigInteger.ZERO),
                                        TypeAndValue("string", "test0"),
                                        TypeAndValue("bytes", emptyList<String>()),
                                        TypeAndValue("address", WalletAddress("def").rawValue)
                                    )
                                )
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyCreateInputArgsForNestedTupleTypeArrayWithNestedTuples() {
        val service = ContractImportServiceImpl(
            abiDecoderService = mock(),
            contractDecompilerService = mock(),
            contractDeploymentRequestRepository = mock(),
            contractMetadataRepository = mock(),
            contractDecoratorRepository = mock(),
            importedContractDecoratorRepository = mock(),
            blockchainService = mock(),
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            objectMapper = mock()
        )

        verify("input args are correctly created for tuple type") {
            val result = service.inputArgs(
                listOf(
                    param(
                        "tuple[][]",
                        listOf(
                            param("uint"),
                            param("string"),
                            param("bytes"),
                            param("address"),
                            param(
                                "tuple[]",
                                listOf(
                                    param("int[]"),
                                    param(
                                        "tuple",
                                        listOf(
                                            param("bool[]"),
                                            param("string")
                                        )
                                    )
                                )
                            )
                        )
                    )
                ),
                listOf(
                    listOf(
                        listOf(
                            tupleOf(
                                BigInteger.ZERO,
                                "test0",
                                listOf("1", "2", "3"),
                                WalletAddress("123").rawValue,
                                listOf(
                                    tupleOf(
                                        listOf("100", "200"),
                                        tupleOf(
                                            listOf(true, false),
                                            "nested-string-1"
                                        )
                                    ),
                                    tupleOf(
                                        listOf("600", "700", "800"),
                                        tupleOf(
                                            listOf(false, false),
                                            "nested-string-2"
                                        )
                                    )
                                )
                            ),
                            tupleOf(
                                BigInteger.ONE,
                                "test1",
                                listOf("4"),
                                WalletAddress("456").rawValue,
                                emptyList<Tuple>()
                            ),
                            tupleOf(
                                BigInteger.TWO,
                                "test2",
                                listOf("5", "6"),
                                WalletAddress("789").rawValue,
                                listOf(
                                    tupleOf(
                                        listOf("900"),
                                        tupleOf(
                                            listOf(true, true),
                                            "nested-string-3"
                                        )
                                    )
                                )
                            )
                        ),
                        listOf(
                            tupleOf(
                                BigInteger.TEN,
                                "test10",
                                listOf("10", "11"),
                                WalletAddress("abc").rawValue,
                                listOf(
                                    tupleOf(
                                        emptyList<String>(),
                                        tupleOf(
                                            emptyList<Boolean>(),
                                            "nested-string-4"
                                        )
                                    )
                                )
                            ),
                            tupleOf(
                                BigInteger.ZERO,
                                "test0",
                                emptyList<String>(),
                                WalletAddress("def").rawValue,
                                emptyList<Tuple>()
                            )
                        )
                    )
                )
            )

            assertThat(result).withMessage()
                .isEqualTo(
                    listOf(
                        TypeAndValue(
                            type = "tuple[][]",
                            value = listOf(
                                listOf(
                                    listOf(
                                        TypeAndValue("uint", BigInteger.ZERO),
                                        TypeAndValue("string", "test0"),
                                        TypeAndValue("bytes", listOf("1", "2", "3")),
                                        TypeAndValue("address", WalletAddress("123").rawValue),
                                        TypeAndValue(
                                            "tuple[]",
                                            listOf(
                                                listOf(
                                                    TypeAndValue("int[]", listOf("100", "200")),
                                                    TypeAndValue(
                                                        "tuple",
                                                        listOf(
                                                            TypeAndValue("bool[]", listOf(true, false)),
                                                            TypeAndValue("string", "nested-string-1")
                                                        )
                                                    )
                                                ),
                                                listOf(
                                                    TypeAndValue("int[]", listOf("600", "700", "800")),
                                                    TypeAndValue(
                                                        "tuple",
                                                        listOf(
                                                            TypeAndValue("bool[]", listOf(false, false)),
                                                            TypeAndValue("string", "nested-string-2")
                                                        )
                                                    )
                                                )
                                            )
                                        )
                                    ),
                                    listOf(
                                        TypeAndValue("uint", BigInteger.ONE),
                                        TypeAndValue("string", "test1"),
                                        TypeAndValue("bytes", listOf("4")),
                                        TypeAndValue("address", WalletAddress("456").rawValue),
                                        TypeAndValue("tuple[]", emptyList<Tuple>())
                                    ),
                                    listOf(
                                        TypeAndValue("uint", BigInteger.TWO),
                                        TypeAndValue("string", "test2"),
                                        TypeAndValue("bytes", listOf("5", "6")),
                                        TypeAndValue("address", WalletAddress("789").rawValue),
                                        TypeAndValue(
                                            "tuple[]",
                                            listOf(
                                                listOf(
                                                    TypeAndValue("int[]", listOf("900")),
                                                    TypeAndValue(
                                                        "tuple",
                                                        listOf(
                                                            TypeAndValue("bool[]", listOf(true, true)),
                                                            TypeAndValue("string", "nested-string-3")
                                                        )
                                                    )
                                                )
                                            )
                                        )
                                    )
                                ),
                                listOf(
                                    listOf(
                                        TypeAndValue("uint", BigInteger.TEN),
                                        TypeAndValue("string", "test10"),
                                        TypeAndValue("bytes", listOf("10", "11")),
                                        TypeAndValue("address", WalletAddress("abc").rawValue),
                                        TypeAndValue(
                                            "tuple[]",
                                            listOf(
                                                listOf(
                                                    TypeAndValue("int[]", emptyList<String>()),
                                                    TypeAndValue(
                                                        "tuple",
                                                        listOf(
                                                            TypeAndValue("bool[]", emptyList<Boolean>()),
                                                            TypeAndValue("string", "nested-string-4")
                                                        )
                                                    )
                                                )
                                            )
                                        )
                                    ),
                                    listOf(
                                        TypeAndValue("uint", BigInteger.ZERO),
                                        TypeAndValue("string", "test0"),
                                        TypeAndValue("bytes", emptyList<String>()),
                                        TypeAndValue("address", WalletAddress("def").rawValue),
                                        TypeAndValue("tuple[]", emptyList<Tuple>())
                                    )
                                )
                            )
                        )
                    )
                )
        }
    }

    private fun param(solidityType: String, parameters: List<ContractParameter>? = null) = ContractParameter(
        name = "",
        description = "",
        solidityName = "",
        solidityType = solidityType,
        recommendedTypes = emptyList(),
        parameters = parameters
    )

    private fun tupleOf(vararg elems: Any) = Tuple(elems.toList())
}
