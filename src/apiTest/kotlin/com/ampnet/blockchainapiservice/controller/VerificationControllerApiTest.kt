package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.ControllerTestBase
import com.ampnet.blockchainapiservice.model.response.GenerateVerificationMessageResponse
import com.ampnet.blockchainapiservice.model.result.UnsignedVerificationMessage
import com.ampnet.blockchainapiservice.repository.UnsignedVerificationMessageRepository
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import com.ampnet.blockchainapiservice.generated.jooq.tables.UnsignedVerificationMessage as UnsignedVerificationMessageTable

class VerificationControllerApiTest : ControllerTestBase() {

    @Autowired
    private lateinit var unsignedVerificationMessageRepository: UnsignedVerificationMessageRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        dslContext.deleteFrom(UnsignedVerificationMessageTable.UNSIGNED_VERIFICATION_MESSAGE).execute()
    }

    @Test
    fun mustCorrectlyGenerateVerificationMessageAndStoreItInDatabase() {
        val generatedMessage = suppose("request to generate verification message is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/verification/${walletAddress.rawValue}/generate")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, GenerateVerificationMessageResponse::class.java)
        }

        val databaseMessage = unsignedVerificationMessageRepository.getById(generatedMessage.id)

        verify("generated verification message is present in database") {
            assertThat(databaseMessage).withMessage()
                .isNotNull()
                .isEqualTo(
                    UnsignedVerificationMessage(
                        id = databaseMessage!!.id,
                        walletAddress = walletAddress,
                        createdAt = databaseMessage.createdAt,
                        validUntil = databaseMessage.validUntil
                    )
                )
        }

        verify("generated response message has correct payload") {
            assertThat(generatedMessage).withMessage()
                .isEqualTo(
                    GenerateVerificationMessageResponse(
                        id = databaseMessage!!.id,
                        message = databaseMessage.toStringMessage(),
                        validUntil = databaseMessage.validUntil.value
                    )
                )
        }
    }
}
