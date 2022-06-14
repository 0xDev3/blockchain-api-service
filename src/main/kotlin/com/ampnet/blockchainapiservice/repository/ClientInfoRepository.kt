package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.model.result.ClientInfo

@Deprecated("for removal")
interface ClientInfoRepository {
    fun getById(clientId: String): ClientInfo?
}
