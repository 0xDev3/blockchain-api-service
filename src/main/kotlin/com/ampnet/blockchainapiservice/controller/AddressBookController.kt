package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.config.binding.annotation.UserIdentifierBinding
import com.ampnet.blockchainapiservice.config.validation.ValidEthAddress
import com.ampnet.blockchainapiservice.model.request.CreateOrUpdateAddressBookEntryRequest
import com.ampnet.blockchainapiservice.model.response.AddressBookEntriesResponse
import com.ampnet.blockchainapiservice.model.response.AddressBookEntryResponse
import com.ampnet.blockchainapiservice.model.result.UserIdentifier
import com.ampnet.blockchainapiservice.service.AddressBookService
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import javax.validation.Valid

@Validated
@RestController
class AddressBookController(private val addressBookService: AddressBookService) {

    @PostMapping("/v1/address-book")
    fun createAddressBookEntry(
        @UserIdentifierBinding userIdentifier: UserIdentifier,
        @Valid @RequestBody requestBody: CreateOrUpdateAddressBookEntryRequest
    ): ResponseEntity<AddressBookEntryResponse> {
        return ResponseEntity.ok(
            AddressBookEntryResponse(
                addressBookService.createAddressBookEntry(
                    request = requestBody,
                    userIdentifier = userIdentifier
                )
            )
        )
    }

    @PatchMapping("/v1/address-book/{id}")
    fun updateAddressBookEntry(
        @PathVariable("id") id: UUID,
        @UserIdentifierBinding userIdentifier: UserIdentifier,
        @Valid @RequestBody requestBody: CreateOrUpdateAddressBookEntryRequest
    ): ResponseEntity<AddressBookEntryResponse> {
        return ResponseEntity.ok(
            AddressBookEntryResponse(
                addressBookService.updateAddressBookEntry(
                    addressBookEntryId = id,
                    request = requestBody,
                    userIdentifier = userIdentifier
                )
            )
        )
    }

    @DeleteMapping("/v1/address-book/{id}")
    fun deleteAddressBookEntry(
        @PathVariable("id") id: UUID,
        @UserIdentifierBinding userIdentifier: UserIdentifier
    ) {
        addressBookService.deleteAddressBookEntryById(id, userIdentifier)
    }

    @GetMapping("/v1/address-book/{id}")
    fun getAddressBookEntryById(
        @PathVariable("id") id: UUID
    ): ResponseEntity<AddressBookEntryResponse> {
        return ResponseEntity.ok(AddressBookEntryResponse(addressBookService.getAddressBookEntryById(id = id)))
    }

    @GetMapping("/v1/address-book/by-alias/{alias}")
    fun getAddressBookEntryByAlias(
        @PathVariable("alias") alias: String,
        @UserIdentifierBinding userIdentifier: UserIdentifier
    ): ResponseEntity<AddressBookEntryResponse> {
        return ResponseEntity.ok(
            AddressBookEntryResponse(
                addressBookService.getAddressBookEntryByAlias(
                    alias = alias,
                    userIdentifier = userIdentifier
                )
            )
        )
    }

    @GetMapping("/v1/address-book/by-wallet-address/{walletAddress}")
    fun getAddressBookEntriesForWalletAddress(
        @ValidEthAddress @PathVariable walletAddress: String
    ): ResponseEntity<AddressBookEntriesResponse> {
        return ResponseEntity.ok(
            AddressBookEntriesResponse(
                addressBookService.getAddressBookEntriesByWalletAddress(WalletAddress(walletAddress)).map {
                    AddressBookEntryResponse(it)
                }
            )
        )
    }
}
