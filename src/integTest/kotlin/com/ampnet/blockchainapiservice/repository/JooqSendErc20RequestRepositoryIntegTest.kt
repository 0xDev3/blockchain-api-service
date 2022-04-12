package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.config.JsonConfig
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.SendErc20RequestRecord
import com.ampnet.blockchainapiservice.model.SendScreenConfig
import com.ampnet.blockchainapiservice.model.params.StoreSendErc20RequestParams
import com.ampnet.blockchainapiservice.model.result.SendErc20Request
import com.ampnet.blockchainapiservice.model.result.TransactionData
import com.ampnet.blockchainapiservice.testcontainers.PostgresTestContainer
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.jooq.JSON
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.Import
import java.math.BigInteger
import java.util.UUID

@JooqTest
@Import(JooqSendErc20RequestRepository::class, JsonConfig::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JooqSendErc20RequestRepositoryIntegTest : TestBase() {

    companion object {
        private val CHAIN_ID = ChainId(1337L)
        private const val REDIRECT_URL = "redirect-url"
        private val TOKEN_ADDRESS = ContractAddress("a")
        private val AMOUNT = Balance(BigInteger.valueOf(123456L))
        private val FROM_ADDRESS = WalletAddress("b")
        private val TO_ADDRESS = WalletAddress("c")
        private const val ARBITRARY_DATA = "{}"
        private const val SEND_SCREEN_TITLE = "send-screen-title"
        private const val SEND_SCREEN_MESSAGE = "send-screen-message"
        private const val SEND_SCREEN_LOGO = "send-screen-logo"
        private const val TX_HASH = "tx-hash"
    }

    @Suppress("unused")
    private val postgresContainer = PostgresTestContainer()

    @Autowired
    private lateinit var repository: JooqSendErc20RequestRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun mustCorrectlyFetchSendErc20RequestById() {
        val id = UUID.randomUUID()

        suppose("some send ERC20 request exists in database") {
            dslContext.executeInsert(
                SendErc20RequestRecord(
                    id = id,
                    chainId = CHAIN_ID.value,
                    redirectUrl = REDIRECT_URL,
                    tokenAddress = TOKEN_ADDRESS.rawValue,
                    amount = AMOUNT.rawValue,
                    fromAddress = FROM_ADDRESS.rawValue,
                    toAddress = TO_ADDRESS.rawValue,
                    arbitraryData = JSON.valueOf(ARBITRARY_DATA),
                    sendScreenTitle = SEND_SCREEN_TITLE,
                    sendScreenMessage = SEND_SCREEN_MESSAGE,
                    sendScreenLogo = SEND_SCREEN_LOGO,
                    txHash = TX_HASH
                )
            )
        }

        verify("send ERC20 request is correctly fetched by ID") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(
                    SendErc20Request(
                        id = id,
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        tokenAddress = TOKEN_ADDRESS,
                        amount = AMOUNT,
                        arbitraryData = objectMapper.readTree(ARBITRARY_DATA),
                        sendScreenConfig = SendScreenConfig(
                            title = SEND_SCREEN_TITLE,
                            message = SEND_SCREEN_MESSAGE,
                            logo = SEND_SCREEN_LOGO
                        ),
                        transactionData = TransactionData(
                            txHash = TX_HASH,
                            fromAddress = FROM_ADDRESS,
                            toAddress = TO_ADDRESS
                        )
                    )
                )
        }
    }

    @Test
    fun mustReturnNullWhenFetchingNonExistentSendErc20RequestById() {
        verify("null is returned when fetching non-existent send ERC20 request") {
            val result = repository.getById(UUID.randomUUID())

            assertThat(result).withMessage()
                .isNull()
        }
    }

    @Test
    fun mustCorrectlySendErc20Request() {
        val id = UUID.randomUUID()
        val params = StoreSendErc20RequestParams(
            id = id,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            tokenAddress = TOKEN_ADDRESS,
            amount = AMOUNT,
            fromAddress = FROM_ADDRESS,
            toAddress = TO_ADDRESS,
            arbitraryData = objectMapper.readTree(ARBITRARY_DATA),
            screenConfig = SendScreenConfig(
                title = SEND_SCREEN_TITLE,
                message = SEND_SCREEN_MESSAGE,
                logo = SEND_SCREEN_LOGO
            )
        )

        val storedSendErc20Request = suppose("send ERC20 request is stored in database") {
            repository.store(params)
        }

        val expectedSendErc20Request = SendErc20Request(
            id = id,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            tokenAddress = TOKEN_ADDRESS,
            amount = AMOUNT,
            arbitraryData = objectMapper.readTree(ARBITRARY_DATA),
            sendScreenConfig = SendScreenConfig(
                title = SEND_SCREEN_TITLE,
                message = SEND_SCREEN_MESSAGE,
                logo = SEND_SCREEN_LOGO
            ),
            transactionData = TransactionData(
                txHash = null,
                fromAddress = FROM_ADDRESS,
                toAddress = TO_ADDRESS
            )
        )

        verify("storing send ERC20 request returns correct result") {
            assertThat(storedSendErc20Request).withMessage()
                .isEqualTo(expectedSendErc20Request)
        }

        verify("send ERC20 request was stored in database") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(expectedSendErc20Request)
        }
    }

    @Test
    fun mustCorrectlySetTxHashForSendErc20RequestWithNullTxHash() {
        val id = UUID.randomUUID()
        val params = StoreSendErc20RequestParams(
            id = id,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            tokenAddress = TOKEN_ADDRESS,
            amount = AMOUNT,
            fromAddress = FROM_ADDRESS,
            toAddress = TO_ADDRESS,
            arbitraryData = objectMapper.readTree(ARBITRARY_DATA),
            screenConfig = SendScreenConfig(
                title = SEND_SCREEN_TITLE,
                message = SEND_SCREEN_MESSAGE,
                logo = SEND_SCREEN_LOGO
            )
        )

        suppose("send ERC20 request is stored in database") {
            repository.store(params)
        }

        verify("setting txHash will succeed") {
            assertThat(repository.setTxHash(id, TX_HASH)).withMessage()
                .isTrue()
        }

        verify("txHash was correctly set in database") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(
                    SendErc20Request(
                        id = id,
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        tokenAddress = TOKEN_ADDRESS,
                        amount = AMOUNT,
                        arbitraryData = objectMapper.readTree(ARBITRARY_DATA),
                        sendScreenConfig = SendScreenConfig(
                            title = SEND_SCREEN_TITLE,
                            message = SEND_SCREEN_MESSAGE,
                            logo = SEND_SCREEN_LOGO
                        ),
                        transactionData = TransactionData(
                            txHash = TX_HASH,
                            fromAddress = FROM_ADDRESS,
                            toAddress = TO_ADDRESS
                        )
                    )
                )
        }
    }

    @Test
    fun mustNotSetTxHashForSendErc20RequestWhenTxHashIsAlreadySet() {
        val id = UUID.randomUUID()
        val params = StoreSendErc20RequestParams(
            id = id,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            tokenAddress = TOKEN_ADDRESS,
            amount = AMOUNT,
            fromAddress = FROM_ADDRESS,
            toAddress = TO_ADDRESS,
            arbitraryData = objectMapper.readTree(ARBITRARY_DATA),
            screenConfig = SendScreenConfig(
                title = SEND_SCREEN_TITLE,
                message = SEND_SCREEN_MESSAGE,
                logo = SEND_SCREEN_LOGO
            )
        )

        suppose("send ERC20 request is stored in database") {
            repository.store(params)
        }

        verify("setting txHash will succeed") {
            assertThat(repository.setTxHash(id, TX_HASH)).withMessage()
                .isTrue()
        }

        verify("setting another txHash will not succeed") {
            assertThat(repository.setTxHash(id, "different-tx-hash")).withMessage()
                .isFalse()
        }

        verify("first txHash remains in database") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(
                    SendErc20Request(
                        id = id,
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        tokenAddress = TOKEN_ADDRESS,
                        amount = AMOUNT,
                        arbitraryData = objectMapper.readTree(ARBITRARY_DATA),
                        sendScreenConfig = SendScreenConfig(
                            title = SEND_SCREEN_TITLE,
                            message = SEND_SCREEN_MESSAGE,
                            logo = SEND_SCREEN_LOGO
                        ),
                        transactionData = TransactionData(
                            txHash = TX_HASH,
                            fromAddress = FROM_ADDRESS,
                            toAddress = TO_ADDRESS
                        )
                    )
                )
        }
    }
}
