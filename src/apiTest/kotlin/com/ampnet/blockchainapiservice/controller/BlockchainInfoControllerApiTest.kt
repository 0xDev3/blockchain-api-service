package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.ControllerTestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.blockchain.SimpleERC20
import com.ampnet.blockchainapiservice.config.binding.RpcUrlSpecResolver
import com.ampnet.blockchainapiservice.exception.ErrorCode
import com.ampnet.blockchainapiservice.generated.jooq.tables.SignedVerificationMessageTable
import com.ampnet.blockchainapiservice.model.response.FetchErc20TokenBalanceResponse
import com.ampnet.blockchainapiservice.repository.SignedVerificationMessageRepository
import com.ampnet.blockchainapiservice.service.UtcDateTimeProvider
import com.ampnet.blockchainapiservice.testcontainers.HardhatTestContainer
import com.ampnet.blockchainapiservice.util.AccountBalance
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ContractAddress
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.web3j.protocol.core.RemoteCall
import org.web3j.tx.gas.DefaultGasProvider
import java.math.BigInteger
import java.time.Duration

class BlockchainInfoControllerApiTest : ControllerTestBase() {

    private val accounts = HardhatTestContainer.accounts

    @Autowired
    private lateinit var signedVerificationMessageRepository: SignedVerificationMessageRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @MockBean
    private lateinit var utcDateTimeProvider: UtcDateTimeProvider

    @BeforeEach
    fun beforeEach() {
        dslContext.deleteFrom(SignedVerificationMessageTable.SIGNED_VERIFICATION_MESSAGE).execute()
    }

    @Test
    fun mustCorrectlyFetchErc20Balance() {
        suppose("some fixed date-time will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.SIGNED_MESSAGE.createdAt)
        }

        val mainAccount = accounts[0]
        val accountBalance = AccountBalance(TestData.SIGNED_MESSAGE.walletAddress, Balance(BigInteger("10000")))

        val contract = suppose("simple ERC20 contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(accountBalance.address.rawValue),
                listOf(accountBalance.balance.rawValue),
                mainAccount.address
            ).sendAndMine()
        }

        suppose("some signed message is in the database") {
            signedVerificationMessageRepository.store(TestData.SIGNED_MESSAGE)
        }

        val blockNumber = hardhatContainer.blockNumber()
        val contractAddress = ContractAddress(contract.contractAddress)

        val erc20AccountBalance = suppose("request to fetch ERC20 balance is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get(
                    "/info/${chainId.value}/${TestData.SIGNED_MESSAGE.id}/erc20-balance/${contractAddress.rawValue}"
                )
                    .queryParam("blockNumber", blockNumber.value.toString())
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, FetchErc20TokenBalanceResponse::class.java)
        }

        verify("correct ERC20 balance is returned") {
            assertThat(erc20AccountBalance).withMessage()
                .isEqualTo(
                    FetchErc20TokenBalanceResponse(
                        walletAddress = accountBalance.address.rawValue,
                        tokenBalance = accountBalance.balance.rawValue,
                        tokenAddress = contractAddress.rawValue
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchErc20BalanceWhenCustomRpcIsSpecified() {
        suppose("some fixed date-time will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.SIGNED_MESSAGE.createdAt)
        }

        val mainAccount = accounts[0]
        val accountBalance = AccountBalance(TestData.SIGNED_MESSAGE.walletAddress, Balance(BigInteger("10000")))

        val contract = suppose("simple ERC20 contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(accountBalance.address.rawValue),
                listOf(accountBalance.balance.rawValue),
                mainAccount.address
            ).sendAndMine()
        }

        suppose("some signed message is in the database") {
            signedVerificationMessageRepository.store(TestData.SIGNED_MESSAGE)
        }

        val blockNumber = hardhatContainer.blockNumber()
        val contractAddress = ContractAddress(contract.contractAddress)

        val erc20AccountBalance = suppose("request to fetch ERC20 balance is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get(
                    "/info/123456/${TestData.SIGNED_MESSAGE.id}/erc20-balance/${contractAddress.rawValue}"
                )
                    .queryParam("blockNumber", blockNumber.value.toString())
                    .header(
                        RpcUrlSpecResolver.RPC_URL_OVERRIDE_HEADER,
                        "http://localhost:${hardhatContainer.mappedPort}"
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, FetchErc20TokenBalanceResponse::class.java)
        }

        verify("correct ERC20 balance is returned") {
            assertThat(erc20AccountBalance).withMessage()
                .isEqualTo(
                    FetchErc20TokenBalanceResponse(
                        walletAddress = accountBalance.address.rawValue,
                        tokenBalance = accountBalance.balance.rawValue,
                        tokenAddress = contractAddress.rawValue
                    )
                )
        }
    }

    @Test
    fun mustReturn404NotFoundWhenFetchingErc20BalanceOfNonExistentMessage() {
        suppose("some fixed date-time will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.SIGNED_MESSAGE.createdAt)
        }

        val contractAddress = ContractAddress("abc")

        verify("404 is returned for non-existent message") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get(
                    "/info/${chainId.value}/${TestData.SIGNED_MESSAGE.id}/erc20-balance/${contractAddress.rawValue}"
                )
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    fun mustReturn400BadRequestWhenFetchingErc20BalanceOfExpiredMessage() {
        suppose("some fixed date-time will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.SIGNED_MESSAGE.validUntil + Duration.ofMinutes(1L))
        }

        suppose("some signed message is in the database") {
            signedVerificationMessageRepository.store(TestData.SIGNED_MESSAGE)
        }

        val contractAddress = ContractAddress("abc")

        verify("400 is returned for expired message") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get(
                    "/info/${chainId.value}/${TestData.SIGNED_MESSAGE.id}/erc20-balance/${contractAddress.rawValue}"
                )
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.VALIDATION_MESSAGE_EXPIRED)
        }
    }

    @Test
    fun mustReturn400BadRequestWhenFetchingErc20BalanceOfNonExistentTokenContract() {
        suppose("some fixed date-time will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.SIGNED_MESSAGE.createdAt)
        }

        suppose("some signed message is in the database") {
            signedVerificationMessageRepository.store(TestData.SIGNED_MESSAGE)
        }

        val contractAddress = ContractAddress("abc")

        verify("400 is returned for invalid cotnract address") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get(
                    "/info/${chainId.value}/${TestData.SIGNED_MESSAGE.id}/erc20-balance/${contractAddress.rawValue}"
                )
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.BLOCKCHAIN_READ_ERROR)
        }
    }

    private fun <T> RemoteCall<T>.sendAndMine(): T {
        val future = sendAsync()
        hardhatContainer.waitAndMine()
        return future.get()
    }
}
