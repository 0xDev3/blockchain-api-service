package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.model.result.UnsignedVerificationMessage
import com.ampnet.blockchainapiservice.util.WalletAddress

interface VerificationService {
    fun createUnsignedVerificationMessage(walletAddress: WalletAddress): UnsignedVerificationMessage
}
