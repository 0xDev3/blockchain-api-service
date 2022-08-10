package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.blockchain.BlockchainService
import com.ampnet.blockchainapiservice.blockchain.properties.ChainSpec
import com.ampnet.blockchainapiservice.config.JsonConfig
import com.ampnet.blockchainapiservice.exception.CannotAttachTxInfoException
import com.ampnet.blockchainapiservice.exception.ResourceNotFoundException
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.filters.ContractFunctionCallRequestFilters
import com.ampnet.blockchainapiservice.model.params.CreateContractFunctionCallRequestParams
import com.ampnet.blockchainapiservice.model.params.DeployedContractIdIdentifier
import com.ampnet.blockchainapiservice.model.params.StoreContractFunctionCallRequestParams
import com.ampnet.blockchainapiservice.model.result.BlockchainTransactionInfo
import com.ampnet.blockchainapiservice.model.result.ContractDeploymentRequest
import com.ampnet.blockchainapiservice.model.result.ContractFunctionCallRequest
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.repository.ContractDeploymentRequestRepository
import com.ampnet.blockchainapiservice.repository.ContractFunctionCallRequestRepository
import com.ampnet.blockchainapiservice.repository.ProjectRepository
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.BaseUrl
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.ContractBinaryData
import com.ampnet.blockchainapiservice.util.ContractId
import com.ampnet.blockchainapiservice.util.FunctionArgument
import com.ampnet.blockchainapiservice.util.FunctionData
import com.ampnet.blockchainapiservice.util.JsonNodeConverter
import com.ampnet.blockchainapiservice.util.Status
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.ampnet.blockchainapiservice.util.WithFunctionData
import org.assertj.core.api.Assertions.assertThat
import org.jooq.JSON
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoMoreInteractions
import java.math.BigInteger
import java.util.UUID
import org.mockito.kotlin.verify as verifyMock

class ContractFunctionCallRequestServiceTest : TestBase() {

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
        private val OBJECT_MAPPER = JsonConfig().objectMapper()
        private val RAW_FUNCTION_PARAMS = JsonNodeConverter().from(
            JSON.valueOf("[{\"type\": \"string\", \"value\": \"test\"}]")
        )!!
        private val DEPLOYED_CONTRACT_ID = UUID.randomUUID()
        private val DEPLOYED_CONTRACT_ID_CREATE_PARAMS = CreateContractFunctionCallRequestParams(
            identifier = DeployedContractIdIdentifier(DEPLOYED_CONTRACT_ID),
            functionName = "test",
            functionParams = OBJECT_MAPPER.treeToValue(RAW_FUNCTION_PARAMS, Array<FunctionArgument>::class.java)
                .toList(),
            ethAmount = Balance(BigInteger("10000")),
            redirectUrl = "redirect-url/\${id}",
            callerAddress = WalletAddress("a"),
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            )
        )
        private val CONTRACT_ADDRESS = ContractAddress("abc123")
        private val ID = UUID.randomUUID()
        private val ENCODED_FUNCTION_DATA = FunctionData("0x1234")
        private val STORE_PARAMS = StoreContractFunctionCallRequestParams(
            id = ID,
            deployedContractId = DEPLOYED_CONTRACT_ID,
            contractAddress = CONTRACT_ADDRESS,
            functionName = DEPLOYED_CONTRACT_ID_CREATE_PARAMS.functionName,
            functionParams = RAW_FUNCTION_PARAMS,
            ethAmount = DEPLOYED_CONTRACT_ID_CREATE_PARAMS.ethAmount,
            chainId = PROJECT.chainId,
            redirectUrl = DEPLOYED_CONTRACT_ID_CREATE_PARAMS.redirectUrl!!.replace("\${id}", ID.toString()),
            projectId = PROJECT.id,
            createdAt = TestData.TIMESTAMP,
            arbitraryData = DEPLOYED_CONTRACT_ID_CREATE_PARAMS.arbitraryData,
            screenConfig = DEPLOYED_CONTRACT_ID_CREATE_PARAMS.screenConfig,
            callerAddress = DEPLOYED_CONTRACT_ID_CREATE_PARAMS.callerAddress
        )
        private val TX_HASH = TransactionHash("tx-hash")
        private val STORED_REQUEST = ContractFunctionCallRequest(
            id = ID,
            deployedContractId = DEPLOYED_CONTRACT_ID,
            contractAddress = CONTRACT_ADDRESS,
            functionName = STORE_PARAMS.functionName,
            functionParams = STORE_PARAMS.functionParams,
            ethAmount = STORE_PARAMS.ethAmount,
            chainId = STORE_PARAMS.chainId,
            redirectUrl = STORE_PARAMS.redirectUrl,
            projectId = PROJECT.id,
            createdAt = TestData.TIMESTAMP,
            arbitraryData = STORE_PARAMS.arbitraryData,
            screenConfig = STORE_PARAMS.screenConfig,
            callerAddress = STORE_PARAMS.callerAddress,
            txHash = TX_HASH
        )
        private val CHAIN_SPEC = ChainSpec(STORED_REQUEST.chainId, null)
        private val TRANSACTION_INFO = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = STORED_REQUEST.callerAddress!!,
            to = CONTRACT_ADDRESS,
            deployedContractAddress = null,
            data = ENCODED_FUNCTION_DATA,
            value = STORED_REQUEST.ethAmount,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true
        )
        private val DEPLOYED_CONTRACT = ContractDeploymentRequest(
            id = DEPLOYED_CONTRACT_ID,
            alias = "contract-alias",
            contractId = ContractId("cid"),
            contractData = ContractBinaryData("00"),
            constructorParams = TestData.EMPTY_JSON_ARRAY,
            contractTags = emptyList(),
            contractImplements = emptyList(),
            initialEthAmount = Balance(BigInteger.ZERO),
            chainId = CHAIN_SPEC.chainId,
            redirectUrl = "redirect-url",
            projectId = PROJECT.id,
            createdAt = TestData.TIMESTAMP,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = STORE_PARAMS.screenConfig,
            contractAddress = CONTRACT_ADDRESS,
            deployerAddress = STORE_PARAMS.callerAddress,
            txHash = TransactionHash("deployed-contract-hash")
        )
    }

    @Test
    fun mustCorrectlyCreateFunctionCallRequest() {
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
        val createParams = DEPLOYED_CONTRACT_ID_CREATE_PARAMS

        suppose("function will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = createParams.functionName,
                    arguments = createParams.functionParams
                )
            )
                .willReturn(ENCODED_FUNCTION_DATA)
        }

        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("deployed contract is returned from database") {
            given(contractDeploymentRequestRepository.getById(DEPLOYED_CONTRACT_ID))
                .willReturn(DEPLOYED_CONTRACT)
        }

        val contractFunctionCallRequestRepository = mock<ContractFunctionCallRequestRepository>()

        suppose("contract function call request is stored in database") {
            given(contractFunctionCallRequestRepository.store(STORE_PARAMS))
                .willReturn(STORED_REQUEST)
        }

        val service = ContractFunctionCallRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            contractFunctionCallRequestRepository = contractFunctionCallRequestRepository,
            deployedContractIdentifierResolverService = service(contractDeploymentRequestRepository),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = uuidProvider,
                utcDateTimeProvider = utcDateTimeProvider,
                blockchainService = mock()
            ),
            projectRepository = mock(),
            objectMapper = JsonConfig().objectMapper()
        )

        verify("contract function call request is correctly created") {
            assertThat(service.createContractFunctionCallRequest(createParams, PROJECT)).withMessage()
                .isEqualTo(WithFunctionData(STORED_REQUEST, ENCODED_FUNCTION_DATA))

            verifyMock(contractFunctionCallRequestRepository)
                .store(STORE_PARAMS)
            verifyNoMoreInteractions(contractFunctionCallRequestRepository)
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionForNonExistentContractFunctionCallRequest() {
        val contractFunctionCallRequestRepository = mock<ContractFunctionCallRequestRepository>()

        suppose("contract function call request is not found in the database") {
            given(contractFunctionCallRequestRepository.getById(ID))
                .willReturn(null)
        }

        val service = ContractFunctionCallRequestServiceImpl(
            functionEncoderService = mock(),
            contractFunctionCallRequestRepository = contractFunctionCallRequestRepository,
            deployedContractIdentifierResolverService = service(mock()),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = mock(),
            objectMapper = JsonConfig().objectMapper()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.getContractFunctionCallRequest(ID)
            }
        }
    }

    @Test
    fun mustReturnContractFunctionCallRequestWithPendingStatusWhenContractFunctionCallRequestHasNullTxHash() {
        val contractFunctionCallRequestRepository = mock<ContractFunctionCallRequestRepository>()
        val request = STORED_REQUEST.copy(txHash = null)

        suppose("contract function call request exists in the database") {
            given(contractFunctionCallRequestRepository.getById(ID))
                .willReturn(request)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = request.functionName,
                    arguments = DEPLOYED_CONTRACT_ID_CREATE_PARAMS.functionParams
                )
            )
                .willReturn(ENCODED_FUNCTION_DATA)
        }

        val service = ContractFunctionCallRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            contractFunctionCallRequestRepository = contractFunctionCallRequestRepository,
            deployedContractIdentifierResolverService = service(mock()),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = projectRepositoryMock(PROJECT.id),
            objectMapper = JsonConfig().objectMapper()
        )

        verify("contract function call request with pending status is returned") {
            assertThat(service.getContractFunctionCallRequest(ID)).withMessage()
                .isEqualTo(
                    request.withTransactionAndFunctionData(
                        status = Status.PENDING,
                        data = ENCODED_FUNCTION_DATA,
                        transactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnContractFunctionCallRequestWithPendingStatusWhenTransactionIsNotYetMined() {
        val contractFunctionCallRequestRepository = mock<ContractFunctionCallRequestRepository>()
        val request = STORED_REQUEST

        suppose("contract function call request exists in the database") {
            given(contractFunctionCallRequestRepository.getById(ID))
                .willReturn(request)
        }

        val blockchainService = mock<BlockchainService>()

        suppose("transaction is not yet mined") {
            given(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH))
                .willReturn(null)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = request.functionName,
                    arguments = DEPLOYED_CONTRACT_ID_CREATE_PARAMS.functionParams
                )
            )
                .willReturn(ENCODED_FUNCTION_DATA)
        }

        val service = ContractFunctionCallRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            contractFunctionCallRequestRepository = contractFunctionCallRequestRepository,
            deployedContractIdentifierResolverService = service(mock()),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(PROJECT.id),
            objectMapper = JsonConfig().objectMapper()
        )

        verify("contract function call request with pending status is returned") {
            assertThat(service.getContractFunctionCallRequest(ID)).withMessage()
                .isEqualTo(
                    request.withTransactionAndFunctionData(
                        status = Status.PENDING,
                        data = ENCODED_FUNCTION_DATA,
                        transactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnContractFunctionCallRequestWithFailedStatusWhenTransactionIsNotSuccessful() {
        val contractFunctionCallRequestRepository = mock<ContractFunctionCallRequestRepository>()
        val request = STORED_REQUEST

        suppose("contract function call request exists in the database") {
            given(contractFunctionCallRequestRepository.getById(ID))
                .willReturn(request)
        }

        val blockchainService = mock<BlockchainService>()
        val transactionInfo = TRANSACTION_INFO.copy(success = false)

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = request.functionName,
                    arguments = DEPLOYED_CONTRACT_ID_CREATE_PARAMS.functionParams
                )
            )
                .willReturn(ENCODED_FUNCTION_DATA)
        }

        val service = ContractFunctionCallRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            contractFunctionCallRequestRepository = contractFunctionCallRequestRepository,
            deployedContractIdentifierResolverService = service(mock()),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(PROJECT.id),
            objectMapper = JsonConfig().objectMapper()
        )

        verify("contract function call request with failed status is returned") {
            assertThat(service.getContractFunctionCallRequest(ID)).withMessage()
                .isEqualTo(
                    request.withTransactionAndFunctionData(
                        status = Status.FAILED,
                        data = ENCODED_FUNCTION_DATA,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnContractFunctionCallRequestWithFailedStatusWhenTransactionHasWrongTxHash() {
        val contractFunctionCallRequestRepository = mock<ContractFunctionCallRequestRepository>()
        val request = STORED_REQUEST

        suppose("contract function call request exists in the database") {
            given(contractFunctionCallRequestRepository.getById(ID))
                .willReturn(request)
        }

        val blockchainService = mock<BlockchainService>()
        val transactionInfo = TRANSACTION_INFO.copy(hash = TransactionHash("other-tx-hash"))

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = request.functionName,
                    arguments = DEPLOYED_CONTRACT_ID_CREATE_PARAMS.functionParams
                )
            )
                .willReturn(ENCODED_FUNCTION_DATA)
        }

        val service = ContractFunctionCallRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            contractFunctionCallRequestRepository = contractFunctionCallRequestRepository,
            deployedContractIdentifierResolverService = service(mock()),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(PROJECT.id),
            objectMapper = JsonConfig().objectMapper()
        )

        verify("contract function call request with failed status is returned") {
            assertThat(service.getContractFunctionCallRequest(ID)).withMessage()
                .isEqualTo(
                    request.withTransactionAndFunctionData(
                        status = Status.FAILED,
                        data = ENCODED_FUNCTION_DATA,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnContractFunctionCallRequestWithFailedStatusWhenTransactionHasWrongCallerAddress() {
        val contractFunctionCallRequestRepository = mock<ContractFunctionCallRequestRepository>()
        val request = STORED_REQUEST

        suppose("contract function call request exists in the database") {
            given(contractFunctionCallRequestRepository.getById(ID))
                .willReturn(request)
        }

        val blockchainService = mock<BlockchainService>()
        val transactionInfo = TRANSACTION_INFO.copy(from = WalletAddress("dead"))

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = request.functionName,
                    arguments = DEPLOYED_CONTRACT_ID_CREATE_PARAMS.functionParams
                )
            )
                .willReturn(ENCODED_FUNCTION_DATA)
        }

        val service = ContractFunctionCallRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            contractFunctionCallRequestRepository = contractFunctionCallRequestRepository,
            deployedContractIdentifierResolverService = service(mock()),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(PROJECT.id),
            objectMapper = JsonConfig().objectMapper()
        )

        verify("contract function call request with failed status is returned") {
            assertThat(service.getContractFunctionCallRequest(ID)).withMessage()
                .isEqualTo(
                    request.withTransactionAndFunctionData(
                        status = Status.FAILED,
                        data = ENCODED_FUNCTION_DATA,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnContractFunctionCallRequestWithFailedStatusWhenTransactionHasWrongContractAddress() {
        val contractFunctionCallRequestRepository = mock<ContractFunctionCallRequestRepository>()
        val request = STORED_REQUEST

        suppose("contract function call request exists in the database") {
            given(contractFunctionCallRequestRepository.getById(ID))
                .willReturn(request)
        }

        val blockchainService = mock<BlockchainService>()
        val transactionInfo = TRANSACTION_INFO.copy(to = ContractAddress("dead"))

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = request.functionName,
                    arguments = DEPLOYED_CONTRACT_ID_CREATE_PARAMS.functionParams
                )
            )
                .willReturn(ENCODED_FUNCTION_DATA)
        }

        val service = ContractFunctionCallRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            contractFunctionCallRequestRepository = contractFunctionCallRequestRepository,
            deployedContractIdentifierResolverService = service(mock()),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(PROJECT.id),
            objectMapper = JsonConfig().objectMapper()
        )

        verify("contract function call request with failed status is returned") {
            assertThat(service.getContractFunctionCallRequest(ID)).withMessage()
                .isEqualTo(
                    request.withTransactionAndFunctionData(
                        status = Status.FAILED,
                        data = ENCODED_FUNCTION_DATA,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnContractFunctionCallRequestWithFailedStatusWhenTransactionHasWrongData() {
        val contractFunctionCallRequestRepository = mock<ContractFunctionCallRequestRepository>()
        val request = STORED_REQUEST

        suppose("contract function call request exists in the database") {
            given(contractFunctionCallRequestRepository.getById(ID))
                .willReturn(request)
        }

        val blockchainService = mock<BlockchainService>()
        val transactionInfo = TRANSACTION_INFO.copy(data = FunctionData("dead"))

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = request.functionName,
                    arguments = DEPLOYED_CONTRACT_ID_CREATE_PARAMS.functionParams
                )
            )
                .willReturn(ENCODED_FUNCTION_DATA)
        }

        val service = ContractFunctionCallRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            contractFunctionCallRequestRepository = contractFunctionCallRequestRepository,
            deployedContractIdentifierResolverService = service(mock()),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(PROJECT.id),
            objectMapper = JsonConfig().objectMapper()
        )

        verify("contract function call request with failed status is returned") {
            assertThat(service.getContractFunctionCallRequest(ID)).withMessage()
                .isEqualTo(
                    request.withTransactionAndFunctionData(
                        status = Status.FAILED,
                        data = ENCODED_FUNCTION_DATA,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnContractFunctionCallRequestWithFailedStatusWhenTransactionHasWrongValue() {
        val contractFunctionCallRequestRepository = mock<ContractFunctionCallRequestRepository>()
        val request = STORED_REQUEST

        suppose("contract function call request exists in the database") {
            given(contractFunctionCallRequestRepository.getById(ID))
                .willReturn(request)
        }

        val blockchainService = mock<BlockchainService>()
        val transactionInfo = TRANSACTION_INFO.copy(value = Balance(BigInteger.valueOf(123456L)))

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = request.functionName,
                    arguments = DEPLOYED_CONTRACT_ID_CREATE_PARAMS.functionParams
                )
            )
                .willReturn(ENCODED_FUNCTION_DATA)
        }

        val service = ContractFunctionCallRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            contractFunctionCallRequestRepository = contractFunctionCallRequestRepository,
            deployedContractIdentifierResolverService = service(mock()),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(PROJECT.id),
            objectMapper = JsonConfig().objectMapper()
        )

        verify("contract function call request with failed status is returned") {
            assertThat(service.getContractFunctionCallRequest(ID)).withMessage()
                .isEqualTo(
                    request.withTransactionAndFunctionData(
                        status = Status.FAILED,
                        data = ENCODED_FUNCTION_DATA,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnContractFunctionCallRequestWithSuccessfulStatusWhenCallerAddressIsNull() {
        val contractFunctionCallRequestRepository = mock<ContractFunctionCallRequestRepository>()
        val request = STORED_REQUEST.copy(callerAddress = null)

        suppose("contract function call request exists in the database") {
            given(contractFunctionCallRequestRepository.getById(ID))
                .willReturn(request)
        }

        val blockchainService = mock<BlockchainService>()
        val transactionInfo = TRANSACTION_INFO.copy(from = WalletAddress("dead"))

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = request.functionName,
                    arguments = DEPLOYED_CONTRACT_ID_CREATE_PARAMS.functionParams
                )
            )
                .willReturn(ENCODED_FUNCTION_DATA)
        }

        val service = ContractFunctionCallRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            contractFunctionCallRequestRepository = contractFunctionCallRequestRepository,
            deployedContractIdentifierResolverService = service(mock()),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(PROJECT.id),
            objectMapper = JsonConfig().objectMapper()
        )

        verify("contract function call request with successful status is returned") {
            assertThat(service.getContractFunctionCallRequest(ID)).withMessage()
                .isEqualTo(
                    request.withTransactionAndFunctionData(
                        status = Status.SUCCESS,
                        data = ENCODED_FUNCTION_DATA,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnContractFunctionCallRequestWithSuccessfulStatusWhenCallerAddressIsNotNull() {
        val contractFunctionCallRequestRepository = mock<ContractFunctionCallRequestRepository>()
        val request = STORED_REQUEST

        suppose("contract function call request exists in the database") {
            given(contractFunctionCallRequestRepository.getById(ID))
                .willReturn(request)
        }

        val blockchainService = mock<BlockchainService>()
        val transactionInfo = TRANSACTION_INFO

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = request.functionName,
                    arguments = DEPLOYED_CONTRACT_ID_CREATE_PARAMS.functionParams
                )
            )
                .willReturn(ENCODED_FUNCTION_DATA)
        }

        val service = ContractFunctionCallRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            contractFunctionCallRequestRepository = contractFunctionCallRequestRepository,
            deployedContractIdentifierResolverService = service(mock()),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(PROJECT.id),
            objectMapper = JsonConfig().objectMapper()
        )

        verify("contract function call request with successful status is returned") {
            assertThat(service.getContractFunctionCallRequest(ID)).withMessage()
                .isEqualTo(
                    request.withTransactionAndFunctionData(
                        status = Status.SUCCESS,
                        data = ENCODED_FUNCTION_DATA,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyReturnListOfContractFunctionCallRequestsByProjectId() {
        val contractFunctionCallRequestRepository = mock<ContractFunctionCallRequestRepository>()
        val filters = ContractFunctionCallRequestFilters(
            deployedContractId = UUID.randomUUID(),
            contractAddress = ContractAddress("cafebabe")
        )

        val request = STORED_REQUEST.copy(txHash = null)

        suppose("contract function call request exists in the database") {
            given(contractFunctionCallRequestRepository.getAllByProjectId(PROJECT.id, filters))
                .willReturn(listOf(request))
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = request.functionName,
                    arguments = DEPLOYED_CONTRACT_ID_CREATE_PARAMS.functionParams
                )
            )
                .willReturn(ENCODED_FUNCTION_DATA)
        }

        val service = ContractFunctionCallRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            contractFunctionCallRequestRepository = contractFunctionCallRequestRepository,
            deployedContractIdentifierResolverService = service(mock()),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = projectRepositoryMock(PROJECT.id),
            objectMapper = JsonConfig().objectMapper()
        )

        verify("contract function call request with pending status is returned") {
            assertThat(service.getContractFunctionCallRequestsByProjectIdAndFilters(PROJECT.id, filters)).withMessage()
                .isEqualTo(
                    listOf(
                        request.withTransactionAndFunctionData(
                            status = Status.PENDING,
                            data = ENCODED_FUNCTION_DATA,
                            transactionInfo = null
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyReturnEmptyListOfContractFunctionCallRequestsForNonExistentProject() {
        val projectId = UUID.randomUUID()
        val filters = ContractFunctionCallRequestFilters(
            deployedContractId = UUID.randomUUID(),
            contractAddress = ContractAddress("cafebabe")
        )

        val service = ContractFunctionCallRequestServiceImpl(
            functionEncoderService = mock(),
            contractFunctionCallRequestRepository = mock(),
            deployedContractIdentifierResolverService = service(mock()),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = projectRepositoryMock(projectId),
            objectMapper = JsonConfig().objectMapper()
        )

        verify("empty list is returned") {
            assertThat(service.getContractFunctionCallRequestsByProjectIdAndFilters(PROJECT.id, filters)).withMessage()
                .isEmpty()
        }
    }

    @Test
    fun mustSuccessfullyAttachTxInfo() {
        val contractFunctionCallRequestRepository = mock<ContractFunctionCallRequestRepository>()
        val caller = WalletAddress("0xbc25524e0daacB1F149BA55279f593F5E3FB73e9")

        suppose("txInfo will be successfully attached to the request") {
            given(contractFunctionCallRequestRepository.setTxInfo(ID, TX_HASH, caller))
                .willReturn(true)
        }

        val service = ContractFunctionCallRequestServiceImpl(
            functionEncoderService = mock(),
            contractFunctionCallRequestRepository = contractFunctionCallRequestRepository,
            deployedContractIdentifierResolverService = service(mock()),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = mock(),
            objectMapper = JsonConfig().objectMapper()
        )

        verify("txInfo was successfully attached") {
            service.attachTxInfo(ID, TX_HASH, caller)

            verifyMock(contractFunctionCallRequestRepository)
                .setTxInfo(ID, TX_HASH, caller)
            verifyNoMoreInteractions(contractFunctionCallRequestRepository)
        }
    }

    @Test
    fun mustThrowCannotAttachTxInfoExceptionWhenAttachingTxInfoFails() {
        val contractFunctionCallRequestRepository = mock<ContractFunctionCallRequestRepository>()
        val caller = WalletAddress("0xbc25524e0daacB1F149BA55279f593F5E3FB73e9")

        suppose("attaching txInfo will fail") {
            given(contractFunctionCallRequestRepository.setTxInfo(ID, TX_HASH, caller))
                .willReturn(false)
        }

        val service = ContractFunctionCallRequestServiceImpl(
            functionEncoderService = mock(),
            contractFunctionCallRequestRepository = contractFunctionCallRequestRepository,
            deployedContractIdentifierResolverService = service(mock()),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = mock(),
            objectMapper = JsonConfig().objectMapper()
        )

        verify("CannotAttachTxInfoException is thrown") {
            assertThrows<CannotAttachTxInfoException>(message) {
                service.attachTxInfo(ID, TX_HASH, caller)
            }

            verifyMock(contractFunctionCallRequestRepository)
                .setTxInfo(ID, TX_HASH, caller)
            verifyNoMoreInteractions(contractFunctionCallRequestRepository)
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

    private fun service(repository: ContractDeploymentRequestRepository) =
        DeployedContractIdentifierResolverServiceImpl(repository, mock())
}
