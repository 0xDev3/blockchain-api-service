package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.blockchain.BlockchainService
import com.ampnet.blockchainapiservice.blockchain.properties.Chain
import com.ampnet.blockchainapiservice.blockchain.properties.ChainSpec
import com.ampnet.blockchainapiservice.blockchain.properties.RpcUrlSpec
import com.ampnet.blockchainapiservice.exception.CannotAttachTxHashException
import com.ampnet.blockchainapiservice.exception.IncompleteRequestException
import com.ampnet.blockchainapiservice.exception.NonExistentClientIdException
import com.ampnet.blockchainapiservice.exception.ResourceNotFoundException
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.params.CreateErc20SendRequestParams
import com.ampnet.blockchainapiservice.model.params.StoreErc20SendRequestParams
import com.ampnet.blockchainapiservice.model.result.BlockchainTransactionInfo
import com.ampnet.blockchainapiservice.model.result.ClientInfo
import com.ampnet.blockchainapiservice.model.result.Erc20SendRequest
import com.ampnet.blockchainapiservice.repository.ClientInfoRepository
import com.ampnet.blockchainapiservice.repository.Erc20SendRequestRepository
import com.ampnet.blockchainapiservice.util.AbiType.AbiType
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.FunctionArgument
import com.ampnet.blockchainapiservice.util.FunctionData
import com.ampnet.blockchainapiservice.util.Status
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.ampnet.blockchainapiservice.util.WithFunctionData
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
        private const val CLIENT_ID = "client-id"
        private val CREATE_PARAMS = CreateErc20SendRequestParams(
            clientId = CLIENT_ID,
            chainId = ChainId(1337L),
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
    fun mustSuccessfullyCreateErc20SendRequestWhenClientIdIsProvided() {
        val uuidProvider = mock<UuidProvider>()
        val uuid = UUID.randomUUID()

        suppose("some UUID will be generated") {
            given(uuidProvider.getUuid())
                .willReturn(uuid)
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

        val chainId = ChainId(123456789L)
        val redirectUrl = "different-redirect-url/\${id}/rest"
        val tokenAddress = ContractAddress("cafebabe")
        val clientInfoRepository = mock<ClientInfoRepository>()

        suppose("client info will be fetched from database") {
            given(clientInfoRepository.getById(CLIENT_ID))
                .willReturn(
                    ClientInfo(
                        clientId = CLIENT_ID,
                        chainId = chainId,
                        sendRedirectUrl = redirectUrl,
                        balanceRedirectUrl = null,
                        lockRedirectUrl = null,
                        tokenAddress = tokenAddress
                    )
                )
        }

        val erc20SendRequestRepository = mock<Erc20SendRequestRepository>()

        val storeParams = StoreErc20SendRequestParams(
            id = uuid,
            chainId = chainId,
            redirectUrl = redirectUrl.replace("\${id}", uuid.toString()),
            tokenAddress = tokenAddress,
            tokenAmount = CREATE_PARAMS.tokenAmount,
            tokenSenderAddress = CREATE_PARAMS.tokenSenderAddress,
            tokenRecipientAddress = CREATE_PARAMS.tokenRecipientAddress,
            arbitraryData = CREATE_PARAMS.arbitraryData,
            screenConfig = CREATE_PARAMS.screenConfig
        )

        val storedRequest = Erc20SendRequest(
            id = uuid,
            chainId = chainId,
            redirectUrl = storeParams.redirectUrl,
            tokenAddress = tokenAddress,
            tokenAmount = CREATE_PARAMS.tokenAmount,
            tokenSenderAddress = CREATE_PARAMS.tokenSenderAddress,
            tokenRecipientAddress = CREATE_PARAMS.tokenRecipientAddress,
            txHash = null,
            arbitraryData = CREATE_PARAMS.arbitraryData,
            screenConfig = CREATE_PARAMS.screenConfig
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
                clientInfoRepository = clientInfoRepository,
                blockchainService = mock()
            )
        )

        val createParams = CREATE_PARAMS.copy(
            chainId = null,
            redirectUrl = null,
            tokenAddress = null
        )

        verify("ERC20 send request is correctly created") {
            assertThat(service.createErc20SendRequest(createParams)).withMessage()
                .isEqualTo(
                    WithFunctionData(
                        storedRequest.copy(redirectUrl = storedRequest.redirectUrl.replace("\${id}", uuid.toString())),
                        encodedData
                    )
                )

            verifyMock(erc20SendRequestRepository)
                .store(storeParams)
            verifyNoMoreInteractions(erc20SendRequestRepository)
        }
    }

    @Test
    fun mustSuccessfullyCreateErc20SendRequestWhenClientIdIsNotProvided() {
        val uuidProvider = mock<UuidProvider>()
        val uuid = UUID.randomUUID()

        suppose("some UUID will be generated") {
            given(uuidProvider.getUuid())
                .willReturn(uuid)
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
        val chainId = CREATE_PARAMS.chainId!!
        val redirectUrl = CREATE_PARAMS.redirectUrl!!

        val storeParams = StoreErc20SendRequestParams(
            id = uuid,
            chainId = chainId,
            redirectUrl = redirectUrl.replace("\${id}", uuid.toString()),
            tokenAddress = CREATE_PARAMS.tokenAddress!!,
            tokenAmount = CREATE_PARAMS.tokenAmount,
            tokenSenderAddress = CREATE_PARAMS.tokenSenderAddress,
            tokenRecipientAddress = CREATE_PARAMS.tokenRecipientAddress,
            arbitraryData = CREATE_PARAMS.arbitraryData,
            screenConfig = CREATE_PARAMS.screenConfig
        )

        val storedRequest = Erc20SendRequest(
            id = uuid,
            chainId = chainId,
            redirectUrl = storeParams.redirectUrl,
            tokenAddress = CREATE_PARAMS.tokenAddress!!,
            tokenAmount = CREATE_PARAMS.tokenAmount,
            tokenSenderAddress = CREATE_PARAMS.tokenSenderAddress,
            tokenRecipientAddress = CREATE_PARAMS.tokenRecipientAddress,
            txHash = null,
            arbitraryData = CREATE_PARAMS.arbitraryData,
            screenConfig = CREATE_PARAMS.screenConfig
        )

        suppose("ERC20 send request is stored in database") {
            given(erc20SendRequestRepository.store(storeParams))
                .willReturn(storedRequest)
        }

        val createParams = suppose("clientId is missing from params") {
            CREATE_PARAMS.copy(clientId = null)
        }

        val service = Erc20SendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20SendRequestRepository = erc20SendRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = uuidProvider,
                clientInfoRepository = mock(),
                blockchainService = mock()
            )
        )

        verify("ERC20 send request is correctly created") {
            assertThat(service.createErc20SendRequest(createParams)).withMessage()
                .isEqualTo(WithFunctionData(storedRequest, encodedData))

            verifyMock(erc20SendRequestRepository)
                .store(storeParams)
            verifyNoMoreInteractions(erc20SendRequestRepository)
        }
    }

    @Test
    fun mustThrowNonExistentClientIdExceptionWhenClientInfoIsNotInDatabase() {
        val clientInfoRepository = mock<ClientInfoRepository>()

        suppose("client info does not exist in database") {
            given(clientInfoRepository.getById(CLIENT_ID))
                .willReturn(null)
        }

        val service = Erc20SendRequestServiceImpl(
            functionEncoderService = mock(),
            erc20SendRequestRepository = mock(),
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = mock(),
                clientInfoRepository = clientInfoRepository,
                blockchainService = mock()
            )
        )

        verify("NonExistentClientIdException is thrown") {
            assertThrows<NonExistentClientIdException> {
                service.createErc20SendRequest(CREATE_PARAMS)
            }
        }
    }

    @Test
    fun mustThrowIncompleteRequestExceptionWhenClientIdAndChainIdAreMissing() {
        val params = suppose("clientId and chainId are missing from params") {
            CREATE_PARAMS.copy(clientId = null, chainId = null)
        }

        val uuidProvider = mock<UuidProvider>()
        val uuid = UUID.randomUUID()

        suppose("some UUID will be generated") {
            given(uuidProvider.getUuid())
                .willReturn(uuid)
        }

        val service = Erc20SendRequestServiceImpl(
            functionEncoderService = mock(),
            erc20SendRequestRepository = mock(),
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = uuidProvider,
                clientInfoRepository = mock(),
                blockchainService = mock()
            )
        )

        verify("IncompleteRequestException is thrown") {
            assertThrows<IncompleteRequestException> {
                service.createErc20SendRequest(params)
            }
        }
    }

    @Test
    fun mustThrowIncompleteRequestExceptionWhenClientIdAndRedirectUrlAreMissing() {
        val params = suppose("clientId and redirectUrl are missing from params") {
            CREATE_PARAMS.copy(clientId = null, redirectUrl = null)
        }

        val uuidProvider = mock<UuidProvider>()
        val uuid = UUID.randomUUID()

        suppose("some UUID will be generated") {
            given(uuidProvider.getUuid())
                .willReturn(uuid)
        }

        val service = Erc20SendRequestServiceImpl(
            functionEncoderService = mock(),
            erc20SendRequestRepository = mock(),
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = uuidProvider,
                clientInfoRepository = mock(),
                blockchainService = mock()
            )
        )

        verify("IncompleteRequestException is thrown") {
            assertThrows<IncompleteRequestException> {
                service.createErc20SendRequest(params)
            }
        }
    }

    @Test
    fun mustThrowIncompleteRequestExceptionWhenClientIdAndTokenAddressAreMissing() {
        val params = suppose("clientId and tokenAddress are missing from params") {
            CREATE_PARAMS.copy(clientId = null, tokenAddress = null)
        }

        val uuidProvider = mock<UuidProvider>()
        val uuid = UUID.randomUUID()

        suppose("some UUID will be generated") {
            given(uuidProvider.getUuid())
                .willReturn(uuid)
        }

        val service = Erc20SendRequestServiceImpl(
            functionEncoderService = mock(),
            erc20SendRequestRepository = mock(),
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = uuidProvider,
                clientInfoRepository = mock(),
                blockchainService = mock()
            )
        )

        verify("IncompleteRequestException is thrown") {
            assertThrows<IncompleteRequestException> {
                service.createErc20SendRequest(params)
            }
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
                clientInfoRepository = mock(),
                blockchainService = mock()
            )
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.getErc20SendRequest(id = UUID.randomUUID(), rpcSpec = RpcUrlSpec(null, null))
            }
        }
    }

    @Test
    fun mustReturnErc20SendRequestWithPendingStatusWhenErc20SendRequestHasNullTxHash() {
        val id = UUID.randomUUID()
        val sendRequest = Erc20SendRequest(
            id = id,
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
            )
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
                clientInfoRepository = mock(),
                blockchainService = mock()
            )
        )

        verify("ERC20 send request with pending status is returned") {
            assertThat(service.getErc20SendRequest(id = id, rpcSpec = RpcUrlSpec(null, null))).withMessage()
                .isEqualTo(
                    sendRequest.withTransactionData(
                        status = Status.PENDING,
                        data = encodedData,
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
            )
        )
        val erc20SendRequestRepository = mock<Erc20SendRequestRepository>()

        suppose("ERC20 send request exists in database") {
            given(erc20SendRequestRepository.getById(id))
                .willReturn(sendRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(sendRequest.chainId, RpcUrlSpec("url", "url-override"))

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
                clientInfoRepository = mock(),
                blockchainService = mock()
            )
        )

        verify("ERC20 send request with pending status is returned") {
            assertThat(service.getErc20SendRequest(id = id, rpcSpec = chainSpec.rpcSpec)).withMessage()
                .isEqualTo(
                    sendRequest.withTransactionData(
                        status = Status.PENDING,
                        data = encodedData,
                        transactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20SendRequestWithFailedStatusWhenTransactionHasWrongToAddress() {
        val id = UUID.randomUUID()
        val sendRequest = Erc20SendRequest(
            id = id,
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
            )
        )
        val erc20SendRequestRepository = mock<Erc20SendRequestRepository>()

        suppose("ERC20 send request exists in database") {
            given(erc20SendRequestRepository.getById(id))
                .willReturn(sendRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(sendRequest.chainId, RpcUrlSpec("url", "url-override"))
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = sendRequest.tokenSenderAddress!!,
            to = WalletAddress("dead"),
            data = encodedData,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP
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
                clientInfoRepository = mock(),
                blockchainService = blockchainService
            )
        )

        verify("ERC20 send request with failed status is returned") {
            assertThat(service.getErc20SendRequest(id = id, rpcSpec = chainSpec.rpcSpec)).withMessage()
                .isEqualTo(
                    sendRequest.withTransactionData(
                        status = Status.FAILED,
                        data = encodedData,
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
            )
        )
        val erc20SendRequestRepository = mock<Erc20SendRequestRepository>()

        suppose("ERC20 send request exists in database") {
            given(erc20SendRequestRepository.getById(id))
                .willReturn(sendRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(sendRequest.chainId, RpcUrlSpec("url", "url-override"))
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TransactionHash("wrong-hash"),
            from = sendRequest.tokenSenderAddress!!,
            to = sendRequest.tokenAddress.toWalletAddress(),
            data = encodedData,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP
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
                clientInfoRepository = mock(),
                blockchainService = blockchainService
            )
        )

        verify("ERC20 send request with failed status is returned") {
            assertThat(service.getErc20SendRequest(id = id, rpcSpec = chainSpec.rpcSpec)).withMessage()
                .isEqualTo(
                    sendRequest.withTransactionData(
                        status = Status.FAILED,
                        data = encodedData,
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
            )
        )
        val erc20SendRequestRepository = mock<Erc20SendRequestRepository>()

        suppose("ERC20 send request exists in database") {
            given(erc20SendRequestRepository.getById(id))
                .willReturn(sendRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(sendRequest.chainId, RpcUrlSpec("url", "url-override"))
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = WalletAddress("dead"),
            to = sendRequest.tokenAddress.toWalletAddress(),
            data = encodedData,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP
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
                clientInfoRepository = mock(),
                blockchainService = blockchainService
            )
        )

        verify("ERC20 send request with failed status is returned") {
            assertThat(service.getErc20SendRequest(id = id, rpcSpec = chainSpec.rpcSpec)).withMessage()
                .isEqualTo(
                    sendRequest.withTransactionData(
                        status = Status.FAILED,
                        data = encodedData,
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
            )
        )
        val erc20SendRequestRepository = mock<Erc20SendRequestRepository>()

        suppose("ERC20 send request exists in database") {
            given(erc20SendRequestRepository.getById(id))
                .willReturn(sendRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(sendRequest.chainId, RpcUrlSpec("url", "url-override"))
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = sendRequest.tokenSenderAddress!!,
            to = sendRequest.tokenAddress.toWalletAddress(),
            data = FunctionData("wrong-data"),
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP
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
                clientInfoRepository = mock(),
                blockchainService = blockchainService
            )
        )

        verify("ERC20 send request with failed status is returned") {
            assertThat(service.getErc20SendRequest(id = id, rpcSpec = chainSpec.rpcSpec)).withMessage()
                .isEqualTo(
                    sendRequest.withTransactionData(
                        status = Status.FAILED,
                        data = encodedData,
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
            )
        )
        val erc20SendRequestRepository = mock<Erc20SendRequestRepository>()

        suppose("ERC20 send request exists in database") {
            given(erc20SendRequestRepository.getById(id))
                .willReturn(sendRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(sendRequest.chainId, RpcUrlSpec("url", "url-override"))
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = WalletAddress("0cafe0babe"),
            to = sendRequest.tokenAddress.toWalletAddress(),
            data = encodedData,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP
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
                clientInfoRepository = mock(),
                blockchainService = blockchainService
            )
        )

        verify("ERC20 send request with successful status is returned") {
            assertThat(service.getErc20SendRequest(id = id, rpcSpec = chainSpec.rpcSpec)).withMessage()
                .isEqualTo(
                    sendRequest.withTransactionData(
                        status = Status.SUCCESS,
                        data = encodedData,
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
            )
        )
        val erc20SendRequestRepository = mock<Erc20SendRequestRepository>()

        suppose("ERC20 send request exists in database") {
            given(erc20SendRequestRepository.getById(id))
                .willReturn(sendRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(sendRequest.chainId, RpcUrlSpec("url", "url-override"))
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = sendRequest.tokenSenderAddress!!,
            to = sendRequest.tokenAddress.toWalletAddress(),
            data = encodedData,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP
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
                clientInfoRepository = mock(),
                blockchainService = blockchainService
            )
        )

        verify("ERC20 send request with successful status is returned") {
            assertThat(service.getErc20SendRequest(id = id, rpcSpec = chainSpec.rpcSpec)).withMessage()
                .isEqualTo(
                    sendRequest.withTransactionData(
                        status = Status.SUCCESS,
                        data = encodedData,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustSuccessfullyAttachTxHash() {
        val erc20SendRequestRepository = mock<Erc20SendRequestRepository>()
        val id = UUID.randomUUID()

        suppose("txHash will be successfully attached to the request") {
            given(erc20SendRequestRepository.setTxHash(id, TX_HASH))
                .willReturn(true)
        }

        val service = Erc20SendRequestServiceImpl(
            functionEncoderService = mock(),
            erc20SendRequestRepository = erc20SendRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = mock(),
                clientInfoRepository = mock(),
                blockchainService = mock()
            )
        )

        verify("txHash was successfully attached") {
            service.attachTxHash(id, TX_HASH)

            verifyMock(erc20SendRequestRepository)
                .setTxHash(id, TX_HASH)
            verifyNoMoreInteractions(erc20SendRequestRepository)
        }
    }

    @Test
    fun mustThrowCannotAttachTxHashExceptionWhenAttachingTxHashFails() {
        val erc20SendRequestRepository = mock<Erc20SendRequestRepository>()
        val id = UUID.randomUUID()

        suppose("attaching txHash will fails") {
            given(erc20SendRequestRepository.setTxHash(id, TX_HASH))
                .willReturn(false)
        }

        val service = Erc20SendRequestServiceImpl(
            functionEncoderService = mock(),
            erc20SendRequestRepository = erc20SendRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = mock(),
                clientInfoRepository = mock(),
                blockchainService = mock()
            )
        )

        verify("CannotAttachTxHashException is thrown") {
            assertThrows<CannotAttachTxHashException>(message) {
                service.attachTxHash(id, TX_HASH)
            }

            verifyMock(erc20SendRequestRepository)
                .setTxHash(id, TX_HASH)
            verifyNoMoreInteractions(erc20SendRequestRepository)
        }
    }
}
