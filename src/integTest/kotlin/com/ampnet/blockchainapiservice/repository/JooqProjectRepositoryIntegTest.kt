package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.generated.jooq.enums.UserIdentifierType
import com.ampnet.blockchainapiservice.generated.jooq.tables.ProjectTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.UserIdentifierTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ProjectRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.UserIdentifierRecord
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.testcontainers.PostgresTestContainer
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.Import
import java.util.UUID

@JooqTest
@Import(JooqProjectRepository::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JooqProjectRepositoryIntegTest : TestBase() {

    companion object {
        private val OWNER_ID = UUID.randomUUID()
        private val ISSUER_CONTRACT_ADDRESS = ContractAddress("1550e4")
        private const val REDIRECT_URL = "redirect-url"
        private val CHAIN_ID = ChainId(1337L)
        private const val CUSTOM_RPC_URL = "custom-rpc-url"
    }

    @Suppress("unused")
    private val postgresContainer = PostgresTestContainer()

    @Autowired
    private lateinit var repository: JooqProjectRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        dslContext.delete(UserIdentifierTable.USER_IDENTIFIER).execute()
        dslContext.delete(ProjectTable.PROJECT).execute()

        dslContext.executeInsert(
            UserIdentifierRecord(
                id = OWNER_ID,
                userIdentifier = "user-identifier",
                identifierType = UserIdentifierType.ETH_WALLET_ADDRESS
            )
        )
    }

    @Test
    fun mustCorrectlyFetchProjectById() {
        val id = UUID.randomUUID()

        suppose("some project is stored in database") {
            dslContext.executeInsert(
                ProjectRecord(
                    id = id,
                    ownerId = OWNER_ID,
                    issuerContractAddress = ISSUER_CONTRACT_ADDRESS,
                    redirectUrl = REDIRECT_URL,
                    chainId = CHAIN_ID,
                    customRpcUrl = CUSTOM_RPC_URL
                )
            )
        }

        verify("project is correctly fetched by ID") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(
                    Project(
                        id = id,
                        ownerId = OWNER_ID,
                        issuerContractAddress = ISSUER_CONTRACT_ADDRESS,
                        redirectUrl = REDIRECT_URL,
                        chainId = CHAIN_ID,
                        customRpcUrl = CUSTOM_RPC_URL
                    )
                )
        }
    }

    @Test
    fun mustReturnNullWhenFetchingNonExistentProjectById() {
        verify("null is returned when fetching non-existent project") {
            val result = repository.getById(UUID.randomUUID())

            assertThat(result).withMessage()
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyStoreProject() {
        val id = UUID.randomUUID()
        val project = Project(
            id = id,
            ownerId = OWNER_ID,
            issuerContractAddress = ISSUER_CONTRACT_ADDRESS,
            redirectUrl = REDIRECT_URL,
            chainId = CHAIN_ID,
            customRpcUrl = CUSTOM_RPC_URL
        )

        val storedProject = suppose("project is stored in database") {
            repository.store(project)
        }

        verify("storing project returns correct result") {
            assertThat(storedProject).withMessage()
                .isEqualTo(project)
        }

        verify("project was stored in database") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(project)
        }
    }
}
