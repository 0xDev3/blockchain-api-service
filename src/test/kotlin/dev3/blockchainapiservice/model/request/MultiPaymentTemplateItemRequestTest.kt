package dev3.blockchainapiservice.model.request

import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.config.validation.ValidationConstants
import dev3.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigInteger
import javax.validation.Validation
import javax.validation.Validator
import javax.validation.ValidatorFactory

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MultiPaymentTemplateItemRequestTest : TestBase() {

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
    fun mustNotAllowInvalidEthAddressForWalletAddress() {
        val requestWithInvalidEthAddress = suppose("request with invalid eth address is created") {
            MultiPaymentTemplateItemRequest(
                walletAddress = "invalid",
                itemName = null,
                amount = BigInteger.ZERO
            )
        }

        verify("request with invalid eth address is marked as invalid") {
            val violations = validator.validate(requestWithInvalidEthAddress).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("value must be a valid Ethereum address")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("walletAddress")
        }

        val requestWithTooLongEthAddress = suppose("request with too long eth address is created") {
            MultiPaymentTemplateItemRequest(
                walletAddress = WalletAddress("a").rawValue + "b",
                itemName = null,
                amount = BigInteger.ZERO
            )
        }

        verify("request with too long eth address is marked as invalid") {
            val violations = validator.validate(requestWithTooLongEthAddress).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("value must be a valid Ethereum address")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("walletAddress")
        }

        val requestWithEmptyEthAddress = suppose("request with empty eth address is created") {
            MultiPaymentTemplateItemRequest(
                walletAddress = "",
                itemName = null,
                amount = BigInteger.ZERO
            )
        }

        verify("request with empty eth address is marked as invalid") {
            val violations = validator.validate(requestWithEmptyEthAddress).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("value must be a valid Ethereum address")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("walletAddress")
        }

        val requestWithValidEthAddress = suppose("request with valid eth address is created") {
            MultiPaymentTemplateItemRequest(
                walletAddress = WalletAddress("a").rawValue,
                itemName = null,
                amount = BigInteger.ZERO
            )
        }

        verify("request with valid eth address is marked as valid") {
            val violations = validator.validate(requestWithValidEthAddress).toList()

            assertThat(violations).withMessage()
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowTooLongStringForItemName() {
        val requestWithTooLongString = suppose("request with too long string is created") {
            MultiPaymentTemplateItemRequest(
                walletAddress = WalletAddress("0").rawValue,
                itemName = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH + 1),
                amount = BigInteger.ZERO
            )
        }

        verify("request with too long string is marked as invalid") {
            val violations = validator.validate(requestWithTooLongString).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("size must be between 0 and ${ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH}")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("itemName")
        }

        val requestWithValidLengthString = suppose("request with valid length string is created") {
            MultiPaymentTemplateItemRequest(
                walletAddress = WalletAddress("0").rawValue,
                itemName = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH),
                amount = BigInteger.ZERO
            )
        }

        verify("request with valid length string is marked as valid") {
            val violations = validator.validate(requestWithValidLengthString).toList()

            assertThat(violations).withMessage()
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowNegativeOrTooBigValueForAmount() {
        val requestWithNegativeUint256 = suppose("request with negative uint256 is created") {
            MultiPaymentTemplateItemRequest(
                walletAddress = WalletAddress("0").rawValue,
                itemName = null,
                amount = BigInteger.valueOf(-1L)
            )
        }

        verify("request with negative uint256 is marked as invalid") {
            val violations = validator.validate(requestWithNegativeUint256).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("value must be within range [0, 2^256]")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("amount")
        }

        val requestWithTooBigUint256 = suppose("request with too big uint256 is created") {
            MultiPaymentTemplateItemRequest(
                walletAddress = WalletAddress("0").rawValue,
                itemName = null,
                amount = BigInteger.TWO.pow(256) + BigInteger.ONE
            )
        }

        verify("request with too big uint256 is marked as invalid") {
            val violations = validator.validate(requestWithTooBigUint256).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("value must be within range [0, 2^256]")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("amount")
        }

        val requestWithValidUint256 = suppose("request with valid uint256 is created") {
            MultiPaymentTemplateItemRequest(
                walletAddress = WalletAddress("0").rawValue,
                itemName = null,
                amount = BigInteger.TWO.pow(256)
            )
        }

        verify("request with valid uint256 is marked as valid") {
            val violations = validator.validate(requestWithValidUint256).toList()

            assertThat(violations).withMessage()
                .isEmpty()
        }
    }
}
