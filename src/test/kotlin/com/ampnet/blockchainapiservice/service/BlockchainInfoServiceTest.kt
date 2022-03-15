package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.blockchain.BlockchainService
import com.ampnet.blockchainapiservice.blockchain.properties.Chain
import com.ampnet.blockchainapiservice.exception.ExpiredValidationMessageException
import com.ampnet.blockchainapiservice.exception.ResourceNotFoundException
import com.ampnet.blockchainapiservice.repository.SignedVerificationMessageRepository
import com.ampnet.blockchainapiservice.util.AccountBalance
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.BlockNumber
import com.ampnet.blockchainapiservice.util.ContractAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import java.math.BigInteger
import java.time.Duration
import java.util.UUID

class BlockchainInfoServiceTest : TestBase() {

    @Test
    fun mustCorrectlyFetchErc20AccountBalanceBasedOnSignedMessage() {
        val signedMessageRepository = mock<SignedVerificationMessageRepository>()

        suppose("signed verification message repository will return signed message") {
            given(signedMessageRepository.getById(TestData.SIGNED_MESSAGE.id))
                .willReturn(TestData.SIGNED_MESSAGE)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("current time is before signed message validity time") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.SIGNED_MESSAGE.verifiedAt)
        }

        val chainId = Chain.HARDHAT_TESTNET.id
        val contractAddress = ContractAddress("a")
        val block = BlockNumber(BigInteger("123"))
        val accountBalance = AccountBalance(TestData.SIGNED_MESSAGE.walletAddress, Balance(BigInteger("10000")))
        val blockchainService = mock<BlockchainService>()

        suppose("blockchain service will return ERC20 balance of account") {
            given(
                blockchainService.fetchErc20AccountBalance(
                    chainId = chainId,
                    contractAddress = contractAddress,
                    walletAddress = TestData.SIGNED_MESSAGE.walletAddress,
                    block = block
                )
            ).willReturn(accountBalance)
        }

        val service = BlockchainInfoServiceImpl(
            signedVerificationMessageRepository = signedMessageRepository,
            blockchainService = blockchainService,
            utcDateTimeProvider = utcDateTimeProvider
        )

        verify("correct ERC20 balance is fetched") {
            val result = service.fetchErc20AccountBalanceFromSignedMessage(
                messageId = TestData.SIGNED_MESSAGE.id,
                chainId = chainId,
                contractAddress = contractAddress,
                block = block
            )

            assertThat(result).withMessage()
                .isEqualTo(accountBalance)
        }
    }

    @Test
    fun mustThrowExpiredValidationMessageExceptionWhenSignedMessageHasExpired() {
        val signedMessageRepository = mock<SignedVerificationMessageRepository>()

        suppose("signed verification message repository will return signed message") {
            given(signedMessageRepository.getById(TestData.SIGNED_MESSAGE.id))
                .willReturn(TestData.SIGNED_MESSAGE)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("current time is after signed message validity time") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.SIGNED_MESSAGE.validUntil + Duration.ofMinutes(1L))
        }

        val chainId = Chain.HARDHAT_TESTNET.id
        val contractAddress = ContractAddress("a")
        val block = BlockNumber(BigInteger("123"))
        val blockchainService = mock<BlockchainService>()

        val service = BlockchainInfoServiceImpl(
            signedVerificationMessageRepository = signedMessageRepository,
            blockchainService = blockchainService,
            utcDateTimeProvider = utcDateTimeProvider
        )

        verify("ExpiredValidationMessageException is thrown") {
            assertThrows<ExpiredValidationMessageException>(message) {
                service.fetchErc20AccountBalanceFromSignedMessage(
                    messageId = TestData.SIGNED_MESSAGE.id,
                    chainId = chainId,
                    contractAddress = contractAddress,
                    block = block
                )
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenSignedMessageDoesNotExist() {
        val signedMessageRepository = mock<SignedVerificationMessageRepository>()

        suppose("signed verification message repository will return null") {
            given(signedMessageRepository.getById(any()))
                .willReturn(null)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()
        val blockchainService = mock<BlockchainService>()
        val service = BlockchainInfoServiceImpl(
            signedVerificationMessageRepository = signedMessageRepository,
            blockchainService = blockchainService,
            utcDateTimeProvider = utcDateTimeProvider
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.fetchErc20AccountBalanceFromSignedMessage(
                    messageId = UUID.randomUUID(),
                    chainId = Chain.HARDHAT_TESTNET.id,
                    contractAddress = ContractAddress("a")
                )
            }
        }
    }
}
