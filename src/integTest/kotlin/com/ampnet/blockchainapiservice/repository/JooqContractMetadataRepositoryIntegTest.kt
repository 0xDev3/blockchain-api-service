package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.generated.jooq.tables.ContractMetadataTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ContractMetadataRecord
import com.ampnet.blockchainapiservice.model.result.ContractMetadata
import com.ampnet.blockchainapiservice.testcontainers.SharedTestContainers
import com.ampnet.blockchainapiservice.util.ContractId
import com.ampnet.blockchainapiservice.util.ContractTag
import com.ampnet.blockchainapiservice.util.InterfaceId
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import java.util.UUID

@JooqTest
@Import(JooqContractMetadataRepository::class)
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JooqContractMetadataRepositoryIntegTest : TestBase() {

    @Suppress("unused")
    private val postgresContainer = SharedTestContainers.postgresContainer

    @Autowired
    private lateinit var repository: JooqContractMetadataRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        postgresContainer.cleanAllDatabaseTables(dslContext)
    }

    @Test
    fun mustCorrectlyCreateContractMetadata() {
        val id = UUID.randomUUID()
        val name = "name"
        val description = "description"
        val contractId = ContractId("cid")
        val contractTags = listOf(ContractTag("tag"))
        val contractImplements = listOf(InterfaceId("trait"))
        val projectId = UUID.randomUUID()

        suppose("contract metadata is stored into the database") {
            repository.createOrUpdate(
                ContractMetadata(
                    id = id,
                    name = name,
                    description = description,
                    contractId = contractId,
                    contractTags = contractTags,
                    contractImplements = contractImplements,
                    projectId = projectId
                )
            )
        }

        verify("contract metadata is correctly stored into the database") {
            val record = dslContext.selectFrom(ContractMetadataTable)
                .where(ContractMetadataTable.ID.eq(id))
                .fetchOne()

            assertThat(record).withMessage()
                .isEqualTo(
                    ContractMetadataRecord(
                        id = id,
                        name = name,
                        description = description,
                        contractId = contractId,
                        contractTags = contractTags.map { it.value }.toTypedArray(),
                        contractImplements = contractImplements.map { it.value }.toTypedArray(),
                        projectId = projectId
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyUpdateExistingContractMetadata() {
        val id = UUID.randomUUID()
        val name = "name"
        val description = "description"
        val contractId = ContractId("cid")
        val contractTags = listOf(ContractTag("tag"))
        val contractImplements = listOf(InterfaceId("trait"))
        val projectId = UUID.randomUUID()

        suppose("contract metadata is stored into the database") {
            repository.createOrUpdate(
                ContractMetadata(
                    id = id,
                    name = name,
                    description = description,
                    contractId = contractId,
                    contractTags = contractTags,
                    contractImplements = contractImplements,
                    projectId = projectId
                )
            )
        }

        val otherId = UUID.randomUUID()
        val otherName = "name"
        val otherDescription = "description"
        val otherTags = listOf(ContractTag("other-tag-1"), ContractTag("other-tag-2"))
        val otherImplements = listOf(InterfaceId("other-trait-1"), InterfaceId("other-trait-2"))

        suppose("contract metadata is stored into the database with different data") {
            repository.createOrUpdate(
                ContractMetadata(
                    id = otherId,
                    name = otherName,
                    description = otherDescription,
                    contractId = contractId,
                    contractTags = otherTags,
                    contractImplements = otherImplements,
                    projectId = projectId
                )
            )
        }

        verify("contract metadata is correctly updated in the database") {
            val record = dslContext.selectFrom(ContractMetadataTable)
                .where(ContractMetadataTable.ID.eq(id))
                .fetchOne()

            assertThat(record).withMessage()
                .isEqualTo(
                    ContractMetadataRecord(
                        id = id,
                        name = name,
                        description = description,
                        contractId = contractId,
                        contractTags = otherTags.map { it.value }.toTypedArray(),
                        contractImplements = otherImplements.map { it.value }.toTypedArray(),
                        projectId = projectId
                    )
                )
        }

        verify("contract metadata is not created for different UUID") {
            val record = dslContext.selectFrom(ContractMetadataTable)
                .where(ContractMetadataTable.ID.eq(otherId))
                .fetchOne()

            assertThat(record).withMessage()
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyCheckIfContractMetadataExists() {
        val id = UUID.randomUUID()
        val name = "name"
        val description = "description"
        val contractId = ContractId("cid")
        val contractTags = listOf(ContractTag("tag"))
        val contractImplements = listOf(InterfaceId("trait"))
        val projectId = UUID.randomUUID()

        suppose("contract metadata is stored into the database") {
            repository.createOrUpdate(
                ContractMetadata(
                    id = id,
                    name = name,
                    description = description,
                    contractId = contractId,
                    contractTags = contractTags,
                    contractImplements = contractImplements,
                    projectId = projectId
                )
            )
        }

        verify("contract metadata exists in the database") {
            assertThat(repository.exists(contractId, projectId)).withMessage()
                .isTrue()
        }

        verify("no other contract metadata exists in the database") {
            assertThat(repository.exists(ContractId("non-existent"), projectId)).withMessage()
                .isFalse()
        }
    }
}
