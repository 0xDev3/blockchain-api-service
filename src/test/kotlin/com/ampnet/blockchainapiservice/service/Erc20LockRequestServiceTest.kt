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
import com.ampnet.blockchainapiservice.model.params.CreateErc20LockRequestParams
import com.ampnet.blockchainapiservice.model.params.StoreErc20LockRequestParams
import com.ampnet.blockchainapiservice.model.result.BlockchainTransactionInfo
import com.ampnet.blockchainapiservice.model.result.ClientInfo
import com.ampnet.blockchainapiservice.model.result.Erc20LockRequest
import com.ampnet.blockchainapiservice.repository.ClientInfoRepository
import com.ampnet.blockchainapiservice.repository.Erc20LockRequestRepository
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

class Erc20LockRequestServiceTest : TestBase() {

    companion object {
        private const val CLIENT_ID = "client-id"
        private val CREATE_PARAMS = CreateErc20LockRequestParams(
            clientId = CLIENT_ID,
            chainId = ChainId(1337L),
            redirectUrl = "redirect-url/\${id}",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.valueOf(123456L)),
            lockContractAddress = ContractAddress("b"),
            tokenSenderAddress = WalletAddress("c"),
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            )
        )
        private val TX_HASH = TransactionHash("tx-hash")
    }

    @Test
    fun mustSuccessfullyCreateErc20LockRequestWhenClientIdIsProvided() {
        val uuidProvider = mock<UuidProvider>()
        val uuid = UUID.randomUUID()

        suppose("some UUID will be generated") {
            given(uuidProvider.getUuid())
                .willReturn(uuid)
        }

        val functionEncoderService = mock<FunctionEncoderService>()
        val encodedData = FunctionData("encoded")
        val tokenAddress = ContractAddress("cafebabe")

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "lock",
                    arguments = listOf(
                        FunctionArgument(abiType = AbiType.Address, value = tokenAddress),
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
        val clientInfoRepository = mock<ClientInfoRepository>()

        suppose("client info will be fetched from database") {
            given(clientInfoRepository.getById(CLIENT_ID))
                .willReturn(
                    ClientInfo(
                        clientId = CLIENT_ID,
                        chainId = chainId,
                        sendRedirectUrl = null,
                        balanceRedirectUrl = null,
                        lockRedirectUrl = redirectUrl,
                        tokenAddress = tokenAddress
                    )
                )
        }

        val erc20LockRequestRepository = mock<Erc20LockRequestRepository>()

        val storeParams = StoreErc20LockRequestParams(
            id = uuid,
            chainId = chainId,
            redirectUrl = redirectUrl.replace("\${id}", uuid.toString()),
            tokenAddress = tokenAddress,
            tokenAmount = CREATE_PARAMS.tokenAmount,
            lockContractAddress = CREATE_PARAMS.lockContractAddress,
            tokenSenderAddress = CREATE_PARAMS.tokenSenderAddress,
            arbitraryData = CREATE_PARAMS.arbitraryData,
            screenConfig = CREATE_PARAMS.screenConfig
        )

        val storedRequest = Erc20LockRequest(
            id = uuid,
            chainId = chainId,
            redirectUrl = storeParams.redirectUrl,
            tokenAddress = tokenAddress,
            tokenAmount = CREATE_PARAMS.tokenAmount,
            lockContractAddress = CREATE_PARAMS.lockContractAddress,
            tokenSenderAddress = CREATE_PARAMS.tokenSenderAddress,
            txHash = null,
            arbitraryData = CREATE_PARAMS.arbitraryData,
            screenConfig = CREATE_PARAMS.screenConfig
        )

        suppose("ERC20 lock request is stored in database") {
            given(erc20LockRequestRepository.store(storeParams))
                .willReturn(storedRequest)
        }

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20LockRequestRepository = erc20LockRequestRepository,
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

        verify("ERC20 lock request is correctly created") {
            assertThat(service.createErc20LockRequest(createParams)).withMessage()
                .isEqualTo(
                    WithFunctionData(
                        storedRequest.copy(redirectUrl = storedRequest.redirectUrl.replace("\${id}", uuid.toString())),
                        encodedData
                    )
                )

            verifyMock(erc20LockRequestRepository)
                .store(storeParams)
            verifyNoMoreInteractions(erc20LockRequestRepository)
        }
    }

    @Test
    fun mustSuccessfullyCreateErc20LockRequestWhenClientIdIsNotProvided() {
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
                    functionName = "lock",
                    arguments = listOf(
                        FunctionArgument(abiType = AbiType.Address, value = CREATE_PARAMS.tokenAddress!!),
                        FunctionArgument(abiType = AbiType.Uint256, value = CREATE_PARAMS.tokenAmount)
                    ),
                    abiOutputTypes = listOf(AbiType.Bool),
                    additionalData = listOf(Utf8String(uuid.toString()))
                )
            )
                .willReturn(encodedData)
        }

        val erc20LockRequestRepository = mock<Erc20LockRequestRepository>()
        val chainId = CREATE_PARAMS.chainId!!
        val redirectUrl = CREATE_PARAMS.redirectUrl!!

        val storeParams = StoreErc20LockRequestParams(
            id = uuid,
            chainId = chainId,
            redirectUrl = redirectUrl.replace("\${id}", uuid.toString()),
            tokenAddress = CREATE_PARAMS.tokenAddress!!,
            tokenAmount = CREATE_PARAMS.tokenAmount,
            lockContractAddress = CREATE_PARAMS.lockContractAddress,
            tokenSenderAddress = CREATE_PARAMS.tokenSenderAddress,
            arbitraryData = CREATE_PARAMS.arbitraryData,
            screenConfig = CREATE_PARAMS.screenConfig
        )

        val storedRequest = Erc20LockRequest(
            id = uuid,
            chainId = chainId,
            redirectUrl = storeParams.redirectUrl,
            tokenAddress = CREATE_PARAMS.tokenAddress!!,
            tokenAmount = CREATE_PARAMS.tokenAmount,
            lockContractAddress = CREATE_PARAMS.lockContractAddress,
            tokenSenderAddress = CREATE_PARAMS.tokenSenderAddress,
            txHash = null,
            arbitraryData = CREATE_PARAMS.arbitraryData,
            screenConfig = CREATE_PARAMS.screenConfig
        )

        suppose("ERC20 lock request is stored in database") {
            given(erc20LockRequestRepository.store(storeParams))
                .willReturn(storedRequest)
        }

        val createParams = suppose("clientId is missing from params") {
            CREATE_PARAMS.copy(clientId = null)
        }

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20LockRequestRepository = erc20LockRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = uuidProvider,
                clientInfoRepository = mock(),
                blockchainService = mock()
            )
        )

        verify("ERC20 lock request is correctly created") {
            assertThat(service.createErc20LockRequest(createParams)).withMessage()
                .isEqualTo(WithFunctionData(storedRequest, encodedData))

            verifyMock(erc20LockRequestRepository)
                .store(storeParams)
            verifyNoMoreInteractions(erc20LockRequestRepository)
        }
    }

    @Test
    fun mustThrowNonExistentClientIdExceptionWhenClientInfoIsNotInDatabase() {
        val clientInfoRepository = mock<ClientInfoRepository>()

        suppose("client info does not exist in database") {
            given(clientInfoRepository.getById(CLIENT_ID))
                .willReturn(null)
        }

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = mock(),
            erc20LockRequestRepository = mock(),
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = mock(),
                clientInfoRepository = clientInfoRepository,
                blockchainService = mock()
            )
        )

        verify("NonExistentClientIdException is thrown") {
            assertThrows<NonExistentClientIdException> {
                service.createErc20LockRequest(CREATE_PARAMS)
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

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = mock(),
            erc20LockRequestRepository = mock(),
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = uuidProvider,
                clientInfoRepository = mock(),
                blockchainService = mock()
            )
        )

        verify("IncompleteRequestException is thrown") {
            assertThrows<IncompleteRequestException> {
                service.createErc20LockRequest(params)
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

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = mock(),
            erc20LockRequestRepository = mock(),
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = uuidProvider,
                clientInfoRepository = mock(),
                blockchainService = mock()
            )
        )

        verify("IncompleteRequestException is thrown") {
            assertThrows<IncompleteRequestException> {
                service.createErc20LockRequest(params)
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

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = mock(),
            erc20LockRequestRepository = mock(),
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = uuidProvider,
                clientInfoRepository = mock(),
                blockchainService = mock()
            )
        )

        verify("IncompleteRequestException is thrown") {
            assertThrows<IncompleteRequestException> {
                service.createErc20LockRequest(params)
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionForNonExistentErc20LockRequest() {
        val erc20LockRequestRepository = mock<Erc20LockRequestRepository>()

        suppose("ERC20 lock request does not exist in database") {
            given(erc20LockRequestRepository.getById(any()))
                .willReturn(null)
        }

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = mock(),
            erc20LockRequestRepository = erc20LockRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = mock(),
                clientInfoRepository = mock(),
                blockchainService = mock()
            )
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.getErc20LockRequest(id = UUID.randomUUID(), rpcSpec = RpcUrlSpec(null, null))
            }
        }
    }

    @Test
    fun mustReturnErc20LockRequestWithPendingStatusWhenErc20LockRequestHasNullTxHash() {
        val id = UUID.randomUUID()
        val lockRequest = Erc20LockRequest(
            id = id,
            chainId = Chain.HARDHAT_TESTNET.id,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            lockContractAddress = ContractAddress("b"),
            tokenSenderAddress = WalletAddress("c"),
            txHash = null,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            )
        )
        val erc20LockRequestRepository = mock<Erc20LockRequestRepository>()

        suppose("ERC20 lock request exists in database") {
            given(erc20LockRequestRepository.getById(id))
                .willReturn(lockRequest)
        }

        val functionEncoderService = mock<FunctionEncoderService>()
        val encodedData = FunctionData("encoded")

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "lock",
                    arguments = listOf(
                        FunctionArgument(abiType = AbiType.Address, value = lockRequest.tokenAddress),
                        FunctionArgument(abiType = AbiType.Uint256, value = lockRequest.tokenAmount)
                    ),
                    abiOutputTypes = listOf(AbiType.Bool),
                    additionalData = listOf(Utf8String(id.toString()))
                )
            )
                .willReturn(encodedData)
        }

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20LockRequestRepository = erc20LockRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = mock(),
                clientInfoRepository = mock(),
                blockchainService = mock()
            )
        )

        verify("ERC20 lock request with pending status is returned") {
            assertThat(service.getErc20LockRequest(id = id, rpcSpec = RpcUrlSpec(null, null))).withMessage()
                .isEqualTo(
                    lockRequest.withTransactionData(
                        status = Status.PENDING,
                        data = encodedData,
                        transactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20LockRequestWithPendingStatusWhenTransactionIsNotYetMined() {
        val id = UUID.randomUUID()
        val lockRequest = Erc20LockRequest(
            id = id,
            chainId = Chain.HARDHAT_TESTNET.id,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            lockContractAddress = ContractAddress("b"),
            tokenSenderAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            )
        )
        val erc20LockRequestRepository = mock<Erc20LockRequestRepository>()

        suppose("ERC20 lock request exists in database") {
            given(erc20LockRequestRepository.getById(id))
                .willReturn(lockRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(lockRequest.chainId, RpcUrlSpec("url", "url-override"))

        suppose("transaction is not yet mined") {
            given(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH))
                .willReturn(null)
        }

        val functionEncoderService = mock<FunctionEncoderService>()
        val encodedData = FunctionData("encoded")

        suppose("function data will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = "lock",
                    arguments = listOf(
                        FunctionArgument(abiType = AbiType.Address, value = lockRequest.tokenAddress),
                        FunctionArgument(abiType = AbiType.Uint256, value = lockRequest.tokenAmount)
                    ),
                    abiOutputTypes = listOf(AbiType.Bool),
                    additionalData = listOf(Utf8String(id.toString()))
                )
            )
                .willReturn(encodedData)
        }

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20LockRequestRepository = erc20LockRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = mock(),
                clientInfoRepository = mock(),
                blockchainService = mock()
            )
        )

        verify("ERC20 lock request with pending status is returned") {
            assertThat(service.getErc20LockRequest(id = id, rpcSpec = chainSpec.rpcSpec)).withMessage()
                .isEqualTo(
                    lockRequest.withTransactionData(
                        status = Status.PENDING,
                        data = encodedData,
                        transactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20LockRequestWithFailedStatusWhenTransactionHasWrongToAddress() {
        val id = UUID.randomUUID()
        val lockRequest = Erc20LockRequest(
            id = id,
            chainId = Chain.HARDHAT_TESTNET.id,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            lockContractAddress = ContractAddress("b"),
            tokenSenderAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            )
        )
        val erc20LockRequestRepository = mock<Erc20LockRequestRepository>()

        suppose("ERC20 lock request exists in database") {
            given(erc20LockRequestRepository.getById(id))
                .willReturn(lockRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(lockRequest.chainId, RpcUrlSpec("url", "url-override"))
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = lockRequest.tokenSenderAddress!!,
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
                    functionName = "lock",
                    arguments = listOf(
                        FunctionArgument(abiType = AbiType.Address, value = lockRequest.tokenAddress),
                        FunctionArgument(abiType = AbiType.Uint256, value = lockRequest.tokenAmount)
                    ),
                    abiOutputTypes = listOf(AbiType.Bool),
                    additionalData = listOf(Utf8String(id.toString()))
                )
            )
                .willReturn(encodedData)
        }

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20LockRequestRepository = erc20LockRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = mock(),
                clientInfoRepository = mock(),
                blockchainService = blockchainService
            )
        )

        verify("ERC20 lock request with failed status is returned") {
            assertThat(service.getErc20LockRequest(id = id, rpcSpec = chainSpec.rpcSpec)).withMessage()
                .isEqualTo(
                    lockRequest.withTransactionData(
                        status = Status.FAILED,
                        data = encodedData,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20LockRequestWithFailedStatusWhenTransactionHasWrongTxHash() {
        val id = UUID.randomUUID()
        val lockRequest = Erc20LockRequest(
            id = id,
            chainId = Chain.HARDHAT_TESTNET.id,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            lockContractAddress = ContractAddress("b"),
            tokenSenderAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            )
        )
        val erc20LockRequestRepository = mock<Erc20LockRequestRepository>()

        suppose("ERC20 lock request exists in database") {
            given(erc20LockRequestRepository.getById(id))
                .willReturn(lockRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(lockRequest.chainId, RpcUrlSpec("url", "url-override"))
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TransactionHash("wrong-hash"),
            from = lockRequest.tokenSenderAddress!!,
            to = lockRequest.lockContractAddress.toWalletAddress(),
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
                    functionName = "lock",
                    arguments = listOf(
                        FunctionArgument(abiType = AbiType.Address, value = lockRequest.tokenAddress),
                        FunctionArgument(abiType = AbiType.Uint256, value = lockRequest.tokenAmount)
                    ),
                    abiOutputTypes = listOf(AbiType.Bool),
                    additionalData = listOf(Utf8String(id.toString()))
                )
            )
                .willReturn(encodedData)
        }

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20LockRequestRepository = erc20LockRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = mock(),
                clientInfoRepository = mock(),
                blockchainService = blockchainService
            )
        )

        verify("ERC20 lock request with failed status is returned") {
            assertThat(service.getErc20LockRequest(id = id, rpcSpec = chainSpec.rpcSpec)).withMessage()
                .isEqualTo(
                    lockRequest.withTransactionData(
                        status = Status.FAILED,
                        data = encodedData,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20LockRequestWithFailedStatusWhenTransactionHasWrongFromAddress() {
        val id = UUID.randomUUID()
        val lockRequest = Erc20LockRequest(
            id = id,
            chainId = Chain.HARDHAT_TESTNET.id,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            lockContractAddress = ContractAddress("b"),
            tokenSenderAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            )
        )
        val erc20LockRequestRepository = mock<Erc20LockRequestRepository>()

        suppose("ERC20 lock request exists in database") {
            given(erc20LockRequestRepository.getById(id))
                .willReturn(lockRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(lockRequest.chainId, RpcUrlSpec("url", "url-override"))
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = WalletAddress("dead"),
            to = lockRequest.lockContractAddress.toWalletAddress(),
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
                    functionName = "lock",
                    arguments = listOf(
                        FunctionArgument(abiType = AbiType.Address, value = lockRequest.tokenAddress),
                        FunctionArgument(abiType = AbiType.Uint256, value = lockRequest.tokenAmount)
                    ),
                    abiOutputTypes = listOf(AbiType.Bool),
                    additionalData = listOf(Utf8String(id.toString()))
                )
            )
                .willReturn(encodedData)
        }

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20LockRequestRepository = erc20LockRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = mock(),
                clientInfoRepository = mock(),
                blockchainService = blockchainService
            )
        )

        verify("ERC20 lock request with failed status is returned") {
            assertThat(service.getErc20LockRequest(id = id, rpcSpec = chainSpec.rpcSpec)).withMessage()
                .isEqualTo(
                    lockRequest.withTransactionData(
                        status = Status.FAILED,
                        data = encodedData,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20LockRequestWithFailedStatusWhenTransactionHasWrongData() {
        val id = UUID.randomUUID()
        val lockRequest = Erc20LockRequest(
            id = id,
            chainId = Chain.HARDHAT_TESTNET.id,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            lockContractAddress = ContractAddress("b"),
            tokenSenderAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            )
        )
        val erc20LockRequestRepository = mock<Erc20LockRequestRepository>()

        suppose("ERC20 lock request exists in database") {
            given(erc20LockRequestRepository.getById(id))
                .willReturn(lockRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(lockRequest.chainId, RpcUrlSpec("url", "url-override"))
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = lockRequest.tokenSenderAddress!!,
            to = lockRequest.lockContractAddress.toWalletAddress(),
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
                    functionName = "lock",
                    arguments = listOf(
                        FunctionArgument(abiType = AbiType.Address, value = lockRequest.tokenAddress),
                        FunctionArgument(abiType = AbiType.Uint256, value = lockRequest.tokenAmount)
                    ),
                    abiOutputTypes = listOf(AbiType.Bool),
                    additionalData = listOf(Utf8String(id.toString()))
                )
            )
                .willReturn(encodedData)
        }

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20LockRequestRepository = erc20LockRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = mock(),
                clientInfoRepository = mock(),
                blockchainService = blockchainService
            )
        )

        verify("ERC20 lock request with failed status is returned") {
            assertThat(service.getErc20LockRequest(id = id, rpcSpec = chainSpec.rpcSpec)).withMessage()
                .isEqualTo(
                    lockRequest.withTransactionData(
                        status = Status.FAILED,
                        data = encodedData,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20LockRequestWithSuccessfulStatusWhenFromAddressIsNull() {
        val id = UUID.randomUUID()
        val lockRequest = Erc20LockRequest(
            id = id,
            chainId = Chain.HARDHAT_TESTNET.id,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            lockContractAddress = ContractAddress("b"),
            tokenSenderAddress = null,
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            )
        )
        val erc20LockRequestRepository = mock<Erc20LockRequestRepository>()

        suppose("ERC20 lock request exists in database") {
            given(erc20LockRequestRepository.getById(id))
                .willReturn(lockRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(lockRequest.chainId, RpcUrlSpec("url", "url-override"))
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = WalletAddress("0cafe0babe"),
            to = lockRequest.lockContractAddress.toWalletAddress(),
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
                    functionName = "lock",
                    arguments = listOf(
                        FunctionArgument(abiType = AbiType.Address, value = lockRequest.tokenAddress),
                        FunctionArgument(abiType = AbiType.Uint256, value = lockRequest.tokenAmount)
                    ),
                    abiOutputTypes = listOf(AbiType.Bool),
                    additionalData = listOf(Utf8String(id.toString()))
                )
            )
                .willReturn(encodedData)
        }

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20LockRequestRepository = erc20LockRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = mock(),
                clientInfoRepository = mock(),
                blockchainService = blockchainService
            )
        )

        verify("ERC20 lock request with successful status is returned") {
            assertThat(service.getErc20LockRequest(id = id, rpcSpec = chainSpec.rpcSpec)).withMessage()
                .isEqualTo(
                    lockRequest.withTransactionData(
                        status = Status.SUCCESS,
                        data = encodedData,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20LockRequestWithSuccessfulStatusWhenFromAddressIsSpecified() {
        val id = UUID.randomUUID()
        val lockRequest = Erc20LockRequest(
            id = id,
            chainId = Chain.HARDHAT_TESTNET.id,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            lockContractAddress = ContractAddress("b"),
            tokenSenderAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            )
        )
        val erc20LockRequestRepository = mock<Erc20LockRequestRepository>()

        suppose("ERC20 lock request exists in database") {
            given(erc20LockRequestRepository.getById(id))
                .willReturn(lockRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(lockRequest.chainId, RpcUrlSpec("url", "url-override"))
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = lockRequest.tokenSenderAddress!!,
            to = lockRequest.lockContractAddress.toWalletAddress(),
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
                    functionName = "lock",
                    arguments = listOf(
                        FunctionArgument(abiType = AbiType.Address, value = lockRequest.tokenAddress),
                        FunctionArgument(abiType = AbiType.Uint256, value = lockRequest.tokenAmount)
                    ),
                    abiOutputTypes = listOf(AbiType.Bool),
                    additionalData = listOf(Utf8String(id.toString()))
                )
            )
                .willReturn(encodedData)
        }

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20LockRequestRepository = erc20LockRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = mock(),
                clientInfoRepository = mock(),
                blockchainService = blockchainService
            )
        )

        verify("ERC20 lock request with successful status is returned") {
            assertThat(service.getErc20LockRequest(id = id, rpcSpec = chainSpec.rpcSpec)).withMessage()
                .isEqualTo(
                    lockRequest.withTransactionData(
                        status = Status.SUCCESS,
                        data = encodedData,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustSuccessfullyAttachTxHash() {
        val erc20LockRequestRepository = mock<Erc20LockRequestRepository>()
        val id = UUID.randomUUID()

        suppose("txHash will be successfully attached to the request") {
            given(erc20LockRequestRepository.setTxHash(id, TX_HASH))
                .willReturn(true)
        }

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = mock(),
            erc20LockRequestRepository = erc20LockRequestRepository,
            erc20CommonService = Erc20CommonServiceImpl(
                uuidProvider = mock(),
                clientInfoRepository = mock(),
                blockchainService = mock()
            )
        )

        verify("txHash was successfully attached") {
            service.attachTxHash(id, TX_HASH)

            verifyMock(erc20LockRequestRepository)
                .setTxHash(id, TX_HASH)
            verifyNoMoreInteractions(erc20LockRequestRepository)
        }
    }

    @Test
    fun mustThrowCannotAttachTxHashExceptionWhenAttachingTxHashFails() {
        val erc20LockRequestRepository = mock<Erc20LockRequestRepository>()
        val id = UUID.randomUUID()

        suppose("attaching txHash will fails") {
            given(erc20LockRequestRepository.setTxHash(id, TX_HASH))
                .willReturn(false)
        }

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = mock(),
            erc20LockRequestRepository = erc20LockRequestRepository,
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

            verifyMock(erc20LockRequestRepository)
                .setTxHash(id, TX_HASH)
            verifyNoMoreInteractions(erc20LockRequestRepository)
        }
    }
}
