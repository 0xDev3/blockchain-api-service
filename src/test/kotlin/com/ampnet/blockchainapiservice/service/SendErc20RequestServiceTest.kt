package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.exception.CannotAttachTxHashException
import com.ampnet.blockchainapiservice.exception.IncompleteSendErc20RequestException
import com.ampnet.blockchainapiservice.exception.NonExistentClientIdException
import com.ampnet.blockchainapiservice.model.SendScreenConfig
import com.ampnet.blockchainapiservice.model.params.CreateSendErc20RequestParams
import com.ampnet.blockchainapiservice.model.params.StoreSendErc20RequestParams
import com.ampnet.blockchainapiservice.model.result.ClientInfo
import com.ampnet.blockchainapiservice.model.result.SendErc20Request
import com.ampnet.blockchainapiservice.model.result.TransactionData
import com.ampnet.blockchainapiservice.repository.ClientInfoRepository
import com.ampnet.blockchainapiservice.repository.SendErc20RequestRepository
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.FunctionArgument
import com.ampnet.blockchainapiservice.util.FunctionData
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.ampnet.blockchainapiservice.util.WithFunctionData
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
            redirectUrl = "redirect-url",
            tokenAddress = ContractAddress("a"),
            amount = Balance(BigInteger.valueOf(123456L)),
            fromAddress = WalletAddress("b"),
            toAddress = WalletAddress("c"),
            arbitraryData = null,
            screenConfig = SendScreenConfig(
                title = "title",
                message = "message",
                logo = "logo"
            )
        )
        private const val TX_HASH = "tx-hash"
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
                        FunctionArgument(abiType = "address", value = CREATE_PARAMS.toAddress),
                        FunctionArgument(abiType = "uint256", value = CREATE_PARAMS.amount.rawValue)
                    ),
                    abiOutputTypes = listOf("bool"),
                    additionalData = listOf(Utf8String(uuid.toString()))
                )
            )
                .willReturn(encodedData)
        }

        val chainId = ChainId(123456789L)
        val redirectUrl = "different-redirect-url"
        val clientInfoRepository = mock<ClientInfoRepository>()

        suppose("client info will be fetched from database") {
            given(clientInfoRepository.getById(CLIENT_ID))
                .willReturn(
                    ClientInfo(
                        clientId = CLIENT_ID,
                        chainId = chainId,
                        redirectUrl = redirectUrl
                    )
                )
        }

        val sendErc20RequestRepository = mock<SendErc20RequestRepository>()

        val storeParams = StoreSendErc20RequestParams(
            id = uuid,
            chainId = chainId,
            redirectUrl = redirectUrl,
            tokenAddress = CREATE_PARAMS.tokenAddress,
            amount = CREATE_PARAMS.amount,
            fromAddress = CREATE_PARAMS.fromAddress,
            toAddress = CREATE_PARAMS.toAddress,
            arbitraryData = CREATE_PARAMS.arbitraryData,
            screenConfig = CREATE_PARAMS.screenConfig
        )

        val storedRequest = SendErc20Request(
            id = uuid,
            chainId = chainId,
            redirectUrl = redirectUrl,
            tokenAddress = CREATE_PARAMS.tokenAddress,
            amount = CREATE_PARAMS.amount,
            arbitraryData = CREATE_PARAMS.arbitraryData,
            sendScreenConfig = CREATE_PARAMS.screenConfig,
            transactionData = TransactionData(
                txHash = null,
                fromAddress = CREATE_PARAMS.fromAddress,
                toAddress = CREATE_PARAMS.toAddress
            )
        )

        suppose("send ERC20 request is stored in database") {
            given(sendErc20RequestRepository.store(storeParams))
                .willReturn(storedRequest)
        }

        val service = SendErc20RequestServiceImpl(
            uuidProvider = uuidProvider,
            functionEncoderService = functionEncoderService,
            clientInfoRepository = clientInfoRepository,
            sendErc20RequestRepository = sendErc20RequestRepository
        )

        verify("send ERC20 request is correctly created") {
            assertThat(service.createSendErc20Request(CREATE_PARAMS)).withMessage()
                .isEqualTo(WithFunctionData(storedRequest, encodedData))

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
                        FunctionArgument(abiType = "address", value = CREATE_PARAMS.toAddress),
                        FunctionArgument(abiType = "uint256", value = CREATE_PARAMS.amount.rawValue)
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
            redirectUrl = redirectUrl,
            tokenAddress = CREATE_PARAMS.tokenAddress,
            amount = CREATE_PARAMS.amount,
            fromAddress = CREATE_PARAMS.fromAddress,
            toAddress = CREATE_PARAMS.toAddress,
            arbitraryData = CREATE_PARAMS.arbitraryData,
            screenConfig = CREATE_PARAMS.screenConfig
        )

        val storedRequest = SendErc20Request(
            id = uuid,
            chainId = chainId,
            redirectUrl = redirectUrl,
            tokenAddress = CREATE_PARAMS.tokenAddress,
            amount = CREATE_PARAMS.amount,
            arbitraryData = CREATE_PARAMS.arbitraryData,
            sendScreenConfig = CREATE_PARAMS.screenConfig,
            transactionData = TransactionData(
                txHash = null,
                fromAddress = CREATE_PARAMS.fromAddress,
                toAddress = CREATE_PARAMS.toAddress
            )
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
