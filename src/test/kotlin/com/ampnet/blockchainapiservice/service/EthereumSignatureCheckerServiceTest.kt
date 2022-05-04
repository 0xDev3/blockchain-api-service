package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.util.SignedMessage
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EthereumSignatureCheckerServiceTest : TestBase() {

    companion object {
        const val MESSAGE = "Verification message ID to sign: 7d86b0ac-a9a6-40fc-ac6d-2a29ca687f73"

        val WALLET_ADDRESS = WalletAddress("0x865f603F42ca1231e5B5F90e15663b0FE19F0b21")
        val VALID_SIGNATURE = SignedMessage(
            "0xfc90c8aa9f2164234b8826144d8ecfc287b5d7c168d0e9d284baf76dbef55c4c5761cf46e34b7cdb72cc97f1fb1c19f315ee7a" +
                "430dd6111fa6c693b41c96c5501c"
        )

        // also signed using Metamask, but using another address
        val OTHER_SIGNATURE = SignedMessage(
            "0x653d99ce15acbfe1cb0c967ecac59781a6d5192b2c50d3ae89c8fdc14c60e37e24704719abb1d34572335861ff48d0d22adaf5" +
                "145339de09afc8820d82fba77b1b"
        )
        val TOO_SHORT_SIGNATURE = SignedMessage("0x")
        val INVALID_SIGNATURE = SignedMessage(
            "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
                "xxxxxxxxxxxxxxxxxxxxxxxxxxxx"
        )
    }

    @Test
    fun mustReturnTrueForMatchingSignature() {
        val service = EthereumSignatureCheckerService()

        verify("signature matches") {
            assertThat(service.signatureMatches(MESSAGE, VALID_SIGNATURE, WALLET_ADDRESS)).withMessage()
                .isTrue()
        }
    }

    @Test
    fun mustReturnFalseForMismatchingSignature() {
        val service = EthereumSignatureCheckerService()

        verify("signature does not match") {
            assertThat(service.signatureMatches(MESSAGE, OTHER_SIGNATURE, WALLET_ADDRESS)).withMessage()
                .isFalse()
        }
    }

    @Test
    fun mustReturnFalseWhenSignatureIsTooShort() {
        val service = EthereumSignatureCheckerService()

        verify("signature does not match") {
            assertThat(service.signatureMatches(MESSAGE, TOO_SHORT_SIGNATURE, WALLET_ADDRESS)).withMessage()
                .isFalse()
        }
    }

    @Test
    fun mustReturnFalseWhenSignatureIsInvalid() {
        val service = EthereumSignatureCheckerService()

        verify("signature does not match") {
            assertThat(service.signatureMatches(MESSAGE, INVALID_SIGNATURE, WALLET_ADDRESS)).withMessage()
                .isFalse()
        }
    }
}
