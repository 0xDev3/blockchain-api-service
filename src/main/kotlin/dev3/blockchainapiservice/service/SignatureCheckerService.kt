package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.util.SignedMessage
import com.ampnet.blockchainapiservice.util.WalletAddress

interface SignatureCheckerService {
    fun signatureMatches(message: String, signedMessage: SignedMessage, signer: WalletAddress): Boolean
}
