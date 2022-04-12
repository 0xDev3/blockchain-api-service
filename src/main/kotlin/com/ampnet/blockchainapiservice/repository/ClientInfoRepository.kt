package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.model.result.ClientInfo

interface ClientInfoRepository {
    fun getById(clientId: String): ClientInfo?
}
