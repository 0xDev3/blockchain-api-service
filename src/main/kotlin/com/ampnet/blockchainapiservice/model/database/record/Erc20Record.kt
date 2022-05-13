package com.ampnet.blockchainapiservice.model.database.record

import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID

interface Erc20Record {
    val id: UUID?
    val chainId: ChainId?
    val redirectUrl: String?
    val tokenAddress: ContractAddress?
    val arbitraryData: JsonNode?
    val screenBeforeActionMessage: String?
    val screenAfterActionMessage: String?
}
