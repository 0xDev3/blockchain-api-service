package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.exception.CannotAttachTxHashException
import com.ampnet.blockchainapiservice.repository.SendErc20RequestRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoMoreInteractions
import java.util.UUID
import org.mockito.kotlin.verify as verifyMock

class SendErc20RequestServiceTest : TestBase() {

    companion object {
        private const val TX_HASH = "tx-hash"
    }

    @Test
    fun mustSuccessfullyAttachTxHash() {
        val sendErc20RequestRepository = mock<SendErc20RequestRepository>()
        val id = UUID.randomUUID()

        suppose("txHash will be successfully attached to the request") {
            given(sendErc20RequestRepository.setTxHash(id, TX_HASH))
                .willReturn(true)
        }

        val service = SendErc20RequestServiceImpl(
            uuidProvider = mock(),
            functionEncoderService = mock(),
            clientInfoRepository = mock(),
            sendErc20RequestRepository = sendErc20RequestRepository
        )

        verify("txHash was successfully attached") {
            service.attachTxHash(id, TX_HASH)

            verifyMock(sendErc20RequestRepository)
                .setTxHash(id, TX_HASH)
            verifyNoMoreInteractions(sendErc20RequestRepository)
        }
    }

    @Test
    fun mustThrowCannotAttachTxHashExceptionWhenAttachingTxHashFails() {
        val sendErc20RequestRepository = mock<SendErc20RequestRepository>()
        val id = UUID.randomUUID()

        suppose("attaching txHash will fails") {
            given(sendErc20RequestRepository.setTxHash(id, TX_HASH))
                .willReturn(false)
        }

        val service = SendErc20RequestServiceImpl(
            uuidProvider = mock(),
            functionEncoderService = mock(),
            clientInfoRepository = mock(),
            sendErc20RequestRepository = sendErc20RequestRepository
        )

        verify("CannotAttachTxHashException is thrown") {
            assertThrows<CannotAttachTxHashException>(message) {
                service.attachTxHash(id, TX_HASH)
            }

            verifyMock(sendErc20RequestRepository)
                .setTxHash(id, TX_HASH)
            verifyNoMoreInteractions(sendErc20RequestRepository)
        }
    }
}
