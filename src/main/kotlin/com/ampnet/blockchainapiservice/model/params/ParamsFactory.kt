package com.ampnet.blockchainapiservice.model.params

import com.ampnet.blockchainapiservice.model.result.ClientInfo
import java.util.UUID

interface ParamsFactory<P : ClientIdParam, R> {
    fun fromCreateParams(id: UUID, params: P, clientInfo: ClientInfo): R
}
