package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.generated.jooq.enums.UserIdentifierType
import com.ampnet.blockchainapiservice.generated.jooq.tables.ApiKeyTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.ProjectTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.UserIdentifierTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.UserIdentifierRecord
import com.ampnet.blockchainapiservice.model.result.UserWalletAddressIdentifier
import com.ampnet.blockchainapiservice.testcontainers.PostgresTestContainer
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.Import
import java.util.UUID

@JooqTest
@Import(JooqUserIdentifierRepository::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JooqUserIdentifierRepositoryIntegTest : TestBase() {

    companion object {
        private val USER_WALLET_ADDRESS = WalletAddress("fade")
        private val IDENTIFIER_TYPE = UserIdentifierType.ETH_WALLET_ADDRESS
    }

    @Suppress("unused")
    private val postgresContainer = PostgresTestContainer()

    @Autowired
    private lateinit var repository: JooqUserIdentifierRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        dslContext.delete(ApiKeyTable.API_KEY).execute()
        dslContext.delete(ProjectTable.PROJECT).execute()
        dslContext.delete(UserIdentifierTable.USER_IDENTIFIER).execute()
    }

    @Test
    fun mustCorrectlyFetchUserIdentifierById() {
        val id = UUID.randomUUID()

        suppose("some user identifier is stored in database") {
            dslContext.executeInsert(
                UserIdentifierRecord(
                    id = id,
                    userIdentifier = USER_WALLET_ADDRESS.rawValue,
                    identifierType = IDENTIFIER_TYPE
                )
            )
        }

        verify("user identifier is correctly fetched by ID") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(
                    UserWalletAddressIdentifier(
                        id = id,
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
                    identifierType = IDENTIFIER_TYPE
                )
            )
        }

        verify("user identifier is correctly fetched by identifier") {
            val result = repository.getByUserIdentifier(USER_WALLET_ADDRESS.rawValue, IDENTIFIER_TYPE)

            assertThat(result).withMessage()
                .isEqualTo(
                    UserWalletAddressIdentifier(
                        id = id,
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
                    identifierType = IDENTIFIER_TYPE
                )
            )
        }

        verify("user identifier is correctly fetched by identifier") {
            val result = repository.getByWalletAddress(walletAddress)

            assertThat(result).withMessage()
                .isEqualTo(
                    UserWalletAddressIdentifier(
                        id = id,
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
}
