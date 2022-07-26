package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.config.binding.annotation.ApiKeyBinding
import com.ampnet.blockchainapiservice.model.request.CreateOrUpdateAddressBookEntryRequest
import com.ampnet.blockchainapiservice.model.response.AddressBookEntriesResponse
import com.ampnet.blockchainapiservice.model.response.AddressBookEntryResponse
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.service.AddressBookService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class AddressBookController(private val addressBookService: AddressBookService) {

    @PostMapping("/v1/address-book")
    fun createAddressBookEntry(
        @ApiKeyBinding project: Project,
        @RequestBody requestBody: CreateOrUpdateAddressBookEntryRequest
    ): ResponseEntity<AddressBookEntryResponse> {
        return ResponseEntity.ok(
            AddressBookEntryResponse(
                addressBookService.createAddressBookEntry(
                    request = requestBody,
                    project = project
                )
            )
        )
    }

    @PatchMapping("/v1/address-book/{id}")
    fun updateAddressBookEntry(
        @PathVariable("id") id: UUID,
        @ApiKeyBinding project: Project,
        @RequestBody requestBody: CreateOrUpdateAddressBookEntryRequest
    ): ResponseEntity<AddressBookEntryResponse> {
        return ResponseEntity.ok(
            AddressBookEntryResponse(
                addressBookService.updateAddressBookEntry(
                    addressBookEntryId = id,
                    request = requestBody,
                    project = project
                )
            )
        )
    }

    @DeleteMapping("/v1/address-book/{id}")
    fun deleteAddressBookEntry(
        @PathVariable("id") id: UUID,
        @ApiKeyBinding project: Project
    ) {
        addressBookService.deleteAddressBookEntryById(id, project)
    }

    @GetMapping("/v1/address-book/{id}")
    fun getAddressBookEntryById(
        @PathVariable("id") id: UUID,
        @ApiKeyBinding project: Project
    ): ResponseEntity<AddressBookEntryResponse> {
        return ResponseEntity.ok(
            AddressBookEntryResponse(
                addressBookService.getAddressBookEntryById(
                    id = id,
                    project = project
                )
            )
        )
    }

    @GetMapping("/v1/address-book/by-alias/{alias}")
    fun getAddressBookEntryByAlias(
        @PathVariable("alias") alias: String,
        @ApiKeyBinding project: Project
    ): ResponseEntity<AddressBookEntryResponse> {
        return ResponseEntity.ok(
            AddressBookEntryResponse(
                addressBookService.getAddressBookEntryByAlias(
                    alias = alias,
                    project = project
                )
            )
        )
    }

    @GetMapping("/v1/address-book")
    fun getAddressBookEntriesForProject(
        @ApiKeyBinding project: Project
    ): ResponseEntity<AddressBookEntriesResponse> {
        return ResponseEntity.ok(
            AddressBookEntriesResponse(
                addressBookService.getAddressBookEntriesByProjectId(project.id).map {
                    AddressBookEntryResponse(it)
                }
            )
        )
    }
}
