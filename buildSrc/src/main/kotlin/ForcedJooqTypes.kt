import org.gradle.configurationcache.extensions.capitalized

object ForcedJooqTypes {

    data class JooqType(
        val userType: String,
        val includeExpression: String,
        val includeTypes: String,
        val converter: String
    ) {
        constructor(userType: String, includeExpression: String, includeTypes: String) : this(
            userType, includeExpression, includeTypes, userType + "Converter"
        )
    }

    private val domainIdTypes = listOf(
        "ADDRESS_BOOK_ID",
        "API_KEY_ID",
        "API_USAGE_PERIOD_ID",
        "ASSET_BALANCE_REQUEST_ID",
        "ASSET_MULTI_SEND_REQUEST_ID",
        "ASSET_SEND_REQUEST_ID",
        "AUTHORIZATION_REQUEST_ID",
        "CONTRACT_ARBITRARY_CALL_REQUEST_ID",
        "CONTRACT_DEPLOYMENT_REQUEST_ID",
        "CONTRACT_DEPLOYMENT_TRANSACTION_CACHE_ID",
        "CONTRACT_FUNCTION_CALL_REQUEST_ID",
        "CONTRACT_METADATA_ID",
        "ERC20_LOCK_REQUEST_ID",
        "FETCH_ACCOUNT_BALANCE_CACHE_ID",
        "FETCH_ERC20_ACCOUNT_BALANCE_CACHE_ID",
        "FETCH_TRANSACTION_INFO_CACHE_ID",
        "IMPORTED_CONTRACT_DECORATOR_ID",
        "MULTI_PAYMENT_TEMPLATE_ID",
        "MULTI_PAYMENT_TEMPLATE_ITEM_ID",
        "PROJECT_ID",
        "USER_ID"
    )

    val types = listOf(
        JooqType(
            userType = "dev3.blockchainapiservice.util.ChainId",
            includeExpression = "chain_id",
            includeTypes = "BIGINT"
        ),
        JooqType(
            userType = "dev3.blockchainapiservice.util.ContractAddress",
            includeExpression = "token_address|.*_contract_address|contract_address",
            includeTypes = "VARCHAR"
        ),
        JooqType(
            userType = "dev3.blockchainapiservice.util.WalletAddress",
            includeExpression = ".*_address",
            includeTypes = "VARCHAR"
        ),
        JooqType(
            userType = "dev3.blockchainapiservice.util.Balance",
            includeExpression = ".*_amount",
            includeTypes = "NUMERIC"
        ),
        JooqType(
            userType = "dev3.blockchainapiservice.util.BlockNumber",
            includeExpression = "block_number",
            includeTypes = "NUMERIC"
        ),
        JooqType(
            userType = "dev3.blockchainapiservice.util.TransactionHash",
            includeExpression = ".*tx_hash",
            includeTypes = "VARCHAR"
        ),
        JooqType(
            userType = "dev3.blockchainapiservice.model.json.ManifestJson",
            converter = "dev3.blockchainapiservice.util.ManifestJsonConverter",
            includeExpression = "manifest_json",
            includeTypes = "JSON"
        ),
        JooqType(
            userType = "dev3.blockchainapiservice.model.json.ArtifactJson",
            converter = "dev3.blockchainapiservice.util.ArtifactJsonConverter",
            includeExpression = "artifact_json",
            includeTypes = "JSON"
        ),
        JooqType(
            userType = "dev3.blockchainapiservice.util.SignedMessage",
            includeExpression = "signed_message",
            includeTypes = "VARCHAR"
        ),
        JooqType(
            userType = "dev3.blockchainapiservice.util.DurationSeconds",
            includeExpression = ".*_duration_seconds",
            includeTypes = "NUMERIC"
        ),
        JooqType(
            userType = "com.fasterxml.jackson.databind.JsonNode",
            converter = "dev3.blockchainapiservice.util.JsonNodeConverter",
            includeExpression = ".*",
            includeTypes = "JSON"
        ),
        JooqType(
            userType = "dev3.blockchainapiservice.util.UtcDateTime",
            includeExpression = ".*",
            includeTypes = "TIMESTAMPTZ"
        ),
        JooqType(
            userType = "dev3.blockchainapiservice.util.BaseUrl",
            includeExpression = "base_redirect_url",
            includeTypes = "VARCHAR"
        ),
        JooqType(
            userType = "dev3.blockchainapiservice.util.ContractId",
            includeExpression = "contract_id",
            includeTypes = "VARCHAR"
        ),
        JooqType(
            userType = "dev3.blockchainapiservice.util.ContractBinaryData",
            includeExpression = "contract_data",
            includeTypes = "BYTEA"
        ),
        JooqType(
            userType = "dev3.blockchainapiservice.util.FunctionData",
            includeExpression = "tx_data|function_data",
            includeTypes = "BYTEA"
        ),
        JooqType(
            userType = "dev3.blockchainapiservice.features.payout.util.HashFunction",
            includeExpression = ".*",
            includeTypes = "HASH_FUNCTION"
        ),
        JooqType(
            userType = "dev3.blockchainapiservice.features.payout.util.SnapshotStatus",
            includeExpression = ".*",
            includeTypes = "SNAPSHOT_STATUS"
        ),
        JooqType(
            userType = "dev3.blockchainapiservice.features.payout.util.SnapshotFailureCause",
            includeExpression = ".*",
            includeTypes = "SNAPSHOT_FAILURE_CAUSE"
        )
    ) + domainIdTypes.map {
        val typeName = it.split("_").joinToString("") { it.toLowerCase().capitalized() }

        JooqType(
            userType = "${Configurations.Jooq.packageName}.id.$typeName",
            converter = "${Configurations.Jooq.packageName}.converters.${typeName}Converter",
            includeExpression = ".*",
            includeTypes = it
        )
    }
}
