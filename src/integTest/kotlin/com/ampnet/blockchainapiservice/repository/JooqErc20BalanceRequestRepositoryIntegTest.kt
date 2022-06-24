package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.generated.jooq.enums.UserIdentifierType
import com.ampnet.blockchainapiservice.generated.jooq.tables.ApiKeyTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.ProjectTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.UserIdentifierTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.Erc20BalanceRequestRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ProjectRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.UserIdentifierRecord
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.params.StoreErc20BalanceRequestParams
import com.ampnet.blockchainapiservice.model.result.Erc20BalanceRequest
import com.ampnet.blockchainapiservice.testcontainers.PostgresTestContainer
import com.ampnet.blockchainapiservice.util.BaseUrl
import com.ampnet.blockchainapiservice.util.BlockNumber
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.SignedMessage
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.Import
import java.math.BigInteger
import java.util.UUID

@JooqTest
@Import(JooqErc20BalanceRequestRepository::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JooqErc20BalanceRequestRepositoryIntegTest : TestBase() {

    companion object {
        private val CHAIN_ID = ChainId(1337L)
        private const val REDIRECT_URL = "redirect-url"
        private val TOKEN_ADDRESS = ContractAddress("a")
        private val BLOCK_NUMBER = BlockNumber(BigInteger.valueOf(123L))
        private val REQUESTED_WALLET_ADDRESS = WalletAddress("b")
        private val ARBITRARY_DATA = TestData.EMPTY_JSON_OBJECT
        private const val BALANCE_SCREEN_BEFORE_ACTION_MESSAGE = "balance-screen-before-action-message"
        private const val BALANCE_SCREEN_AFTER_ACTION_MESSAGE = "balance-screen-after-action-message"
        private val ACTUAL_WALLET_ADDRESS = WalletAddress("c")
        private val SIGNED_MESSAGE = SignedMessage("signed-message")
        private val PROJECT_ID = UUID.randomUUID()
        private val OWNER_ID = UUID.randomUUID()
    }

    @Suppress("unused")
    private val postgresContainer = PostgresTestContainer()

    @Autowired
    private lateinit var repository: JooqErc20BalanceRequestRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        dslContext.delete(ApiKeyTable.API_KEY).execute()
        dslContext.delete(ProjectTable.PROJECT).execute()
        dslContext.delete(UserIdentifierTable.USER_IDENTIFIER).execute()

        dslContext.executeInsert(
            UserIdentifierRecord(
                id = OWNER_ID,
                userIdentifier = "user-identifier",
                identifierType = UserIdentifierType.ETH_WALLET_ADDRESS
            )
        )

        dslContext.executeInsert(
            ProjectRecord(
                id = PROJECT_ID,
                ownerId = OWNER_ID,
                issuerContractAddress = ContractAddress("0"),
                baseRedirectUrl = BaseUrl("base-redirect-url"),
                chainId = ChainId(1337L),
                customRpcUrl = "custom-rpc-url",
                createdAt = TestData.TIMESTAMP
            )
        )
    }

    @Test
    fun mustCorrectlyFetchErc20BalanceRequestById() {
        val id = UUID.randomUUID()

        suppose("some ERC20 balance request exists in database") {
            dslContext.executeInsert(
                Erc20BalanceRequestRecord(
                    id = id,
                    projectId = PROJECT_ID,
                    chainId = CHAIN_ID,
                    redirectUrl = REDIRECT_URL,
                    tokenAddress = TOKEN_ADDRESS,
                    blockNumber = BLOCK_NUMBER,
                    requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
                    arbitraryData = ARBITRARY_DATA,
                    screenBeforeActionMessage = BALANCE_SCREEN_BEFORE_ACTION_MESSAGE,
                    screenAfterActionMessage = BALANCE_SCREEN_AFTER_ACTION_MESSAGE,
                    actualWalletAddress = ACTUAL_WALLET_ADDRESS,
                    signedMessage = SIGNED_MESSAGE,
                    createdAt = TestData.TIMESTAMP
                )
            )
        }

        verify("ERC20 balance request is correctly fetched by ID") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(
                    Erc20BalanceRequest(
                        id = id,
                        projectId = PROJECT_ID,
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        tokenAddress = TOKEN_ADDRESS,
                        blockNumber = BLOCK_NUMBER,
                        requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
                        actualWalletAddress = ACTUAL_WALLET_ADDRESS,
                        signedMessage = SIGNED_MESSAGE,
                        arbitraryData = ARBITRARY_DATA,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = BALANCE_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = BALANCE_SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        createdAt = TestData.TIMESTAMP
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
    fun mustCorrectlyFetchErc20LockRequestsByProject() {
        val otherProjectId = UUID.randomUUID()

        suppose("some other project is in database") {
            dslContext.executeInsert(
                ProjectRecord(
                    id = otherProjectId,
                    ownerId = OWNER_ID,
                    issuerContractAddress = ContractAddress("1"),
                    baseRedirectUrl = BaseUrl("base-redirect-url"),
                    chainId = ChainId(1337L),
                    customRpcUrl = "custom-rpc-url",
                    createdAt = TestData.TIMESTAMP
                )
            )
        }

        val projectRequests = listOf(
            Erc20BalanceRequestRecord(
                id = UUID.randomUUID(),
                projectId = PROJECT_ID,
                chainId = CHAIN_ID,
                redirectUrl = REDIRECT_URL,
                tokenAddress = TOKEN_ADDRESS,
                blockNumber = BLOCK_NUMBER,
                requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
                arbitraryData = ARBITRARY_DATA,
                screenBeforeActionMessage = BALANCE_SCREEN_BEFORE_ACTION_MESSAGE,
                screenAfterActionMessage = BALANCE_SCREEN_AFTER_ACTION_MESSAGE,
                actualWalletAddress = ACTUAL_WALLET_ADDRESS,
                signedMessage = SIGNED_MESSAGE,
                createdAt = TestData.TIMESTAMP
            ),
            Erc20BalanceRequestRecord(
                id = UUID.randomUUID(),
                projectId = PROJECT_ID,
                chainId = CHAIN_ID,
                redirectUrl = REDIRECT_URL,
                tokenAddress = TOKEN_ADDRESS,
                blockNumber = BLOCK_NUMBER,
                requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
                arbitraryData = ARBITRARY_DATA,
                screenBeforeActionMessage = BALANCE_SCREEN_BEFORE_ACTION_MESSAGE,
                screenAfterActionMessage = BALANCE_SCREEN_AFTER_ACTION_MESSAGE,
                actualWalletAddress = ACTUAL_WALLET_ADDRESS,
                signedMessage = SIGNED_MESSAGE,
                createdAt = TestData.TIMESTAMP
            )
        )
        val otherRequests = listOf(
            Erc20BalanceRequestRecord(
                id = UUID.randomUUID(),
                projectId = otherProjectId,
                chainId = CHAIN_ID,
                redirectUrl = REDIRECT_URL,
                tokenAddress = TOKEN_ADDRESS,
                blockNumber = BLOCK_NUMBER,
                requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
                arbitraryData = ARBITRARY_DATA,
                screenBeforeActionMessage = BALANCE_SCREEN_BEFORE_ACTION_MESSAGE,
                screenAfterActionMessage = BALANCE_SCREEN_AFTER_ACTION_MESSAGE,
                actualWalletAddress = ACTUAL_WALLET_ADDRESS,
                signedMessage = SIGNED_MESSAGE,
                createdAt = TestData.TIMESTAMP
            ),
            Erc20BalanceRequestRecord(
                id = UUID.randomUUID(),
                projectId = otherProjectId,
                chainId = CHAIN_ID,
                redirectUrl = REDIRECT_URL,
                tokenAddress = TOKEN_ADDRESS,
                blockNumber = BLOCK_NUMBER,
                requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
                arbitraryData = ARBITRARY_DATA,
                screenBeforeActionMessage = BALANCE_SCREEN_BEFORE_ACTION_MESSAGE,
                screenAfterActionMessage = BALANCE_SCREEN_AFTER_ACTION_MESSAGE,
                actualWalletAddress = ACTUAL_WALLET_ADDRESS,
                signedMessage = SIGNED_MESSAGE,
                createdAt = TestData.TIMESTAMP
            )
        )

        suppose("some ERC20 balance requests exist in database") {
            dslContext.batchInsert(projectRequests + otherRequests).execute()
        }

        verify("ERC20 balance requests are correctly fetched by project") {
            val result = repository.getAllByProjectId(PROJECT_ID)

            assertThat(result).withMessage()
                .containsExactlyInAnyOrderElementsOf(
                    projectRequests.map {
                        Erc20BalanceRequest(
                            id = it.id!!,
                            projectId = it.projectId!!,
                            chainId = it.chainId!!,
                            redirectUrl = it.redirectUrl!!,
                            tokenAddress = it.tokenAddress!!,
                            blockNumber = it.blockNumber,
                            requestedWalletAddress = it.requestedWalletAddress,
                            actualWalletAddress = it.actualWalletAddress,
                            signedMessage = it.signedMessage,
                            arbitraryData = it.arbitraryData,
                            screenConfig = ScreenConfig(
                                beforeActionMessage = it.screenBeforeActionMessage,
                                afterActionMessage = it.screenAfterActionMessage
                            ),
                            createdAt = it.createdAt!!
                        )
                    }
                )
        }
    }

    @Test
    fun mustCorrectlyStoreErc20BalanceRequest() {
        val id = UUID.randomUUID()
        val params = StoreErc20BalanceRequestParams(
            id = id,
            projectId = PROJECT_ID,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            tokenAddress = TOKEN_ADDRESS,
            blockNumber = BLOCK_NUMBER,
            requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = BALANCE_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = BALANCE_SCREEN_AFTER_ACTION_MESSAGE
            ),
            createdAt = TestData.TIMESTAMP
        )

        val storedErc20BalanceRequest = suppose("ERC20 balance request is stored in database") {
            repository.store(params)
        }

        val expectedErc20BalanceRequest = Erc20BalanceRequest(
            id = id,
            projectId = PROJECT_ID,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            tokenAddress = TOKEN_ADDRESS,
            blockNumber = BLOCK_NUMBER,
            requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
            actualWalletAddress = null,
            signedMessage = null,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = BALANCE_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = BALANCE_SCREEN_AFTER_ACTION_MESSAGE
            ),
            createdAt = TestData.TIMESTAMP
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
            projectId = PROJECT_ID,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            tokenAddress = TOKEN_ADDRESS,
            blockNumber = BLOCK_NUMBER,
            requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = BALANCE_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = BALANCE_SCREEN_AFTER_ACTION_MESSAGE
            ),
            createdAt = TestData.TIMESTAMP
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
                        projectId = PROJECT_ID,
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        tokenAddress = TOKEN_ADDRESS,
                        blockNumber = BLOCK_NUMBER,
                        requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
                        actualWalletAddress = ACTUAL_WALLET_ADDRESS,
                        signedMessage = SIGNED_MESSAGE,
                        arbitraryData = ARBITRARY_DATA,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = BALANCE_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = BALANCE_SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }
    }

    @Test
    fun mustNotSetWalletAddressAndSignedMessageForErc20BalanceRequestWhenWalletAddressAndSignedMessageAreAlreadySet() {
        val id = UUID.randomUUID()
        val params = StoreErc20BalanceRequestParams(
            id = id,
            projectId = PROJECT_ID,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            tokenAddress = TOKEN_ADDRESS,
            blockNumber = BLOCK_NUMBER,
            requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = BALANCE_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = BALANCE_SCREEN_AFTER_ACTION_MESSAGE
            ),
            createdAt = TestData.TIMESTAMP
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
                        projectId = PROJECT_ID,
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        tokenAddress = TOKEN_ADDRESS,
                        blockNumber = BLOCK_NUMBER,
                        requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
                        actualWalletAddress = ACTUAL_WALLET_ADDRESS,
                        signedMessage = SIGNED_MESSAGE,
                        arbitraryData = ARBITRARY_DATA,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = BALANCE_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = BALANCE_SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }
    }
}
