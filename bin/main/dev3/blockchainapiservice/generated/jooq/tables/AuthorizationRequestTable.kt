/*
 * This file is generated by jOOQ.
 */
package dev3.blockchainapiservice.generated.jooq.tables


import com.fasterxml.jackson.databind.JsonNode

import dev3.blockchainapiservice.generated.jooq.BlockchainApiService
import dev3.blockchainapiservice.generated.jooq.tables.records.AuthorizationRequestRecord
import dev3.blockchainapiservice.util.JsonNodeConverter
import dev3.blockchainapiservice.util.SignedMessage
import dev3.blockchainapiservice.util.SignedMessageConverter
import dev3.blockchainapiservice.util.UtcDateTime
import dev3.blockchainapiservice.util.UtcDateTimeConverter
import dev3.blockchainapiservice.util.WalletAddress
import dev3.blockchainapiservice.util.WalletAddressConverter

import java.util.UUID

import org.jooq.Field
import org.jooq.ForeignKey
import org.jooq.Name
import org.jooq.Record
import org.jooq.Schema
import org.jooq.Table
import org.jooq.TableField
import org.jooq.TableOptions
import org.jooq.UniqueKey
import org.jooq.impl.DSL
import org.jooq.impl.Internal
import org.jooq.impl.SQLDataType
import org.jooq.impl.TableImpl


@Suppress("UNCHECKED_CAST")
open class AuthorizationRequestTable(
    alias: Name,
    child: Table<out Record>?,
    path: ForeignKey<out Record, AuthorizationRequestRecord>?,
    aliased: Table<AuthorizationRequestRecord>?,
    parameters: Array<Field<*>?>?
): TableImpl<AuthorizationRequestRecord>(
    alias,
    BlockchainApiService.BLOCKCHAIN_API_SERVICE,
    child,
    path,
    aliased,
    parameters,
    DSL.comment(""),
    TableOptions.table()
) {
    companion object : AuthorizationRequestTable()

    override fun getRecordType(): Class<AuthorizationRequestRecord> = AuthorizationRequestRecord::class.java

    val ID: TableField<AuthorizationRequestRecord, UUID> = createField(DSL.name("id"), SQLDataType.UUID.nullable(false), this, "")

    val PROJECT_ID: TableField<AuthorizationRequestRecord, UUID> = createField(DSL.name("project_id"), SQLDataType.UUID.nullable(false), this, "")

    val REDIRECT_URL: TableField<AuthorizationRequestRecord, String> = createField(DSL.name("redirect_url"), SQLDataType.VARCHAR.nullable(false), this, "")

    val REQUESTED_WALLET_ADDRESS: TableField<AuthorizationRequestRecord, WalletAddress?> = createField(DSL.name("requested_wallet_address"), SQLDataType.VARCHAR, this, "", WalletAddressConverter())

    val ACTUAL_WALLET_ADDRESS: TableField<AuthorizationRequestRecord, WalletAddress?> = createField(DSL.name("actual_wallet_address"), SQLDataType.VARCHAR, this, "", WalletAddressConverter())

    val SIGNED_MESSAGE: TableField<AuthorizationRequestRecord, SignedMessage?> = createField(DSL.name("signed_message"), SQLDataType.VARCHAR, this, "", SignedMessageConverter())

    val ARBITRARY_DATA: TableField<AuthorizationRequestRecord, JsonNode?> = createField(DSL.name("arbitrary_data"), SQLDataType.JSON, this, "", JsonNodeConverter())

    val SCREEN_BEFORE_ACTION_MESSAGE: TableField<AuthorizationRequestRecord, String?> = createField(DSL.name("screen_before_action_message"), SQLDataType.VARCHAR, this, "")

    val SCREEN_AFTER_ACTION_MESSAGE: TableField<AuthorizationRequestRecord, String?> = createField(DSL.name("screen_after_action_message"), SQLDataType.VARCHAR, this, "")

    val CREATED_AT: TableField<AuthorizationRequestRecord, UtcDateTime> = createField(DSL.name("created_at"), SQLDataType.TIMESTAMPWITHTIMEZONE(6).nullable(false), this, "", UtcDateTimeConverter())

    val MESSAGE_TO_SIGN_OVERRIDE: TableField<AuthorizationRequestRecord, String?> = createField(DSL.name("message_to_sign_override"), SQLDataType.VARCHAR, this, "")

    val STORE_INDEFINITELY: TableField<AuthorizationRequestRecord, Boolean> = createField(DSL.name("store_indefinitely"), SQLDataType.BOOLEAN.nullable(false).defaultValue(DSL.field("true", SQLDataType.BOOLEAN)), this, "")

    private constructor(alias: Name, aliased: Table<AuthorizationRequestRecord>?): this(alias, null, null, aliased, null)
    private constructor(alias: Name, aliased: Table<AuthorizationRequestRecord>?, parameters: Array<Field<*>?>?): this(alias, null, null, aliased, parameters)

    constructor(alias: String): this(DSL.name(alias))

    constructor(alias: Name): this(alias, null)

    constructor(): this(DSL.name("authorization_request"), null)
    override fun getSchema(): Schema? = if (aliased()) null else BlockchainApiService.BLOCKCHAIN_API_SERVICE
    override fun getPrimaryKey(): UniqueKey<AuthorizationRequestRecord> = Internal.createUniqueKey(AuthorizationRequestTable, DSL.name("authorization_request_pkey"), arrayOf(AuthorizationRequestTable.ID), true)
    override fun `as`(alias: String): AuthorizationRequestTable = AuthorizationRequestTable(DSL.name(alias), this)
    override fun `as`(alias: Name): AuthorizationRequestTable = AuthorizationRequestTable(alias, this)

    override fun rename(name: String): AuthorizationRequestTable = AuthorizationRequestTable(DSL.name(name), null)

    override fun rename(name: Name): AuthorizationRequestTable = AuthorizationRequestTable(name, null)
}
