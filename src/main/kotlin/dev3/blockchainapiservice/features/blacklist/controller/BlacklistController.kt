package dev3.blockchainapiservice.features.blacklist.controller

import dev3.blockchainapiservice.config.binding.annotation.UserIdentifierBinding
import dev3.blockchainapiservice.config.validation.ValidEthAddress
import dev3.blockchainapiservice.features.blacklist.model.response.BlacklistedAddressesResponse
import dev3.blockchainapiservice.features.blacklist.service.BlacklistService
import dev3.blockchainapiservice.model.result.UserIdentifier
import dev3.blockchainapiservice.util.WalletAddress
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
class BlacklistController(private val blacklistService: BlacklistService) {

    @PostMapping("/v1/blacklist/{address}")
    fun addAddress(
        @UserIdentifierBinding userIdentifier: UserIdentifier,
        @ValidEthAddress @PathVariable address: String
    ) {
        blacklistService.addAddress(userIdentifier, WalletAddress(address))
    }

    @DeleteMapping("/v1/blacklist/{address}")
    fun removeAddress(
        @UserIdentifierBinding userIdentifier: UserIdentifier,
        @ValidEthAddress @PathVariable address: String
    ) {
        blacklistService.removeAddress(userIdentifier, WalletAddress(address))
    }

    @GetMapping("/v1/blacklist")
    fun listAddresses(
        @UserIdentifierBinding userIdentifier: UserIdentifier
    ): ResponseEntity<BlacklistedAddressesResponse> =
        ResponseEntity.ok(
            BlacklistedAddressesResponse(
                blacklistService.listAddresses(userIdentifier).map { it.rawValue }
            )
        )
}
