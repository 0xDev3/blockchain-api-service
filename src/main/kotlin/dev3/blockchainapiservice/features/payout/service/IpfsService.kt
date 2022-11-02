package dev3.blockchainapiservice.features.payout.service

import com.fasterxml.jackson.databind.JsonNode
import dev3.blockchainapiservice.features.payout.util.IpfsHash

interface IpfsService {
    fun pinJsonToIpfs(json: JsonNode): IpfsHash
}
