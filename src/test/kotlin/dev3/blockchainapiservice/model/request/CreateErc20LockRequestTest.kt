package com.ampnet.blockchainapiservice.model.request

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.config.validation.ValidationConstants
import com.ampnet.blockchainapiservice.config.validation.ValidationConstants.REQUEST_BODY_MAX_JSON_CHARS
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.util.JsonNodeConverter
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.jooq.JSON
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigInteger
import javax.validation.Validation
import javax.validation.Validator
import javax.validation.ValidatorFactory

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreateErc20LockRequestTest : TestBase() {

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
    fun mustNotAllowTooLongStringForRedirectUrl() {
        val requestWithTooLongString = suppose("request with too long string is created") {
            CreateErc20LockRequest(
                redirectUrl = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH + 1),
                tokenAddress = WalletAddress("0").rawValue,
                amount = BigInteger.ZERO,
                lockDurationInSeconds = BigInteger.ZERO,
                lockContractAddress = WalletAddress("0").rawValue,
                senderAddress = null,
                arbitraryData = null,
                screenConfig = null
            )
        }

        verify("request with too long string is marked as invalid") {
            val violations = validator.validate(requestWithTooLongString).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("size must be between 0 and 256")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("redirectUrl")
        }

        val requestWithValidLengthString = suppose("request with valid length string is created") {
            CreateErc20LockRequest(
                redirectUrl = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH),
                tokenAddress = WalletAddress("0").rawValue,
                amount = BigInteger.ZERO,
                lockDurationInSeconds = BigInteger.ZERO,
                lockContractAddress = WalletAddress("0").rawValue,
                senderAddress = null,
                arbitraryData = null,
                screenConfig = null
            )
        }

        verify("request with valid length string is marked as valid") {
            val violations = validator.validate(requestWithValidLengthString).toList()

            assertThat(violations).withMessage()
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowInvalidEthAddressForTokenAddress() {
        val requestWithInvalidEthAddress = suppose("request with invalid eth address is created") {
            CreateErc20LockRequest(
                redirectUrl = null,
                tokenAddress = "invalid",
                amount = BigInteger.ZERO,
                lockDurationInSeconds = BigInteger.ZERO,
                lockContractAddress = WalletAddress("0").rawValue,
                senderAddress = null,
                arbitraryData = null,
                screenConfig = null
            )
        }

        verify("request with invalid eth address is marked as invalid") {
            val violations = validator.validate(requestWithInvalidEthAddress).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("value must be a valid Ethereum address")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("tokenAddress")
        }

        val requestWithTooLongEthAddress = suppose("request with too long eth address is created") {
            CreateErc20LockRequest(
                redirectUrl = null,
                tokenAddress = WalletAddress("a").rawValue + "b",
                amount = BigInteger.ZERO,
                lockDurationInSeconds = BigInteger.ZERO,
                lockContractAddress = WalletAddress("0").rawValue,
                senderAddress = null,
                arbitraryData = null,
                screenConfig = null
            )
        }

        verify("request with too long eth address is marked as invalid") {
            val violations = validator.validate(requestWithTooLongEthAddress).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("value must be a valid Ethereum address")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("tokenAddress")
        }

        val requestWithEmptyEthAddress = suppose("request with empty eth address is created") {
            CreateErc20LockRequest(
                redirectUrl = null,
                tokenAddress = "",
                amount = BigInteger.ZERO,
                lockDurationInSeconds = BigInteger.ZERO,
                lockContractAddress = WalletAddress("0").rawValue,
                senderAddress = null,
                arbitraryData = null,
                screenConfig = null
            )
        }

        verify("request with empty eth address is marked as invalid") {
            val violations = validator.validate(requestWithEmptyEthAddress).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("value must be a valid Ethereum address")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("tokenAddress")
        }

        val requestWithValidEthAddress = suppose("request with valid eth address is created") {
            CreateErc20LockRequest(
                redirectUrl = null,
                tokenAddress = WalletAddress("a").rawValue,
                amount = BigInteger.ZERO,
                lockDurationInSeconds = BigInteger.ZERO,
                lockContractAddress = WalletAddress("0").rawValue,
                senderAddress = null,
                arbitraryData = null,
                screenConfig = null
            )
        }

        verify("request with valid eth address is marked as valid") {
            val violations = validator.validate(requestWithValidEthAddress).toList()

            assertThat(violations).withMessage()
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowNegativeOrTooBigValueForAmount() {
        val requestWithNegativeUint256 = suppose("request with negative uint256 is created") {
            CreateErc20LockRequest(
                redirectUrl = null,
                tokenAddress = WalletAddress("0").rawValue,
                amount = BigInteger.valueOf(-1L),
                lockDurationInSeconds = BigInteger.ZERO,
                lockContractAddress = WalletAddress("0").rawValue,
                senderAddress = null,
                arbitraryData = null,
                screenConfig = null
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
            CreateErc20LockRequest(
                redirectUrl = null,
                tokenAddress = WalletAddress("0").rawValue,
                amount = BigInteger.TWO.pow(256) + BigInteger.ONE,
                lockDurationInSeconds = BigInteger.ZERO,
                lockContractAddress = WalletAddress("0").rawValue,
                senderAddress = null,
                arbitraryData = null,
                screenConfig = null
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
            CreateErc20LockRequest(
                redirectUrl = null,
                tokenAddress = WalletAddress("0").rawValue,
                amount = BigInteger.TWO.pow(256),
                lockDurationInSeconds = BigInteger.ZERO,
                lockContractAddress = WalletAddress("0").rawValue,
                senderAddress = null,
                arbitraryData = null,
                screenConfig = null
            )
        }

        verify("request with valid uint256 is marked as valid") {
            val violations = validator.validate(requestWithValidUint256).toList()

            assertThat(violations).withMessage()
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowNegativeOrTooBigValueForLockDurationInSeconds() {
        val requestWithNegativeUint256 = suppose("request with negative uint256 is created") {
            CreateErc20LockRequest(
                redirectUrl = null,
                tokenAddress = WalletAddress("0").rawValue,
                amount = BigInteger.ZERO,
                lockDurationInSeconds = BigInteger.valueOf(-1L),
                lockContractAddress = WalletAddress("0").rawValue,
                senderAddress = null,
                arbitraryData = null,
                screenConfig = null
            )
        }

        verify("request with negative uint256 is marked as invalid") {
            val violations = validator.validate(requestWithNegativeUint256).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("value must be within range [0, 2^256]")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("lockDurationInSeconds")
        }

        val requestWithTooBigUint256 = suppose("request with too big uint256 is created") {
            CreateErc20LockRequest(
                redirectUrl = null,
                tokenAddress = WalletAddress("0").rawValue,
                amount = BigInteger.ZERO,
                lockDurationInSeconds = BigInteger.TWO.pow(256) + BigInteger.ONE,
                lockContractAddress = WalletAddress("0").rawValue,
                senderAddress = null,
                arbitraryData = null,
                screenConfig = null
            )
        }

        verify("request with too big uint256 is marked as invalid") {
            val violations = validator.validate(requestWithTooBigUint256).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("value must be within range [0, 2^256]")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("lockDurationInSeconds")
        }

        val requestWithValidUint256 = suppose("request with valid uint256 is created") {
            CreateErc20LockRequest(
                redirectUrl = null,
                tokenAddress = WalletAddress("0").rawValue,
                amount = BigInteger.ZERO,
                lockDurationInSeconds = BigInteger.TWO.pow(256),
                lockContractAddress = WalletAddress("0").rawValue,
                senderAddress = null,
                arbitraryData = null,
                screenConfig = null
            )
        }

        verify("request with valid uint256 is marked as valid") {
            val violations = validator.validate(requestWithValidUint256).toList()

            assertThat(violations).withMessage()
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowInvalidEthAddressForLockContractAddress() {
        val requestWithInvalidEthAddress = suppose("request with invalid eth address is created") {
            CreateErc20LockRequest(
                redirectUrl = null,
                tokenAddress = WalletAddress("0").rawValue,
                amount = BigInteger.ZERO,
                lockDurationInSeconds = BigInteger.ZERO,
                lockContractAddress = "invalid",
                senderAddress = null,
                arbitraryData = null,
                screenConfig = null
            )
        }

        verify("request with invalid eth address is marked as invalid") {
            val violations = validator.validate(requestWithInvalidEthAddress).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("value must be a valid Ethereum address")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("lockContractAddress")
        }

        val requestWithTooLongEthAddress = suppose("request with too long eth address is created") {
            CreateErc20LockRequest(
                redirectUrl = null,
                tokenAddress = WalletAddress("0").rawValue,
                amount = BigInteger.ZERO,
                lockDurationInSeconds = BigInteger.ZERO,
                lockContractAddress = WalletAddress("a").rawValue + "b",
                senderAddress = null,
                arbitraryData = null,
                screenConfig = null
            )
        }

        verify("request with too long eth address is marked as invalid") {
            val violations = validator.validate(requestWithTooLongEthAddress).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("value must be a valid Ethereum address")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("lockContractAddress")
        }

        val requestWithEmptyEthAddress = suppose("request with empty eth address is created") {
            CreateErc20LockRequest(
                redirectUrl = null,
                tokenAddress = WalletAddress("0").rawValue,
                amount = BigInteger.ZERO,
                lockDurationInSeconds = BigInteger.ZERO,
                lockContractAddress = "",
                senderAddress = null,
                arbitraryData = null,
                screenConfig = null
            )
        }

        verify("request with empty eth address is marked as invalid") {
            val violations = validator.validate(requestWithEmptyEthAddress).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("value must be a valid Ethereum address")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("lockContractAddress")
        }

        val requestWithValidEthAddress = suppose("request with valid eth address is created") {
            CreateErc20LockRequest(
                redirectUrl = null,
                tokenAddress = WalletAddress("0").rawValue,
                amount = BigInteger.ZERO,
                lockDurationInSeconds = BigInteger.ZERO,
                lockContractAddress = WalletAddress("a").rawValue,
                senderAddress = null,
                arbitraryData = null,
                screenConfig = null
            )
        }

        verify("request with valid eth address is marked as valid") {
            val violations = validator.validate(requestWithValidEthAddress).toList()

            assertThat(violations).withMessage()
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowInvalidEthAddressForLockSenderAddress() {
        val requestWithInvalidEthAddress = suppose("request with invalid eth address is created") {
            CreateErc20LockRequest(
                redirectUrl = null,
                tokenAddress = WalletAddress("0").rawValue,
                amount = BigInteger.ZERO,
                lockDurationInSeconds = BigInteger.ZERO,
                lockContractAddress = WalletAddress("0").rawValue,
                senderAddress = "invalid",
                arbitraryData = null,
                screenConfig = null
            )
        }

        verify("request with invalid eth address is marked as invalid") {
            val violations = validator.validate(requestWithInvalidEthAddress).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("value must be a valid Ethereum address")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("senderAddress")
        }

        val requestWithTooLongEthAddress = suppose("request with too long eth address is created") {
            CreateErc20LockRequest(
                redirectUrl = null,
                tokenAddress = WalletAddress("0").rawValue,
                amount = BigInteger.ZERO,
                lockDurationInSeconds = BigInteger.ZERO,
                lockContractAddress = WalletAddress("0").rawValue,
                senderAddress = WalletAddress("a").rawValue + "b",
                arbitraryData = null,
                screenConfig = null
            )
        }

        verify("request with too long eth address is marked as invalid") {
            val violations = validator.validate(requestWithTooLongEthAddress).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("value must be a valid Ethereum address")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("senderAddress")
        }

        val requestWithEmptyEthAddress = suppose("request with empty eth address is created") {
            CreateErc20LockRequest(
                redirectUrl = null,
                tokenAddress = WalletAddress("0").rawValue,
                amount = BigInteger.ZERO,
                lockDurationInSeconds = BigInteger.ZERO,
                lockContractAddress = WalletAddress("0").rawValue,
                senderAddress = "",
                arbitraryData = null,
                screenConfig = null
            )
        }

        verify("request with empty eth address is marked as invalid") {
            val violations = validator.validate(requestWithEmptyEthAddress).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("value must be a valid Ethereum address")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("senderAddress")
        }

        val requestWithValidEthAddress = suppose("request with valid eth address is created") {
            CreateErc20LockRequest(
                redirectUrl = null,
                tokenAddress = WalletAddress("0").rawValue,
                amount = BigInteger.ZERO,
                lockDurationInSeconds = BigInteger.ZERO,
                lockContractAddress = WalletAddress("0").rawValue,
                senderAddress = WalletAddress("a").rawValue,
                arbitraryData = null,
                screenConfig = null
            )
        }

        verify("request with valid eth address is marked as valid") {
            val violations = validator.validate(requestWithValidEthAddress).toList()

            assertThat(violations).withMessage()
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowTooLongJsonForArbitraryData() {
        val tooLongValue = "a".repeat(REQUEST_BODY_MAX_JSON_CHARS + 1)
        val requestWithTooLongJson = suppose("request with too long JSON is created") {
            CreateErc20LockRequest(
                redirectUrl = null,
                tokenAddress = WalletAddress("0").rawValue,
                amount = BigInteger.ZERO,
                lockDurationInSeconds = BigInteger.ZERO,
                lockContractAddress = WalletAddress("0").rawValue,
                senderAddress = null,
                arbitraryData = JsonNodeConverter().from(JSON.valueOf("{\"value\":\"$tooLongValue\"}")),
                screenConfig = null
            )
        }

        verify("request with too long JSON is marked as invalid") {
            val violations = validator.validate(requestWithTooLongJson).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("value must be a valid JSON of at most 5000 characters")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("arbitraryData")
        }

        val requestWithValidLengthJson = suppose("request with valid length JSON is created") {
            CreateErc20LockRequest(
                redirectUrl = null,
                tokenAddress = WalletAddress("0").rawValue,
                amount = BigInteger.ZERO,
                lockDurationInSeconds = BigInteger.ZERO,
                lockContractAddress = WalletAddress("0").rawValue,
                senderAddress = null,
                arbitraryData = TestData.EMPTY_JSON_OBJECT,
                screenConfig = null
            )
        }

        verify("request with valid JSON string is marked as valid") {
            val violations = validator.validate(requestWithValidLengthJson).toList()

            assertThat(violations).withMessage()
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowTooLongStringForScreenConfigBeforeActionMessage() {
        val requestWithTooLongString = suppose("request with too long string is created") {
            CreateErc20LockRequest(
                redirectUrl = null,
                tokenAddress = WalletAddress("0").rawValue,
                amount = BigInteger.ZERO,
                lockDurationInSeconds = BigInteger.ZERO,
                lockContractAddress = WalletAddress("0").rawValue,
                senderAddress = null,
                arbitraryData = null,
                screenConfig = ScreenConfig(
                    beforeActionMessage = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH + 1),
                    afterActionMessage = null
                )
            )
        }

        verify("request with too long string is marked as invalid") {
            val violations = validator.validate(requestWithTooLongString).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("size must be between 0 and 256")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("screenConfig.beforeActionMessage")
        }

        val requestWithValidLengthString = suppose("request with valid length string is created") {
            CreateErc20LockRequest(
                redirectUrl = null,
                tokenAddress = WalletAddress("0").rawValue,
                amount = BigInteger.ZERO,
                lockDurationInSeconds = BigInteger.ZERO,
                lockContractAddress = WalletAddress("0").rawValue,
                senderAddress = null,
                arbitraryData = null,
                screenConfig = ScreenConfig(
                    beforeActionMessage = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH),
                    afterActionMessage = null
                )
            )
        }

        verify("request with valid length string is marked as valid") {
            val violations = validator.validate(requestWithValidLengthString).toList()

            assertThat(violations).withMessage()
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowTooLongStringForScreenConfigAfterActionMessage() {
        val requestWithTooLongString = suppose("request with too long string is created") {
            CreateErc20LockRequest(
                redirectUrl = null,
                tokenAddress = WalletAddress("0").rawValue,
                amount = BigInteger.ZERO,
                lockDurationInSeconds = BigInteger.ZERO,
                lockContractAddress = WalletAddress("0").rawValue,
                senderAddress = null,
                arbitraryData = null,
                screenConfig = ScreenConfig(
                    beforeActionMessage = null,
                    afterActionMessage = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH + 1)
                )
            )
        }

        verify("request with too long string is marked as invalid") {
            val violations = validator.validate(requestWithTooLongString).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("size must be between 0 and 256")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("screenConfig.afterActionMessage")
        }

        val requestWithValidLengthString = suppose("request with valid length string is created") {
            CreateErc20LockRequest(
                redirectUrl = null,
                tokenAddress = WalletAddress("0").rawValue,
                amount = BigInteger.ZERO,
                lockDurationInSeconds = BigInteger.ZERO,
                lockContractAddress = WalletAddress("0").rawValue,
                senderAddress = null,
                arbitraryData = null,
                screenConfig = ScreenConfig(
                    beforeActionMessage = null,
                    afterActionMessage = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH)
                )
            )
        }

        verify("request with valid length string is marked as valid") {
            val violations = validator.validate(requestWithValidLengthString).toList()

            assertThat(violations).withMessage()
                .isEmpty()
        }
    }
}
