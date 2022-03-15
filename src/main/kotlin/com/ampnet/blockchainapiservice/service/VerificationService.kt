package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.model.result.SignedVerificationMessage
import com.ampnet.blockchainapiservice.model.result.UnsignedVerificationMessage
import com.ampnet.blockchainapiservice.util.WalletAddress
import java.util.UUID

interface VerificationService {
    fun createUnsignedVerificationMessage(walletAddress: WalletAddress): UnsignedVerificationMessage
    fun verifyAndStoreMessageSignature(messageId: UUID, signature: String): SignedVerificationMessage
}
