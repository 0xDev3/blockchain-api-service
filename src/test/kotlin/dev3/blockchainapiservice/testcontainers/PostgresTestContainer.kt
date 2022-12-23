package dev3.blockchainapiservice.testcontainers

import dev3.blockchainapiservice.generated.jooq.tables.AddressBookTable
import dev3.blockchainapiservice.generated.jooq.tables.ApiKeyTable
import dev3.blockchainapiservice.generated.jooq.tables.ApiReadCallTable
import dev3.blockchainapiservice.generated.jooq.tables.ApiUsagePeriodTable
import dev3.blockchainapiservice.generated.jooq.tables.ApiWriteCallTable
import dev3.blockchainapiservice.generated.jooq.tables.AssetBalanceRequestTable
import dev3.blockchainapiservice.generated.jooq.tables.AssetMultiSendRequestTable
import dev3.blockchainapiservice.generated.jooq.tables.AssetSendRequestTable
import dev3.blockchainapiservice.generated.jooq.tables.AuthorizationRequestTable
import dev3.blockchainapiservice.generated.jooq.tables.BlacklistedAddressTable
import dev3.blockchainapiservice.generated.jooq.tables.ContractArbitraryCallRequestTable
import dev3.blockchainapiservice.generated.jooq.tables.ContractDeploymentRequestTable
import dev3.blockchainapiservice.generated.jooq.tables.ContractDeploymentTransactionCacheTable
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
import dev3.blockchainapiservice.generated.jooq.tables.PromoCodeTable
import dev3.blockchainapiservice.generated.jooq.tables.PromoCodeUsageTable
import dev3.blockchainapiservice.generated.jooq.tables.UserIdentifierTable
import org.jooq.DSLContext
import org.testcontainers.containers.PostgreSQLContainer

class PostgresTestContainer : PostgreSQLContainer<PostgresTestContainer>("postgres:13.4-alpine") {

    init {
        start()
        System.setProperty("POSTGRES_PORT", getMappedPort(POSTGRESQL_PORT).toString())
    }

    fun cleanAllDatabaseTables(dslContext: DSLContext) {
        dslContext.apply {
            deleteFrom(AddressBookTable).execute()
            deleteFrom(AuthorizationRequestTable).execute()
            deleteFrom(AssetBalanceRequestTable).execute()
            deleteFrom(AssetSendRequestTable).execute()
            deleteFrom(AssetMultiSendRequestTable).execute()
            deleteFrom(ContractFunctionCallRequestTable).execute()
            deleteFrom(ContractArbitraryCallRequestTable).execute()
            deleteFrom(ContractDeploymentRequestTable).execute()
            deleteFrom(ContractMetadataTable).execute()
            deleteFrom(Erc20LockRequestTable).execute()
            deleteFrom(MultiPaymentTemplateItemTable).execute()
            deleteFrom(MultiPaymentTemplateTable).execute()
            deleteFrom(ImportedContractDecoratorTable).execute()
            deleteFrom(ApiUsagePeriodTable).execute()
            deleteFrom(ApiWriteCallTable).execute()
            deleteFrom(ApiReadCallTable).execute()
            deleteFrom(FetchAccountBalanceCacheTable).execute()
            deleteFrom(FetchErc20AccountBalanceCacheTable).execute()
            deleteFrom(FetchTransactionInfoCacheTable).execute()
            deleteFrom(ContractDeploymentTransactionCacheTable).execute()
            deleteFrom(PromoCodeUsageTable).execute()
            deleteFrom(PromoCodeTable).execute()
            deleteFrom(ApiKeyTable).execute()
            deleteFrom(ProjectTable).execute()
            deleteFrom(UserIdentifierTable).execute()
            deleteFrom(BlacklistedAddressTable).execute()
            // TODO deleteFrom(SnapshotTable).execute()
            // TODO deleteFrom(MerkleTreeLeafNodeTable).execute()
            // TODO deleteFrom(MerkleTreeRootTable).execute()
        }
    }
}
