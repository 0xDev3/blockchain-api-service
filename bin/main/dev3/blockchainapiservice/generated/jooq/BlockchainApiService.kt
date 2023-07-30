/*
 * This file is generated by jOOQ.
 */
package dev3.blockchainapiservice.generated.jooq


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

import kotlin.collections.List

import org.jooq.Catalog
import org.jooq.Table
import org.jooq.impl.SchemaImpl


@Suppress("UNCHECKED_CAST")
open class BlockchainApiService : SchemaImpl("blockchain_api_service", DefaultCatalog.DEFAULT_CATALOG) {
    public companion object {

        val BLOCKCHAIN_API_SERVICE: BlockchainApiService = BlockchainApiService()
    }

    override fun getCatalog(): Catalog = DefaultCatalog.DEFAULT_CATALOG

    override fun getTables(): List<Table<*>> = listOf(
        AddressBookTable,
        ApiKeyTable,
        AssetBalanceRequestTable,
        AssetMultiSendRequestTable,
        AssetSendRequestTable,
        AuthorizationRequestTable,
        ContractDeploymentRequestTable,
        ContractFunctionCallRequestTable,
        ContractMetadataTable,
        Erc20LockRequestTable,
        FetchAccountBalanceCacheTable,
        FetchErc20AccountBalanceCacheTable,
        FetchTransactionInfoCacheTable,
        ImportedContractDecoratorTable,
        MultiPaymentTemplateTable,
        MultiPaymentTemplateItemTable,
        ProjectTable,
        UserIdentifierTable
    )
}
