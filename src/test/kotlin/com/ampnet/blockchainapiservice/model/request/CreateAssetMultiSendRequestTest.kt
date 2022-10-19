package com.ampnet.blockchainapiservice.model.request

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.config.validation.ValidationConstants
import com.ampnet.blockchainapiservice.config.validation.ValidationConstants.REQUEST_BODY_MAX_JSON_CHARS
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.util.AssetType
import com.ampnet.blockchainapiservice.util.ContractAddress
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
class CreateAssetMultiSendRequestTest : TestBase() {

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
            CreateAssetMultiSendRequest(
                redirectUrl = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH + 1),
                tokenAddress = null,
                disperseContractAddress = ContractAddress("0").rawValue,
                assetType = AssetType.NATIVE,
                items = emptyList(),
                senderAddress = null,
                arbitraryData = null,
                approveScreenConfig = null,
                disperseScreenConfig = null
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
            CreateAssetMultiSendRequest(
                redirectUrl = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH),
                tokenAddress = null,
                disperseContractAddress = ContractAddress("0").rawValue,
                assetType = AssetType.NATIVE,
                items = emptyList(),
                senderAddress = null,
                arbitraryData = null,
                approveScreenConfig = null,
                disperseScreenConfig = null
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
            CreateAssetMultiSendRequest(
                redirectUrl = null,
                tokenAddress = "invalid",
                disperseContractAddress = ContractAddress("0").rawValue,
                assetType = AssetType.TOKEN,
                items = emptyList(),
                senderAddress = null,
                arbitraryData = null,
                approveScreenConfig = null,
                disperseScreenConfig = null
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
            CreateAssetMultiSendRequest(
                redirectUrl = null,
                tokenAddress = WalletAddress("a").rawValue + "b",
                disperseContractAddress = ContractAddress("0").rawValue,
                assetType = AssetType.TOKEN,
                items = emptyList(),
                senderAddress = null,
                arbitraryData = null,
                approveScreenConfig = null,
                disperseScreenConfig = null
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
            CreateAssetMultiSendRequest(
                redirectUrl = null,
                tokenAddress = "",
                disperseContractAddress = ContractAddress("0").rawValue,
                assetType = AssetType.TOKEN,
                items = emptyList(),
                senderAddress = null,
                arbitraryData = null,
                approveScreenConfig = null,
                disperseScreenConfig = null
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
            CreateAssetMultiSendRequest(
                redirectUrl = null,
                tokenAddress = WalletAddress("a").rawValue,
                disperseContractAddress = ContractAddress("0").rawValue,
                assetType = AssetType.TOKEN,
                items = emptyList(),
                senderAddress = null,
                arbitraryData = null,
                approveScreenConfig = null,
                disperseScreenConfig = null
            )
        }

        verify("request with valid eth address is marked as valid") {
            val violations = validator.validate(requestWithValidEthAddress).toList()

            assertThat(violations).withMessage()
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowInvalidEthAddressForDisperseContractAddress() {
        val requestWithInvalidEthAddress = suppose("request with invalid eth address is created") {
            CreateAssetMultiSendRequest(
                redirectUrl = null,
                tokenAddress = ContractAddress("0").rawValue,
                disperseContractAddress = "invalid",
                assetType = AssetType.TOKEN,
                items = emptyList(),
                senderAddress = null,
                arbitraryData = null,
                approveScreenConfig = null,
                disperseScreenConfig = null
            )
        }

        verify("request with invalid eth address is marked as invalid") {
            val violations = validator.validate(requestWithInvalidEthAddress).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("value must be a valid Ethereum address")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("disperseContractAddress")
        }

        val requestWithTooLongEthAddress = suppose("request with too long eth address is created") {
            CreateAssetMultiSendRequest(
                redirectUrl = null,
                tokenAddress = ContractAddress("0").rawValue,
                disperseContractAddress = WalletAddress("a").rawValue + "b",
                assetType = AssetType.TOKEN,
                items = emptyList(),
                senderAddress = null,
                arbitraryData = null,
                approveScreenConfig = null,
                disperseScreenConfig = null
            )
        }

        verify("request with too long eth address is marked as invalid") {
            val violations = validator.validate(requestWithTooLongEthAddress).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("value must be a valid Ethereum address")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("disperseContractAddress")
        }

        val requestWithEmptyEthAddress = suppose("request with empty eth address is created") {
            CreateAssetMultiSendRequest(
                redirectUrl = null,
                tokenAddress = ContractAddress("0").rawValue,
                disperseContractAddress = "",
                assetType = AssetType.TOKEN,
                items = emptyList(),
                senderAddress = null,
                arbitraryData = null,
                approveScreenConfig = null,
                disperseScreenConfig = null
            )
        }

        verify("request with empty eth address is marked as invalid") {
            val violations = validator.validate(requestWithEmptyEthAddress).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("value must be a valid Ethereum address")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("disperseContractAddress")
        }

        val requestWithValidEthAddress = suppose("request with valid eth address is created") {
            CreateAssetMultiSendRequest(
                redirectUrl = null,
                tokenAddress = WalletAddress("a").rawValue,
                disperseContractAddress = ContractAddress("0").rawValue,
                assetType = AssetType.TOKEN,
                items = emptyList(),
                senderAddress = null,
                arbitraryData = null,
                approveScreenConfig = null,
                disperseScreenConfig = null
            )
        }

        verify("request with valid eth address is marked as valid") {
            val violations = validator.validate(requestWithValidEthAddress).toList()

            assertThat(violations).withMessage()
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowInvalidEthAddressForItemWalletAddress() {
        val requestWithInvalidEthAddress = suppose("request with invalid eth address is created") {
            CreateAssetMultiSendRequest(
                redirectUrl = null,
                tokenAddress = null,
                disperseContractAddress = ContractAddress("0").rawValue,
                assetType = AssetType.NATIVE,
                items = listOf(
                    MultiPaymentTemplateItemRequest(
                        walletAddress = "invalid",
                        amount = BigInteger.ONE,
                        itemName = null
                    )
                ),
                senderAddress = null,
                arbitraryData = null,
                approveScreenConfig = null,
                disperseScreenConfig = null
            )
        }

        verify("request with invalid eth address is marked as invalid") {
            val violations = validator.validate(requestWithInvalidEthAddress).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("value must be a valid Ethereum address")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("items[0].walletAddress")
        }

        val requestWithTooLongEthAddress = suppose("request with too long eth address is created") {
            CreateAssetMultiSendRequest(
                redirectUrl = null,
                tokenAddress = null,
                disperseContractAddress = ContractAddress("0").rawValue,
                assetType = AssetType.NATIVE,
                items = listOf(
                    MultiPaymentTemplateItemRequest(
                        walletAddress = WalletAddress("a").rawValue + "b",
                        amount = BigInteger.ONE,
                        itemName = null
                    )
                ),
                senderAddress = null,
                arbitraryData = null,
                approveScreenConfig = null,
                disperseScreenConfig = null
            )
        }

        verify("request with too long eth address is marked as invalid") {
            val violations = validator.validate(requestWithTooLongEthAddress).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("value must be a valid Ethereum address")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("items[0].walletAddress")
        }

        val requestWithEmptyEthAddress = suppose("request with empty eth address is created") {
            CreateAssetMultiSendRequest(
                redirectUrl = null,
                tokenAddress = null,
                disperseContractAddress = ContractAddress("0").rawValue,
                assetType = AssetType.NATIVE,
                items = listOf(
                    MultiPaymentTemplateItemRequest(
                        walletAddress = "",
                        amount = BigInteger.ONE,
                        itemName = null
                    )
                ),
                senderAddress = null,
                arbitraryData = null,
                approveScreenConfig = null,
                disperseScreenConfig = null
            )
        }

        verify("request with empty eth address is marked as invalid") {
            val violations = validator.validate(requestWithEmptyEthAddress).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("value must be a valid Ethereum address")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("items[0].walletAddress")
        }

        val requestWithValidEthAddress = suppose("request with valid eth address is created") {
            CreateAssetMultiSendRequest(
                redirectUrl = null,
                tokenAddress = null,
                disperseContractAddress = ContractAddress("0").rawValue,
                assetType = AssetType.NATIVE,
                items = listOf(
                    MultiPaymentTemplateItemRequest(
                        walletAddress = WalletAddress("a").rawValue,
                        amount = BigInteger.ONE,
                        itemName = null
                    )
                ),
                senderAddress = null,
                arbitraryData = null,
                approveScreenConfig = null,
                disperseScreenConfig = null
            )
        }

        verify("request with valid eth address is marked as valid") {
            val violations = validator.validate(requestWithValidEthAddress).toList()

            assertThat(violations).withMessage()
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowNegativeOrTooBigValueForItemAmount() {
        val requestWithNegativeUint256 = suppose("request with negative uint256 is created") {
            CreateAssetMultiSendRequest(
                redirectUrl = null,
                tokenAddress = null,
                disperseContractAddress = ContractAddress("0").rawValue,
                assetType = AssetType.NATIVE,
                items = listOf(
                    MultiPaymentTemplateItemRequest(
                        walletAddress = WalletAddress("a").rawValue,
                        amount = BigInteger.valueOf(-1L),
                        itemName = null
                    )
                ),
                senderAddress = null,
                arbitraryData = null,
                approveScreenConfig = null,
                disperseScreenConfig = null
            )
        }

        verify("request with negative uint256 is marked as invalid") {
            val violations = validator.validate(requestWithNegativeUint256).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("value must be within range [0, 2^256]")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("items[0].amount")
        }

        val requestWithTooBigUint256 = suppose("request with too big uint256 is created") {
            CreateAssetMultiSendRequest(
                redirectUrl = null,
                tokenAddress = null,
                disperseContractAddress = ContractAddress("0").rawValue,
                assetType = AssetType.NATIVE,
                items = listOf(
                    MultiPaymentTemplateItemRequest(
                        walletAddress = WalletAddress("a").rawValue,
                        amount = BigInteger.TWO.pow(256) + BigInteger.ONE,
                        itemName = null
                    )
                ),
                senderAddress = null,
                arbitraryData = null,
                approveScreenConfig = null,
                disperseScreenConfig = null
            )
        }

        verify("request with too big uint256 is marked as invalid") {
            val violations = validator.validate(requestWithTooBigUint256).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("value must be within range [0, 2^256]")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("items[0].amount")
        }

        val requestWithValidUint256 = suppose("request with valid uint256 is created") {
            CreateAssetMultiSendRequest(
                redirectUrl = null,
                tokenAddress = null,
                disperseContractAddress = ContractAddress("0").rawValue,
                assetType = AssetType.NATIVE,
                items = listOf(
                    MultiPaymentTemplateItemRequest(
                        walletAddress = WalletAddress("a").rawValue,
                        amount = BigInteger.TWO.pow(256),
                        itemName = null
                    )
                ),
                senderAddress = null,
                arbitraryData = null,
                approveScreenConfig = null,
                disperseScreenConfig = null
            )
        }

        verify("request with valid uint256 is marked as valid") {
            val violations = validator.validate(requestWithValidUint256).toList()

            assertThat(violations).withMessage()
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowTooLongStringForItemName() {
        val requestWithTooLongString = suppose("request with too long string is created") {
            CreateAssetMultiSendRequest(
                redirectUrl = null,
                tokenAddress = null,
                disperseContractAddress = ContractAddress("0").rawValue,
                assetType = AssetType.NATIVE,
                items = listOf(
                    MultiPaymentTemplateItemRequest(
                        walletAddress = WalletAddress("a").rawValue,
                        amount = BigInteger.ONE,
                        itemName = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH + 1)
                    )
                ),
                senderAddress = null,
                arbitraryData = null,
                approveScreenConfig = null,
                disperseScreenConfig = null
            )
        }

        verify("request with too long string is marked as invalid") {
            val violations = validator.validate(requestWithTooLongString).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("size must be between 0 and 256")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("items[0].itemName")
        }

        val requestWithValidLengthString = suppose("request with valid length string is created") {
            CreateAssetMultiSendRequest(
                redirectUrl = null,
                tokenAddress = null,
                disperseContractAddress = ContractAddress("0").rawValue,
                assetType = AssetType.NATIVE,
                items = listOf(
                    MultiPaymentTemplateItemRequest(
                        walletAddress = WalletAddress("a").rawValue,
                        amount = BigInteger.ONE,
                        itemName = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH)
                    )
                ),
                senderAddress = null,
                arbitraryData = null,
                approveScreenConfig = null,
                disperseScreenConfig = null
            )
        }

        verify("request with valid length string is marked as valid") {
            val violations = validator.validate(requestWithValidLengthString).toList()

            assertThat(violations).withMessage()
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowInvalidEthAddressForSenderAddress() {
        val requestWithInvalidEthAddress = suppose("request with invalid eth address is created") {
            CreateAssetMultiSendRequest(
                redirectUrl = null,
                tokenAddress = null,
                disperseContractAddress = ContractAddress("0").rawValue,
                assetType = AssetType.NATIVE,
                items = emptyList(),
                senderAddress = "invalid",
                arbitraryData = null,
                approveScreenConfig = null,
                disperseScreenConfig = null
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
            CreateAssetMultiSendRequest(
                redirectUrl = null,
                tokenAddress = null,
                disperseContractAddress = ContractAddress("0").rawValue,
                assetType = AssetType.NATIVE,
                items = emptyList(),
                senderAddress = WalletAddress("a").rawValue + "b",
                arbitraryData = null,
                approveScreenConfig = null,
                disperseScreenConfig = null
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
            CreateAssetMultiSendRequest(
                redirectUrl = null,
                tokenAddress = null,
                disperseContractAddress = ContractAddress("0").rawValue,
                assetType = AssetType.NATIVE,
                items = emptyList(),
                senderAddress = "",
                arbitraryData = null,
                approveScreenConfig = null,
                disperseScreenConfig = null
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
            CreateAssetMultiSendRequest(
                redirectUrl = null,
                tokenAddress = null,
                disperseContractAddress = ContractAddress("0").rawValue,
                assetType = AssetType.NATIVE,
                items = emptyList(),
                senderAddress = WalletAddress("a").rawValue,
                arbitraryData = null,
                approveScreenConfig = null,
                disperseScreenConfig = null
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
            CreateAssetMultiSendRequest(
                redirectUrl = null,
                tokenAddress = null,
                disperseContractAddress = ContractAddress("0").rawValue,
                assetType = AssetType.NATIVE,
                items = emptyList(),
                senderAddress = WalletAddress("a").rawValue,
                arbitraryData = JsonNodeConverter().from(JSON.valueOf("{\"value\":\"$tooLongValue\"}")),
                approveScreenConfig = null,
                disperseScreenConfig = null
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
            CreateAssetMultiSendRequest(
                redirectUrl = null,
                tokenAddress = null,
                disperseContractAddress = ContractAddress("0").rawValue,
                assetType = AssetType.NATIVE,
                items = emptyList(),
                senderAddress = WalletAddress("a").rawValue,
                arbitraryData = TestData.EMPTY_JSON_OBJECT,
                approveScreenConfig = null,
                disperseScreenConfig = null
            )
        }

        verify("request with valid JSON string is marked as valid") {
            val violations = validator.validate(requestWithValidLengthJson).toList()

            assertThat(violations).withMessage()
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowTooLongStringForApproveScreenConfigBeforeActionMessage() {
        val requestWithTooLongString = suppose("request with too long string is created") {
            CreateAssetMultiSendRequest(
                redirectUrl = null,
                tokenAddress = null,
                disperseContractAddress = ContractAddress("0").rawValue,
                assetType = AssetType.NATIVE,
                items = emptyList(),
                senderAddress = WalletAddress("a").rawValue,
                arbitraryData = null,
                approveScreenConfig = ScreenConfig(
                    beforeActionMessage = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH + 1),
                    afterActionMessage = null
                ),
                disperseScreenConfig = null
            )
        }

        verify("request with too long string is marked as invalid") {
            val violations = validator.validate(requestWithTooLongString).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("size must be between 0 and 256")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("approveScreenConfig.beforeActionMessage")
        }

        val requestWithValidLengthString = suppose("request with valid length string is created") {
            CreateAssetMultiSendRequest(
                redirectUrl = null,
                tokenAddress = null,
                disperseContractAddress = ContractAddress("0").rawValue,
                assetType = AssetType.NATIVE,
                items = emptyList(),
                senderAddress = WalletAddress("a").rawValue,
                arbitraryData = null,
                approveScreenConfig = ScreenConfig(
                    beforeActionMessage = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH),
                    afterActionMessage = null
                ),
                disperseScreenConfig = null
            )
        }

        verify("request with valid length string is marked as valid") {
            val violations = validator.validate(requestWithValidLengthString).toList()

            assertThat(violations).withMessage()
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowTooLongStringForApproveScreenConfigAfterActionMessage() {
        val requestWithTooLongString = suppose("request with too long string is created") {
            CreateAssetMultiSendRequest(
                redirectUrl = null,
                tokenAddress = null,
                disperseContractAddress = ContractAddress("0").rawValue,
                assetType = AssetType.NATIVE,
                items = emptyList(),
                senderAddress = WalletAddress("a").rawValue,
                arbitraryData = null,
                approveScreenConfig = ScreenConfig(
                    beforeActionMessage = null,
                    afterActionMessage = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH + 1)
                ),
                disperseScreenConfig = null
            )
        }

        verify("request with too long string is marked as invalid") {
            val violations = validator.validate(requestWithTooLongString).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("size must be between 0 and 256")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("approveScreenConfig.afterActionMessage")
        }

        val requestWithValidLengthString = suppose("request with valid length string is created") {
            CreateAssetMultiSendRequest(
                redirectUrl = null,
                tokenAddress = null,
                disperseContractAddress = ContractAddress("0").rawValue,
                assetType = AssetType.NATIVE,
                items = emptyList(),
                senderAddress = WalletAddress("a").rawValue,
                arbitraryData = null,
                approveScreenConfig = ScreenConfig(
                    beforeActionMessage = null,
                    afterActionMessage = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH)
                ),
                disperseScreenConfig = null
            )
        }

        verify("request with valid length string is marked as valid") {
            val violations = validator.validate(requestWithValidLengthString).toList()

            assertThat(violations).withMessage()
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowTooLongStringForDisperseScreenConfigBeforeActionMessage() {
        val requestWithTooLongString = suppose("request with too long string is created") {
            CreateAssetMultiSendRequest(
                redirectUrl = null,
                tokenAddress = null,
                disperseContractAddress = ContractAddress("0").rawValue,
                assetType = AssetType.NATIVE,
                items = emptyList(),
                senderAddress = WalletAddress("a").rawValue,
                arbitraryData = null,
                approveScreenConfig = null,
                disperseScreenConfig = ScreenConfig(
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
                .isEqualTo("disperseScreenConfig.beforeActionMessage")
        }

        val requestWithValidLengthString = suppose("request with valid length string is created") {
            CreateAssetMultiSendRequest(
                redirectUrl = null,
                tokenAddress = null,
                disperseContractAddress = ContractAddress("0").rawValue,
                assetType = AssetType.NATIVE,
                items = emptyList(),
                senderAddress = WalletAddress("a").rawValue,
                arbitraryData = null,
                approveScreenConfig = null,
                disperseScreenConfig = ScreenConfig(
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
    fun mustNotAllowTooLongStringForDisperseScreenConfigAfterActionMessage() {
        val requestWithTooLongString = suppose("request with too long string is created") {
            CreateAssetMultiSendRequest(
                redirectUrl = null,
                tokenAddress = null,
                disperseContractAddress = ContractAddress("0").rawValue,
                assetType = AssetType.NATIVE,
                items = emptyList(),
                senderAddress = WalletAddress("a").rawValue,
                arbitraryData = null,
                approveScreenConfig = null,
                disperseScreenConfig = ScreenConfig(
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
                .isEqualTo("disperseScreenConfig.afterActionMessage")
        }

        val requestWithValidLengthString = suppose("request with valid length string is created") {
            CreateAssetMultiSendRequest(
                redirectUrl = null,
                tokenAddress = null,
                disperseContractAddress = ContractAddress("0").rawValue,
                assetType = AssetType.NATIVE,
                items = emptyList(),
                senderAddress = WalletAddress("a").rawValue,
                arbitraryData = null,
                approveScreenConfig = null,
                disperseScreenConfig = ScreenConfig(
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