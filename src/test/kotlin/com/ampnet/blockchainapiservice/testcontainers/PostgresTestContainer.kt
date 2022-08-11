package com.ampnet.blockchainapiservice.testcontainers

import com.ampnet.blockchainapiservice.generated.jooq.tables.AddressBookTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.ApiKeyTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.AssetBalanceRequestTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.AssetSendRequestTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.ContractDeploymentRequestTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.ContractFunctionCallRequestTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.ContractMetadataTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.Erc20LockRequestTable
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
        dslContext.deleteFrom(AssetBalanceRequestTable.ASSET_BALANCE_REQUEST).execute()
        dslContext.deleteFrom(AssetSendRequestTable.ASSET_SEND_REQUEST).execute()
        dslContext.deleteFrom(ContractFunctionCallRequestTable.CONTRACT_FUNCTION_CALL_REQUEST).execute()
        dslContext.deleteFrom(ContractDeploymentRequestTable.CONTRACT_DEPLOYMENT_REQUEST).execute()
        dslContext.deleteFrom(ContractMetadataTable.CONTRACT_METADATA).execute()
        dslContext.deleteFrom(Erc20LockRequestTable.ERC20_LOCK_REQUEST).execute()
        dslContext.deleteFrom(ApiKeyTable.API_KEY).execute()
        dslContext.deleteFrom(ProjectTable.PROJECT).execute()
        dslContext.deleteFrom(UserIdentifierTable.USER_IDENTIFIER).execute()
    }
}
