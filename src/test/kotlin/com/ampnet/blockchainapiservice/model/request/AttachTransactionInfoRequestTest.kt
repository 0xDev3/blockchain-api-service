package com.ampnet.blockchainapiservice.model.request

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import javax.validation.Validation
import javax.validation.Validator
import javax.validation.ValidatorFactory

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AttachTransactionInfoRequestTest : TestBase() {

    private lateinit var validatorFactory: ValidatorFactory
    private lateinit var validator: Validator

    @BeforeAll
    fun beforeAll() {
        validatorFactory = Validation.buildDefaultValidatorFactory()
        validator = validatorFactory.validator
    }

    @AfterAll
    fun afterAll() {
        validatorFactory.close()
    }

    @Test
    fun mustNotAllowInvalidEthTxHashForTxHash() {
        val requestWithInvalidTxHash = suppose("request with invalid eth tx hash is created") {
            AttachTransactionInfoRequest(
                txHash = "invalid",
                callerAddress = WalletAddress("0").rawValue
            )
        }

        verify("request with invalid tx hash is marked as invalid") {
            val violations = validator.validate(requestWithInvalidTxHash).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("value must be a valid Ethereum transaction hash")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("txHash")
        }

        val requestWithTooLongTxHash = suppose("request with too long tx hash is created") {
            AttachTransactionInfoRequest(
                txHash = "0x" + "a".repeat(65),
                callerAddress = WalletAddress("0").rawValue
            )
        }

        verify("request with too long tx hash is marked as invalid") {
            val violations = validator.validate(requestWithTooLongTxHash).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("value must be a valid Ethereum transaction hash")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("txHash")
        }

        val requestWithEmptyTxHash = suppose("request with empty tx hash is created") {
            AttachTransactionInfoRequest(
                txHash = "",
                callerAddress = WalletAddress("0").rawValue
            )
        }

        verify("request with empty tx hash is marked as invalid") {
            val violations = validator.validate(requestWithEmptyTxHash).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("value must be a valid Ethereum transaction hash")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("txHash")
        }

        val requestWithValidTxHash = suppose("request with valid tx hash is created") {
            AttachTransactionInfoRequest(
                txHash = WalletAddress("a").rawValue,
                callerAddress = WalletAddress("0").rawValue
            )
        }

        verify("request with valid tx hash is marked as valid") {
            val violations = validator.validate(requestWithValidTxHash).toList()

            assertThat(violations).withMessage()
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowInvalidEthAddressForCallerAddress() {
        val requestWithInvalidEthAddress = suppose("request with invalid eth address is created") {
            AttachTransactionInfoRequest(
                txHash = "0x0",
                callerAddress = "invalid"
            )
        }

        verify("request with invalid eth address is marked as invalid") {
            val violations = validator.validate(requestWithInvalidEthAddress).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("value must be a valid Ethereum address")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("callerAddress")
        }

        val requestWithTooLongEthAddress = suppose("request with too long eth address is created") {
            AttachTransactionInfoRequest(
                txHash = "0x0",
                callerAddress = WalletAddress("a").rawValue + "b"
            )
        }

        verify("request with too long eth address is marked as invalid") {
            val violations = validator.validate(requestWithTooLongEthAddress).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("value must be a valid Ethereum address")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("callerAddress")
        }

        val requestWithEmptyEthAddress = suppose("request with empty eth address is created") {
            AttachTransactionInfoRequest(
                txHash = "0x0",
                callerAddress = ""
            )
        }

        verify("request with empty eth address is marked as invalid") {
            val violations = validator.validate(requestWithEmptyEthAddress).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("value must be a valid Ethereum address")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("callerAddress")
        }

        val requestWithValidEthAddress = suppose("request with valid eth address is created") {
            AttachTransactionInfoRequest(
                txHash = "0x0",
                callerAddress = WalletAddress("a").rawValue
            )
        }

        verify("request with valid eth address is marked as valid") {
            val violations = validator.validate(requestWithValidEthAddress).toList()

            assertThat(violations).withMessage()
                .isEmpty()
        }
    }
}
