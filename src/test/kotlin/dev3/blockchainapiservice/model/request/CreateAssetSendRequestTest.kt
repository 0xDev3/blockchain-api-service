package dev3.blockchainapiservice.model.request

import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.config.validation.ValidationConstants
import dev3.blockchainapiservice.config.validation.ValidationConstants.REQUEST_BODY_MAX_JSON_CHARS
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.util.AssetType
import dev3.blockchainapiservice.util.JsonNodeConverter
import dev3.blockchainapiservice.util.WalletAddress
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
class CreateAssetSendRequestTest : TestBase() {

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
            CreateAssetSendRequest(
                redirectUrl = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH + 1),
                tokenAddress = null,
                assetType = AssetType.NATIVE,
                amount = BigInteger.ZERO,
                senderAddress = null,
                recipientAddress = WalletAddress("0").rawValue,
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
            CreateAssetSendRequest(
                redirectUrl = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH),
                tokenAddress = null,
                assetType = AssetType.NATIVE,
                amount = BigInteger.ZERO,
                senderAddress = null,
                recipientAddress = WalletAddress("0").rawValue,
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
            CreateAssetSendRequest(
                redirectUrl = null,
                tokenAddress = "invalid",
                assetType = AssetType.TOKEN,
                amount = BigInteger.ZERO,
                senderAddress = null,
                recipientAddress = WalletAddress("0").rawValue,
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
            CreateAssetSendRequest(
                redirectUrl = null,
                tokenAddress = WalletAddress("a").rawValue + "b",
                assetType = AssetType.TOKEN,
                amount = BigInteger.ZERO,
                senderAddress = null,
                recipientAddress = WalletAddress("0").rawValue,
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
            CreateAssetSendRequest(
                redirectUrl = null,
                tokenAddress = "",
                assetType = AssetType.TOKEN,
                amount = BigInteger.ZERO,
                senderAddress = null,
                recipientAddress = WalletAddress("0").rawValue,
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
            CreateAssetSendRequest(
                redirectUrl = null,
                tokenAddress = WalletAddress("a").rawValue,
                assetType = AssetType.TOKEN,
                amount = BigInteger.ZERO,
                senderAddress = null,
                recipientAddress = WalletAddress("0").rawValue,
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
            CreateAssetSendRequest(
                redirectUrl = null,
                tokenAddress = null,
                assetType = AssetType.NATIVE,
                amount = BigInteger.valueOf(-1L),
                senderAddress = null,
                recipientAddress = WalletAddress("0").rawValue,
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
            CreateAssetSendRequest(
                redirectUrl = null,
                tokenAddress = null,
                assetType = AssetType.NATIVE,
                amount = BigInteger.TWO.pow(256) + BigInteger.ONE,
                senderAddress = null,
                recipientAddress = WalletAddress("0").rawValue,
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
            CreateAssetSendRequest(
                redirectUrl = null,
                tokenAddress = null,
                assetType = AssetType.NATIVE,
                amount = BigInteger.TWO.pow(256),
                senderAddress = null,
                recipientAddress = WalletAddress("0").rawValue,
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
    fun mustNotAllowInvalidEthAddressForSenderAddress() {
        val requestWithInvalidEthAddress = suppose("request with invalid eth address is created") {
            CreateAssetSendRequest(
                redirectUrl = null,
                tokenAddress = null,
                assetType = AssetType.NATIVE,
                amount = BigInteger.ZERO,
                senderAddress = "invalid",
                recipientAddress = WalletAddress("0").rawValue,
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
            CreateAssetSendRequest(
                redirectUrl = null,
                tokenAddress = null,
                assetType = AssetType.NATIVE,
                amount = BigInteger.ZERO,
                senderAddress = WalletAddress("a").rawValue + "b",
                recipientAddress = WalletAddress("0").rawValue,
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
            CreateAssetSendRequest(
                redirectUrl = null,
                tokenAddress = null,
                assetType = AssetType.NATIVE,
                amount = BigInteger.ZERO,
                senderAddress = "",
                recipientAddress = WalletAddress("0").rawValue,
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
            CreateAssetSendRequest(
                redirectUrl = null,
                tokenAddress = null,
                assetType = AssetType.NATIVE,
                amount = BigInteger.ZERO,
                senderAddress = WalletAddress("a").rawValue,
                recipientAddress = WalletAddress("0").rawValue,
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
    fun mustNotAllowInvalidEthAddressForRecipientAddress() {
        val requestWithInvalidEthAddress = suppose("request with invalid eth address is created") {
            CreateAssetSendRequest(
                redirectUrl = null,
                tokenAddress = null,
                assetType = AssetType.NATIVE,
                amount = BigInteger.ZERO,
                senderAddress = null,
                recipientAddress = "invalid",
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
                .isEqualTo("recipientAddress")
        }

        val requestWithTooLongEthAddress = suppose("request with too long eth address is created") {
            CreateAssetSendRequest(
                redirectUrl = null,
                tokenAddress = null,
                assetType = AssetType.NATIVE,
                amount = BigInteger.ZERO,
                senderAddress = null,
                recipientAddress = WalletAddress("a").rawValue + "b",
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
                .isEqualTo("recipientAddress")
        }

        val requestWithEmptyEthAddress = suppose("request with empty eth address is created") {
            CreateAssetSendRequest(
                redirectUrl = null,
                tokenAddress = null,
                assetType = AssetType.NATIVE,
                amount = BigInteger.ZERO,
                senderAddress = null,
                recipientAddress = "",
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
                .isEqualTo("recipientAddress")
        }

        val requestWithValidEthAddress = suppose("request with valid eth address is created") {
            CreateAssetSendRequest(
                redirectUrl = null,
                tokenAddress = null,
                assetType = AssetType.NATIVE,
                amount = BigInteger.ZERO,
                senderAddress = null,
                recipientAddress = WalletAddress("a").rawValue,
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
            CreateAssetSendRequest(
                redirectUrl = null,
                tokenAddress = null,
                assetType = AssetType.NATIVE,
                amount = BigInteger.ZERO,
                senderAddress = null,
                recipientAddress = WalletAddress("0").rawValue,
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
            CreateAssetSendRequest(
                redirectUrl = null,
                tokenAddress = null,
                assetType = AssetType.NATIVE,
                amount = BigInteger.ZERO,
                senderAddress = null,
                recipientAddress = WalletAddress("0").rawValue,
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
            CreateAssetSendRequest(
                redirectUrl = null,
                tokenAddress = null,
                assetType = AssetType.NATIVE,
                amount = BigInteger.ZERO,
                senderAddress = null,
                recipientAddress = WalletAddress("0").rawValue,
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
            CreateAssetSendRequest(
                redirectUrl = null,
                tokenAddress = null,
                assetType = AssetType.NATIVE,
                amount = BigInteger.ZERO,
                senderAddress = null,
                recipientAddress = WalletAddress("0").rawValue,
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
            CreateAssetSendRequest(
                redirectUrl = null,
                tokenAddress = null,
                assetType = AssetType.NATIVE,
                amount = BigInteger.ZERO,
                senderAddress = null,
                recipientAddress = WalletAddress("0").rawValue,
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
            CreateAssetSendRequest(
                redirectUrl = null,
                tokenAddress = null,
                assetType = AssetType.NATIVE,
                amount = BigInteger.ZERO,
                senderAddress = null,
                recipientAddress = WalletAddress("0").rawValue,
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
