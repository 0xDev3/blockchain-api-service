package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.exception.AliasAlreadyInUseException
import com.ampnet.blockchainapiservice.generated.jooq.enums.UserIdentifierType
import com.ampnet.blockchainapiservice.generated.jooq.tables.interfaces.IAddressBookRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.AddressBookRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.UserIdentifierRecord
import com.ampnet.blockchainapiservice.model.result.AddressBookEntry
import com.ampnet.blockchainapiservice.testcontainers.SharedTestContainers
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.Import
import java.time.Duration
import java.util.UUID

@JooqTest
@Import(JooqAddressBookRepository::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JooqAddressBookRepositoryIntegTest : TestBase() {

    companion object {
        private const val ALIAS = "ALIAS"
        private val ADDRESS = WalletAddress("a")
        private const val PHONE_NUMBER = "phone-number"
        private const val EMAIL = "email"
        private val OWNER_ID = UUID.randomUUID()
        private val OWNER_ADDRESS = WalletAddress("cafebabe")
    }

    @Suppress("unused")
    private val postgresContainer = SharedTestContainers.postgresContainer

    @Autowired
    private lateinit var repository: JooqAddressBookRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        postgresContainer.cleanAllDatabaseTables(dslContext)

        dslContext.executeInsert(
            UserIdentifierRecord(
                id = OWNER_ID,
                userIdentifier = OWNER_ADDRESS.rawValue,
                identifierType = UserIdentifierType.ETH_WALLET_ADDRESS
            )
        )
    }

    @Test
    fun mustCorrectlyFetchAddressBookEntryById() {
        val record = AddressBookRecord(
            id = UUID.randomUUID(),
            alias = ALIAS,
            walletAddress = ADDRESS,
            phoneNumber = PHONE_NUMBER,
            email = EMAIL,
            createdAt = TestData.TIMESTAMP,
            userId = OWNER_ID
        )

        suppose("some address book entry is stored in database") {
            dslContext.executeInsert(record)
        }

        verify("address book entry is correctly fetched by ID") {
            val result = repository.getById(record.id!!)

            assertThat(result).withMessage()
                .isEqualTo(record.toModel())
        }
    }

    @Test
    fun mustCorrectlyFetchAddressBookEntryByAliasAndUserId() {
        val record = AddressBookRecord(
            id = UUID.randomUUID(),
            alias = ALIAS,
            walletAddress = ADDRESS,
            phoneNumber = PHONE_NUMBER,
            email = EMAIL,
            createdAt = TestData.TIMESTAMP,
            userId = OWNER_ID
        )

        suppose("some address book entry is stored in database") {
            dslContext.executeInsert(record)
        }

        verify("address book entry is correctly fetched by alias and user ID") {
            val result = repository.getByAliasAndUserId(ALIAS, OWNER_ID)

            assertThat(result).withMessage()
                .isEqualTo(record.toModel())
        }
    }

    @Test
    fun mustCorrectlyFetchAddressBookEntriesByWalletAddress() {
        val records = listOf(
            AddressBookRecord(
                id = UUID.randomUUID(),
                alias = "alias-1",
                walletAddress = WalletAddress("a"),
                phoneNumber = "phone-number-1",
                email = "email-1",
                createdAt = TestData.TIMESTAMP,
                userId = OWNER_ID
            ),
            AddressBookRecord(
                id = UUID.randomUUID(),
                alias = "alias-2",
                walletAddress = WalletAddress("b"),
                phoneNumber = "phone-number-2",
                email = "email-2",
                createdAt = TestData.TIMESTAMP + Duration.ofSeconds(1L),
                userId = OWNER_ID
            )
        )

        suppose("some address book entries are stored in database") {
            dslContext.batchInsert(records).execute()
        }

        verify("address book entries are correctly fetched by wallet address") {
            val result = repository.getAllByWalletAddress(OWNER_ADDRESS)

            assertThat(result).withMessage()
                .isEqualTo(records.map { it.toModel() })
        }
    }

    @Test
    fun mustCorrectlyStoreAddressBookEntry() {
        val addressBookEntry = AddressBookEntry(
            id = UUID.randomUUID(),
            alias = ALIAS,
            address = ADDRESS,
            phoneNumber = PHONE_NUMBER,
            email = EMAIL,
            createdAt = TestData.TIMESTAMP,
            userId = OWNER_ID
        )

        val storedAddressBookEntry = suppose("address book entry is stored in database") {
            repository.store(addressBookEntry)
        }

        verify("storing address book entry returns correct result") {
            assertThat(storedAddressBookEntry).withMessage()
                .isEqualTo(addressBookEntry)
        }

        verify("address book entry was stored in database") {
            val result = repository.getById(addressBookEntry.id)

            assertThat(result).withMessage()
                .isEqualTo(addressBookEntry)
        }

        verify("storing address book entry with conflicting alias throws AliasAlreadyInUseException") {
            assertThrows<AliasAlreadyInUseException>(message) {
                repository.store(addressBookEntry.copy(id = UUID.randomUUID()))
            }
        }
    }

    @Test
    fun mustCorrectlyUpdateAddressBookEntry() {
        val addressBookEntry = AddressBookEntry(
            id = UUID.randomUUID(),
            alias = ALIAS,
            address = ADDRESS,
            phoneNumber = PHONE_NUMBER,
            email = EMAIL,
            createdAt = TestData.TIMESTAMP,
            userId = OWNER_ID
        )

        suppose("address book entry is stored in database") {
            repository.store(addressBookEntry)
        }

        val nonNullUpdates = AddressBookEntry(
            id = addressBookEntry.id,
            alias = "new-alias",
            address = WalletAddress("cafe0babe1"),
            phoneNumber = "new-phone-number",
            email = "new-email",
            createdAt = TestData.TIMESTAMP + Duration.ofSeconds(1L),
            userId = OWNER_ID
        )

        val updatedNonNullAddressBookEntry = suppose("address book entry is updated in database") {
            repository.update(nonNullUpdates)
        }

        verify("updating address book entry returns correct result") {
            assertThat(updatedNonNullAddressBookEntry).withMessage()
                .isEqualTo(nonNullUpdates.copy(createdAt = addressBookEntry.createdAt))
        }

        verify("address book entry was updated in database") {
            val result = repository.getById(addressBookEntry.id)

            assertThat(result).withMessage()
                .isEqualTo(nonNullUpdates.copy(createdAt = addressBookEntry.createdAt))
        }

        val nullUpdates = nonNullUpdates.copy(
            phoneNumber = null,
            email = null,
            createdAt = TestData.TIMESTAMP + Duration.ofSeconds(1L)
        )

        val updatedNullAddressBookEntry = suppose("address book entry is updated in database with null values") {
            repository.update(nullUpdates)
        }

        verify("updating address book entry with null values returns correct result") {
            assertThat(updatedNullAddressBookEntry).withMessage()
                .isEqualTo(nullUpdates.copy(createdAt = addressBookEntry.createdAt))
        }

        verify("address book entry was updated in database with null values") {
            val result = repository.getById(addressBookEntry.id)

            assertThat(result).withMessage()
                .isEqualTo(nullUpdates.copy(createdAt = addressBookEntry.createdAt))
        }

        val otherEntry = AddressBookEntry(
            id = UUID.randomUUID(),
            alias = "other-alias",
            address = WalletAddress("c"),
            phoneNumber = "other-phone-number",
            email = "other-email",
            createdAt = TestData.TIMESTAMP,
            userId = OWNER_ID
        )

        suppose("other address book entry is stored in database") {
            repository.store(otherEntry)
        }

        verify("updating entry to have same alias as other entry throws AliasAlreadyInUseException") {
            assertThrows<AliasAlreadyInUseException>(message) {
                repository.update(otherEntry.copy(alias = "new-alias"))
            }
        }
    }

    private fun IAddressBookRecord.toModel() =
        AddressBookEntry(
            id = id!!,
            alias = alias!!,
            address = walletAddress!!,
            phoneNumber = phoneNumber,
            email = email,
            createdAt = createdAt!!,
            userId = userId!!
        )
}
