package dev3.blockchainapiservice.controller

import dev3.blockchainapiservice.ControllerTestBase
import dev3.blockchainapiservice.exception.ErrorCode
import dev3.blockchainapiservice.features.blacklist.model.response.BlacklistedAddressesResponse
import dev3.blockchainapiservice.features.blacklist.repository.BlacklistedAddressRepository
import dev3.blockchainapiservice.security.WithMockUser
import dev3.blockchainapiservice.testcontainers.HardhatTestContainer
import dev3.blockchainapiservice.util.WalletAddress
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

class BlacklistControllerApiTest : ControllerTestBase() {

    companion object {
        private val ADDRESS = WalletAddress("cafebabe")
    }

    @Autowired
    private lateinit var blacklistedAddressRepository: BlacklistedAddressRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        postgresContainer.cleanAllDatabaseTables(dslContext)
    }

    @Test
    @WithMockUser
    fun mustCorrectlyAddAddressToBlacklistForAllowedUser() {
        suppose("request to add address to blacklist is made") {
            mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/blacklist/${ADDRESS.rawValue}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
        }

        verify("address is successfully blacklisted") {
            expectThat(blacklistedAddressRepository.exists(ADDRESS))
                .isTrue()
        }
    }

    @Test
    @WithMockUser(address = HardhatTestContainer.ACCOUNT_ADDRESS_3)
    fun mustReturn403ForbiddenWhenAddingAddressToBlacklistForDisallowedUser() {
        verify("403 is returned for disallowed user") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/blacklist/${ADDRESS.rawValue}")
            )
                .andExpect(MockMvcResultMatchers.status().isForbidden)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.ACCESS_FORBIDDEN)
        }
    }

    @Test
    @WithMockUser
    fun mustCorrectlyRemoveAddressFromBlacklistForAllowedUser() {
        suppose("some address is on blacklist") {
            blacklistedAddressRepository.addAddress(ADDRESS)
        }

        suppose("request to remove address from blacklist is made") {
            mockMvc.perform(
                MockMvcRequestBuilders.delete("/v1/blacklist/${ADDRESS.rawValue}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
        }

        verify("address is successfully removed from blacklist") {
            expectThat(blacklistedAddressRepository.exists(ADDRESS))
                .isFalse()
        }
    }

    @Test
    @WithMockUser(address = HardhatTestContainer.ACCOUNT_ADDRESS_3)
    fun mustReturn403ForbiddenWhenRemovingAddressFromBlacklistForDisallowedUser() {
        verify("403 is returned for disallowed user") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.delete("/v1/blacklist/${ADDRESS.rawValue}")
            )
                .andExpect(MockMvcResultMatchers.status().isForbidden)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.ACCESS_FORBIDDEN)
        }
    }

    @Test
    @WithMockUser
    fun mustCorrectlyListBlacklistedAddressesForAllowedUser() {
        suppose("some address is on blacklist") {
            blacklistedAddressRepository.addAddress(ADDRESS)
        }

        val response = suppose("request to list blacklisted addresses is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/blacklist")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, BlacklistedAddressesResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    BlacklistedAddressesResponse(listOf(ADDRESS.rawValue))
                )
        }
    }

    @Test
    @WithMockUser(address = HardhatTestContainer.ACCOUNT_ADDRESS_3)
    fun mustReturn403ForbiddenWhenListingBlacklistAddressesForDisallowedUser() {
        verify("403 is returned for disallowed user") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/blacklist")
            )
                .andExpect(MockMvcResultMatchers.status().isForbidden)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.ACCESS_FORBIDDEN)
        }
    }
}
