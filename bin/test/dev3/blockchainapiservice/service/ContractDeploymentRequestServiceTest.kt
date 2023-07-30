package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.blockchain.BlockchainService
import dev3.blockchainapiservice.blockchain.properties.ChainSpec
import dev3.blockchainapiservice.exception.CannotAttachTxInfoException
import dev3.blockchainapiservice.exception.ResourceNotFoundException
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.model.filters.ContractDeploymentRequestFilters
import dev3.blockchainapiservice.model.filters.OrList
import dev3.blockchainapiservice.model.json.ArtifactJson
import dev3.blockchainapiservice.model.json.ManifestJson
import dev3.blockchainapiservice.model.params.CreateContractDeploymentRequestParams
import dev3.blockchainapiservice.model.params.StoreContractDeploymentRequestParams
import dev3.blockchainapiservice.model.result.BlockchainTransactionInfo
import dev3.blockchainapiservice.model.result.ContractDecorator
import dev3.blockchainapiservice.model.result.ContractDeploymentRequest
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.repository.ContractDecoratorRepository
import dev3.blockchainapiservice.repository.ContractDeploymentRequestRepository
import dev3.blockchainapiservice.repository.ContractMetadataRepository
import dev3.blockchainapiservice.repository.ProjectRepository
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.BaseUrl
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.Constants
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.ContractBinaryData
import dev3.blockchainapiservice.util.ContractId
import dev3.blockchainapiservice.util.ContractTag
import dev3.blockchainapiservice.util.FunctionArgument
import dev3.blockchainapiservice.util.FunctionData
import dev3.blockchainapiservice.util.InterfaceId
import dev3.blockchainapiservice.util.Status
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress
import dev3.blockchainapiservice.util.ZeroAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoMoreInteractions
import java.math.BigInteger
import java.util.UUID
import org.mockito.kotlin.verify as verifyMock

class ContractDeploymentRequestServiceTest : TestBase() {

    companion object {
        private val PROJECT = Project(
            id = UUID.randomUUID(),
            ownerId = UUID.randomUUID(),
            issuerContractAddress = ContractAddress("a"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = ChainId(1337L),
            customRpcUrl = "custom-rpc-url",
            createdAt = TestData.TIMESTAMP
        )
        private val CONTRACT_ID = ContractId("contract-id")
        private val CREATE_PARAMS = CreateContractDeploymentRequestParams(
            alias = "alias",
            contractId = CONTRACT_ID,
            constructorParams = listOf(FunctionArgument("test")),
            deployerAddress = WalletAddress("a"),
            initialEthAmount = Balance(BigInteger("10000")),
            redirectUrl = "redirect-url/\${id}",
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            )
        )
        private val CONTRACT_DECORATOR = ContractDecorator(
            id = CONTRACT_ID,
            name = "name",
            description = "description",
            binary = ContractBinaryData("12"),
            tags = listOf(ContractTag("test-tag")),
            implements = listOf(InterfaceId("test-trait")),
            constructors = emptyList(),
            functions = emptyList(),
            events = emptyList(),
            manifest = ManifestJson.EMPTY,
            artifact = ArtifactJson.EMPTY
        )
        private val ID = UUID.randomUUID()
        private val ENCODED_CONSTRUCTOR = FunctionData("0x1234")
        private val STORE_PARAMS = StoreContractDeploymentRequestParams(
            id = ID,
            alias = CREATE_PARAMS.alias,
            contractId = CONTRACT_ID,
            contractData = ContractBinaryData(CONTRACT_DECORATOR.binary.value + ENCODED_CONSTRUCTOR.withoutPrefix),
            constructorParams = TestData.EMPTY_JSON_ARRAY,
            deployerAddress = CREATE_PARAMS.deployerAddress,
            initialEthAmount = CREATE_PARAMS.initialEthAmount,
            chainId = PROJECT.chainId,
            redirectUrl = CREATE_PARAMS.redirectUrl!!.replace("\${id}", ID.toString()),
            projectId = PROJECT.id,
            createdAt = TestData.TIMESTAMP,
            arbitraryData = CREATE_PARAMS.arbitraryData,
            screenConfig = CREATE_PARAMS.screenConfig,
            imported = false,
            proxy = false,
            implementationContractAddress = null
        )
        private val TX_HASH = TransactionHash("tx-hash")
        private val CONTRACT_ADDRESS = ContractAddress("cafebabe")
        private val STORED_REQUEST = ContractDeploymentRequest(
            id = ID,
            alias = STORE_PARAMS.alias,
            name = CONTRACT_DECORATOR.name,
            description = CONTRACT_DECORATOR.description,
            contractId = CONTRACT_ID,
            contractData = STORE_PARAMS.contractData,
            constructorParams = TestData.EMPTY_JSON_ARRAY,
            contractTags = CONTRACT_DECORATOR.tags,
            contractImplements = CONTRACT_DECORATOR.implements,
            initialEthAmount = STORE_PARAMS.initialEthAmount,
            chainId = STORE_PARAMS.chainId,
            redirectUrl = STORE_PARAMS.redirectUrl,
            projectId = PROJECT.id,
            createdAt = TestData.TIMESTAMP,
            arbitraryData = STORE_PARAMS.arbitraryData,
            screenConfig = STORE_PARAMS.screenConfig,
            contractAddress = CONTRACT_ADDRESS,
            deployerAddress = STORE_PARAMS.deployerAddress,
            txHash = TX_HASH,
            imported = STORE_PARAMS.imported,
            proxy = STORE_PARAMS.proxy,
            implementationContractAddress = STORE_PARAMS.implementationContractAddress
        )
        val CHAIN_SPEC = ChainSpec(STORED_REQUEST.chainId, null)
        val TRANSACTION_INFO = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = STORED_REQUEST.deployerAddress!!,
            to = ZeroAddress,
            deployedContractAddress = CONTRACT_ADDRESS,
            data = FunctionData(STORED_REQUEST.contractData.value),
            value = STORED_REQUEST.initialEthAmount,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true
        )
    }

    @Test
    fun mustSuccessfullyCreateContractDeploymentRequest() {
        val contractDecoratorRepository = mock<ContractDecoratorRepository>()

        suppose("some contract decorator will be returned") {
            given(contractDecoratorRepository.getById(CONTRACT_ID))
                .willReturn(CONTRACT_DECORATOR)
        }

        val uuidProvider = mock<UuidProvider>()

        suppose("some UUID will be generated") {
            given(uuidProvider.getUuid())
                .willReturn(ID)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some timestamp will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("constructor will be encoded") {
            given(functionEncoderService.encodeConstructor(arguments = CREATE_PARAMS.constructorParams))
                .willReturn(ENCODED_CONSTRUCTOR)
        }

        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("contract deployment request is stored in database") {
            given(contractDeploymentRequestRepository.store(STORE_PARAMS, Constants.NIL_UUID))
                .willReturn(STORED_REQUEST)
        }

        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepositoryMock(exists = true),
            contractDecoratorRepository = contractDecoratorRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = uuidProvider,
                utcDateTimeProvider = utcDateTimeProvider,
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("contract deployment request is correctly created") {
            assertThat(service.createContractDeploymentRequest(CREATE_PARAMS, PROJECT)).withMessage()
                .isEqualTo(STORED_REQUEST)

            verifyMock(contractDeploymentRequestRepository)
                .store(STORE_PARAMS, Constants.NIL_UUID)
            verifyNoMoreInteractions(contractDeploymentRequestRepository)
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionForNonExistentContractDecorator() {
        val contractDecoratorRepository = mock<ContractDecoratorRepository>()

        suppose("null will be returned when fetching contract decorator") {
            given(contractDecoratorRepository.getById(CONTRACT_ID))
                .willReturn(null)
        }

        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = mock(),
            contractDecoratorRepository = contractDecoratorRepository,
            contractMetadataRepository = contractMetadataRepositoryMock(exists = true),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.createContractDeploymentRequest(CREATE_PARAMS, PROJECT)
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionForNonExistentContractMetadata() {
        val contractDecoratorRepository = mock<ContractDecoratorRepository>()

        suppose("some contract decorator will be returned") {
            given(contractDecoratorRepository.getById(CONTRACT_ID))
                .willReturn(CONTRACT_DECORATOR)
        }

        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = mock(),
            contractMetadataRepository = contractMetadataRepositoryMock(exists = false),
            contractDecoratorRepository = contractDecoratorRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.createContractDeploymentRequest(CREATE_PARAMS, PROJECT)
            }
        }
    }

    @Test
    fun mustSuccessfullyMarkContractDeploymentRequestAsDeleted() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("contract deployment request exists in database") {
            given(contractDeploymentRequestRepository.getById(ID))
                .willReturn(STORED_REQUEST)
        }

        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = mock(),
            contractDecoratorRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("contract deployment request is successfully marked as deleted") {
            service.markContractDeploymentRequestAsDeleted(ID, STORED_REQUEST.projectId)

            verifyMock(contractDeploymentRequestRepository)
                .getById(ID)
            verifyMock(contractDeploymentRequestRepository)
                .markAsDeleted(ID)
            verifyNoMoreInteractions(contractDeploymentRequestRepository)
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenMarkingNonOwnedContractDeploymentRequestAsDeleted() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("contract deployment request exists in database") {
            given(contractDeploymentRequestRepository.getById(ID))
                .willReturn(STORED_REQUEST)
        }

        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = mock(),
            contractDecoratorRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("contract deployment request is successfully marked as deleted") {
            assertThrows<ResourceNotFoundException>(message) {
                service.markContractDeploymentRequestAsDeleted(ID, UUID.randomUUID())
            }

            verifyMock(contractDeploymentRequestRepository)
                .getById(ID)
            verifyNoMoreInteractions(contractDeploymentRequestRepository)
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenMarkingNonExistentContractDeploymentRequestAsDeleted() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("contract deployment request exists in database") {
            given(contractDeploymentRequestRepository.getById(ID))
                .willReturn(null)
        }

        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = mock(),
            contractDecoratorRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("contract deployment request is successfully marked as deleted") {
            assertThrows<ResourceNotFoundException>(message) {
                service.markContractDeploymentRequestAsDeleted(ID, STORED_REQUEST.projectId)
            }

            verifyMock(contractDeploymentRequestRepository)
                .getById(ID)
            verifyNoMoreInteractions(contractDeploymentRequestRepository)
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionForNonExistentContractDeploymentRequest() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("contract deployment request does not exist in database") {
            given(contractDeploymentRequestRepository.getById(any()))
                .willReturn(null)
            given(contractDeploymentRequestRepository.getByAliasAndProjectId(any(), any()))
                .willReturn(null)
        }

        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepositoryMock(exists = true),
            contractDecoratorRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("ResourceNotFoundException is thrown when fetching by id") {
            assertThrows<ResourceNotFoundException>(message) {
                service.getContractDeploymentRequest(id = UUID.randomUUID())
            }
        }

        verify("ResourceNotFoundException is thrown when fetching by alias and project id") {
            assertThrows<ResourceNotFoundException>(message) {
                service.getContractDeploymentRequestByProjectIdAndAlias(
                    projectId = UUID.randomUUID(),
                    alias = "random-alias"
                )
            }
        }
    }

    @Test
    fun mustReturnContractDeploymentRequestWithPendingStatusWhenContractDeploymentRequestHasNullTxHash() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()
        val request = STORED_REQUEST.copy(
            txHash = null,
            contractAddress = null
        )

        suppose("contract deployment request exists in database") {
            given(contractDeploymentRequestRepository.getById(ID))
                .willReturn(request)
            given(contractDeploymentRequestRepository.getByAliasAndProjectId(CREATE_PARAMS.alias, PROJECT.id))
                .willReturn(request)
        }

        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepositoryMock(exists = true),
            contractDecoratorRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = projectRepositoryMock(PROJECT.id)
        )

        verify("contract deployment request by id with pending status is returned") {
            assertThat(service.getContractDeploymentRequest(ID)).withMessage()
                .isEqualTo(
                    request.withTransactionData(
                        status = Status.PENDING,
                        transactionInfo = null
                    )
                )
        }

        verify("contract deployment request by alias and projectId with pending status is returned") {
            assertThat(service.getContractDeploymentRequestByProjectIdAndAlias(PROJECT.id, CREATE_PARAMS.alias))
                .withMessage()
                .isEqualTo(
                    request.withTransactionData(
                        status = Status.PENDING,
                        transactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnContractDeploymentRequestWithPendingStatusWhenTransactionIsNotYetMined() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("contract deployment request exists in database") {
            given(contractDeploymentRequestRepository.getById(ID))
                .willReturn(STORED_REQUEST)
            given(contractDeploymentRequestRepository.getByAliasAndProjectId(CREATE_PARAMS.alias, PROJECT.id))
                .willReturn(STORED_REQUEST)
        }

        val blockchainService = mock<BlockchainService>()

        suppose("transaction is not yet mined") {
            given(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH))
                .willReturn(null)
        }

        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepositoryMock(exists = true),
            contractDecoratorRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(PROJECT.id)
        )

        verify("contract deployment request by id with pending status is returned") {
            assertThat(service.getContractDeploymentRequest(ID)).withMessage()
                .isEqualTo(
                    STORED_REQUEST.withTransactionData(
                        status = Status.PENDING,
                        transactionInfo = null
                    )
                )
        }

        verify("contract deployment request by alias and projectId with pending status is returned") {
            assertThat(service.getContractDeploymentRequestByProjectIdAndAlias(PROJECT.id, CREATE_PARAMS.alias))
                .withMessage()
                .isEqualTo(
                    STORED_REQUEST.withTransactionData(
                        status = Status.PENDING,
                        transactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnContractDeploymentRequestWithFailedStatusWhenTransactionIsNotSuccessful() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("contract deployment request exists in database") {
            given(contractDeploymentRequestRepository.getById(ID))
                .willReturn(STORED_REQUEST)
            given(contractDeploymentRequestRepository.getByAliasAndProjectId(CREATE_PARAMS.alias, PROJECT.id))
                .willReturn(STORED_REQUEST)
        }

        val blockchainService = mock<BlockchainService>()
        val transactionInfo = TRANSACTION_INFO.copy(success = false)

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH))
                .willReturn(transactionInfo)
        }

        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepositoryMock(exists = true),
            contractDecoratorRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(PROJECT.id)
        )

        verify("contract deployment request by id with failed status is returned") {
            assertThat(service.getContractDeploymentRequest(ID)).withMessage()
                .isEqualTo(
                    STORED_REQUEST.withTransactionData(
                        status = Status.FAILED,
                        transactionInfo = transactionInfo
                    )
                )
        }

        verify("contract deployment request by alias and projectId with failed status is returned") {
            assertThat(service.getContractDeploymentRequestByProjectIdAndAlias(PROJECT.id, CREATE_PARAMS.alias))
                .withMessage()
                .isEqualTo(
                    STORED_REQUEST.withTransactionData(
                        status = Status.FAILED,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnContractDeploymentRequestWithFailedStatusWhenTransactionHasWrongTxHash() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("contract deployment request exists in database") {
            given(contractDeploymentRequestRepository.getById(ID))
                .willReturn(STORED_REQUEST)
            given(contractDeploymentRequestRepository.getByAliasAndProjectId(CREATE_PARAMS.alias, PROJECT.id))
                .willReturn(STORED_REQUEST)
        }

        val blockchainService = mock<BlockchainService>()
        val transactionInfo = TRANSACTION_INFO.copy(hash = TransactionHash("wrong"))

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH))
                .willReturn(transactionInfo)
        }

        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepositoryMock(exists = true),
            contractDecoratorRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(PROJECT.id)
        )

        verify("contract deployment request by id with failed status is returned") {
            assertThat(service.getContractDeploymentRequest(ID)).withMessage()
                .isEqualTo(
                    STORED_REQUEST.withTransactionData(
                        status = Status.FAILED,
                        transactionInfo = transactionInfo
                    )
                )
        }

        verify("contract deployment request by alias and projectId with failed status is returned") {
            assertThat(service.getContractDeploymentRequestByProjectIdAndAlias(PROJECT.id, CREATE_PARAMS.alias))
                .withMessage()
                .isEqualTo(
                    STORED_REQUEST.withTransactionData(
                        status = Status.FAILED,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnContractDeploymentRequestWithFailedStatusWhenTransactionHasWrongDeployerAddress() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("contract deployment request exists in database") {
            given(contractDeploymentRequestRepository.getById(ID))
                .willReturn(STORED_REQUEST)
            given(contractDeploymentRequestRepository.getByAliasAndProjectId(CREATE_PARAMS.alias, PROJECT.id))
                .willReturn(STORED_REQUEST)
        }

        val blockchainService = mock<BlockchainService>()
        val transactionInfo = TRANSACTION_INFO.copy(from = WalletAddress("1337"))

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH))
                .willReturn(transactionInfo)
        }

        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepositoryMock(exists = true),
            contractDecoratorRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(PROJECT.id)
        )

        verify("contract deployment request by id with failed status is returned") {
            assertThat(service.getContractDeploymentRequest(ID)).withMessage()
                .isEqualTo(
                    STORED_REQUEST.withTransactionData(
                        status = Status.FAILED,
                        transactionInfo = transactionInfo
                    )
                )
        }

        verify("contract deployment request by alias and projectId with failed status is returned") {
            assertThat(service.getContractDeploymentRequestByProjectIdAndAlias(PROJECT.id, CREATE_PARAMS.alias))
                .withMessage()
                .isEqualTo(
                    STORED_REQUEST.withTransactionData(
                        status = Status.FAILED,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnContractDeploymentRequestWithFailedStatusWhenTransactionHasNullContractAddress() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("contract deployment request exists in database") {
            given(contractDeploymentRequestRepository.getById(ID))
                .willReturn(STORED_REQUEST)
            given(contractDeploymentRequestRepository.getByAliasAndProjectId(CREATE_PARAMS.alias, PROJECT.id))
                .willReturn(STORED_REQUEST)
        }

        val blockchainService = mock<BlockchainService>()
        val transactionInfo = TRANSACTION_INFO.copy(deployedContractAddress = null)

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH))
                .willReturn(transactionInfo)
        }

        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepositoryMock(exists = true),
            contractDecoratorRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(PROJECT.id)
        )

        verify("contract deployment request by id with failed status is returned") {
            assertThat(service.getContractDeploymentRequest(ID)).withMessage()
                .isEqualTo(
                    STORED_REQUEST.withTransactionData(
                        status = Status.FAILED,
                        transactionInfo = transactionInfo
                    )
                )
        }

        verify("contract deployment request by alias and projectId with failed status is returned") {
            assertThat(service.getContractDeploymentRequestByProjectIdAndAlias(PROJECT.id, CREATE_PARAMS.alias))
                .withMessage()
                .isEqualTo(
                    STORED_REQUEST.withTransactionData(
                        status = Status.FAILED,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnContractDeploymentRequestWithFailedStatusWhenTransactionHasWrongContractAddress() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("contract deployment request exists in database") {
            given(contractDeploymentRequestRepository.getById(ID))
                .willReturn(STORED_REQUEST)
            given(contractDeploymentRequestRepository.getByAliasAndProjectId(CREATE_PARAMS.alias, PROJECT.id))
                .willReturn(STORED_REQUEST)
        }

        val blockchainService = mock<BlockchainService>()
        val transactionInfo = TRANSACTION_INFO.copy(deployedContractAddress = ContractAddress("1337"))

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH))
                .willReturn(transactionInfo)
        }

        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepositoryMock(exists = true),
            contractDecoratorRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(PROJECT.id)
        )

        verify("contract deployment request by id with failed status is returned") {
            assertThat(service.getContractDeploymentRequest(ID)).withMessage()
                .isEqualTo(
                    STORED_REQUEST.withTransactionData(
                        status = Status.FAILED,
                        transactionInfo = transactionInfo
                    )
                )
        }

        verify("contract deployment request by alias and projectId with failed status is returned") {
            assertThat(service.getContractDeploymentRequestByProjectIdAndAlias(PROJECT.id, CREATE_PARAMS.alias))
                .withMessage()
                .isEqualTo(
                    STORED_REQUEST.withTransactionData(
                        status = Status.FAILED,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnContractDeploymentRequestWithFailedStatusWhenTransactionHasWrongData() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("contract deployment request exists in database") {
            given(contractDeploymentRequestRepository.getById(ID))
                .willReturn(STORED_REQUEST)
            given(contractDeploymentRequestRepository.getByAliasAndProjectId(CREATE_PARAMS.alias, PROJECT.id))
                .willReturn(STORED_REQUEST)
        }

        val blockchainService = mock<BlockchainService>()
        val transactionInfo = TRANSACTION_INFO.copy(data = FunctionData("wrong"))

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH))
                .willReturn(transactionInfo)
        }

        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepositoryMock(exists = true),
            contractDecoratorRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(PROJECT.id)
        )

        verify("contract deployment request by id with failed status is returned") {
            assertThat(service.getContractDeploymentRequest(ID)).withMessage()
                .isEqualTo(
                    STORED_REQUEST.withTransactionData(
                        status = Status.FAILED,
                        transactionInfo = transactionInfo
                    )
                )
        }

        verify("contract deployment request by alias and projectId with failed status is returned") {
            assertThat(service.getContractDeploymentRequestByProjectIdAndAlias(PROJECT.id, CREATE_PARAMS.alias))
                .withMessage()
                .isEqualTo(
                    STORED_REQUEST.withTransactionData(
                        status = Status.FAILED,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnContractDeploymentRequestWithFailedStatusWhenTransactionHasWrongValue() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("contract deployment request exists in database") {
            given(contractDeploymentRequestRepository.getById(ID))
                .willReturn(STORED_REQUEST)
            given(contractDeploymentRequestRepository.getByAliasAndProjectId(CREATE_PARAMS.alias, PROJECT.id))
                .willReturn(STORED_REQUEST)
        }

        val blockchainService = mock<BlockchainService>()
        val transactionInfo = TRANSACTION_INFO.copy(value = Balance(BigInteger("123456789")))

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH))
                .willReturn(transactionInfo)
        }

        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepositoryMock(exists = true),
            contractDecoratorRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(PROJECT.id)
        )

        verify("contract deployment request with failed status is returned") {
            assertThat(service.getContractDeploymentRequest(ID)).withMessage()
                .isEqualTo(
                    STORED_REQUEST.withTransactionData(
                        status = Status.FAILED,
                        transactionInfo = transactionInfo
                    )
                )
        }

        verify("contract deployment request with failed status is returned") {
            assertThat(service.getContractDeploymentRequestByProjectIdAndAlias(PROJECT.id, CREATE_PARAMS.alias))
                .withMessage()
                .isEqualTo(
                    STORED_REQUEST.withTransactionData(
                        status = Status.FAILED,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnContractDeploymentRequestWithSuccessfulStatusWhenDeployerAddressIsNull() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()
        val request = STORED_REQUEST.copy(deployerAddress = null)

        suppose("contract deployment request exists in database") {
            given(contractDeploymentRequestRepository.getById(ID))
                .willReturn(request)
            given(contractDeploymentRequestRepository.getByAliasAndProjectId(CREATE_PARAMS.alias, PROJECT.id))
                .willReturn(request)
        }

        val blockchainService = mock<BlockchainService>()

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH))
                .willReturn(TRANSACTION_INFO)
        }

        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepositoryMock(exists = true),
            contractDecoratorRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(PROJECT.id)
        )

        verify("contract deployment request by id with successful status is returned") {
            assertThat(service.getContractDeploymentRequest(ID)).withMessage()
                .isEqualTo(
                    request.withTransactionData(
                        status = Status.SUCCESS,
                        transactionInfo = TRANSACTION_INFO
                    )
                )
        }

        verify("contract deployment request by alias and contractId with successful status is returned") {
            assertThat(service.getContractDeploymentRequestByProjectIdAndAlias(PROJECT.id, CREATE_PARAMS.alias))
                .withMessage()
                .isEqualTo(
                    request.withTransactionData(
                        status = Status.SUCCESS,
                        transactionInfo = TRANSACTION_INFO
                    )
                )
        }
    }

    @Test
    fun mustReturnContractDeploymentRequestWithSuccessfulStatusWhenDeployerAddressIsNotNull() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("contract deployment request exists in database") {
            given(contractDeploymentRequestRepository.getById(ID))
                .willReturn(STORED_REQUEST)
            given(contractDeploymentRequestRepository.getByAliasAndProjectId(CREATE_PARAMS.alias, PROJECT.id))
                .willReturn(STORED_REQUEST)
        }

        val blockchainService = mock<BlockchainService>()

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH))
                .willReturn(TRANSACTION_INFO)
        }

        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepositoryMock(exists = true),
            contractDecoratorRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(PROJECT.id)
        )

        verify("contract deployment request by id with successful status is returned") {
            assertThat(service.getContractDeploymentRequest(ID)).withMessage()
                .isEqualTo(
                    STORED_REQUEST.withTransactionData(
                        status = Status.SUCCESS,
                        transactionInfo = TRANSACTION_INFO
                    )
                )
        }

        verify("contract deployment request by alias and projectId with successful status is returned") {
            assertThat(service.getContractDeploymentRequestByProjectIdAndAlias(PROJECT.id, STORED_REQUEST.alias))
                .withMessage()
                .isEqualTo(
                    STORED_REQUEST.withTransactionData(
                        status = Status.SUCCESS,
                        transactionInfo = TRANSACTION_INFO
                    )
                )
        }
    }

    @Test
    fun mustReturnContractDeploymentRequestWithSuccessfulStatusAndSetContractAddress() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("contract deployment request exists in database") {
            given(contractDeploymentRequestRepository.getById(ID))
                .willReturn(STORED_REQUEST.copy(contractAddress = null))
        }

        suppose("setting contract address will succeed") {
            given(contractDeploymentRequestRepository.setContractAddress(ID, CONTRACT_ADDRESS))
                .willReturn(true)
        }

        val blockchainService = mock<BlockchainService>()

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH))
                .willReturn(TRANSACTION_INFO)
        }

        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepositoryMock(exists = true),
            contractDecoratorRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(PROJECT.id)
        )

        verify("contract deployment request with successful status is returned") {
            assertThat(service.getContractDeploymentRequest(ID)).withMessage()
                .isEqualTo(
                    STORED_REQUEST.withTransactionData(
                        status = Status.SUCCESS,
                        transactionInfo = TRANSACTION_INFO
                    )
                )

            verifyMock(contractDeploymentRequestRepository)
                .getById(ID)
            verifyMock(contractDeploymentRequestRepository)
                .setContractAddress(ID, CONTRACT_ADDRESS)
            verifyNoMoreInteractions(contractDeploymentRequestRepository)
        }
    }

    @Test
    fun mustCorrectlyReturnListOfContractDeploymentRequestsByProjectId() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()
        val filters = ContractDeploymentRequestFilters(
            contractIds = OrList(),
            contractTags = OrList(),
            contractImplements = OrList(),
            deployedOnly = false
        )

        val pendingRequest = STORED_REQUEST.copy(contractAddress = null, txHash = TransactionHash("other-tx-hash"))

        suppose("contract deployment request exists in database") {
            given(contractDeploymentRequestRepository.getAllByProjectId(PROJECT.id, filters))
                .willReturn(
                    listOf(
                        STORED_REQUEST,
                        pendingRequest
                    )
                )
        }

        val blockchainService = mock<BlockchainService>()

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH))
                .willReturn(TRANSACTION_INFO)
        }

        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepositoryMock(exists = true),
            contractDecoratorRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(PROJECT.id)
        )

        verify("contract deployment request with successful status is returned") {
            assertThat(service.getContractDeploymentRequestsByProjectIdAndFilters(PROJECT.id, filters))
                .withMessage()
                .isEqualTo(
                    listOf(
                        STORED_REQUEST.withTransactionData(
                            status = Status.SUCCESS,
                            transactionInfo = TRANSACTION_INFO
                        ),
                        pendingRequest.withTransactionData(
                            status = Status.PENDING,
                            transactionInfo = null
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyReturnListOfContractDeploymentRequestsByProjectIdAndDeployedFilter() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()
        val filters = ContractDeploymentRequestFilters(
            contractIds = OrList(),
            contractTags = OrList(),
            contractImplements = OrList(),
            deployedOnly = true
        )

        suppose("contract deployment request exists in database") {
            given(contractDeploymentRequestRepository.getAllByProjectId(PROJECT.id, filters))
                .willReturn(
                    listOf(
                        STORED_REQUEST,
                        STORED_REQUEST.copy(contractAddress = null, txHash = TransactionHash("other-tx-hash"))
                    )
                )
        }

        val blockchainService = mock<BlockchainService>()

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH))
                .willReturn(TRANSACTION_INFO)
        }

        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepositoryMock(exists = true),
            contractDecoratorRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(PROJECT.id)
        )

        verify("contract deployment request with successful status is returned") {
            assertThat(service.getContractDeploymentRequestsByProjectIdAndFilters(PROJECT.id, filters))
                .withMessage()
                .isEqualTo(
                    listOf(
                        STORED_REQUEST.withTransactionData(
                            status = Status.SUCCESS,
                            transactionInfo = TRANSACTION_INFO
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyReturnEmptyListOfContractDeploymentRequestsForNonExistentProject() {
        val projectId = UUID.randomUUID()
        val filters = ContractDeploymentRequestFilters(
            contractIds = OrList(),
            contractTags = OrList(),
            contractImplements = OrList(),
            deployedOnly = false
        )
        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = mock(),
            contractMetadataRepository = contractMetadataRepositoryMock(exists = true),
            contractDecoratorRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = projectRepositoryMock(projectId)
        )

        verify("empty list is returned") {
            val result = service.getContractDeploymentRequestsByProjectIdAndFilters(PROJECT.id, filters)

            assertThat(result).withMessage()
                .isEmpty()
        }
    }

    @Test
    fun mustSuccessfullyAttachTxInfo() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()
        val deployer = WalletAddress("0xbc25524e0daacB1F149BA55279f593F5E3FB73e9")

        suppose("txInfo will be successfully attached to the request") {
            given(contractDeploymentRequestRepository.setTxInfo(ID, TX_HASH, deployer))
                .willReturn(true)
        }

        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepositoryMock(exists = true),
            contractDecoratorRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = projectRepositoryMock(PROJECT.id)
        )

        verify("txInfo was successfully attached") {
            service.attachTxInfo(ID, TX_HASH, deployer)

            verifyMock(contractDeploymentRequestRepository)
                .setTxInfo(ID, TX_HASH, deployer)
            verifyNoMoreInteractions(contractDeploymentRequestRepository)
        }
    }

    @Test
    fun mustThrowCannotAttachTxInfoExceptionWhenAttachingTxInfoFails() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()
        val deployer = WalletAddress("0xbc25524e0daacB1F149BA55279f593F5E3FB73e9")

        suppose("attaching txInfo will fail") {
            given(contractDeploymentRequestRepository.setTxInfo(ID, TX_HASH, deployer))
                .willReturn(false)
        }

        val service = ContractDeploymentRequestServiceImpl(
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepositoryMock(exists = true),
            contractDecoratorRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = projectRepositoryMock(PROJECT.id)
        )

        verify("CannotAttachTxInfoException is thrown") {
            assertThrows<CannotAttachTxInfoException>(message) {
                service.attachTxInfo(ID, TX_HASH, deployer)
            }

            verifyMock(contractDeploymentRequestRepository)
                .setTxInfo(ID, TX_HASH, deployer)
            verifyNoMoreInteractions(contractDeploymentRequestRepository)
        }
    }

    private fun projectRepositoryMock(projectId: UUID): ProjectRepository {
        val projectRepository = mock<ProjectRepository>()

        given(projectRepository.getById(projectId))
            .willReturn(
                Project(
                    id = projectId,
                    ownerId = UUID.randomUUID(),
                    issuerContractAddress = ContractAddress("dead"),
                    baseRedirectUrl = BaseUrl(""),
                    chainId = ChainId(0L),
                    customRpcUrl = null,
                    createdAt = TestData.TIMESTAMP
                )
            )

        return projectRepository
    }

    private fun contractMetadataRepositoryMock(exists: Boolean): ContractMetadataRepository {
        val contractMetadataRepository = mock<ContractMetadataRepository>()

        given(contractMetadataRepository.exists(CONTRACT_ID, Constants.NIL_UUID))
            .willReturn(exists)

        return contractMetadataRepository
    }
}
