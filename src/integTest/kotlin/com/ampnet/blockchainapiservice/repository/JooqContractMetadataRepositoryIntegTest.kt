package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.generated.jooq.tables.ContractMetadataTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ContractMetadataRecord
import com.ampnet.blockchainapiservice.testcontainers.SharedTestContainers
import com.ampnet.blockchainapiservice.util.ContractId
import com.ampnet.blockchainapiservice.util.ContractTag
import com.ampnet.blockchainapiservice.util.ContractTrait
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
@Import(JooqContractMetadataRepository::class)
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
        val contractId = ContractId("cid")
        val contractTags = listOf(ContractTag("tag"))
        val contractImplements = listOf(ContractTrait("trait"))

        suppose("contract metadata is stored into the database") {
            repository.createOrUpdate(id, contractId, contractTags, contractImplements)
        }

        verify("contract metadata is correctly stored into the database") {
            val record = dslContext.selectFrom(ContractMetadataTable.CONTRACT_METADATA)
                .where(ContractMetadataTable.CONTRACT_METADATA.ID.eq(id))
                .fetchOne()

            assertThat(record).withMessage()
                .isEqualTo(
                    ContractMetadataRecord(
                        id = id,
                        contractId = contractId,
                        contractTags = contractTags.map { it.value }.toTypedArray(),
                        contractImplements = contractImplements.map { it.value }.toTypedArray()
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyUpdateExistingContractMetadata() {
        val id = UUID.randomUUID()
        val contractId = ContractId("cid")
        val contractTags = listOf(ContractTag("tag"))
        val contractImplements = listOf(ContractTrait("trait"))

        suppose("contract metadata is stored into the database") {
            repository.createOrUpdate(id, contractId, contractTags, contractImplements)
        }

        val otherId = UUID.randomUUID()
        val otherTags = listOf(ContractTag("other-tag-1"), ContractTag("other-tag-2"))
        val otherImplements = listOf(ContractTrait("other-trait-1"), ContractTrait("other-trait-2"))

        suppose("contract metadata is stored into the database with different data") {
            repository.createOrUpdate(otherId, contractId, otherTags, otherImplements)
        }

        verify("contract metadata is correctly updated in the database") {
            val record = dslContext.selectFrom(ContractMetadataTable.CONTRACT_METADATA)
                .where(ContractMetadataTable.CONTRACT_METADATA.ID.eq(id))
                .fetchOne()

            assertThat(record).withMessage()
                .isEqualTo(
                    ContractMetadataRecord(
                        id = id,
                        contractId = contractId,
                        contractTags = otherTags.map { it.value }.toTypedArray(),
                        contractImplements = otherImplements.map { it.value }.toTypedArray()
                    )
                )
        }

        verify("contract metadata is not created for different UUID") {
            val record = dslContext.selectFrom(ContractMetadataTable.CONTRACT_METADATA)
                .where(ContractMetadataTable.CONTRACT_METADATA.ID.eq(otherId))
                .fetchOne()

            assertThat(record).withMessage()
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyCheckIfContractMetadataExists() {
        val id = UUID.randomUUID()
        val contractId = ContractId("cid")
        val contractTags = listOf(ContractTag("tag"))
        val contractImplements = listOf(ContractTrait("trait"))

        suppose("contract metadata is stored into the database") {
            repository.createOrUpdate(id, contractId, contractTags, contractImplements)
        }

        verify("contract metadata exists in the database") {
            assertThat(repository.exists(contractId)).withMessage()
                .isTrue()
        }

        verify("no other contract metadata exists in the database") {
            assertThat(repository.exists(ContractId("non-existent"))).withMessage()
                .isFalse()
        }
    }
}
