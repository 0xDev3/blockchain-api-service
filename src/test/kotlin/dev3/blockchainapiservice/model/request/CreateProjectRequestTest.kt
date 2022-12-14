package dev3.blockchainapiservice.model.request

import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.config.validation.ValidationConstants
import dev3.blockchainapiservice.features.api.access.model.request.CreateProjectRequest
import dev3.blockchainapiservice.util.WalletAddress
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

            expectThat(violations.size)
                .isOne()
            expectThat(violations[0].message)
                .isEqualTo("value must be a valid Ethereum address")
            expectThat(violations[0].propertyPath.toString())
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

            expectThat(violations.size)
                .isOne()
            expectThat(violations[0].message)
                .isEqualTo("value must be a valid Ethereum address")
            expectThat(violations[0].propertyPath.toString())
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

            expectThat(violations.size)
                .isOne()
            expectThat(violations[0].message)
                .isEqualTo("value must be a valid Ethereum address")
            expectThat(violations[0].propertyPath.toString())
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

            expectThat(violations)
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

            expectThat(violations.size)
                .isOne()
            expectThat(violations[0].message)
                .isEqualTo("size must be between 0 and ${ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH}")
            expectThat(violations[0].propertyPath.toString())
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

            expectThat(violations)
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

            expectThat(violations.size)
                .isOne()
            expectThat(violations[0].message)
                .isEqualTo("size must be between 0 and ${ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH}")
            expectThat(violations[0].propertyPath.toString())
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

            expectThat(violations)
                .isEmpty()
        }
    }
}
