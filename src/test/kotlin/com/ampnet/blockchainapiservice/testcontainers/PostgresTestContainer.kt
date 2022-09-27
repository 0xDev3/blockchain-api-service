package com.ampnet.blockchainapiservice.testcontainers

import com.ampnet.blockchainapiservice.generated.jooq.tables.AddressBookTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.ApiKeyTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.AssetBalanceRequestTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.AssetSendRequestTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.AuthorizationRequestTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.ContractDeploymentRequestTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.ContractFunctionCallRequestTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.ContractMetadataTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.Erc20LockRequestTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.FetchAccountBalanceCacheTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.FetchErc20AccountBalanceCacheTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.FetchTransactionInfoCacheTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.ImportedContractDecoratorTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.MultiPaymentTemplateItemTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.MultiPaymentTemplateTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.ProjectTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.UserIdentifierTable
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
    }
}
