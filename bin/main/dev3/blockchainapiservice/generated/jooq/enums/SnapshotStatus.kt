/*
 * This file is generated by jOOQ.
 */
package dev3.blockchainapiservice.generated.jooq.enums


import dev3.blockchainapiservice.generated.jooq.BlockchainApiService

import org.jooq.Catalog
import org.jooq.EnumType
import org.jooq.Schema


@Suppress("UNCHECKED_CAST")
enum class SnapshotStatus(@get:JvmName("literal") public val literal: String) : EnumType {
    PENDING("PENDING"),
    SUCCESS("SUCCESS"),
    FAILED("FAILED");
    override fun getCatalog(): Catalog? = schema.catalog
    override fun getSchema(): Schema = BlockchainApiService.BLOCKCHAIN_API_SERVICE
    override fun getName(): String = "snapshot_status"
    override fun getLiteral(): String = literal
}
