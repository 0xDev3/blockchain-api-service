package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.blockchain.BlockchainService
import com.ampnet.blockchainapiservice.blockchain.properties.Chain
import com.ampnet.blockchainapiservice.blockchain.properties.ChainSpec
import com.ampnet.blockchainapiservice.blockchain.properties.RpcUrlSpec
import com.ampnet.blockchainapiservice.exception.CannotAttachTxHashException
import com.ampnet.blockchainapiservice.exception.IncompleteSendErc20RequestException
import com.ampnet.blockchainapiservice.exception.NonExistentClientIdException
import com.ampnet.blockchainapiservice.exception.ResourceNotFoundException
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.params.CreateSendErc20RequestParams
import com.ampnet.blockchainapiservice.model.params.StoreSendErc20RequestParams
import com.ampnet.blockchainapiservice.model.result.BlockchainTransactionInfo
import com.ampnet.blockchainapiservice.model.result.ClientInfo
import com.ampnet.blockchainapiservice.model.result.FullSendErc20Request
import com.ampnet.blockchainapiservice.model.result.SendErc20Request
import com.ampnet.blockchainapiservice.repository.ClientInfoRepository
import com.ampnet.blockchainapiservice.repository.SendErc20RequestRepository
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

class SendErc20RequestServiceTest : TestBase() {

    companion object {
        private const val CLIENT_ID = "client-id"
        private val CREATE_PARAMS = CreateSendErc20RequestParams(
            clientId = CLIENT_ID,
            chainId = ChainId(1337L),
            redirectUrl = "redirect-url/\${id}",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.valueOf(123456L)),
            tokenSenderAddress = WalletAddress("b"),
            tokenRecipientAddress = WalletAddress("c"),
            arbitraryData = null,
            screenConfig = ScreenConfig(
                title = "title",
                message = "message",
                logo = "logo"
            )
        )
        private val TX_HASH = TransactionHash("tx-hash")
    }

    @Test
    fun mustSuccessfullyCreateSendErc20RequestWhenClientIdIsProvided() {
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
                        FunctionArgument(abiType = "address", value = CREATE_PARAMS.tokenRecipientAddress.rawValue),
                        FunctionArgument(abiType = "uint256", value = CREATE_PARAMS.tokenAmount.rawValue)
                    ),
                    abiOutputTypes = listOf("bool"),
                    additionalData = listOf(Utf8String(uuid.toString()))
                )
            )
                .willReturn(encodedData)
        }

        val chainId = ChainId(123456789L)
        val redirectUrl = "different-redirect-url/\${id}/rest"
        val clientInfoRepository = mock<ClientInfoRepository>()

        suppose("client info will be fetched from database") {
            given(clientInfoRepository.getById(CLIENT_ID))
                .willReturn(
                    ClientInfo(
                        clientId = CLIENT_ID,
                        chainId = chainId,
                        sendRedirectUrl = redirectUrl,
                        balanceRedirectUrl = null, // TODO SD-769 test more cases
                        tokenAddress = null
                    )
                )
        }

        val sendErc20RequestRepository = mock<SendErc20RequestRepository>()

        val storeParams = StoreSendErc20RequestParams(
            id = uuid,
            chainId = chainId,
            redirectUrl = redirectUrl.replace("\${id}", uuid.toString()),
            tokenAddress = CREATE_PARAMS.tokenAddress,
            tokenAmount = CREATE_PARAMS.tokenAmount,
            tokenSenderAddress = CREATE_PARAMS.tokenSenderAddress,
            tokenRecipientAddress = CREATE_PARAMS.tokenRecipientAddress,
            arbitraryData = CREATE_PARAMS.arbitraryData,
            screenConfig = CREATE_PARAMS.screenConfig
        )

        val storedRequest = SendErc20Request(
            id = uuid,
            chainId = chainId,
            redirectUrl = storeParams.redirectUrl,
            tokenAddress = CREATE_PARAMS.tokenAddress,
            tokenAmount = CREATE_PARAMS.tokenAmount,
            tokenSenderAddress = CREATE_PARAMS.tokenSenderAddress,
            tokenRecipientAddress = CREATE_PARAMS.tokenRecipientAddress,
            txHash = null,
            arbitraryData = CREATE_PARAMS.arbitraryData,
            screenConfig = CREATE_PARAMS.screenConfig
        )

        suppose("send ERC20 request is stored in database") {
            given(sendErc20RequestRepository.store(storeParams))
                .willReturn(storedRequest)
        }

        val service = SendErc20RequestServiceImpl(
            uuidProvider = uuidProvider,
            functionEncoderService = functionEncoderService,
            blockchainService = mock(),
            clientInfoRepository = clientInfoRepository,
            sendErc20RequestRepository = sendErc20RequestRepository
        )

        verify("send ERC20 request is correctly created") {
            assertThat(service.createSendErc20Request(CREATE_PARAMS)).withMessage()
                .isEqualTo(
                    WithFunctionData(
                        storedRequest.copy(redirectUrl = storedRequest.redirectUrl.replace("\${id}", uuid.toString())),
                        encodedData
                    )
                )

            verifyMock(sendErc20RequestRepository)
                .store(storeParams)
            verifyNoMoreInteractions(sendErc20RequestRepository)
        }
    }

    @Test
    fun mustSuccessfullyCreateSendErc20RequestWhenClientIdIsNotProvided() {
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
                        FunctionArgument(abiType = "address", value = CREATE_PARAMS.tokenRecipientAddress.rawValue),
                        FunctionArgument(abiType = "uint256", value = CREATE_PARAMS.tokenAmount.rawValue)
                    ),
                    abiOutputTypes = listOf("bool"),
                    additionalData = listOf(Utf8String(uuid.toString()))
                )
            )
                .willReturn(encodedData)
        }

        val sendErc20RequestRepository = mock<SendErc20RequestRepository>()
        val chainId = CREATE_PARAMS.chainId!!
        val redirectUrl = CREATE_PARAMS.redirectUrl!!

        val storeParams = StoreSendErc20RequestParams(
            id = uuid,
            chainId = chainId,
            redirectUrl = redirectUrl.replace("\${id}", uuid.toString()),
            tokenAddress = CREATE_PARAMS.tokenAddress,
            tokenAmount = CREATE_PARAMS.tokenAmount,
            tokenSenderAddress = CREATE_PARAMS.tokenSenderAddress,
            tokenRecipientAddress = CREATE_PARAMS.tokenRecipientAddress,
            arbitraryData = CREATE_PARAMS.arbitraryData,
            screenConfig = CREATE_PARAMS.screenConfig
        )

        val storedRequest = SendErc20Request(
            id = uuid,
            chainId = chainId,
            redirectUrl = storeParams.redirectUrl,
            tokenAddress = CREATE_PARAMS.tokenAddress,
            tokenAmount = CREATE_PARAMS.tokenAmount,
            tokenSenderAddress = CREATE_PARAMS.tokenSenderAddress,
            tokenRecipientAddress = CREATE_PARAMS.tokenRecipientAddress,
            txHash = null,
            arbitraryData = CREATE_PARAMS.arbitraryData,
            screenConfig = CREATE_PARAMS.screenConfig
        )

        suppose("send ERC20 request is stored in database") {
            given(sendErc20RequestRepository.store(storeParams))
                .willReturn(storedRequest)
        }

        val createParams = suppose("clientId is missing from params") {
            CREATE_PARAMS.copy(clientId = null)
        }

        val service = SendErc20RequestServiceImpl(
            uuidProvider = uuidProvider,
            functionEncoderService = functionEncoderService,
            blockchainService = mock(),
            clientInfoRepository = mock(),
            sendErc20RequestRepository = sendErc20RequestRepository
        )

        verify("send ERC20 request is correctly created") {
            assertThat(service.createSendErc20Request(createParams)).withMessage()
                .isEqualTo(WithFunctionData(storedRequest, encodedData))

            verifyMock(sendErc20RequestRepository)
                .store(storeParams)
            verifyNoMoreInteractions(sendErc20RequestRepository)
        }
    }

    @Test
    fun mustThrowNonExistentClientIdExceptionWhenClientInfoIsNotInDatabase() {
        val clientInfoRepository = mock<ClientInfoRepository>()

        suppose("client info does not exist in database") {
            given(clientInfoRepository.getById(CLIENT_ID))
                .willReturn(null)
        }

        val service = SendErc20RequestServiceImpl(
            uuidProvider = mock(),
            functionEncoderService = mock(),
            blockchainService = mock(),
            clientInfoRepository = clientInfoRepository,
            sendErc20RequestRepository = mock()
        )

        verify("NonExistentClientIdException is thrown") {
            assertThrows<NonExistentClientIdException> {
                service.createSendErc20Request(CREATE_PARAMS)
            }
        }
    }

    @Test
    fun mustThrowIncompleteSendErc20RequestExceptionWhenClientIdAndChainIdAreMissing() {
        val params = suppose("clientId and chainId are missing from params") {
            CREATE_PARAMS.copy(clientId = null, chainId = null)
        }

        val service = SendErc20RequestServiceImpl(
            uuidProvider = mock(),
            functionEncoderService = mock(),
            blockchainService = mock(),
            clientInfoRepository = mock(),
            sendErc20RequestRepository = mock()
        )

        verify("IncompleteSendErc20RequestException is thrown") {
            assertThrows<IncompleteSendErc20RequestException> {
                service.createSendErc20Request(params)
            }
        }
    }

    @Test
    fun mustThrowIncompleteSendErc20RequestExceptionWhenClientIdAndRedirectUrlAreMissing() {
        val params = suppose("clientId and redirectUrl are missing from params") {
            CREATE_PARAMS.copy(clientId = null, redirectUrl = null)
        }

        val service = SendErc20RequestServiceImpl(
            uuidProvider = mock(),
            functionEncoderService = mock(),
            blockchainService = mock(),
            clientInfoRepository = mock(),
            sendErc20RequestRepository = mock()
        )

        verify("IncompleteSendErc20RequestException is thrown") {
            assertThrows<IncompleteSendErc20RequestException> {
                service.createSendErc20Request(params)
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionForNonExistentSendErc20Request() {
        val sendErc20RequestRepository = mock<SendErc20RequestRepository>()

        suppose("send ERC20 request does not exist in database") {
            given(sendErc20RequestRepository.getById(any()))
                .willReturn(null)
        }

        val service = SendErc20RequestServiceImpl(
            uuidProvider = mock(),
            functionEncoderService = mock(),
            blockchainService = mock(),
            clientInfoRepository = mock(),
            sendErc20RequestRepository = sendErc20RequestRepository
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.getSendErc20Request(id = UUID.randomUUID(), rpcSpec = RpcUrlSpec(null, null))
            }
        }
    }

    @Test
    fun mustReturnSendErc20RequestWithPendingStatusWhenSendErc20RequestHasNullTxHash() {
        val id = UUID.randomUUID()
        val sendRequest = SendErc20Request(
            id = id,
            chainId = Chain.HARDHAT_TESTNET.id,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            tokenSenderAddress = WalletAddress("b"),
            tokenRecipientAddress = WalletAddress("c"),
            txHash = null,
            arbitraryData = null,
            screenConfig = ScreenConfig(
                title = "title",
                message = "message",
                logo = "logo"
            )
        )
        val sendErc20RequestRepository = mock<SendErc20RequestRepository>()

        suppose("send ERC20 request exists in database") {
            given(sendErc20RequestRepository.getById(id))
                .willReturn(sendRequest)
        }

        val functionEncoderService = mock<FunctionEncoderService>()
        val encodedData = FunctionData("encoded")

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "transfer",
                    arguments = listOf(
                        FunctionArgument(abiType = "address", value = sendRequest.tokenRecipientAddress.rawValue),
                        FunctionArgument(abiType = "uint256", value = sendRequest.tokenAmount.rawValue)
                    ),
                    abiOutputTypes = listOf("bool"),
                    additionalData = listOf(Utf8String(id.toString()))
                )
            )
                .willReturn(encodedData)
        }

        val service = SendErc20RequestServiceImpl(
            uuidProvider = mock(),
            functionEncoderService = functionEncoderService,
            blockchainService = mock(),
            clientInfoRepository = mock(),
            sendErc20RequestRepository = sendErc20RequestRepository
        )

        verify("send ERC20 request with pending status is returned") {
            assertThat(service.getSendErc20Request(id = id, rpcSpec = RpcUrlSpec(null, null))).withMessage()
                .isEqualTo(
                    FullSendErc20Request.fromSendErc20Request(
                        request = sendRequest,
                        status = Status.PENDING,
                        data = encodedData,
                        transactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnSendErc20RequestWithPendingStatusWhenTransactionIsNotYetMined() {
        val id = UUID.randomUUID()
        val sendRequest = SendErc20Request(
            id = id,
            chainId = Chain.HARDHAT_TESTNET.id,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            tokenSenderAddress = WalletAddress("b"),
            tokenRecipientAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = null,
            screenConfig = ScreenConfig(
                title = "title",
                message = "message",
                logo = "logo"
            )
        )
        val sendErc20RequestRepository = mock<SendErc20RequestRepository>()

        suppose("send ERC20 request exists in database") {
            given(sendErc20RequestRepository.getById(id))
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
                        FunctionArgument(abiType = "address", value = sendRequest.tokenRecipientAddress.rawValue),
                        FunctionArgument(abiType = "uint256", value = sendRequest.tokenAmount.rawValue)
                    ),
                    abiOutputTypes = listOf("bool"),
                    additionalData = listOf(Utf8String(id.toString()))
                )
            )
                .willReturn(encodedData)
        }

        val service = SendErc20RequestServiceImpl(
            uuidProvider = mock(),
            functionEncoderService = functionEncoderService,
            blockchainService = mock(),
            clientInfoRepository = mock(),
            sendErc20RequestRepository = sendErc20RequestRepository
        )

        verify("send ERC20 request with pending status is returned") {
            assertThat(service.getSendErc20Request(id = id, rpcSpec = chainSpec.rpcSpec)).withMessage()
                .isEqualTo(
                    FullSendErc20Request.fromSendErc20Request(
                        request = sendRequest,
                        status = Status.PENDING,
                        data = encodedData,
                        transactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnSendErc20RequestWithFailedStatusWhenTransactionHasWrongToAddress() {
        val id = UUID.randomUUID()
        val sendRequest = SendErc20Request(
            id = id,
            chainId = Chain.HARDHAT_TESTNET.id,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            tokenSenderAddress = WalletAddress("b"),
            tokenRecipientAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = null,
            screenConfig = ScreenConfig(
                title = "title",
                message = "message",
                logo = "logo"
            )
        )
        val sendErc20RequestRepository = mock<SendErc20RequestRepository>()

        suppose("send ERC20 request exists in database") {
            given(sendErc20RequestRepository.getById(id))
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
            blockConfirmations = BigInteger.ONE
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
                        FunctionArgument(abiType = "address", value = sendRequest.tokenRecipientAddress.rawValue),
                        FunctionArgument(abiType = "uint256", value = sendRequest.tokenAmount.rawValue)
                    ),
                    abiOutputTypes = listOf("bool"),
                    additionalData = listOf(Utf8String(id.toString()))
                )
            )
                .willReturn(encodedData)
        }

        val service = SendErc20RequestServiceImpl(
            uuidProvider = mock(),
            functionEncoderService = functionEncoderService,
            blockchainService = blockchainService,
            clientInfoRepository = mock(),
            sendErc20RequestRepository = sendErc20RequestRepository
        )

        verify("send ERC20 request with failed status is returned") {
            assertThat(service.getSendErc20Request(id = id, rpcSpec = chainSpec.rpcSpec)).withMessage()
                .isEqualTo(
                    FullSendErc20Request.fromSendErc20Request(
                        request = sendRequest,
                        status = Status.FAILED,
                        data = encodedData,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnSendErc20RequestWithFailedStatusWhenTransactionHasWrongTxHash() {
        val id = UUID.randomUUID()
        val sendRequest = SendErc20Request(
            id = id,
            chainId = Chain.HARDHAT_TESTNET.id,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            tokenSenderAddress = WalletAddress("b"),
            tokenRecipientAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = null,
            screenConfig = ScreenConfig(
                title = "title",
                message = "message",
                logo = "logo"
            )
        )
        val sendErc20RequestRepository = mock<SendErc20RequestRepository>()

        suppose("send ERC20 request exists in database") {
            given(sendErc20RequestRepository.getById(id))
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
            blockConfirmations = BigInteger.ONE
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
                        FunctionArgument(abiType = "address", value = sendRequest.tokenRecipientAddress.rawValue),
                        FunctionArgument(abiType = "uint256", value = sendRequest.tokenAmount.rawValue)
                    ),
                    abiOutputTypes = listOf("bool"),
                    additionalData = listOf(Utf8String(id.toString()))
                )
            )
                .willReturn(encodedData)
        }

        val service = SendErc20RequestServiceImpl(
            uuidProvider = mock(),
            functionEncoderService = functionEncoderService,
            blockchainService = blockchainService,
            clientInfoRepository = mock(),
            sendErc20RequestRepository = sendErc20RequestRepository
        )

        verify("send ERC20 request with failed status is returned") {
            assertThat(service.getSendErc20Request(id = id, rpcSpec = chainSpec.rpcSpec)).withMessage()
                .isEqualTo(
                    FullSendErc20Request.fromSendErc20Request(
                        request = sendRequest,
                        status = Status.FAILED,
                        data = encodedData,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnSendErc20RequestWithFailedStatusWhenTransactionHasWrongFromAddress() {
        val id = UUID.randomUUID()
        val sendRequest = SendErc20Request(
            id = id,
            chainId = Chain.HARDHAT_TESTNET.id,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            tokenSenderAddress = WalletAddress("b"),
            tokenRecipientAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = null,
            screenConfig = ScreenConfig(
                title = "title",
                message = "message",
                logo = "logo"
            )
        )
        val sendErc20RequestRepository = mock<SendErc20RequestRepository>()

        suppose("send ERC20 request exists in database") {
            given(sendErc20RequestRepository.getById(id))
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
            blockConfirmations = BigInteger.ONE
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
                        FunctionArgument(abiType = "address", value = sendRequest.tokenRecipientAddress.rawValue),
                        FunctionArgument(abiType = "uint256", value = sendRequest.tokenAmount.rawValue)
                    ),
                    abiOutputTypes = listOf("bool"),
                    additionalData = listOf(Utf8String(id.toString()))
                )
            )
                .willReturn(encodedData)
        }

        val service = SendErc20RequestServiceImpl(
            uuidProvider = mock(),
            functionEncoderService = functionEncoderService,
            blockchainService = blockchainService,
            clientInfoRepository = mock(),
            sendErc20RequestRepository = sendErc20RequestRepository
        )

        verify("send ERC20 request with failed status is returned") {
            assertThat(service.getSendErc20Request(id = id, rpcSpec = chainSpec.rpcSpec)).withMessage()
                .isEqualTo(
                    FullSendErc20Request.fromSendErc20Request(
                        request = sendRequest,
                        status = Status.FAILED,
                        data = encodedData,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnSendErc20RequestWithFailedStatusWhenTransactionHasWrongData() {
        val id = UUID.randomUUID()
        val sendRequest = SendErc20Request(
            id = id,
            chainId = Chain.HARDHAT_TESTNET.id,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            tokenSenderAddress = WalletAddress("b"),
            tokenRecipientAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = null,
            screenConfig = ScreenConfig(
                title = "title",
                message = "message",
                logo = "logo"
            )
        )
        val sendErc20RequestRepository = mock<SendErc20RequestRepository>()

        suppose("send ERC20 request exists in database") {
            given(sendErc20RequestRepository.getById(id))
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
            blockConfirmations = BigInteger.ONE
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
                        FunctionArgument(abiType = "address", value = sendRequest.tokenRecipientAddress.rawValue),
                        FunctionArgument(abiType = "uint256", value = sendRequest.tokenAmount.rawValue)
                    ),
                    abiOutputTypes = listOf("bool"),
                    additionalData = listOf(Utf8String(id.toString()))
                )
            )
                .willReturn(encodedData)
        }

        val service = SendErc20RequestServiceImpl(
            uuidProvider = mock(),
            functionEncoderService = functionEncoderService,
            blockchainService = blockchainService,
            clientInfoRepository = mock(),
            sendErc20RequestRepository = sendErc20RequestRepository
        )

        verify("send ERC20 request with failed status is returned") {
            assertThat(service.getSendErc20Request(id = id, rpcSpec = chainSpec.rpcSpec)).withMessage()
                .isEqualTo(
                    FullSendErc20Request.fromSendErc20Request(
                        request = sendRequest,
                        status = Status.FAILED,
                        data = encodedData,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnSendErc20RequestWithSuccessfulStatusWhenFromAddressIsNull() {
        val id = UUID.randomUUID()
        val sendRequest = SendErc20Request(
            id = id,
            chainId = Chain.HARDHAT_TESTNET.id,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            tokenSenderAddress = null,
            tokenRecipientAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = null,
            screenConfig = ScreenConfig(
                title = "title",
                message = "message",
                logo = "logo"
            )
        )
        val sendErc20RequestRepository = mock<SendErc20RequestRepository>()

        suppose("send ERC20 request exists in database") {
            given(sendErc20RequestRepository.getById(id))
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
            blockConfirmations = BigInteger.ONE
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
                        FunctionArgument(abiType = "address", value = sendRequest.tokenRecipientAddress.rawValue),
                        FunctionArgument(abiType = "uint256", value = sendRequest.tokenAmount.rawValue)
                    ),
                    abiOutputTypes = listOf("bool"),
                    additionalData = listOf(Utf8String(id.toString()))
                )
            )
                .willReturn(encodedData)
        }

        val service = SendErc20RequestServiceImpl(
            uuidProvider = mock(),
            functionEncoderService = functionEncoderService,
            blockchainService = blockchainService,
            clientInfoRepository = mock(),
            sendErc20RequestRepository = sendErc20RequestRepository
        )

        verify("send ERC20 request with successful status is returned") {
            assertThat(service.getSendErc20Request(id = id, rpcSpec = chainSpec.rpcSpec)).withMessage()
                .isEqualTo(
                    FullSendErc20Request.fromSendErc20Request(
                        request = sendRequest,
                        status = Status.SUCCESS,
                        data = encodedData,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnSendErc20RequestWithSuccessfulStatusWhenFromAddressIsSpecified() {
        val id = UUID.randomUUID()
        val sendRequest = SendErc20Request(
            id = id,
            chainId = Chain.HARDHAT_TESTNET.id,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            tokenSenderAddress = WalletAddress("b"),
            tokenRecipientAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = null,
            screenConfig = ScreenConfig(
                title = "title",
                message = "message",
                logo = "logo"
            )
        )
        val sendErc20RequestRepository = mock<SendErc20RequestRepository>()

        suppose("send ERC20 request exists in database") {
            given(sendErc20RequestRepository.getById(id))
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
            blockConfirmations = BigInteger.ONE
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
                        FunctionArgument(abiType = "address", value = sendRequest.tokenRecipientAddress.rawValue),
                        FunctionArgument(abiType = "uint256", value = sendRequest.tokenAmount.rawValue)
                    ),
                    abiOutputTypes = listOf("bool"),
                    additionalData = listOf(Utf8String(id.toString()))
                )
            )
                .willReturn(encodedData)
        }

        val service = SendErc20RequestServiceImpl(
            uuidProvider = mock(),
            functionEncoderService = functionEncoderService,
            blockchainService = blockchainService,
            clientInfoRepository = mock(),
            sendErc20RequestRepository = sendErc20RequestRepository
        )

        verify("send ERC20 request with successful status is returned") {
            assertThat(service.getSendErc20Request(id = id, rpcSpec = chainSpec.rpcSpec)).withMessage()
                .isEqualTo(
                    FullSendErc20Request.fromSendErc20Request(
                        request = sendRequest,
                        status = Status.SUCCESS,
                        data = encodedData,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustSuccessfullyAttachTxHash() {
        val sendErc20RequestRepository = mock<SendErc20RequestRepository>()
        val id = UUID.randomUUID()

        suppose("txHash will be successfully attached to the request") {
            given(sendErc20RequestRepository.setTxHash(id, TX_HASH))
                .willReturn(true)
        }

        val service = SendErc20RequestServiceImpl(
            uuidProvider = mock(),
            functionEncoderService = mock(),
            blockchainService = mock(),
            clientInfoRepository = mock(),
            sendErc20RequestRepository = sendErc20RequestRepository
        )

        verify("txHash was successfully attached") {
            service.attachTxHash(id, TX_HASH)

            verifyMock(sendErc20RequestRepository)
                .setTxHash(id, TX_HASH)
            verifyNoMoreInteractions(sendErc20RequestRepository)
        }
    }

    @Test
    fun mustThrowCannotAttachTxHashExceptionWhenAttachingTxHashFails() {
        val sendErc20RequestRepository = mock<SendErc20RequestRepository>()
        val id = UUID.randomUUID()

        suppose("attaching txHash will fails") {
            given(sendErc20RequestRepository.setTxHash(id, TX_HASH))
                .willReturn(false)
        }

        val service = SendErc20RequestServiceImpl(
            uuidProvider = mock(),
            functionEncoderService = mock(),
            blockchainService = mock(),
            clientInfoRepository = mock(),
            sendErc20RequestRepository = sendErc20RequestRepository
        )

        verify("CannotAttachTxHashException is thrown") {
            assertThrows<CannotAttachTxHashException>(message) {
                service.attachTxHash(id, TX_HASH)
            }

            verifyMock(sendErc20RequestRepository)
                .setTxHash(id, TX_HASH)
            verifyNoMoreInteractions(sendErc20RequestRepository)
        }
    }
}
