package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.exception.ResourceNotFoundException
import dev3.blockchainapiservice.model.request.CreateOrUpdateAddressBookEntryRequest
import dev3.blockchainapiservice.model.result.AddressBookEntry
import dev3.blockchainapiservice.model.result.UserIdentifier
import dev3.blockchainapiservice.repository.AddressBookRepository
import dev3.blockchainapiservice.util.WalletAddress
import mu.KLogging
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AddressBookServiceImpl(
    private val addressBookRepository: AddressBookRepository,
    private val uuidProvider: UuidProvider,
    private val utcDateTimeProvider: UtcDateTimeProvider
) : AddressBookService {

    companion object : KLogging()

    override fun createAddressBookEntry(
        request: CreateOrUpdateAddressBookEntryRequest,
        userIdentifier: UserIdentifier
    ): AddressBookEntry {
        logger.info { "Creating address book entry, request: $request, userIdentifier: $userIdentifier" }
        return addressBookRepository.store(
            AddressBookEntry(
                id = uuidProvider.getUuid(),
                createdAt = utcDateTimeProvider.getUtcDateTime(),
                request = request,
                userIdentifier = userIdentifier
            )
        )
    }

    override fun updateAddressBookEntry(
        addressBookEntryId: UUID,
        request: CreateOrUpdateAddressBookEntryRequest,
        userIdentifier: UserIdentifier
    ): AddressBookEntry {
        logger.info {
            "Update address book entry, addressBookEntryId: $addressBookEntryId, request: $request," +
                " userIdentifier: $userIdentifier"
        }
        val entry = getOwnedAddressBookEntryById(addressBookEntryId, userIdentifier)
        return addressBookRepository.update(
            AddressBookEntry(
                id = entry.id,
                createdAt = entry.createdAt,
                request = request,
                userIdentifier = userIdentifier
            )
        ) ?: throw ResourceNotFoundException("Address book entry not found for ID: $addressBookEntryId")
    }

    override fun deleteAddressBookEntryById(id: UUID, userIdentifier: UserIdentifier) {
        logger.info { "Delete address book entry by id: $id, userIdentifier: $userIdentifier" }
        addressBookRepository.delete(getOwnedAddressBookEntryById(id, userIdentifier).id)
    }

    override fun getAddressBookEntryById(id: UUID): AddressBookEntry {
        logger.debug { "Get address book entry by id: $id" }
        return addressBookRepository.getById(id)
            ?: throw ResourceNotFoundException("Address book entry not found for ID: $id")
    }

    override fun getAddressBookEntryByAlias(alias: String, userIdentifier: UserIdentifier): AddressBookEntry {
        logger.debug { "Get address book entry by alias: $alias, userIdentifier: $userIdentifier" }
        return addressBookRepository.getByAliasAndUserId(alias, userIdentifier.id)
            ?: throw ResourceNotFoundException("Address book entry not found for alias: $alias")
    }

    override fun getAddressBookEntriesByWalletAddress(walletAddress: WalletAddress): List<AddressBookEntry> {
        logger.debug { "Get address book entries by walletAddress: $walletAddress" }
        return addressBookRepository.getAllByWalletAddress(walletAddress)
    }

    private fun getOwnedAddressBookEntryById(id: UUID, userIdentifier: UserIdentifier) =
        getAddressBookEntryById(id).takeIf { it.userId == userIdentifier.id }
            ?: throw ResourceNotFoundException("Address book entry not found for ID: $id")
}
