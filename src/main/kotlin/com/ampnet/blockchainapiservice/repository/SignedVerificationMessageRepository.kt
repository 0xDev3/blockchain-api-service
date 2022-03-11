package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.model.result.SignedVerificationMessage
import java.util.UUID

interface SignedVerificationMessageRepository {
    fun getById(id: UUID): SignedVerificationMessage?
    fun store(message: SignedVerificationMessage): SignedVerificationMessage
    fun deleteById(id: UUID): Boolean
    fun deleteAllExpired(): Int
}
