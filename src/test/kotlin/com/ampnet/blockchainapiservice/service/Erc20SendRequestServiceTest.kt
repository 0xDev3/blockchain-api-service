package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.blockchain.BlockchainService
import com.ampnet.blockchainapiservice.blockchain.properties.Chain
import com.ampnet.blockchainapiservice.blockchain.properties.ChainSpec
import com.ampnet.blockchainapiservice.exception.CannotAttachTxInfoException
import com.ampnet.blockchainapiservice.exception.ResourceNotFoundException
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.params.CreateErc20SendRequestParams
import com.ampnet.blockchainapiservice.model.params.StoreErc20SendRequestParams
import com.ampnet.blockchainapiservice.model.result.BlockchainTransactionInfo
import com.ampnet.blockchainapiservice.model.result.Erc20SendRequest
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.repository.Erc20SendRequestRepository
import com.ampnet.blockchainapiservice.repository.ProjectRepository
import com.ampnet.blockchainapiservice.util.AbiType.AbiType
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.BaseUrl
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.FunctionArgument
import com.ampnet.blockchainapiservice.util.FunctionData
import com.ampnet.blockchainapiservice.util.Status
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.ampnet.blockchainapiservice.util.WithFunctionDataOrEthValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoMoreInteractions
import org.web3j.abi.datatypes.Utf8String
import java.math.BigInteger
import java.util.UUID
import org.mockito.kotlin.verify as verifyMock

class Erc20SendRequestServiceTest : TestBase() {

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
        private val CREATE_PARAMS = CreateErc20SendRequestParams(
            redirectUrl = "redirect-url/\${id}",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.valueOf(123456L)),
            tokenSenderAddress = WalletAddress("b"),
            tokenRecipientAddress = WalletAddress("c"),
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            )
        )
        private val TX_HASH = TransactionHash("tx-hash")
    }

    @Test
    fun mustSuccessfullyCreateErc20SendRequestForSomeToken() {
        val uuidProvider = mock<UuidProvider>()
        val uuid = UUID.randomUUID()

        suppose("some UUID will be generated") {
            given(uuidProvider.getUuid())
                .willReturn(uuid)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some timestamp will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val functionEncoderService = mock<FunctionEncoderService>()
        val encodedData = FunctionData("encoded")

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "transfer",
                    arguments = listOf(
                        FunctionArgument(abiType = AbiType.Address, value = CREATE_PARAMS.tokenRecipientAddress),
                        FunctionArgument(abiType = AbiType.Uint256, value = CREATE_PARAMS.tokenAmount)
                    ),
                    abiOutputTypes = listOf(AbiType.Bool),
                    additionalData = listOf(Utf8String(uuid.toString()))
                )
            )
                .willReturn(encodedData)
        }

        val erc20SendRequestRepository = mock<Erc20SendRequestRepository>()
        val redirectUrl = CREATE_PARAMS.redirectUrl!!

        val storeParams = StoreErc20SendRequestParams(
            id = uuid,
            projectId = PROJECT.id,
            chainId = PROJECT.chainId,
            redirectUrl = redirectUrl.replace("\${id}", uuid.toString()),
            tokenAddress = CREATE_PARAMS.tokenAddress,
            tokenAmount = CREATE_PARAMS.tokenAmount,
            tokenSenderAddress = CREATE_PARAMS.tokenSenderAddress,
            tokenRecipientAddress = CREATE_PARAMS.tokenRecipientAddress,
            arbitraryData = CREATE_PARAMS.arbitraryData,
            screenConfig = CREATE_PARAMS.screenConfig,
            createdAt = TestData.TIMESTAMP
        )

        val storedRequest = Erc20SendRequest(
            id = uuid,
            projectId = PROJECT.id,
            chainId = PROJECT.chainId,
            redirectUrl = storeParams.redirectUrl,
            tokenAddress = CREATE_PARAMS.tokenAddress,
            tokenAmount = CREATE_PARAMS.tokenAmount,
            tokenSenderAddress = CREATE_PARAMS.tokenSenderAddress,
            tokenRecipientAddress = CREATE_PARAMS.tokenRecipientAddress,
            txHash = null,
            arbitraryData = CREATE_PARAMS.arbitraryData,
            screenConfig = CREATE_PARAMS.screenConfig,
            createdAt = TestData.TIMESTAMP
        )

        suppose("ERC20 send request is stored in database") {
            given(erc20SendRequestRepository.store(storeParams))
                .willReturn(storedRequest)
        }

        val service = Erc20SendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20SendRequestRepository = erc20SendRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = uuidProvider,
                utcDateTimeProvider = utcDateTimeProvider,
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("ERC20 send request is correctly created") {
            assertThat(service.createErc20SendRequest(CREATE_PARAMS, PROJECT)).withMessage()
                .isEqualTo(WithFunctionDataOrEthValue(storedRequest, encodedData, null))

            verifyMock(erc20SendRequestRepository)
                .store(storeParams)
            verifyNoMoreInteractions(erc20SendRequestRepository)
        }
    }

    @Test
    fun mustSuccessfullyCreateErc20SendRequestForNativeAsset() {
        val uuidProvider = mock<UuidProvider>()
        val uuid = UUID.randomUUID()

        suppose("some UUID will be generated") {
            given(uuidProvider.getUuid())
                .willReturn(uuid)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some timestamp will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val erc20SendRequestRepository = mock<Erc20SendRequestRepository>()
        val redirectUrl = CREATE_PARAMS.redirectUrl!!

        val storeParams = StoreErc20SendRequestParams(
            id = uuid,
            projectId = PROJECT.id,
            chainId = PROJECT.chainId,
            redirectUrl = redirectUrl.replace("\${id}", uuid.toString()),
            tokenAddress = null,
            tokenAmount = CREATE_PARAMS.tokenAmount,
            tokenSenderAddress = CREATE_PARAMS.tokenSenderAddress,
            tokenRecipientAddress = CREATE_PARAMS.tokenRecipientAddress,
            arbitraryData = CREATE_PARAMS.arbitraryData,
            screenConfig = CREATE_PARAMS.screenConfig,
            createdAt = TestData.TIMESTAMP
        )

        val storedRequest = Erc20SendRequest(
            id = uuid,
            projectId = PROJECT.id,
            chainId = PROJECT.chainId,
            redirectUrl = storeParams.redirectUrl,
            tokenAddress = null,
            tokenAmount = CREATE_PARAMS.tokenAmount,
            tokenSenderAddress = CREATE_PARAMS.tokenSenderAddress,
            tokenRecipientAddress = CREATE_PARAMS.tokenRecipientAddress,
            txHash = null,
            arbitraryData = CREATE_PARAMS.arbitraryData,
            screenConfig = CREATE_PARAMS.screenConfig,
            createdAt = TestData.TIMESTAMP
        )

        suppose("ERC20 send request is stored in database") {
            given(erc20SendRequestRepository.store(storeParams))
                .willReturn(storedRequest)
        }

        val service = Erc20SendRequestServiceImpl(
            functionEncoderService = mock(),
            erc20SendRequestRepository = erc20SendRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = uuidProvider,
                utcDateTimeProvider = utcDateTimeProvider,
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("ERC20 send request is correctly created") {
            assertThat(service.createErc20SendRequest(CREATE_PARAMS.copy(tokenAddress = null), PROJECT)).withMessage()
                .isEqualTo(WithFunctionDataOrEthValue(storedRequest, null, CREATE_PARAMS.tokenAmount))

            verifyMock(erc20SendRequestRepository)
                .store(storeParams)
            verifyNoMoreInteractions(erc20SendRequestRepository)
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionForNonExistentErc20SendRequest() {
        val erc20SendRequestRepository = mock<Erc20SendRequestRepository>()

        suppose("ERC20 send request does not exist in database") {
            given(erc20SendRequestRepository.getById(any()))
                .willReturn(null)
        }

        val service = Erc20SendRequestServiceImpl(
            functionEncoderService = mock(),
            erc20SendRequestRepository = erc20SendRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.getErc20SendRequest(id = UUID.randomUUID())
            }
        }
    }

    @Test
    fun mustReturnErc20SendRequestWithPendingStatusWhenErc20SendRequestHasNullTxHash() {
        val id = UUID.randomUUID()
        val sendRequest = Erc20SendRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = Chain.HARDHAT_TESTNET.id,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            tokenSenderAddress = WalletAddress("b"),
            tokenRecipientAddress = WalletAddress("c"),
            txHash = null,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val erc20SendRequestRepository = mock<Erc20SendRequestRepository>()

        suppose("ERC20 send request exists in database") {
            given(erc20SendRequestRepository.getById(id))
                .willReturn(sendRequest)
        }

        val functionEncoderService = mock<FunctionEncoderService>()
        val encodedData = FunctionData("encoded")

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "transfer",
                    arguments = listOf(
                        FunctionArgument(abiType = AbiType.Address, value = sendRequest.tokenRecipientAddress),
                        FunctionArgument(abiType = AbiType.Uint256, value = sendRequest.tokenAmount)
                    ),
                    abiOutputTypes = listOf(AbiType.Bool),
                    additionalData = listOf(Utf8String(id.toString()))
                )
            )
                .willReturn(encodedData)
        }

        val service = Erc20SendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20SendRequestRepository = erc20SendRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = projectRepositoryMock(sendRequest.projectId)
        )

        verify("ERC20 send request with pending status is returned") {
            assertThat(service.getErc20SendRequest(id)).withMessage()
                .isEqualTo(
                    sendRequest.withTransactionData(
                        status = Status.PENDING,
                        data = encodedData,
                        value = null,
                        transactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20SendRequestWithPendingStatusWhenTransactionIsNotYetMined() {
        val id = UUID.randomUUID()
        val sendRequest = Erc20SendRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = Chain.HARDHAT_TESTNET.id,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            tokenSenderAddress = WalletAddress("b"),
            tokenRecipientAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val erc20SendRequestRepository = mock<Erc20SendRequestRepository>()

        suppose("ERC20 send request exists in database") {
            given(erc20SendRequestRepository.getById(id))
                .willReturn(sendRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(sendRequest.chainId, null)

        suppose("transaction is not yet mined") {
            given(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH))
                .willReturn(null)
        }

        val functionEncoderService = mock<FunctionEncoderService>()
        val encodedData = FunctionData("encoded")

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "transfer",
                    arguments = listOf(
                        FunctionArgument(abiType = AbiType.Address, value = sendRequest.tokenRecipientAddress),
                        FunctionArgument(abiType = AbiType.Uint256, value = sendRequest.tokenAmount)
                    ),
                    abiOutputTypes = listOf(AbiType.Bool),
                    additionalData = listOf(Utf8String(id.toString()))
                )
            )
                .willReturn(encodedData)
        }

        val service = Erc20SendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20SendRequestRepository = erc20SendRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = projectRepositoryMock(sendRequest.projectId)
        )

        verify("ERC20 send request with pending status is returned") {
            assertThat(service.getErc20SendRequest(id)).withMessage()
                .isEqualTo(
                    sendRequest.withTransactionData(
                        status = Status.PENDING,
                        data = encodedData,
                        value = null,
                        transactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20SendRequestWithFailedStatusWhenTransactionIsNotSuccessful() {
        val id = UUID.randomUUID()
        val sendRequest = Erc20SendRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = Chain.HARDHAT_TESTNET.id,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            tokenSenderAddress = WalletAddress("b"),
            tokenRecipientAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val erc20SendRequestRepository = mock<Erc20SendRequestRepository>()

        suppose("ERC20 send request exists in database") {
            given(erc20SendRequestRepository.getById(id))
                .willReturn(sendRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(sendRequest.chainId, null)
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = sendRequest.tokenSenderAddress!!,
            to = sendRequest.tokenAddress!!,
            data = encodedData,
            value = Balance(BigInteger.ZERO),
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = false
        )

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "transfer",
                    arguments = listOf(
                        FunctionArgument(abiType = AbiType.Address, value = sendRequest.tokenRecipientAddress),
                        FunctionArgument(abiType = AbiType.Uint256, value = sendRequest.tokenAmount)
                    ),
                    abiOutputTypes = listOf(AbiType.Bool),
                    additionalData = listOf(Utf8String(id.toString()))
                )
            )
                .willReturn(encodedData)
        }

        val service = Erc20SendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20SendRequestRepository = erc20SendRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(sendRequest.projectId)
        )

        verify("ERC20 send request with successful status is returned") {
            assertThat(service.getErc20SendRequest(id)).withMessage()
                .isEqualTo(
                    sendRequest.withTransactionData(
                        status = Status.FAILED,
                        data = encodedData,
                        value = null,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20SendRequestWithFailedStatusWhenTransactionHasWrongToAddress() {
        val id = UUID.randomUUID()
        val sendRequest = Erc20SendRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = Chain.HARDHAT_TESTNET.id,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            tokenSenderAddress = WalletAddress("b"),
            tokenRecipientAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val erc20SendRequestRepository = mock<Erc20SendRequestRepository>()

        suppose("ERC20 send request exists in database") {
            given(erc20SendRequestRepository.getById(id))
                .willReturn(sendRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(sendRequest.chainId, null)
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = sendRequest.tokenSenderAddress!!,
            to = WalletAddress("dead"),
            data = encodedData,
            value = Balance(BigInteger.ZERO),
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true
        )

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "transfer",
                    arguments = listOf(
                        FunctionArgument(abiType = AbiType.Address, value = sendRequest.tokenRecipientAddress),
                        FunctionArgument(abiType = AbiType.Uint256, value = sendRequest.tokenAmount)
                    ),
                    abiOutputTypes = listOf(AbiType.Bool),
                    additionalData = listOf(Utf8String(id.toString()))
                )
            )
                .willReturn(encodedData)
        }

        val service = Erc20SendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20SendRequestRepository = erc20SendRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(sendRequest.projectId)
        )

        verify("ERC20 send request with failed status is returned") {
            assertThat(service.getErc20SendRequest(id)).withMessage()
                .isEqualTo(
                    sendRequest.withTransactionData(
                        status = Status.FAILED,
                        data = encodedData,
                        value = null,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20SendRequestWithFailedStatusWhenTransactionHasWrongTxHash() {
        val id = UUID.randomUUID()
        val sendRequest = Erc20SendRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = Chain.HARDHAT_TESTNET.id,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            tokenSenderAddress = WalletAddress("b"),
            tokenRecipientAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val erc20SendRequestRepository = mock<Erc20SendRequestRepository>()

        suppose("ERC20 send request exists in database") {
            given(erc20SendRequestRepository.getById(id))
                .willReturn(sendRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(sendRequest.chainId, null)
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TransactionHash("wrong-hash"),
            from = sendRequest.tokenSenderAddress!!,
            to = sendRequest.tokenAddress!!,
            data = encodedData,
            value = Balance(BigInteger.ZERO),
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true
        )

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "transfer",
                    arguments = listOf(
                        FunctionArgument(abiType = AbiType.Address, value = sendRequest.tokenRecipientAddress),
                        FunctionArgument(abiType = AbiType.Uint256, value = sendRequest.tokenAmount)
                    ),
                    abiOutputTypes = listOf(AbiType.Bool),
                    additionalData = listOf(Utf8String(id.toString()))
                )
            )
                .willReturn(encodedData)
        }

        val service = Erc20SendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20SendRequestRepository = erc20SendRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(sendRequest.projectId)
        )

        verify("ERC20 send request with failed status is returned") {
            assertThat(service.getErc20SendRequest(id)).withMessage()
                .isEqualTo(
                    sendRequest.withTransactionData(
                        status = Status.FAILED,
                        data = encodedData,
                        value = null,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20SendRequestWithFailedStatusWhenTransactionHasWrongFromAddress() {
        val id = UUID.randomUUID()
        val sendRequest = Erc20SendRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = Chain.HARDHAT_TESTNET.id,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            tokenSenderAddress = WalletAddress("b"),
            tokenRecipientAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val erc20SendRequestRepository = mock<Erc20SendRequestRepository>()

        suppose("ERC20 send request exists in database") {
            given(erc20SendRequestRepository.getById(id))
                .willReturn(sendRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(sendRequest.chainId, null)
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = WalletAddress("dead"),
            to = sendRequest.tokenAddress!!,
            data = encodedData,
            value = Balance(BigInteger.ZERO),
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true
        )

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "transfer",
                    arguments = listOf(
                        FunctionArgument(abiType = AbiType.Address, value = sendRequest.tokenRecipientAddress),
                        FunctionArgument(abiType = AbiType.Uint256, value = sendRequest.tokenAmount)
                    ),
                    abiOutputTypes = listOf(AbiType.Bool),
                    additionalData = listOf(Utf8String(id.toString()))
                )
            )
                .willReturn(encodedData)
        }

        val service = Erc20SendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20SendRequestRepository = erc20SendRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(sendRequest.projectId)
        )

        verify("ERC20 send request with failed status is returned") {
            assertThat(service.getErc20SendRequest(id)).withMessage()
                .isEqualTo(
                    sendRequest.withTransactionData(
                        status = Status.FAILED,
                        data = encodedData,
                        value = null,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20SendRequestWithFailedStatusWhenTransactionHasWrongData() {
        val id = UUID.randomUUID()
        val sendRequest = Erc20SendRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = Chain.HARDHAT_TESTNET.id,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            tokenSenderAddress = WalletAddress("b"),
            tokenRecipientAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val erc20SendRequestRepository = mock<Erc20SendRequestRepository>()

        suppose("ERC20 send request exists in database") {
            given(erc20SendRequestRepository.getById(id))
                .willReturn(sendRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(sendRequest.chainId, null)
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = sendRequest.tokenSenderAddress!!,
            to = sendRequest.tokenAddress!!,
            data = FunctionData("wrong-data"),
            value = Balance(BigInteger.ZERO),
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true
        )

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "transfer",
                    arguments = listOf(
                        FunctionArgument(abiType = AbiType.Address, value = sendRequest.tokenRecipientAddress),
                        FunctionArgument(abiType = AbiType.Uint256, value = sendRequest.tokenAmount)
                    ),
                    abiOutputTypes = listOf(AbiType.Bool),
                    additionalData = listOf(Utf8String(id.toString()))
                )
            )
                .willReturn(encodedData)
        }

        val service = Erc20SendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20SendRequestRepository = erc20SendRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(sendRequest.projectId)
        )

        verify("ERC20 send request with failed status is returned") {
            assertThat(service.getErc20SendRequest(id)).withMessage()
                .isEqualTo(
                    sendRequest.withTransactionData(
                        status = Status.FAILED,
                        data = encodedData,
                        value = null,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20SendRequestWithFailedStatusWhenTransactionHasWrongRecipientAddressForNativeToken() {
        val id = UUID.randomUUID()
        val sendRequest = Erc20SendRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = Chain.HARDHAT_TESTNET.id,
            redirectUrl = "test",
            tokenAddress = null,
            tokenAmount = Balance(BigInteger.TEN),
            tokenSenderAddress = WalletAddress("b"),
            tokenRecipientAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val erc20SendRequestRepository = mock<Erc20SendRequestRepository>()

        suppose("ERC20 send request exists in database") {
            given(erc20SendRequestRepository.getById(id))
                .willReturn(sendRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(sendRequest.chainId, null)
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = sendRequest.tokenSenderAddress!!,
            to = WalletAddress("dead"),
            data = FunctionData.EMPTY,
            value = sendRequest.tokenAmount,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true
        )

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH))
                .willReturn(transactionInfo)
        }

        val service = Erc20SendRequestServiceImpl(
            functionEncoderService = mock(),
            erc20SendRequestRepository = erc20SendRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(sendRequest.projectId)
        )

        verify("ERC20 send request with failed status is returned") {
            assertThat(service.getErc20SendRequest(id)).withMessage()
                .isEqualTo(
                    sendRequest.withTransactionData(
                        status = Status.FAILED,
                        data = null,
                        value = sendRequest.tokenAmount,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20SendRequestWithFailedStatusWhenTransactionHasWrongValueForNativeToken() {
        val id = UUID.randomUUID()
        val sendRequest = Erc20SendRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = Chain.HARDHAT_TESTNET.id,
            redirectUrl = "test",
            tokenAddress = null,
            tokenAmount = Balance(BigInteger.TEN),
            tokenSenderAddress = WalletAddress("b"),
            tokenRecipientAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val erc20SendRequestRepository = mock<Erc20SendRequestRepository>()

        suppose("ERC20 send request exists in database") {
            given(erc20SendRequestRepository.getById(id))
                .willReturn(sendRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(sendRequest.chainId, null)
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = sendRequest.tokenSenderAddress!!,
            to = sendRequest.tokenRecipientAddress,
            data = FunctionData.EMPTY,
            value = Balance(BigInteger.ONE),
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true
        )

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH))
                .willReturn(transactionInfo)
        }

        val service = Erc20SendRequestServiceImpl(
            functionEncoderService = mock(),
            erc20SendRequestRepository = erc20SendRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(sendRequest.projectId)
        )

        verify("ERC20 send request with failed status is returned") {
            assertThat(service.getErc20SendRequest(id)).withMessage()
                .isEqualTo(
                    sendRequest.withTransactionData(
                        status = Status.FAILED,
                        data = null,
                        value = sendRequest.tokenAmount,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20SendRequestWithSuccessfulStatusWhenFromAddressIsNull() {
        val id = UUID.randomUUID()
        val sendRequest = Erc20SendRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = Chain.HARDHAT_TESTNET.id,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            tokenSenderAddress = null,
            tokenRecipientAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val erc20SendRequestRepository = mock<Erc20SendRequestRepository>()

        suppose("ERC20 send request exists in database") {
            given(erc20SendRequestRepository.getById(id))
                .willReturn(sendRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(sendRequest.chainId, null)
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = WalletAddress("0cafe0babe"),
            to = sendRequest.tokenAddress!!,
            data = encodedData,
            value = Balance(BigInteger.ZERO),
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true
        )

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "transfer",
                    arguments = listOf(
                        FunctionArgument(abiType = AbiType.Address, value = sendRequest.tokenRecipientAddress),
                        FunctionArgument(abiType = AbiType.Uint256, value = sendRequest.tokenAmount)
                    ),
                    abiOutputTypes = listOf(AbiType.Bool),
                    additionalData = listOf(Utf8String(id.toString()))
                )
            )
                .willReturn(encodedData)
        }

        val service = Erc20SendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20SendRequestRepository = erc20SendRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(sendRequest.projectId)
        )

        verify("ERC20 send request with successful status is returned") {
            assertThat(service.getErc20SendRequest(id)).withMessage()
                .isEqualTo(
                    sendRequest.withTransactionData(
                        status = Status.SUCCESS,
                        data = encodedData,
                        value = null,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20SendRequestWithSuccessfulStatusWhenFromAddressIsSpecified() {
        val id = UUID.randomUUID()
        val sendRequest = Erc20SendRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = Chain.HARDHAT_TESTNET.id,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            tokenSenderAddress = WalletAddress("b"),
            tokenRecipientAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val erc20SendRequestRepository = mock<Erc20SendRequestRepository>()

        suppose("ERC20 send request exists in database") {
            given(erc20SendRequestRepository.getById(id))
                .willReturn(sendRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(sendRequest.chainId, null)
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = sendRequest.tokenSenderAddress!!,
            to = sendRequest.tokenAddress!!,
            data = encodedData,
            value = Balance(BigInteger.ZERO),
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true
        )

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "transfer",
                    arguments = listOf(
                        FunctionArgument(abiType = AbiType.Address, value = sendRequest.tokenRecipientAddress),
                        FunctionArgument(abiType = AbiType.Uint256, value = sendRequest.tokenAmount)
                    ),
                    abiOutputTypes = listOf(AbiType.Bool),
                    additionalData = listOf(Utf8String(id.toString()))
                )
            )
                .willReturn(encodedData)
        }

        val service = Erc20SendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20SendRequestRepository = erc20SendRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(sendRequest.projectId)
        )

        verify("ERC20 send request with successful status is returned") {
            assertThat(service.getErc20SendRequest(id)).withMessage()
                .isEqualTo(
                    sendRequest.withTransactionData(
                        status = Status.SUCCESS,
                        data = encodedData,
                        value = null,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20SendRequestWithSuccessfulStatusWhenFromAddressIsNullForNativeToken() {
        val id = UUID.randomUUID()
        val sendRequest = Erc20SendRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = Chain.HARDHAT_TESTNET.id,
            redirectUrl = "test",
            tokenAddress = null,
            tokenAmount = Balance(BigInteger.TEN),
            tokenSenderAddress = null,
            tokenRecipientAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val erc20SendRequestRepository = mock<Erc20SendRequestRepository>()

        suppose("ERC20 send request exists in database") {
            given(erc20SendRequestRepository.getById(id))
                .willReturn(sendRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(sendRequest.chainId, null)
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = WalletAddress("0cafe0babe"),
            to = sendRequest.tokenRecipientAddress,
            data = FunctionData.EMPTY,
            value = sendRequest.tokenAmount,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true
        )

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH))
                .willReturn(transactionInfo)
        }

        val service = Erc20SendRequestServiceImpl(
            functionEncoderService = mock(),
            erc20SendRequestRepository = erc20SendRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(sendRequest.projectId)
        )

        verify("ERC20 send request with successful status is returned") {
            assertThat(service.getErc20SendRequest(id)).withMessage()
                .isEqualTo(
                    sendRequest.withTransactionData(
                        status = Status.SUCCESS,
                        data = null,
                        value = sendRequest.tokenAmount,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20SendRequestWithSuccessfulStatusWhenFromAddressIsSpecifiedForNativeToken() {
        val id = UUID.randomUUID()
        val sendRequest = Erc20SendRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = Chain.HARDHAT_TESTNET.id,
            redirectUrl = "test",
            tokenAddress = null,
            tokenAmount = Balance(BigInteger.TEN),
            tokenSenderAddress = WalletAddress("b"),
            tokenRecipientAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val erc20SendRequestRepository = mock<Erc20SendRequestRepository>()

        suppose("ERC20 send request exists in database") {
            given(erc20SendRequestRepository.getById(id))
                .willReturn(sendRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(sendRequest.chainId, null)
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = sendRequest.tokenSenderAddress!!,
            to = sendRequest.tokenRecipientAddress,
            data = FunctionData.EMPTY,
            value = sendRequest.tokenAmount,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true
        )

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH))
                .willReturn(transactionInfo)
        }

        val service = Erc20SendRequestServiceImpl(
            functionEncoderService = mock(),
            erc20SendRequestRepository = erc20SendRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(sendRequest.projectId)
        )

        verify("ERC20 send request with successful status is returned") {
            assertThat(service.getErc20SendRequest(id)).withMessage()
                .isEqualTo(
                    sendRequest.withTransactionData(
                        status = Status.SUCCESS,
                        data = null,
                        value = sendRequest.tokenAmount,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyReturnListOfErc20SendRequestsByProjectId() {
        val id = UUID.randomUUID()
        val sendRequest = Erc20SendRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = Chain.HARDHAT_TESTNET.id,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            tokenSenderAddress = WalletAddress("b"),
            tokenRecipientAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val erc20SendRequestRepository = mock<Erc20SendRequestRepository>()

        suppose("ERC20 send request exists in database") {
            given(erc20SendRequestRepository.getAllByProjectId(PROJECT.id))
                .willReturn(listOf(sendRequest))
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(sendRequest.chainId, null)
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = sendRequest.tokenSenderAddress!!,
            to = sendRequest.tokenAddress!!,
            data = encodedData,
            value = Balance(BigInteger.ZERO),
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true
        )

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "transfer",
                    arguments = listOf(
                        FunctionArgument(abiType = AbiType.Address, value = sendRequest.tokenRecipientAddress),
                        FunctionArgument(abiType = AbiType.Uint256, value = sendRequest.tokenAmount)
                    ),
                    abiOutputTypes = listOf(AbiType.Bool),
                    additionalData = listOf(Utf8String(id.toString()))
                )
            )
                .willReturn(encodedData)
        }

        val service = Erc20SendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20SendRequestRepository = erc20SendRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(sendRequest.projectId)
        )

        verify("ERC20 send request with successful status is returned") {
            assertThat(service.getErc20SendRequestsByProjectId(PROJECT.id))
                .withMessage()
                .isEqualTo(
                    listOf(
                        sendRequest.withTransactionData(
                            status = Status.SUCCESS,
                            data = encodedData,
                            value = null,
                            transactionInfo = transactionInfo
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyReturnListOfErc20SendRequestsBySenderAddress() {
        val id = UUID.randomUUID()
        val sender = WalletAddress("b")
        val sendRequest = Erc20SendRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = Chain.HARDHAT_TESTNET.id,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            tokenSenderAddress = sender,
            tokenRecipientAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val erc20SendRequestRepository = mock<Erc20SendRequestRepository>()

        suppose("ERC20 send request exists in database") {
            given(erc20SendRequestRepository.getBySender(sender))
                .willReturn(listOf(sendRequest))
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(sendRequest.chainId, null)
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = sendRequest.tokenSenderAddress!!,
            to = sendRequest.tokenAddress!!,
            data = encodedData,
            value = Balance(BigInteger.ZERO),
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true
        )

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "transfer",
                    arguments = listOf(
                        FunctionArgument(abiType = AbiType.Address, value = sendRequest.tokenRecipientAddress),
                        FunctionArgument(abiType = AbiType.Uint256, value = sendRequest.tokenAmount)
                    ),
                    abiOutputTypes = listOf(AbiType.Bool),
                    additionalData = listOf(Utf8String(id.toString()))
                )
            )
                .willReturn(encodedData)
        }

        val service = Erc20SendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20SendRequestRepository = erc20SendRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(sendRequest.projectId)
        )

        verify("ERC20 send request with successful status is returned") {
            assertThat(service.getErc20SendRequestsBySender(sender)).withMessage()
                .isEqualTo(
                    listOf(
                        sendRequest.withTransactionData(
                            status = Status.SUCCESS,
                            data = encodedData,
                            value = null,
                            transactionInfo = transactionInfo
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyReturnListOfErc20SendRequestsByRecipientAddress() {
        val id = UUID.randomUUID()
        val recipient = WalletAddress("c")
        val sendRequest = Erc20SendRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = Chain.HARDHAT_TESTNET.id,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            tokenSenderAddress = WalletAddress("b"),
            tokenRecipientAddress = recipient,
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val erc20SendRequestRepository = mock<Erc20SendRequestRepository>()

        suppose("ERC20 send request exists in database") {
            given(erc20SendRequestRepository.getByRecipient(recipient))
                .willReturn(listOf(sendRequest))
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(sendRequest.chainId, null)
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = sendRequest.tokenSenderAddress!!,
            to = sendRequest.tokenAddress!!,
            data = encodedData,
            value = Balance(BigInteger.ZERO),
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true
        )

        suppose("transaction is mined") {
            given(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "transfer",
                    arguments = listOf(
                        FunctionArgument(abiType = AbiType.Address, value = sendRequest.tokenRecipientAddress),
                        FunctionArgument(abiType = AbiType.Uint256, value = sendRequest.tokenAmount)
                    ),
                    abiOutputTypes = listOf(AbiType.Bool),
                    additionalData = listOf(Utf8String(id.toString()))
                )
            )
                .willReturn(encodedData)
        }

        val service = Erc20SendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20SendRequestRepository = erc20SendRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(sendRequest.projectId)
        )

        verify("ERC20 send request with successful status is returned") {
            assertThat(service.getErc20SendRequestsByRecipient(recipient))
                .withMessage()
                .isEqualTo(
                    listOf(
                        sendRequest.withTransactionData(
                            status = Status.SUCCESS,
                            data = encodedData,
                            value = null,
                            transactionInfo = transactionInfo
                        )
                    )
                )
        }
    }

    @Test
    fun mustSuccessfullyAttachTxInfo() {
        val erc20SendRequestRepository = mock<Erc20SendRequestRepository>()
        val id = UUID.randomUUID()
        val caller = WalletAddress("0xbc25524e0daacB1F149BA55279f593F5E3FB73e9")

        suppose("txInfo will be successfully attached to the request") {
            given(erc20SendRequestRepository.setTxInfo(id, TX_HASH, caller))
                .willReturn(true)
        }

        val service = Erc20SendRequestServiceImpl(
            functionEncoderService = mock(),
            erc20SendRequestRepository = erc20SendRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("txInfo was successfully attached") {
            service.attachTxInfo(id, TX_HASH, caller)

            verifyMock(erc20SendRequestRepository)
                .setTxInfo(id, TX_HASH, caller)
            verifyNoMoreInteractions(erc20SendRequestRepository)
        }
    }

    @Test
    fun mustThrowCannotAttachTxInfoExceptionWhenAttachingTxInfoFails() {
        val erc20SendRequestRepository = mock<Erc20SendRequestRepository>()
        val id = UUID.randomUUID()
        val caller = WalletAddress("0xbc25524e0daacB1F149BA55279f593F5E3FB73e9")

        suppose("attaching txInfo will fail") {
            given(erc20SendRequestRepository.setTxInfo(id, TX_HASH, caller))
                .willReturn(false)
        }

        val service = Erc20SendRequestServiceImpl(
            functionEncoderService = mock(),
            erc20SendRequestRepository = erc20SendRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("CannotAttachTxInfoException is thrown") {
            assertThrows<CannotAttachTxInfoException>(message) {
                service.attachTxInfo(id, TX_HASH, caller)
            }

            verifyMock(erc20SendRequestRepository)
                .setTxInfo(id, TX_HASH, caller)
            verifyNoMoreInteractions(erc20SendRequestRepository)
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
}
