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
        dslContext.deleteFrom(AddressBookTable.ADDRESS_BOOK).execute()
        dslContext.deleteFrom(AuthorizationRequestTable.AUTHORIZATION_REQUEST).execute()
        dslContext.deleteFrom(AssetBalanceRequestTable.ASSET_BALANCE_REQUEST).execute()
        dslContext.deleteFrom(AssetSendRequestTable.ASSET_SEND_REQUEST).execute()
        dslContext.deleteFrom(ContractFunctionCallRequestTable.CONTRACT_FUNCTION_CALL_REQUEST).execute()
        dslContext.deleteFrom(ContractDeploymentRequestTable.CONTRACT_DEPLOYMENT_REQUEST).execute()
        dslContext.deleteFrom(ContractMetadataTable.CONTRACT_METADATA).execute()
        dslContext.deleteFrom(Erc20LockRequestTable.ERC20_LOCK_REQUEST).execute()
        dslContext.deleteFrom(MultiPaymentTemplateItemTable.MULTI_PAYMENT_TEMPLATE_ITEM).execute()
        dslContext.deleteFrom(MultiPaymentTemplateTable.MULTI_PAYMENT_TEMPLATE).execute()
        dslContext.deleteFrom(ImportedContractDecoratorTable.IMPORTED_CONTRACT_DECORATOR).execute()
        dslContext.deleteFrom(ApiKeyTable.API_KEY).execute()
        dslContext.deleteFrom(ProjectTable.PROJECT).execute()
        dslContext.deleteFrom(UserIdentifierTable.USER_IDENTIFIER).execute()
        dslContext.deleteFrom(FetchAccountBalanceCacheTable.FETCH_ACCOUNT_BALANCE_CACHE).execute()
        dslContext.deleteFrom(FetchErc20AccountBalanceCacheTable.FETCH_ERC20_ACCOUNT_BALANCE_CACHE).execute()
        dslContext.deleteFrom(FetchTransactionInfoCacheTable.FETCH_TRANSACTION_INFO_CACHE).execute()
    }
}
