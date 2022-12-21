package dev3.blockchainapiservice.repository

import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.features.blacklist.repository.JooqBlacklistedAddressRepository
import dev3.blockchainapiservice.generated.jooq.tables.BlacklistedAddressTable
import dev3.blockchainapiservice.testcontainers.SharedTestContainers
import dev3.blockchainapiservice.util.WalletAddress
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext

@JooqTest
@Import(JooqBlacklistedAddressRepository::class)
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JooqBlacklistedAddressRepositoryIntegTest : TestBase() {

    @Suppress("unused")
    private val postgresContainer = SharedTestContainers.postgresContainer

    @Autowired
    private lateinit var repository: JooqBlacklistedAddressRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        postgresContainer.cleanAllDatabaseTables(dslContext)
    }

    @Test
    fun mustCorrectlyAddAddressToBlacklist() {
        val address = WalletAddress("abc")

        suppose("some address is added to blacklist") {
            repository.addAddress(address)
        }

        verify("address is correctly added to the database") {
            val result = dslContext.fetchExists(
                BlacklistedAddressTable,
                BlacklistedAddressTable.WALLET_ADDRESS.eq(address)
            )

            expectThat(result)
                .isTrue()
        }
    }

    @Test
    fun mustCorrectlyAddAddressToBlacklistMultipleTimes() {
        val address = WalletAddress("abc")

        suppose("some address is added to blacklist") {
            repository.addAddress(address)
        }

        suppose("same address is added to blacklist again") {
            repository.addAddress(address)
        }

        verify("address is correctly added to the database") {
            val result = dslContext.fetchExists(
                BlacklistedAddressTable,
                BlacklistedAddressTable.WALLET_ADDRESS.eq(address)
            )

            expectThat(result)
                .isTrue()
        }
    }

    @Test
    fun mustCorrectlyRemoveAddressFromBlacklist() {
        val address = WalletAddress("abc")

        suppose("some address is added to blacklist") {
            repository.addAddress(address)
        }

        suppose("some address is removed from blacklist") {
            repository.removeAddress(address)
        }

        verify("address is correctly removed from the database") {
            val result = dslContext.fetchExists(
                BlacklistedAddressTable,
                BlacklistedAddressTable.WALLET_ADDRESS.eq(address)
            )

            expectThat(result)
                .isFalse()
        }
    }

    @Test
    fun mustCorrectlyCheckIfAddressExistsOnBlacklist() {
        val address = WalletAddress("abc")

        suppose("some address is added to blacklist") {
            repository.addAddress(address)
        }

        verify("address exists in the database") {
            val result = repository.exists(address)

            expectThat(result)
                .isTrue()
        }

        verify("some other address does not exist in the database") {
            val result = repository.exists(WalletAddress("dead"))

            expectThat(result)
                .isFalse()
        }
    }

    @Test
    fun mustCorrectlyListBlacklistedAddresses() {
        val address = WalletAddress("abc")

        suppose("some address is added to blacklist") {
            repository.addAddress(address)
        }

        verify("address is correctly listed from database") {
            val result = repository.listAddresses()

            expectThat(result)
                .isEqualTo(listOf(address))
        }
    }
}
