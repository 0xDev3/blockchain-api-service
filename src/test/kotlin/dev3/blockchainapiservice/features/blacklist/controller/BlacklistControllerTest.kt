package dev3.blockchainapiservice.features.blacklist.controller

import dev3.blockchainapiservice.JsonSchemaDocumentation
import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.features.api.access.model.result.UserWalletAddressIdentifier
import dev3.blockchainapiservice.features.blacklist.model.response.BlacklistedAddressesResponse
import dev3.blockchainapiservice.features.blacklist.service.BlacklistService
import dev3.blockchainapiservice.generated.jooq.id.UserId
import dev3.blockchainapiservice.util.WalletAddress
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.http.ResponseEntity
import java.util.UUID

class BlacklistControllerTest : TestBase() {

    companion object {
        private val USER_IDENTIFIER = UserWalletAddressIdentifier(
            id = UserId(UUID.randomUUID()),
            stripeClientId = null,
            walletAddress = WalletAddress("abc")
        )
        private val ADDRESS = WalletAddress("cafebabe")
    }

    @Test
    fun mustCorrectlyAddAddressToBlacklist() {
        val service = mock<BlacklistService>()
        val controller = BlacklistController(service)

        verify("service is correctly called") {
            controller.addAddress(USER_IDENTIFIER, ADDRESS.rawValue)

            expectInteractions(service) {
                once.addAddress(USER_IDENTIFIER, ADDRESS)
            }
        }
    }

    @Test
    fun mustCorrectlyRemoveAddressFromBlacklist() {
        val service = mock<BlacklistService>()
        val controller = BlacklistController(service)

        verify("service is correctly called") {
            controller.removeAddress(USER_IDENTIFIER, ADDRESS.rawValue)

            expectInteractions(service) {
                once.removeAddress(USER_IDENTIFIER, ADDRESS)
            }
        }
    }

    @Test
    fun mustCorrectlyListBlacklistedAddresses() {
        val service = mock<BlacklistService>()

        suppose("some blacklisted addresses will be returned") {
            call(service.listAddresses(USER_IDENTIFIER))
                .willReturn(listOf(ADDRESS))
        }

        val controller = BlacklistController(service)

        verify("controller returns correct response") {
            val response = controller.listAddresses(USER_IDENTIFIER)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        BlacklistedAddressesResponse(
                            listOf(ADDRESS.rawValue)
                        )
                    )
                )
        }
    }
}
