package dev3.blockchainapiservice.testcontainers

import dev3.blockchainapiservice.generated.jooq.tables.AddressBookTable
import dev3.blockchainapiservice.generated.jooq.tables.ApiKeyTable
import dev3.blockchainapiservice.generated.jooq.tables.AssetBalanceRequestTable
import dev3.blockchainapiservice.generated.jooq.tables.AssetMultiSendRequestTable
import dev3.blockchainapiservice.generated.jooq.tables.AssetSendRequestTable
import dev3.blockchainapiservice.generated.jooq.tables.AuthorizationRequestTable
import dev3.blockchainapiservice.generated.jooq.tables.ContractDeploymentRequestTable
import dev3.blockchainapiservice.generated.jooq.tables.ContractFunctionCallRequestTable
import dev3.blockchainapiservice.generated.jooq.tables.ContractMetadataTable
import dev3.blockchainapiservice.generated.jooq.tables.Erc20LockRequestTable
import dev3.blockchainapiservice.generated.jooq.tables.FetchAccountBalanceCacheTable
import dev3.blockchainapiservice.generated.jooq.tables.FetchErc20AccountBalanceCacheTable
import dev3.blockchainapiservice.generated.jooq.tables.FetchTransactionInfoCacheTable
import dev3.blockchainapiservice.generated.jooq.tables.ImportedContractDecoratorTable
import dev3.blockchainapiservice.generated.jooq.tables.MultiPaymentTemplateItemTable
import dev3.blockchainapiservice.generated.jooq.tables.MultiPaymentTemplateTable
import dev3.blockchainapiservice.generated.jooq.tables.ProjectTable
import dev3.blockchainapiservice.generated.jooq.tables.UserIdentifierTable
import org.jooq.DSLContext
import org.testcontainers.containers.PostgreSQLContainer

class PostgresTestContainer : PostgreSQLContainer<PostgresTestContainer>("postgres:13.4-alpine") {

    init {
        start()
        System.setProperty("POSTGRES_PORT", getMappedPort(POSTGRESQL_PORT).toString())
    }

    fun cleanAllDatabaseTables(dslContext: DSLContext) {
        dslContext.deleteFrom(AddressBookTable).execute()
        dslContext.deleteFrom(AuthorizationRequestTable).execute()
        dslContext.deleteFrom(AssetBalanceRequestTable).execute()
        dslContext.deleteFrom(AssetSendRequestTable).execute()
        dslContext.deleteFrom(AssetMultiSendRequestTable).execute()
        dslContext.deleteFrom(ContractFunctionCallRequestTable).execute()
        dslContext.deleteFrom(ContractDeploymentRequestTable).execute()
        dslContext.deleteFrom(ContractMetadataTable).execute()
        dslContext.deleteFrom(Erc20LockRequestTable).execute()
        dslContext.deleteFrom(MultiPaymentTemplateItemTable).execute()
        dslContext.deleteFrom(MultiPaymentTemplateTable).execute()
        dslContext.deleteFrom(ImportedContractDecoratorTable).execute()
        dslContext.deleteFrom(ApiKeyTable).execute()
        dslContext.deleteFrom(ProjectTable).execute()
        dslContext.deleteFrom(UserIdentifierTable).execute()
        dslContext.deleteFrom(FetchAccountBalanceCacheTable).execute()
        dslContext.deleteFrom(FetchErc20AccountBalanceCacheTable).execute()
        dslContext.deleteFrom(FetchTransactionInfoCacheTable).execute()
        // TODO dslContext.deleteFrom(SnapshotTable).execute()
        // TODO dslContext.deleteFrom(MerkleTreeLeafNodeTable).execute()
        // TODO dslContext.deleteFrom(MerkleTreeRootTable).execute()
    }
}
