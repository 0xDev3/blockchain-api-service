package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.Erc20SendRequestRecord
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.params.StoreErc20LockRequestParams
import com.ampnet.blockchainapiservice.model.params.StoreErc20SendRequestParams
import com.ampnet.blockchainapiservice.model.result.Erc20LockRequest
import com.ampnet.blockchainapiservice.model.result.Erc20SendRequest
import com.ampnet.blockchainapiservice.testcontainers.PostgresTestContainer
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.Import
import java.math.BigInteger
import java.util.UUID

@JooqTest
@Import(JooqErc20SendRequestRepository::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JooqErc20SendRequestRepositoryIntegTest : TestBase() {

    companion object {
        private val CHAIN_ID = ChainId(1337L)
        private const val REDIRECT_URL = "redirect-url"
        private val TOKEN_ADDRESS = ContractAddress("a")
        private val TOKEN_AMOUNT = Balance(BigInteger.valueOf(123456L))
        private val TOKEN_SENDER_ADDRESS = WalletAddress("b")
        private val TOKEN_RECIPIENT_ADDRESS = WalletAddress("c")
        private val ARBITRARY_DATA = TestData.EMPTY_JSON_OBJECT
        private const val SEND_SCREEN_BEFORE_ACTION_MESSAGE = "send-screen-before-action-message"
        private const val SEND_SCREEN_AFTER_ACTION_MESSAGE = "send-screen-after-action-message"
        private val TX_HASH = TransactionHash("tx-hash")
    }

    @Suppress("unused")
    private val postgresContainer = PostgresTestContainer()

    @Autowired
    private lateinit var repository: JooqErc20SendRequestRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @Test
    fun mustCorrectlyFetchErc20SendRequestById() {
        val id = UUID.randomUUID()

        suppose("some ERC20 send request exists in database") {
            dslContext.executeInsert(
                Erc20SendRequestRecord(
                    id = id,
                    chainId = CHAIN_ID,
                    redirectUrl = REDIRECT_URL,
                    tokenAddress = TOKEN_ADDRESS,
                    tokenAmount = TOKEN_AMOUNT,
                    tokenSenderAddress = TOKEN_SENDER_ADDRESS,
                    tokenRecipientAddress = TOKEN_RECIPIENT_ADDRESS,
                    arbitraryData = ARBITRARY_DATA,
                    screenBeforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                    screenAfterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE,
                    txHash = TX_HASH
                )
            )
        }

        verify("ERC20 send request is correctly fetched by ID") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(
                    Erc20SendRequest(
                        id = id,
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        tokenAddress = TOKEN_ADDRESS,
                        tokenAmount = TOKEN_AMOUNT,
                        tokenSenderAddress = TOKEN_SENDER_ADDRESS,
                        tokenRecipientAddress = TOKEN_RECIPIENT_ADDRESS,
                        txHash = TX_HASH,
                        arbitraryData = ARBITRARY_DATA,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE
                        )
                    )
                )
        }
    }

    @Test
    fun mustReturnNullWhenFetchingNonExistentErc20SendRequestById() {
        verify("null is returned when fetching non-existent ERC20 send request") {
            val result = repository.getById(UUID.randomUUID())

            assertThat(result).withMessage()
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyFetchErc20SendRequestsBySender() {
        val senderRequests = listOf(
            Erc20SendRequestRecord(
                id = UUID.randomUUID(),
                chainId = CHAIN_ID,
                redirectUrl = REDIRECT_URL,
                tokenAddress = TOKEN_ADDRESS,
                tokenAmount = TOKEN_AMOUNT,
                tokenSenderAddress = TOKEN_SENDER_ADDRESS,
                tokenRecipientAddress = TOKEN_RECIPIENT_ADDRESS,
                arbitraryData = ARBITRARY_DATA,
                screenBeforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                screenAfterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE,
                txHash = TX_HASH
            ),
            Erc20SendRequestRecord(
                id = UUID.randomUUID(),
                chainId = CHAIN_ID,
                redirectUrl = REDIRECT_URL,
                tokenAddress = TOKEN_ADDRESS,
                tokenAmount = TOKEN_AMOUNT,
                tokenSenderAddress = TOKEN_SENDER_ADDRESS,
                tokenRecipientAddress = TOKEN_RECIPIENT_ADDRESS,
                arbitraryData = ARBITRARY_DATA,
                screenBeforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                screenAfterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE,
                txHash = TX_HASH
            ),
        )
        val otherRequests = listOf(
            Erc20SendRequestRecord(
                id = UUID.randomUUID(),
                chainId = CHAIN_ID,
                redirectUrl = REDIRECT_URL,
                tokenAddress = TOKEN_ADDRESS,
                tokenAmount = TOKEN_AMOUNT,
                tokenSenderAddress = WalletAddress("dead"),
                tokenRecipientAddress = TOKEN_RECIPIENT_ADDRESS,
                arbitraryData = ARBITRARY_DATA,
                screenBeforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                screenAfterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE,
                txHash = TX_HASH
            ),
            Erc20SendRequestRecord(
                id = UUID.randomUUID(),
                chainId = CHAIN_ID,
                redirectUrl = REDIRECT_URL,
                tokenAddress = TOKEN_ADDRESS,
                tokenAmount = TOKEN_AMOUNT,
                tokenSenderAddress = null,
                tokenRecipientAddress = TOKEN_RECIPIENT_ADDRESS,
                arbitraryData = ARBITRARY_DATA,
                screenBeforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                screenAfterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE,
                txHash = TX_HASH
            )
        )

        suppose("some ERC20 send requests exist in database") {
            dslContext.batchInsert(senderRequests + otherRequests).execute()
        }

        verify("ERC20 send requests are correctly fetched by sender") {
            val result = repository.getBySender(TOKEN_SENDER_ADDRESS)

            assertThat(result).withMessage()
                .containsExactlyInAnyOrderElementsOf(
                    senderRequests.map {
                        Erc20SendRequest(
                            id = it.id!!,
                            chainId = it.chainId!!,
                            redirectUrl = it.redirectUrl!!,
                            tokenAddress = it.tokenAddress!!,
                            tokenAmount = it.tokenAmount!!,
                            tokenSenderAddress = it.tokenSenderAddress,
                            tokenRecipientAddress = it.tokenRecipientAddress!!,
                            txHash = it.txHash,
                            arbitraryData = it.arbitraryData,
                            screenConfig = ScreenConfig(
                                beforeActionMessage = it.screenBeforeActionMessage,
                                afterActionMessage = it.screenAfterActionMessage
                            )
                        )
                    }
                )
        }
    }

    @Test
    fun mustCorrectlyFetchErc20SendRequestsByRecipient() {
        val recipientRequests = listOf(
            Erc20SendRequestRecord(
                id = UUID.randomUUID(),
                chainId = CHAIN_ID,
                redirectUrl = REDIRECT_URL,
                tokenAddress = TOKEN_ADDRESS,
                tokenAmount = TOKEN_AMOUNT,
                tokenSenderAddress = TOKEN_SENDER_ADDRESS,
                tokenRecipientAddress = TOKEN_RECIPIENT_ADDRESS,
                arbitraryData = ARBITRARY_DATA,
                screenBeforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                screenAfterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE,
                txHash = TX_HASH
            ),
            Erc20SendRequestRecord(
                id = UUID.randomUUID(),
                chainId = CHAIN_ID,
                redirectUrl = REDIRECT_URL,
                tokenAddress = TOKEN_ADDRESS,
                tokenAmount = TOKEN_AMOUNT,
                tokenSenderAddress = TOKEN_SENDER_ADDRESS,
                tokenRecipientAddress = TOKEN_RECIPIENT_ADDRESS,
                arbitraryData = ARBITRARY_DATA,
                screenBeforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                screenAfterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE,
                txHash = TX_HASH
            ),
        )
        val otherRequests = listOf(
            Erc20SendRequestRecord(
                id = UUID.randomUUID(),
                chainId = CHAIN_ID,
                redirectUrl = REDIRECT_URL,
                tokenAddress = TOKEN_ADDRESS,
                tokenAmount = TOKEN_AMOUNT,
                tokenSenderAddress = TOKEN_SENDER_ADDRESS,
                tokenRecipientAddress = WalletAddress("dead"),
                arbitraryData = ARBITRARY_DATA,
                screenBeforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                screenAfterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE,
                txHash = TX_HASH
            )
        )

        suppose("some ERC20 send requests exist in database") {
            dslContext.batchInsert(recipientRequests + otherRequests).execute()
        }

        verify("ERC20 send requests are correctly fetched by recipient") {
            val result = repository.getByRecipient(TOKEN_RECIPIENT_ADDRESS)

            assertThat(result).withMessage()
                .containsExactlyInAnyOrderElementsOf(
                    recipientRequests.map {
                        Erc20SendRequest(
                            id = it.id!!,
                            chainId = it.chainId!!,
                            redirectUrl = it.redirectUrl!!,
                            tokenAddress = it.tokenAddress!!,
                            tokenAmount = it.tokenAmount!!,
                            tokenSenderAddress = it.tokenSenderAddress,
                            tokenRecipientAddress = it.tokenRecipientAddress!!,
                            txHash = it.txHash,
                            arbitraryData = it.arbitraryData,
                            screenConfig = ScreenConfig(
                                beforeActionMessage = it.screenBeforeActionMessage,
                                afterActionMessage = it.screenAfterActionMessage
                            )
                        )
                    }
                )
        }
    }

    @Test
    fun mustCorrectlyStoreErc20SendRequest() {
        val id = UUID.randomUUID()
        val params = StoreErc20SendRequestParams(
            id = id,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            tokenAddress = TOKEN_ADDRESS,
            tokenAmount = TOKEN_AMOUNT,
            tokenSenderAddress = TOKEN_SENDER_ADDRESS,
            tokenRecipientAddress = TOKEN_RECIPIENT_ADDRESS,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE
            )
        )

        val storedErc20SendRequest = suppose("ERC20 send request is stored in database") {
            repository.store(params)
        }

        val expectedErc20SendRequest = Erc20SendRequest(
            id = id,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            tokenAddress = TOKEN_ADDRESS,
            tokenAmount = TOKEN_AMOUNT,
            tokenSenderAddress = TOKEN_SENDER_ADDRESS,
            tokenRecipientAddress = TOKEN_RECIPIENT_ADDRESS,
            txHash = null,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE
            )
        )

        verify("storing ERC20 send request returns correct result") {
            assertThat(storedErc20SendRequest).withMessage()
                .isEqualTo(expectedErc20SendRequest)
        }

        verify("ERC20 send request was stored in database") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(expectedErc20SendRequest)
        }
    }

    @Test
    fun mustCorrectlySetTxHashForErc20SendRequestWithNullTxHash() {
        val id = UUID.randomUUID()
        val params = StoreErc20SendRequestParams(
            id = id,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            tokenAddress = TOKEN_ADDRESS,
            tokenAmount = TOKEN_AMOUNT,
            tokenSenderAddress = TOKEN_SENDER_ADDRESS,
            tokenRecipientAddress = TOKEN_RECIPIENT_ADDRESS,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE
            )
        )

        suppose("ERC20 send request is stored in database") {
            repository.store(params)
        }

        verify("setting tx info will succeed") {
            assertThat(repository.setTxInfo(id, TX_HASH, TOKEN_SENDER_ADDRESS)).withMessage()
                .isTrue()
        }

        verify("txHash was correctly set in database") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(
                    Erc20SendRequest(
                        id = id,
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        tokenAddress = TOKEN_ADDRESS,
                        tokenAmount = TOKEN_AMOUNT,
                        tokenSenderAddress = TOKEN_SENDER_ADDRESS,
                        tokenRecipientAddress = TOKEN_RECIPIENT_ADDRESS,
                        txHash = TX_HASH,
                        arbitraryData = ARBITRARY_DATA,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE
                        )
                    )
                )
        }
    }

    @Test
    fun mustNotUpdateTokenSenderAddressForErc20SendRequestWhenTokenSenderIsAlreadySet() {
        val id = UUID.randomUUID()
        val params = StoreErc20SendRequestParams(
            id = id,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            tokenAddress = TOKEN_ADDRESS,
            tokenAmount = TOKEN_AMOUNT,
            tokenSenderAddress = TOKEN_SENDER_ADDRESS,
            tokenRecipientAddress = TOKEN_RECIPIENT_ADDRESS,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE
            )
        )

        suppose("ERC20 send request is stored in database") {
            repository.store(params)
        }

        verify("setting txHash will succeed") {
            val ignoredTokenSender = WalletAddress("f")
            assertThat(repository.setTxInfo(id, TX_HASH, ignoredTokenSender)).withMessage()
                .isTrue()
        }

        verify("txHash was correctly set while token sender was not updated") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(
                    Erc20SendRequest(
                        id = id,
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        tokenAddress = TOKEN_ADDRESS,
                        tokenAmount = TOKEN_AMOUNT,
                        tokenSenderAddress = TOKEN_SENDER_ADDRESS,
                        tokenRecipientAddress = TOKEN_RECIPIENT_ADDRESS,
                        txHash = TX_HASH,
                        arbitraryData = ARBITRARY_DATA,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE
                        )
                    )
                )
        }
    }

    @Test
    fun mustNotSetTxHashForErc20SendRequestWhenTxHashIsAlreadySet() {
        val id = UUID.randomUUID()
        val params = StoreErc20SendRequestParams(
            id = id,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            tokenAddress = TOKEN_ADDRESS,
            tokenAmount = TOKEN_AMOUNT,
            tokenSenderAddress = TOKEN_SENDER_ADDRESS,
            tokenRecipientAddress = TOKEN_RECIPIENT_ADDRESS,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE
            )
        )

        suppose("ERC20 send request is stored in database") {
            repository.store(params)
        }

        verify("setting tx info will succeed") {
            assertThat(repository.setTxInfo(id, TX_HASH, TOKEN_SENDER_ADDRESS)).withMessage()
                .isTrue()
        }

        verify("setting another tx info will not succeed") {
            assertThat(
                repository.setTxInfo(
                    id,
                    TransactionHash("different-tx-hash"),
                    TOKEN_SENDER_ADDRESS
                )
            ).withMessage().isFalse()
        }

        verify("first txHash remains in database") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(
                    Erc20SendRequest(
                        id = id,
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        tokenAddress = TOKEN_ADDRESS,
                        tokenAmount = TOKEN_AMOUNT,
                        tokenSenderAddress = TOKEN_SENDER_ADDRESS,
                        tokenRecipientAddress = TOKEN_RECIPIENT_ADDRESS,
                        txHash = TX_HASH,
                        arbitraryData = ARBITRARY_DATA,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE
                        )
                    )
                )
        }
    }
}
