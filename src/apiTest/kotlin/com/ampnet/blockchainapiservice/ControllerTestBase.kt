package com.ampnet.blockchainapiservice

import com.ampnet.blockchainapiservice.TestBase.Companion.VerifyMessage
import com.ampnet.blockchainapiservice.blockchain.SimpleERC20
import com.ampnet.blockchainapiservice.blockchain.SimpleLockManager
import com.ampnet.blockchainapiservice.exception.ErrorCode
import com.ampnet.blockchainapiservice.exception.ErrorResponse
import com.ampnet.blockchainapiservice.testcontainers.HardhatTestContainer
import com.ampnet.blockchainapiservice.testcontainers.PostgresTestContainer
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.DurationSeconds
import com.ampnet.blockchainapiservice.util.EthereumAddress
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.restdocs.RestDocumentationContextProvider
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation
import org.springframework.restdocs.operation.preprocess.Preprocessors
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.web3j.protocol.core.RemoteCall
import org.web3j.protocol.core.methods.response.TransactionReceipt
import java.util.concurrent.CompletableFuture

@ExtendWith(value = [SpringExtension::class, RestDocumentationExtension::class])
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ControllerTestBase : TestBase() {

    @Suppress("unused")
    protected val postgresContainer = PostgresTestContainer()

    @Suppress("unused")
    protected val hardhatContainer = HardhatTestContainer()

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    protected lateinit var mockMvc: MockMvc

    @BeforeEach
    fun beforeEach(wac: WebApplicationContext, restDocumentation: RestDocumentationContextProvider) {
        hardhatContainer.reset()
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
            .apply<DefaultMockMvcBuilder>(SecurityMockMvcConfigurers.springSecurity())
            .apply<DefaultMockMvcBuilder>(MockMvcRestDocumentation.documentationConfiguration(restDocumentation))
            .alwaysDo<DefaultMockMvcBuilder>(
                MockMvcRestDocumentation.document(
                    "{ClassName}/{methodName}",
                    Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
                    Preprocessors.preprocessResponse(Preprocessors.prettyPrint())
                )
            )
            .build()
    }

    protected fun VerifyMessage.verifyResponseErrorCode(result: MvcResult, expectedErrorCode: ErrorCode) {
        val response: ErrorResponse = objectMapper.readValue(result.response.contentAsString)

        assertThat(response.errorCode).withMessage()
            .isEqualTo(expectedErrorCode)
    }

    protected fun <T> RemoteCall<T>.sendAndMine(): T {
        val future = sendAsync()
        hardhatContainer.waitAndMine()
        return future.get()
    }

    protected fun SimpleERC20.transferAndMine(
        address: EthereumAddress,
        amount: Balance
    ): CompletableFuture<TransactionReceipt?>? {
        val txReceiptFuture = transfer(address.rawValue, amount.rawValue).sendAsync()
        hardhatContainer.mineUntil {
            balanceOf(address.rawValue).send() == amount.rawValue
        }
        return txReceiptFuture
    }

    protected fun SimpleLockManager.lockAndMine(
        tokenAddress: ContractAddress,
        amount: Balance,
        lockDuration: DurationSeconds,
        info: String,
        unlockPrivilegeWallet: EthereumAddress
    ): CompletableFuture<TransactionReceipt?>? {
        val txReceiptFuture = lock(
            tokenAddress.rawValue,
            amount.rawValue,
            lockDuration.rawValue,
            info,
            unlockPrivilegeWallet.rawValue
        ).sendAsync()

        hardhatContainer.mineUntil { isLocked().send() }

        return txReceiptFuture
    }
}
