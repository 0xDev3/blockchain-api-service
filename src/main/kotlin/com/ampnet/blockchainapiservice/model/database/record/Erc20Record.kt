package com.ampnet.blockchainapiservice.model.database.record

import org.jooq.JSON
import java.util.UUID

interface Erc20Record {
    val id: UUID?
    val chainId: Long?
    val redirectUrl: String?
    val tokenAddress: String?
    val arbitraryData: JSON?
    val screenBeforeActionMessage: String?
    val screenAfterActionMessage: String?
}
