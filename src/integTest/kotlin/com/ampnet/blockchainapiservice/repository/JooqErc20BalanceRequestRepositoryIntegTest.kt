package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.config.JsonConfig
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.Erc20BalanceRequestRecord
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.params.StoreErc20BalanceRequestParams
import com.ampnet.blockchainapiservice.model.result.Erc20BalanceRequest
import com.ampnet.blockchainapiservice.testcontainers.PostgresTestContainer
import com.ampnet.blockchainapiservice.util.BlockNumber
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.SignedMessage
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
@Import(JooqErc20BalanceRequestRepository::class, JsonConfig::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JooqErc20BalanceRequestRepositoryIntegTest : TestBase() {

    companion object {
        private val CHAIN_ID = ChainId(1337L)
        private const val REDIRECT_URL = "redirect-url"
        private val TOKEN_ADDRESS = ContractAddress("a")
        private val BLOCK_NUMBER = BlockNumber(BigInteger.valueOf(123L))
        private val REQUESTED_WALLET_ADDRESS = WalletAddress("b")
        private const val ARBITRARY_DATA = "{}"
        private const val BALANCE_SCREEN_BEFORE_ACTION_MESSAGE = "balance-screen-before-action-message"
        private const val BALANCE_SCREEN_AFTER_ACTION_MESSAGE = "balance-screen-after-action-message"
        private val ACTUAL_WALLET_ADDRESS = WalletAddress("c")
        private val SIGNED_MESSAGE = SignedMessage("signed-message")
    }

    @Suppress("unused")
    private val postgresContainer = PostgresTestContainer()

    @Autowired
    private lateinit var repository: JooqErc20BalanceRequestRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun mustCorrectlyFetchErc20BalanceRequestById() {
        val id = UUID.randomUUID()

        suppose("some ERC20 balance request exists in database") {
            dslContext.executeInsert(
                Erc20BalanceRequestRecord(
                    id = id,
                    chainId = CHAIN_ID.value,
                    redirectUrl = REDIRECT_URL,
                    tokenAddress = TOKEN_ADDRESS.rawValue,
                    blockNumber = BLOCK_NUMBER.value,
                    requestedWalletAddress = REQUESTED_WALLET_ADDRESS.rawValue,
                    arbitraryData = JSON.valueOf(ARBITRARY_DATA),
                    balanceScreenBeforeActionMessage = BALANCE_SCREEN_BEFORE_ACTION_MESSAGE,
                    balanceScreenAfterActionMessage = BALANCE_SCREEN_AFTER_ACTION_MESSAGE,
                    actualWalletAddress = ACTUAL_WALLET_ADDRESS.rawValue,
                    signedMessage = SIGNED_MESSAGE.value
                )
            )
        }

        verify("ERC20 balance request is correctly fetched by ID") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(
                    Erc20BalanceRequest(
                        id = id,
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        tokenAddress = TOKEN_ADDRESS,
                        blockNumber = BLOCK_NUMBER,
                        requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
                        actualWalletAddress = ACTUAL_WALLET_ADDRESS,
                        signedMessage = SIGNED_MESSAGE,
                        arbitraryData = objectMapper.readTree(ARBITRARY_DATA),
                        screenConfig = ScreenConfig(
                            beforeActionMessage = BALANCE_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = BALANCE_SCREEN_AFTER_ACTION_MESSAGE
                        )
                    )
                )
        }
    }

    @Test
    fun mustReturnNullWhenFetchingNonExistentErc20BalanceRequestById() {
        verify("null is returned when fetching non-existent ERC20 balance request") {
            val result = repository.getById(UUID.randomUUID())

            assertThat(result).withMessage()
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyStoreErc20BalanceRequest() {
        val id = UUID.randomUUID()
        val params = StoreErc20BalanceRequestParams(
            id = id,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            tokenAddress = TOKEN_ADDRESS,
            blockNumber = BLOCK_NUMBER,
            requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
            arbitraryData = objectMapper.readTree(ARBITRARY_DATA),
            screenConfig = ScreenConfig(
                beforeActionMessage = BALANCE_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = BALANCE_SCREEN_AFTER_ACTION_MESSAGE
            )
        )

        val storedErc20BalanceRequest = suppose("ERC20 balance request is stored in database") {
            repository.store(params)
        }

        val expectedErc20BalanceRequest = Erc20BalanceRequest(
            id = id,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            tokenAddress = TOKEN_ADDRESS,
            blockNumber = BLOCK_NUMBER,
            requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
            actualWalletAddress = null,
            signedMessage = null,
            arbitraryData = objectMapper.readTree(ARBITRARY_DATA),
            screenConfig = ScreenConfig(
                beforeActionMessage = BALANCE_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = BALANCE_SCREEN_AFTER_ACTION_MESSAGE
            )
        )

        verify("storing ERC20 balance request returns correct result") {
            assertThat(storedErc20BalanceRequest).withMessage()
                .isEqualTo(expectedErc20BalanceRequest)
        }

        verify("ERC20 balance request was stored in database") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(expectedErc20BalanceRequest)
        }
    }

    @Test
    fun mustCorrectlySetSignedMessageForErc20BalanceRequestWithNullWalletAddressAndSignedMessage() {
        val id = UUID.randomUUID()
        val params = StoreErc20BalanceRequestParams(
            id = id,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            tokenAddress = TOKEN_ADDRESS,
            blockNumber = BLOCK_NUMBER,
            requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
            arbitraryData = objectMapper.readTree(ARBITRARY_DATA),
            screenConfig = ScreenConfig(
                beforeActionMessage = BALANCE_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = BALANCE_SCREEN_AFTER_ACTION_MESSAGE
            )
        )

        suppose("ERC20 balance request is stored in database") {
            repository.store(params)
        }

        verify("setting walletAddress and signedMessage will succeed") {
            assertThat(repository.setSignedMessage(id, ACTUAL_WALLET_ADDRESS, SIGNED_MESSAGE)).withMessage()
                .isTrue()
        }

        verify("walletAddress and signedMessage were correctly set in database") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(
                    Erc20BalanceRequest(
                        id = id,
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        tokenAddress = TOKEN_ADDRESS,
                        blockNumber = BLOCK_NUMBER,
                        requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
                        actualWalletAddress = ACTUAL_WALLET_ADDRESS,
                        signedMessage = SIGNED_MESSAGE,
                        arbitraryData = objectMapper.readTree(ARBITRARY_DATA),
                        screenConfig = ScreenConfig(
                            beforeActionMessage = BALANCE_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = BALANCE_SCREEN_AFTER_ACTION_MESSAGE
                        )
                    )
                )
        }
    }

    @Test
    fun mustNotSetWalletAddressAndSignedMessageForErc20BalanceRequestWhenWalletAddressAndSignedMessageAreAlreadySet() {
        val id = UUID.randomUUID()
        val params = StoreErc20BalanceRequestParams(
            id = id,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            tokenAddress = TOKEN_ADDRESS,
            blockNumber = BLOCK_NUMBER,
            requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
            arbitraryData = objectMapper.readTree(ARBITRARY_DATA),
            screenConfig = ScreenConfig(
                beforeActionMessage = BALANCE_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = BALANCE_SCREEN_AFTER_ACTION_MESSAGE
            )
        )

        suppose("ERC20 balance request is stored in database") {
            repository.store(params)
        }

        verify("setting walletAddress and signedMessage will succeed") {
            assertThat(repository.setSignedMessage(id, ACTUAL_WALLET_ADDRESS, SIGNED_MESSAGE)).withMessage()
                .isTrue()
        }

        verify("setting another walletAddress and signedMessage will not succeed") {
            assertThat(repository.setSignedMessage(id, WalletAddress("dead"), SignedMessage("another-message")))
                .withMessage()
                .isFalse()
        }

        verify("first walletAddress and signedMessage remain in database") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(
                    Erc20BalanceRequest(
                        id = id,
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        tokenAddress = TOKEN_ADDRESS,
                        blockNumber = BLOCK_NUMBER,
                        requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
                        actualWalletAddress = ACTUAL_WALLET_ADDRESS,
                        signedMessage = SIGNED_MESSAGE,
                        arbitraryData = objectMapper.readTree(ARBITRARY_DATA),
                        screenConfig = ScreenConfig(
                            beforeActionMessage = BALANCE_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = BALANCE_SCREEN_AFTER_ACTION_MESSAGE
                        )
                    )
                )
        }
    }
}
