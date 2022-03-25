package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.blockchain.BlockchainService
import com.ampnet.blockchainapiservice.blockchain.properties.ChainSpec
import com.ampnet.blockchainapiservice.exception.ExpiredValidationMessageException
import com.ampnet.blockchainapiservice.exception.ResourceNotFoundException
import com.ampnet.blockchainapiservice.repository.SignedVerificationMessageRepository
import com.ampnet.blockchainapiservice.util.AccountBalance
import com.ampnet.blockchainapiservice.util.BlockParameter
import com.ampnet.blockchainapiservice.util.ContractAddress
import mu.KLogging
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BlockchainInfoServiceImpl(
    private val signedVerificationMessageRepository: SignedVerificationMessageRepository,
    private val blockchainService: BlockchainService,
    private val utcDateTimeProvider: UtcDateTimeProvider
) : BlockchainInfoService {

    companion object : KLogging()

    override fun fetchErc20AccountBalanceFromSignedMessage(
        messageId: UUID,
        chainSpec: ChainSpec,
        contractAddress: ContractAddress,
        block: BlockParameter
    ): AccountBalance {
        logger.debug {
            "Fetching ERC20 balance, messageId: $messageId, chainSpec: $chainSpec," +
                " contractAddress: $contractAddress, block: $block"
        }

        val signedMessage = signedVerificationMessageRepository.getById(messageId)
            ?: throw ResourceNotFoundException("Message not found for ID: $messageId")

        val now = utcDateTimeProvider.getUtcDateTime()

        if (signedMessage.isExpired(now)) {
            throw ExpiredValidationMessageException(messageId)
        }

        logger.debug { "Message with ID $messageId was signed by ${signedMessage.walletAddress}" }

        return blockchainService.fetchErc20AccountBalance(
            chainSpec = chainSpec,
            contractAddress = contractAddress,
            walletAddress = signedMessage.walletAddress,
            block = block
        )
    }
}
