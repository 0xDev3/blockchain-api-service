package com.ampnet.blockchainapiservice.service

import java.util.UUID

interface SendErc20RequestService {
    fun attachTxHash(id: UUID, txHash: String)
}
