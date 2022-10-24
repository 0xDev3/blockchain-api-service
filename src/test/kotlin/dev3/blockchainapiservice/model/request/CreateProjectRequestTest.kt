package com.ampnet.blockchainapiservice.model.request

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.config.validation.ValidationConstants
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
class CreateProjectRequestTest : TestBase() {

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
    fun mustNotAllowInvalidEthAddressForIssuerContractAddress() {
        val requestWithInvalidEthAddress = suppose("request with invalid eth address is created") {
            CreateProjectRequest(
                issuerContractAddress = "invalid",
                baseRedirectUrl = "",
                chainId = 1L,
                customRpcUrl = null
            )
        }

        verify("request with invalid eth address is marked as invalid") {
            val violations = validator.validate(requestWithInvalidEthAddress).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("value must be a valid Ethereum address")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("issuerContractAddress")
        }

        val requestWithTooLongEthAddress = suppose("request with too long eth address is created") {
            CreateProjectRequest(
                issuerContractAddress = WalletAddress("a").rawValue + "b",
                baseRedirectUrl = "",
                chainId = 1L,
                customRpcUrl = null
            )
        }

        verify("request with too long eth address is marked as invalid") {
            val violations = validator.validate(requestWithTooLongEthAddress).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("value must be a valid Ethereum address")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("issuerContractAddress")
        }

        val requestWithEmptyEthAddress = suppose("request with empty eth address is created") {
            CreateProjectRequest(
                issuerContractAddress = "",
                baseRedirectUrl = "",
                chainId = 1L,
                customRpcUrl = null
            )
        }

        verify("request with empty eth address is marked as invalid") {
            val violations = validator.validate(requestWithEmptyEthAddress).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("value must be a valid Ethereum address")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("issuerContractAddress")
        }

        val requestWithValidEthAddress = suppose("request with valid eth address is created") {
            CreateProjectRequest(
                issuerContractAddress = WalletAddress("a").rawValue,
                baseRedirectUrl = "",
                chainId = 1L,
                customRpcUrl = null
            )
        }

        verify("request with valid eth address is marked as valid") {
            val violations = validator.validate(requestWithValidEthAddress).toList()

            assertThat(violations).withMessage()
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowTooLongStringForBaseRedirectUrl() {
        val requestWithTooLongString = suppose("request with too long string is created") {
            CreateProjectRequest(
                issuerContractAddress = WalletAddress("0").rawValue,
                baseRedirectUrl = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH + 1),
                chainId = 1L,
                customRpcUrl = null
            )
        }

        verify("request with too long string is marked as invalid") {
            val violations = validator.validate(requestWithTooLongString).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("size must be between 0 and 256")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("baseRedirectUrl")
        }

        val requestWithValidLengthString = suppose("request with valid length string is created") {
            CreateProjectRequest(
                issuerContractAddress = WalletAddress("0").rawValue,
                baseRedirectUrl = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH),
                chainId = 1L,
                customRpcUrl = null
            )
        }

        verify("request with valid length string is marked as valid") {
            val violations = validator.validate(requestWithValidLengthString).toList()

            assertThat(violations).withMessage()
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowTooLongStringForCustomRpcUrl() {
        val requestWithTooLongString = suppose("request with too long string is created") {
            CreateProjectRequest(
                issuerContractAddress = WalletAddress("0").rawValue,
                baseRedirectUrl = "",
                chainId = 1L,
                customRpcUrl = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH + 1)
            )
        }

        verify("request with too long string is marked as invalid") {
            val violations = validator.validate(requestWithTooLongString).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("size must be between 0 and 256")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("customRpcUrl")
        }

        val requestWithValidLengthString = suppose("request with valid length string is created") {
            CreateProjectRequest(
                issuerContractAddress = WalletAddress("0").rawValue,
                baseRedirectUrl = "",
                chainId = 1L,
                customRpcUrl = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH)
            )
        }

        verify("request with valid length string is marked as valid") {
            val violations = validator.validate(requestWithValidLengthString).toList()

            assertThat(violations).withMessage()
                .isEmpty()
        }
    }
}
