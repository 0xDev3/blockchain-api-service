package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.util.SignedMessage
import dev3.blockchainapiservice.util.WalletAddress

interface SignatureCheckerService {
    fun signatureMatches(message: String, signedMessage: SignedMessage, signer: WalletAddress): Boolean
}
