package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.config.ApplicationProperties
import com.ampnet.blockchainapiservice.model.result.UnsignedVerificationMessage
import com.ampnet.blockchainapiservice.repository.UnsignedVerificationMessageRepository
import com.ampnet.blockchainapiservice.util.WalletAddress
import mu.KLogging
import org.springframework.stereotype.Service

@Service
class VerificationServiceImpl(
    private val applicationProperties: ApplicationProperties,
    private val unsignedVerificationMessageRepository: UnsignedVerificationMessageRepository,
    private val uuidProvider: UuidProvider,
    private val utcDateTimeProvider: UtcDateTimeProvider
) : VerificationService {

    companion object : KLogging()

    override fun createUnsignedVerificationMessage(walletAddress: WalletAddress): UnsignedVerificationMessage {
        logger.info { "Creating unsigned verification message for wallet address: $walletAddress" }

        val message = UnsignedVerificationMessage(
            id = uuidProvider.getUuid(),
            walletAddress = walletAddress,
            createdAt = utcDateTimeProvider.getUtcDateTime(),
            validityDuration = applicationProperties.verification.unsignedMessageValidity
        )

        return unsignedVerificationMessageRepository.store(message)
    }
}
