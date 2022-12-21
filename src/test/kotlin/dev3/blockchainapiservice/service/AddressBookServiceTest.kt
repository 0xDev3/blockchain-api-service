package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.exception.ResourceNotFoundException
import dev3.blockchainapiservice.model.request.CreateOrUpdateAddressBookEntryRequest
import dev3.blockchainapiservice.model.result.AddressBookEntry
import dev3.blockchainapiservice.model.result.UserWalletAddressIdentifier
import dev3.blockchainapiservice.repository.AddressBookRepository
import dev3.blockchainapiservice.util.WalletAddress
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.util.UUID

class AddressBookServiceTest : TestBase() {

    companion object {
        private val USER_IDENTIFIER = UserWalletAddressIdentifier(
            id = UUID.randomUUID(),
            stripeClientId = null,
            walletAddress = WalletAddress("cafebabe")
        )
        private val ENTRY = AddressBookEntry(
            id = UUID.randomUUID(),
            alias = "alias",
            address = WalletAddress("a"),
            phoneNumber = "phone-number",
            email = "email",
            createdAt = TestData.TIMESTAMP,
            userId = USER_IDENTIFIER.id
        )
    }

    @Test
    fun mustSuccessfullyCreateAddressBookEntry() {
        val uuidProvider = mock<UuidProvider>()

        suppose("some UUID will be returned") {
            call(uuidProvider.getUuid())
                .willReturn(ENTRY.id)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some timestamp will be returned") {
            call(utcDateTimeProvider.getUtcDateTime())
                .willReturn(ENTRY.createdAt)
        }

        val addressBookRepository = mock<AddressBookRepository>()

        suppose("address book entry will be stored into the database") {
            call(addressBookRepository.store(ENTRY))
                .willReturn(ENTRY)
        }

        val service = AddressBookServiceImpl(
            addressBookRepository = addressBookRepository,
            uuidProvider = uuidProvider,
            utcDateTimeProvider = utcDateTimeProvider
        )

        verify("address book entry is stored into the database") {
            val result = service.createAddressBookEntry(
                request = CreateOrUpdateAddressBookEntryRequest(
                    alias = ENTRY.alias,
                    address = ENTRY.address.rawValue,
                    phoneNumber = ENTRY.phoneNumber,
                    email = ENTRY.email
                ),
                userIdentifier = USER_IDENTIFIER
            )

            expectThat(result)
                .isEqualTo(ENTRY)

            expectInteractions(addressBookRepository) {
                once.store(ENTRY)
            }
        }
    }

    @Test
    fun mustCorrectlyUpdateAddressBookEntry() {
        val addressBookRepository = mock<AddressBookRepository>()

        suppose("address book entry is fetched by id") {
            call(addressBookRepository.getById(ENTRY.id))
                .willReturn(ENTRY)
        }

        val updated = ENTRY.copy(
            alias = "new-alias",
            address = WalletAddress("cafebabe"),
            phoneNumber = "new-phone-number",
            email = "new-email"
        )

        suppose("address book entry will be updated in the database") {
            call(addressBookRepository.update(updated))
                .willReturn(updated)
        }

        val service = AddressBookServiceImpl(
            addressBookRepository = addressBookRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = mock()
        )

        verify("address book entry is updated in the database") {
            val result = service.updateAddressBookEntry(
                addressBookEntryId = ENTRY.id,
                request = CreateOrUpdateAddressBookEntryRequest(
                    alias = updated.alias,
                    address = updated.address.rawValue,
                    phoneNumber = updated.phoneNumber,
                    email = updated.email
                ),
                userIdentifier = USER_IDENTIFIER
            )

            expectThat(result)
                .isEqualTo(updated)

            expectInteractions(addressBookRepository) {
                once.getById(ENTRY.id)
                once.update(updated)
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenUpdatingNonOwnedAddressBookEntry() {
        val addressBookRepository = mock<AddressBookRepository>()

        suppose("non-owned address book entry is fetched by id") {
            call(addressBookRepository.getById(ENTRY.id))
                .willReturn(ENTRY.copy(userId = UUID.randomUUID()))
        }

        val service = AddressBookServiceImpl(
            addressBookRepository = addressBookRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.updateAddressBookEntry(
                    addressBookEntryId = ENTRY.id,
                    request = CreateOrUpdateAddressBookEntryRequest(
                        alias = "new-alias",
                        address = "cafebabe",
                        phoneNumber = "new-phone-number",
                        email = "new-email"
                    ),
                    userIdentifier = USER_IDENTIFIER
                )
            }

            expectInteractions(addressBookRepository) {
                once.getById(ENTRY.id)
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenUpdatingNonExistentAddressBookEntry() {
        val addressBookRepository = mock<AddressBookRepository>()

        suppose("address book entry is fetched by id") {
            call(addressBookRepository.getById(ENTRY.id))
                .willReturn(ENTRY)
        }

        val updated = ENTRY.copy(
            alias = "new-alias",
            address = WalletAddress("cafebabe"),
            phoneNumber = "new-phone-number",
            email = "new-email"
        )

        suppose("null will be returned when updating address book entry in the database") {
            call(addressBookRepository.update(updated))
                .willReturn(null)
        }

        val service = AddressBookServiceImpl(
            addressBookRepository = addressBookRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.updateAddressBookEntry(
                    addressBookEntryId = ENTRY.id,
                    request = CreateOrUpdateAddressBookEntryRequest(
                        alias = updated.alias,
                        address = updated.address.rawValue,
                        phoneNumber = updated.phoneNumber,
                        email = updated.email
                    ),
                    userIdentifier = USER_IDENTIFIER
                )
            }

            expectInteractions(addressBookRepository) {
                once.getById(ENTRY.id)
                once.update(updated)
            }
        }
    }

    @Test
    fun mustSuccessfullyDeleteAddressBookEntry() {
        val addressBookRepository = mock<AddressBookRepository>()

        suppose("address book entry is fetched by id") {
            call(addressBookRepository.getById(ENTRY.id))
                .willReturn(ENTRY)
        }

        suppose("address book entry is deleted by id") {
            call(addressBookRepository.delete(ENTRY.id))
                .willReturn(true)
        }

        val service = AddressBookServiceImpl(
            addressBookRepository = addressBookRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = mock()
        )

        verify("address book entry is correctly deleted by id") {
            service.deleteAddressBookEntryById(ENTRY.id, USER_IDENTIFIER)

            expectInteractions(addressBookRepository) {
                once.getById(ENTRY.id)
                once.delete(ENTRY.id)
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenDeletingNonOwnedAddressBookEntry() {
        val addressBookRepository = mock<AddressBookRepository>()

        suppose("non-owned address book entry is fetched by id") {
            call(addressBookRepository.getById(ENTRY.id))
                .willReturn(ENTRY.copy(userId = UUID.randomUUID()))
        }

        suppose("address book entry is deleted by id") {
            call(addressBookRepository.delete(ENTRY.id))
                .willReturn(true)
        }

        val service = AddressBookServiceImpl(
            addressBookRepository = addressBookRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.deleteAddressBookEntryById(ENTRY.id, USER_IDENTIFIER)
            }

            expectInteractions(addressBookRepository) {
                once.getById(ENTRY.id)
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenDeletingNonExistentAddressBookEntry() {
        val addressBookRepository = mock<AddressBookRepository>()

        suppose("null is returned when fetching address book entry by id") {
            call(addressBookRepository.getById(ENTRY.id))
                .willReturn(null)
        }

        val service = AddressBookServiceImpl(
            addressBookRepository = addressBookRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.deleteAddressBookEntryById(ENTRY.id, USER_IDENTIFIER)
            }

            expectInteractions(addressBookRepository) {
                once.getById(ENTRY.id)
            }
        }
    }

    @Test
    fun mustSuccessfullyGetAddressBookEntryById() {
        val addressBookRepository = mock<AddressBookRepository>()

        suppose("address book entry is fetched by id") {
            call(addressBookRepository.getById(ENTRY.id))
                .willReturn(ENTRY)
        }

        val service = AddressBookServiceImpl(
            addressBookRepository = addressBookRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = mock()
        )

        verify("address book entry is correctly fetched by id") {
            val result = service.getAddressBookEntryById(ENTRY.id)

            expectThat(result)
                .isEqualTo(ENTRY)
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenFetchingNonExistentAddressBookEntryById() {
        val addressBookRepository = mock<AddressBookRepository>()

        suppose("null is returned when fetching address book entry by id") {
            call(addressBookRepository.getById(ENTRY.id))
                .willReturn(null)
        }

        val service = AddressBookServiceImpl(
            addressBookRepository = addressBookRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.getAddressBookEntryById(ENTRY.id)
            }
        }
    }

    @Test
    fun mustSuccessfullyGetAddressBookEntryByAlias() {
        val addressBookRepository = mock<AddressBookRepository>()

        suppose("address book entry is fetched by alias") {
            call(addressBookRepository.getByAliasAndUserId(ENTRY.alias, ENTRY.userId))
                .willReturn(ENTRY)
        }

        val service = AddressBookServiceImpl(
            addressBookRepository = addressBookRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = mock()
        )

        verify("address book entry is correctly fetched by alias") {
            val result = service.getAddressBookEntryByAlias(ENTRY.alias, USER_IDENTIFIER)

            expectThat(result)
                .isEqualTo(ENTRY)
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenFetchingNonExistentAddressBookEntryByAlias() {
        val addressBookRepository = mock<AddressBookRepository>()

        suppose("null is returned when fetching address book entry by alias") {
            call(addressBookRepository.getByAliasAndUserId(ENTRY.alias, ENTRY.userId))
                .willReturn(null)
        }

        val service = AddressBookServiceImpl(
            addressBookRepository = addressBookRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.getAddressBookEntryByAlias(ENTRY.alias, USER_IDENTIFIER)
            }
        }
    }

    @Test
    fun mustSuccessfullyGetAddressBookEntriesByWalletAddress() {
        val addressBookRepository = mock<AddressBookRepository>()

        suppose("address book entries is fetched by wallet address") {
            call(addressBookRepository.getAllByWalletAddress(USER_IDENTIFIER.walletAddress))
                .willReturn(listOf(ENTRY))
        }

        val service = AddressBookServiceImpl(
            addressBookRepository = addressBookRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = mock()
        )

        verify("address book entries are correctly fetched by wallet address") {
            val result = service.getAddressBookEntriesByWalletAddress(USER_IDENTIFIER.walletAddress)

            expectThat(result)
                .isEqualTo(listOf(ENTRY))
        }
    }
}
