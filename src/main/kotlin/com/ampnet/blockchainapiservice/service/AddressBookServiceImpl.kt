package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.exception.ResourceNotFoundException
import com.ampnet.blockchainapiservice.model.request.CreateOrUpdateAddressBookEntryRequest
import com.ampnet.blockchainapiservice.model.result.AddressBookEntry
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.repository.AddressBookRepository
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
        project: Project
    ): AddressBookEntry {
        logger.info { "Creating address book entry, request: $request, project: $project" }
        return addressBookRepository.store(
            AddressBookEntry(
                id = uuidProvider.getUuid(),
                createdAt = utcDateTimeProvider.getUtcDateTime(),
                request = request,
                project = project
            )
        )
    }

    override fun updateAddressBookEntry(
        addressBookEntryId: UUID,
        request: CreateOrUpdateAddressBookEntryRequest,
        project: Project
    ): AddressBookEntry {
        logger.info {
            "Update address book entry, addressBookEntryId: $addressBookEntryId, request: $request, project: $project"
        }
        val entry = getAddressBookEntryById(addressBookEntryId, project)
        return addressBookRepository.update(
            AddressBookEntry(
                id = entry.id,
                createdAt = entry.createdAt,
                request = request,
                project = project
            )
        ) ?: throw ResourceNotFoundException("Address book entry not found for ID: $addressBookEntryId")
    }

    override fun deleteAddressBookEntryById(id: UUID) {
        logger.info { "Delete address book entry by id: $id" }
        addressBookRepository.delete(id)
    }

    override fun getAddressBookEntryById(id: UUID, project: Project): AddressBookEntry {
        logger.debug { "Get address book entry by id: $id" }
        return addressBookRepository.getById(id)?.takeIf {
            it.projectId == project.id
        } ?: throw ResourceNotFoundException("Address book entry not found for ID: $id")
    }

    override fun getAddressBookEntryByAlias(alias: String, project: Project): AddressBookEntry {
        logger.debug { "Get address book entry by alias: $alias" }
        return addressBookRepository.getByAliasAndProjectId(alias, project.id)
            ?: throw ResourceNotFoundException("Address book entry not found for alias: $alias")
    }

    override fun getAddressBookEntriesByProjectId(projectId: UUID): List<AddressBookEntry> {
        logger.debug { "Get address book entries by projectId: $projectId" }
        return addressBookRepository.getAllByProjectId(projectId)
    }
}
