package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.model.result.UnsignedVerificationMessage
import java.util.UUID

interface UnsignedVerificationMessageRepository {
    fun getById(id: UUID): UnsignedVerificationMessage?
    fun store(message: UnsignedVerificationMessage): UnsignedVerificationMessage
    fun deleteById(id: UUID): Boolean
    fun deleteAllExpired(): Int
}
