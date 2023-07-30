/*
 * This file is generated by jOOQ.
 */
package dev3.blockchainapiservice.generated.jooq.tables.records


import dev3.blockchainapiservice.generated.jooq.enums.UserIdentifierType
import dev3.blockchainapiservice.generated.jooq.tables.UserIdentifierTable

import java.util.UUID

import org.jooq.Record1
import org.jooq.impl.UpdatableRecordImpl


@Suppress("UNCHECKED_CAST")
open class UserIdentifierRecord() : UpdatableRecordImpl<UserIdentifierRecord>(UserIdentifierTable) {

    var id: UUID
        private set(value): Unit = set(0, value)
        get(): UUID = get(0) as UUID

    var userIdentifier: String
        private set(value): Unit = set(1, value)
        get(): String = get(1) as String

    var identifierType: UserIdentifierType
        private set(value): Unit = set(2, value)
        get(): UserIdentifierType = get(2) as UserIdentifierType
    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    override fun key(): Record1<UUID?> = super.key() as Record1<UUID?>

    constructor(id: UUID, userIdentifier: String, identifierType: UserIdentifierType): this() {
        this.id = id
        this.userIdentifier = userIdentifier
        this.identifierType = identifierType
    }
}
