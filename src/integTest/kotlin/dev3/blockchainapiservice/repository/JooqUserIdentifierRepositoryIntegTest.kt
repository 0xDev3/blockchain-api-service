package dev3.blockchainapiservice.repository

import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.generated.jooq.enums.UserIdentifierType
import dev3.blockchainapiservice.generated.jooq.tables.records.UserIdentifierRecord
import dev3.blockchainapiservice.model.result.UserWalletAddressIdentifier
import dev3.blockchainapiservice.testcontainers.SharedTestContainers
import dev3.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import java.util.UUID

@JooqTest
@Import(JooqUserIdentifierRepository::class)
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JooqUserIdentifierRepositoryIntegTest : TestBase() {

    companion object {
        private val USER_WALLET_ADDRESS = WalletAddress("fade")
        private val IDENTIFIER_TYPE = UserIdentifierType.ETH_WALLET_ADDRESS
    }

    @Suppress("unused")
    private val postgresContainer = SharedTestContainers.postgresContainer

    @Autowired
    private lateinit var repository: JooqUserIdentifierRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        postgresContainer.cleanAllDatabaseTables(dslContext)
    }

    @Test
    fun mustCorrectlyFetchUserIdentifierById() {
        val id = UUID.randomUUID()

        suppose("some user identifier is stored in database") {
            dslContext.executeInsert(
                UserIdentifierRecord(
                    id = id,
                    userIdentifier = USER_WALLET_ADDRESS.rawValue,
                    identifierType = IDENTIFIER_TYPE,
                    stripeClientId = null
                )
            )
        }

        verify("user identifier is correctly fetched by ID") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(
                    UserWalletAddressIdentifier(
                        id = id,
                        stripeClientId = null,
                        walletAddress = USER_WALLET_ADDRESS
                    )
                )
        }
    }

    @Test
    fun mustReturnNullWhenFetchingNonExistentUserIdentifierById() {
        verify("null is returned when fetching non-existent user identifier") {
            val result = repository.getById(UUID.randomUUID())

            assertThat(result).withMessage()
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyFetchUserIdentifierByIdentifier() {
        val id = UUID.randomUUID()

        suppose("some user identifier is stored in database") {
            dslContext.executeInsert(
                UserIdentifierRecord(
                    id = id,
                    userIdentifier = USER_WALLET_ADDRESS.rawValue,
                    identifierType = IDENTIFIER_TYPE,
                    stripeClientId = null
                )
            )
        }

        verify("user identifier is correctly fetched by identifier") {
            val result = repository.getByUserIdentifier(USER_WALLET_ADDRESS.rawValue, IDENTIFIER_TYPE)

            assertThat(result).withMessage()
                .isEqualTo(
                    UserWalletAddressIdentifier(
                        id = id,
                        stripeClientId = null,
                        walletAddress = USER_WALLET_ADDRESS
                    )
                )
        }
    }

    @Test
    fun mustReturnNullWhenFetchingNonExistentUserIdentifierByIdentifier() {
        verify("null is returned when fetching non-existent user identifier") {
            val result = repository.getByUserIdentifier("???", IDENTIFIER_TYPE)

            assertThat(result).withMessage()
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyFetchUserIdentifierByWalletAddress() {
        val id = UUID.randomUUID()
        val walletAddress = WalletAddress("0cafe0babe")

        suppose("some user identifier is stored in database") {
            dslContext.executeInsert(
                UserIdentifierRecord(
                    id = id,
                    userIdentifier = walletAddress.rawValue,
                    identifierType = IDENTIFIER_TYPE,
                    stripeClientId = null
                )
            )
        }

        verify("user identifier is correctly fetched by identifier") {
            val result = repository.getByWalletAddress(walletAddress)

            assertThat(result).withMessage()
                .isEqualTo(
                    UserWalletAddressIdentifier(
                        id = id,
                        stripeClientId = null,
                        walletAddress = walletAddress
                    )
                )
        }
    }

    @Test
    fun mustReturnNullWhenFetchingNonExistentUserIdentifierByWalletAddress() {
        verify("null is returned when fetching non-existent user identifier") {
            val result = repository.getByWalletAddress(WalletAddress("dead"))

            assertThat(result).withMessage()
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyStoreUserIdentifier() {
        val id = UUID.randomUUID()
        val userIdentifier = UserWalletAddressIdentifier(
            id = id,
            stripeClientId = null,
            walletAddress = USER_WALLET_ADDRESS
        )

        val storedUserIdentifier = suppose("user identifier is stored in database") {
            repository.store(userIdentifier)
        }

        verify("storing user identifier returns correct result") {
            assertThat(storedUserIdentifier).withMessage()
                .isEqualTo(userIdentifier)
        }

        verify("user identifier was stored in database") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(userIdentifier)
        }
    }

    @Test
    fun mustCorrectlySetStripeClientId() {
        val id = UUID.randomUUID()
        val userIdentifier = UserWalletAddressIdentifier(
            id = id,
            stripeClientId = null,
            walletAddress = USER_WALLET_ADDRESS
        )

        suppose("user identifier is stored in database") {
            repository.store(userIdentifier)
        }

        val stripeClientId = "stripe-client-id"

        val updateStatus = suppose("stripe client id is set in the database") {
            repository.setStripeClientId(id, stripeClientId)
        }

        verify("stripe client id was stored in database") {
            assertThat(updateStatus).withMessage()
                .isTrue()

            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(userIdentifier.copy(stripeClientId = stripeClientId))
        }
    }
}
