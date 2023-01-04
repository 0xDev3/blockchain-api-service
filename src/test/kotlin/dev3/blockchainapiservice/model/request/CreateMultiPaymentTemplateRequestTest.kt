package dev3.blockchainapiservice.model.request

import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.config.validation.ValidationConstants
import dev3.blockchainapiservice.features.asset.multisend.model.request.CreateMultiPaymentTemplateRequest
import dev3.blockchainapiservice.util.AssetType
import dev3.blockchainapiservice.util.WalletAddress
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import javax.validation.Validation
import javax.validation.Validator
import javax.validation.ValidatorFactory

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreateMultiPaymentTemplateRequestTest : TestBase() {

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
    fun mustNotAllowTooLongStringForTemplateName() {
        val requestWithTooLongString = suppose("request with too long string is created") {
            CreateMultiPaymentTemplateRequest(
                templateName = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH + 1),
                assetType = AssetType.NATIVE,
                tokenAddress = null,
                chainId = 1L,
                items = emptyList()
            )
        }

        verify("request with too long string is marked as invalid") {
            val violations = validator.validate(requestWithTooLongString).toList()

            expectThat(violations.size)
                .isOne()
            expectThat(violations[0].message)
                .isEqualTo("size must be between 0 and ${ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH}")
            expectThat(violations[0].propertyPath.toString())
                .isEqualTo("templateName")
        }

        val requestWithValidLengthString = suppose("request with valid length string is created") {
            CreateMultiPaymentTemplateRequest(
                templateName = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH),
                assetType = AssetType.NATIVE,
                tokenAddress = null,
                chainId = 1L,
                items = emptyList()
            )
        }

        verify("request with valid length string is marked as valid") {
            val violations = validator.validate(requestWithValidLengthString).toList()

            expectThat(violations)
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowInvalidEthAddressForTokenAddress() {
        val requestWithInvalidEthAddress = suppose("request with invalid eth address is created") {
            CreateMultiPaymentTemplateRequest(
                templateName = "",
                tokenAddress = "invalid",
                assetType = AssetType.TOKEN,
                chainId = 1L,
                items = emptyList()
            )
        }

        verify("request with invalid eth address is marked as invalid") {
            val violations = validator.validate(requestWithInvalidEthAddress).toList()

            expectThat(violations.size)
                .isOne()
            expectThat(violations[0].message)
                .isEqualTo("value must be a valid Ethereum address")
            expectThat(violations[0].propertyPath.toString())
                .isEqualTo("tokenAddress")
        }

        val requestWithTooLongEthAddress = suppose("request with too long eth address is created") {
            CreateMultiPaymentTemplateRequest(
                templateName = "",
                tokenAddress = WalletAddress("a").rawValue + "b",
                assetType = AssetType.TOKEN,
                chainId = 1L,
                items = emptyList()
            )
        }

        verify("request with too long eth address is marked as invalid") {
            val violations = validator.validate(requestWithTooLongEthAddress).toList()

            expectThat(violations.size)
                .isOne()
            expectThat(violations[0].message)
                .isEqualTo("value must be a valid Ethereum address")
            expectThat(violations[0].propertyPath.toString())
                .isEqualTo("tokenAddress")
        }

        val requestWithEmptyEthAddress = suppose("request with empty eth address is created") {
            CreateMultiPaymentTemplateRequest(
                templateName = "",
                tokenAddress = "",
                assetType = AssetType.TOKEN,
                chainId = 1L,
                items = emptyList()
            )
        }

        verify("request with empty eth address is marked as invalid") {
            val violations = validator.validate(requestWithEmptyEthAddress).toList()

            expectThat(violations.size)
                .isOne()
            expectThat(violations[0].message)
                .isEqualTo("value must be a valid Ethereum address")
            expectThat(violations[0].propertyPath.toString())
                .isEqualTo("tokenAddress")
        }

        val requestWithValidEthAddress = suppose("request with valid eth address is created") {
            CreateMultiPaymentTemplateRequest(
                templateName = "",
                tokenAddress = WalletAddress("a").rawValue,
                assetType = AssetType.TOKEN,
                chainId = 1L,
                items = emptyList()
            )
        }

        verify("request with valid eth address is marked as valid") {
            val violations = validator.validate(requestWithValidEthAddress).toList()

            expectThat(violations)
                .isEmpty()
        }
    }
}
