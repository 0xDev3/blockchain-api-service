package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.exception.ResourceNotFoundException
import com.ampnet.blockchainapiservice.model.request.CreateOrUpdateAddressBookEntryRequest
import com.ampnet.blockchainapiservice.model.result.AddressBookEntry
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.repository.AddressBookRepository
import com.ampnet.blockchainapiservice.util.BaseUrl
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoMoreInteractions
import java.util.UUID
import org.mockito.kotlin.verify as verifyMock

class AddressBookServiceTest : TestBase() {

    companion object {
        private val PROJECT = Project(
            id = UUID.randomUUID(),
            ownerId = UUID.randomUUID(),
            issuerContractAddress = ContractAddress("0"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = ChainId(1337L),
            customRpcUrl = "custom-rpc-url",
            createdAt = TestData.TIMESTAMP
        )
        private val ENTRY = AddressBookEntry(
            id = UUID.randomUUID(),
            alias = "alias",
            address = WalletAddress("a"),
            phoneNumber = "phone-number",
            email = "email",
            createdAt = TestData.TIMESTAMP,
            projectId = PROJECT.id
        )
    }

    @Test
    fun mustSuccessfullyCreateAddressBookEntry() {
        val uuidProvider = mock<UuidProvider>()

        suppose("some UUID will be returned") {
            given(uuidProvider.getUuid())
                .willReturn(ENTRY.id)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some timestamp will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(ENTRY.createdAt)
        }

        val addressBookRepository = mock<AddressBookRepository>()

        suppose("address book entry will be stored into the database") {
            given(addressBookRepository.store(ENTRY))
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
                project = PROJECT
            )

            assertThat(result).withMessage()
                .isEqualTo(ENTRY)

            verifyMock(addressBookRepository)
                .store(ENTRY)
            verifyNoMoreInteractions(addressBookRepository)
        }
    }

    @Test
    fun mustCorrectlyUpdateAddressBookEntry() {
        val addressBookRepository = mock<AddressBookRepository>()

        suppose("address book entry is fetched by id") {
            given(addressBookRepository.getById(ENTRY.id))
                .willReturn(ENTRY)
        }

        val updated = ENTRY.copy(
            alias = "new-alias",
            address = WalletAddress("cafebabe"),
            phoneNumber = "new-phone-number",
            email = "new-email"
        )

        suppose("address book entry will be updated in the database") {
            given(addressBookRepository.update(updated))
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
                project = PROJECT
            )

            assertThat(result).withMessage()
                .isEqualTo(updated)

            verifyMock(addressBookRepository)
                .getById(ENTRY.id)
            verifyMock(addressBookRepository)
                .update(updated)
            verifyNoMoreInteractions(addressBookRepository)
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenUpdatingNonOwnedAddressBookEntry() {
        val addressBookRepository = mock<AddressBookRepository>()

        suppose("non-owned address book entry is fetched by id") {
            given(addressBookRepository.getById(ENTRY.id))
                .willReturn(ENTRY.copy(projectId = UUID.randomUUID()))
        }

        val service = AddressBookServiceImpl(
            addressBookRepository = addressBookRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.updateAddressBookEntry(
                    addressBookEntryId = ENTRY.id,
                    request = CreateOrUpdateAddressBookEntryRequest(
                        alias = "new-alias",
                        address = "cafebabe",
                        phoneNumber = "new-phone-number",
                        email = "new-email"
                    ),
                    project = PROJECT
                )
            }

            verifyMock(addressBookRepository)
                .getById(ENTRY.id)
            verifyNoMoreInteractions(addressBookRepository)
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenUpdatingNonExistentAddressBookEntry() {
        val addressBookRepository = mock<AddressBookRepository>()

        suppose("address book entry is fetched by id") {
            given(addressBookRepository.getById(ENTRY.id))
                .willReturn(ENTRY)
        }

        val updated = ENTRY.copy(
            alias = "new-alias",
            address = WalletAddress("cafebabe"),
            phoneNumber = "new-phone-number",
            email = "new-email"
        )

        suppose("null will be returned when updating address book entry in the database") {
            given(addressBookRepository.update(updated))
                .willReturn(null)
        }

        val service = AddressBookServiceImpl(
            addressBookRepository = addressBookRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.updateAddressBookEntry(
                    addressBookEntryId = ENTRY.id,
                    request = CreateOrUpdateAddressBookEntryRequest(
                        alias = updated.alias,
                        address = updated.address.rawValue,
                        phoneNumber = updated.phoneNumber,
                        email = updated.email
                    ),
                    project = PROJECT
                )
            }

            verifyMock(addressBookRepository)
                .getById(ENTRY.id)
            verifyMock(addressBookRepository)
                .update(updated)
            verifyNoMoreInteractions(addressBookRepository)
        }
    }

    @Test
    fun mustSuccessfullyDeleteAddressBookEntry() {
        val addressBookRepository = mock<AddressBookRepository>()

        suppose("address book entry is deleted by id") {
            given(addressBookRepository.delete(ENTRY.id))
                .willReturn(true)
        }

        val service = AddressBookServiceImpl(
            addressBookRepository = addressBookRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = mock()
        )

        verify("address book entry is correctly deleted by id") {
            service.deleteAddressBookEntryById(ENTRY.id)

            verifyMock(addressBookRepository)
                .delete(ENTRY.id)
            verifyNoMoreInteractions(addressBookRepository)
        }
    }

    @Test
    fun mustSuccessfullyGetAddressBookEntryById() {
        val addressBookRepository = mock<AddressBookRepository>()

        suppose("address book entry is fetched by id") {
            given(addressBookRepository.getById(ENTRY.id))
                .willReturn(ENTRY)
        }

        val service = AddressBookServiceImpl(
            addressBookRepository = addressBookRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = mock()
        )

        verify("address book entry is correctly fetched by id") {
            val result = service.getAddressBookEntryById(ENTRY.id, PROJECT)

            assertThat(result).withMessage()
                .isEqualTo(ENTRY)
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenFetchingNonExistentAddressBookEntryById() {
        val addressBookRepository = mock<AddressBookRepository>()

        suppose("null is returned when fetching address book entry by id") {
            given(addressBookRepository.getById(ENTRY.id))
                .willReturn(null)
        }

        val service = AddressBookServiceImpl(
            addressBookRepository = addressBookRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.getAddressBookEntryById(ENTRY.id, PROJECT)
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenFetchingNonOwnedAddressBookEntryById() {
        val addressBookRepository = mock<AddressBookRepository>()

        suppose("non-owned address book entry is fetched by id") {
            given(addressBookRepository.getById(ENTRY.id))
                .willReturn(ENTRY.copy(projectId = UUID.randomUUID()))
        }

        val service = AddressBookServiceImpl(
            addressBookRepository = addressBookRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.getAddressBookEntryById(ENTRY.id, PROJECT)
            }
        }
    }

    @Test
    fun mustSuccessfullyGetAddressBookEntryByAlias() {
        val addressBookRepository = mock<AddressBookRepository>()

        suppose("address book entry is fetched by alias") {
            given(addressBookRepository.getByAliasAndProjectId(ENTRY.alias, ENTRY.projectId))
                .willReturn(ENTRY)
        }

        val service = AddressBookServiceImpl(
            addressBookRepository = addressBookRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = mock()
        )

        verify("address book entry is correctly fetched by alias") {
            val result = service.getAddressBookEntryByAlias(ENTRY.alias, PROJECT)

            assertThat(result).withMessage()
                .isEqualTo(ENTRY)
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenFetchingNonExistentAddressBookEntryByAlias() {
        val addressBookRepository = mock<AddressBookRepository>()

        suppose("null is returned when fetching address book entry by alias") {
            given(addressBookRepository.getByAliasAndProjectId(ENTRY.alias, ENTRY.projectId))
                .willReturn(null)
        }

        val service = AddressBookServiceImpl(
            addressBookRepository = addressBookRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.getAddressBookEntryByAlias(ENTRY.alias, PROJECT)
            }
        }
    }

    @Test
    fun mustSuccessfullyGetAddressBookEntriesByProjectId() {
        val addressBookRepository = mock<AddressBookRepository>()

        suppose("address book entries is fetched by project id") {
            given(addressBookRepository.getAllByProjectId(ENTRY.projectId))
                .willReturn(listOf(ENTRY))
        }

        val service = AddressBookServiceImpl(
            addressBookRepository = addressBookRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = mock()
        )

        verify("address book entries are correctly fetched by project id") {
            val result = service.getAddressBookEntriesByProjectId(PROJECT.id)

            assertThat(result).withMessage()
                .isEqualTo(listOf(ENTRY))
        }
    }
}
