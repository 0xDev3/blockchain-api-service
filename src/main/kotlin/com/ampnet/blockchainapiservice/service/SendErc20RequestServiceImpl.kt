package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.exception.CannotAttachTxHashException
import com.ampnet.blockchainapiservice.repository.SendErc20RequestRepository
import mu.KLogging
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SendErc20RequestServiceImpl(
    private val sendErc20RequestRepository: SendErc20RequestRepository
) : SendErc20RequestService {

    companion object : KLogging()

    override fun attachTxHash(id: UUID, txHash: String) {
        logger.info { "Attach txHash to send ERC20 request, id: $id, txHash: $txHash" }

        val txHashAttached = sendErc20RequestRepository.setTxHash(id, txHash)

        if (txHashAttached.not()) {
            throw CannotAttachTxHashException("Unable to attach transaction hash to send request with ID: $id")
        }
    }
}
