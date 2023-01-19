package dev3.blockchainapiservice.features.blacklist.service

import dev3.blockchainapiservice.util.EthereumAddress

interface BlacklistCheckService {
    fun exists(address: EthereumAddress): Boolean
}
