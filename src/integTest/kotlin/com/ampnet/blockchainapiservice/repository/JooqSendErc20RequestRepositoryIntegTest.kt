package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.config.JsonConfig
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.SendErc20RequestRecord
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.params.StoreSendErc20RequestParams
import com.ampnet.blockchainapiservice.model.result.SendErc20Request
import com.ampnet.blockchainapiservice.testcontainers.PostgresTestContainer
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.TransactionHash
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
        private val TOKEN_AMOUNT = Balance(BigInteger.valueOf(123456L))
        private val TOKEN_SENDER_ADDRESS = WalletAddress("b")
        private val TOKEN_RECIPIENT_ADDRESS = WalletAddress("c")
        private const val ARBITRARY_DATA = "{}"
        private const val SEND_SCREEN_BEFORE_ACTION_MESSAGE = "send-screen-before-action-message"
        private const val SEND_SCREEN_AFTER_ACTION_MESSAGE = "send-screen-after-action-message"
        private val TX_HASH = TransactionHash("tx-hash")
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
                    tokenAmount = TOKEN_AMOUNT.rawValue,
                    tokenSenderAddress = TOKEN_SENDER_ADDRESS.rawValue,
                    tokenRecipientAddress = TOKEN_RECIPIENT_ADDRESS.rawValue,
                    arbitraryData = JSON.valueOf(ARBITRARY_DATA),
                    sendScreenBeforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                    sendScreenAfterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE,
                    txHash = TX_HASH.value
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
                        tokenAmount = TOKEN_AMOUNT,
                        tokenSenderAddress = TOKEN_SENDER_ADDRESS,
                        tokenRecipientAddress = TOKEN_RECIPIENT_ADDRESS,
                        txHash = TX_HASH,
                        arbitraryData = objectMapper.readTree(ARBITRARY_DATA),
                        screenConfig = ScreenConfig(
                            beforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE
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
    fun mustCorrectlyStoreSendErc20Request() {
        val id = UUID.randomUUID()
        val params = StoreSendErc20RequestParams(
            id = id,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            tokenAddress = TOKEN_ADDRESS,
            tokenAmount = TOKEN_AMOUNT,
            tokenSenderAddress = TOKEN_SENDER_ADDRESS,
            tokenRecipientAddress = TOKEN_RECIPIENT_ADDRESS,
            arbitraryData = objectMapper.readTree(ARBITRARY_DATA),
            screenConfig = ScreenConfig(
                beforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE
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
            tokenAmount = TOKEN_AMOUNT,
            tokenSenderAddress = TOKEN_SENDER_ADDRESS,
            tokenRecipientAddress = TOKEN_RECIPIENT_ADDRESS,
            txHash = null,
            arbitraryData = objectMapper.readTree(ARBITRARY_DATA),
            screenConfig = ScreenConfig(
                beforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE
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
            tokenAmount = TOKEN_AMOUNT,
            tokenSenderAddress = TOKEN_SENDER_ADDRESS,
            tokenRecipientAddress = TOKEN_RECIPIENT_ADDRESS,
            arbitraryData = objectMapper.readTree(ARBITRARY_DATA),
            screenConfig = ScreenConfig(
                beforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE
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
                        tokenAmount = TOKEN_AMOUNT,
                        tokenSenderAddress = TOKEN_SENDER_ADDRESS,
                        tokenRecipientAddress = TOKEN_RECIPIENT_ADDRESS,
                        txHash = TX_HASH,
                        arbitraryData = objectMapper.readTree(ARBITRARY_DATA),
                        screenConfig = ScreenConfig(
                            beforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE
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
            tokenAmount = TOKEN_AMOUNT,
            tokenSenderAddress = TOKEN_SENDER_ADDRESS,
            tokenRecipientAddress = TOKEN_RECIPIENT_ADDRESS,
            arbitraryData = objectMapper.readTree(ARBITRARY_DATA),
            screenConfig = ScreenConfig(
                beforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE
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
            assertThat(repository.setTxHash(id, TransactionHash("different-tx-hash"))).withMessage()
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
                        tokenAmount = TOKEN_AMOUNT,
                        tokenSenderAddress = TOKEN_SENDER_ADDRESS,
                        tokenRecipientAddress = TOKEN_RECIPIENT_ADDRESS,
                        txHash = TX_HASH,
                        arbitraryData = objectMapper.readTree(ARBITRARY_DATA),
                        screenConfig = ScreenConfig(
                            beforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE
                        )
                    )
                )
        }
    }
}
