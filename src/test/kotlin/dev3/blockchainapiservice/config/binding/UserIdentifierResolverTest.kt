package dev3.blockchainapiservice.config.binding

import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.config.binding.annotation.UserIdentifierBinding
import dev3.blockchainapiservice.exception.BadAuthenticationException
import dev3.blockchainapiservice.model.result.UserIdentifier
import dev3.blockchainapiservice.model.result.UserWalletAddressIdentifier
import dev3.blockchainapiservice.repository.UserIdentifierRepository
import dev3.blockchainapiservice.service.UuidProvider
import dev3.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.springframework.core.MethodParameter
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID

class UserIdentifierResolverTest : TestBase() {

    companion object {
        // @formatter:off
        @Suppress("unused", "UNUSED_PARAMETER")
        fun supportedMethod(@UserIdentifierBinding param: UserIdentifier) {}
        @Suppress("unused", "UNUSED_PARAMETER")
        fun unsupportedMethod1(param: UserIdentifier) {}
        @Suppress("unused", "UNUSED_PARAMETER")
        fun unsupportedMethod2(@UserIdentifierBinding param: String) {}
        // @formatter:on
    }

    @Test
    fun mustSupportAnnotatedUserIdentifierParameter() {
        val resolver = UserIdentifierResolver(mock(), mock())

        verify("annotated UserIdentifier parameter is supported") {
            val method = Companion::class.java.methods.find { it.name == "supportedMethod" }!!
            val parameter = MethodParameter(method, 0)

            assertThat(resolver.supportsParameter(parameter)).withMessage()
                .isTrue()
        }
    }

    @Test
    fun mustNotSupportUnannotatedUserIdentifierParameter() {
        val resolver = UserIdentifierResolver(mock(), mock())

        verify("annotated UserIdentifier parameter is supported") {
            val method = Companion::class.java.methods.find { it.name == "unsupportedMethod1" }!!
            val parameter = MethodParameter(method, 0)

            assertThat(resolver.supportsParameter(parameter)).withMessage()
                .isFalse()
        }
    }

    @Test
    fun mustNotSupportAnnotatedNonUserIdentifierParameter() {
        val resolver = UserIdentifierResolver(mock(), mock())

        verify("annotated UserIdentifier parameter is supported") {
            val method = Companion::class.java.methods.find { it.name == "unsupportedMethod2" }!!
            val parameter = MethodParameter(method, 0)

            assertThat(resolver.supportsParameter(parameter)).withMessage()
                .isFalse()
        }
    }

    @Test
    fun mustCorrectlyFetchExistingUserIdentifierForWalletAddress() {
        val walletAddress = WalletAddress("abc123")
        val authentication = mock<Authentication>()

        suppose("authentication principal will some wallet address") {
            given(authentication.principal)
                .willReturn(walletAddress.rawValue)
        }

        val securityContext = mock<SecurityContext>()

        suppose("security context will return some authentication object") {
            given(securityContext.authentication)
                .willReturn(authentication)
            SecurityContextHolder.setContext(securityContext)
        }

        val repository = mock<UserIdentifierRepository>()
        val identifier = UserWalletAddressIdentifier(
            id = UUID.randomUUID(),
            walletAddress = walletAddress
        )

        suppose("user wallet address is fetched from database") {
            given(repository.getByWalletAddress(walletAddress))
                .willReturn(identifier)
        }

        val resolver = UserIdentifierResolver(mock(), repository)

        verify("user identifier is correctly returned") {
            assertThat(resolver.resolveArgument(mock(), mock(), mock(), mock())).withMessage()
                .isEqualTo(identifier)
        }
    }

    @Test
    fun mustCorrectlyCreateNewUserIdentifierForWalletAddress() {
        val walletAddress = WalletAddress("abc123")
        val authentication = mock<Authentication>()

        suppose("authentication principal will some wallet address") {
            given(authentication.principal)
                .willReturn(walletAddress.rawValue)
        }

        val securityContext = mock<SecurityContext>()

        suppose("security context will return some authentication object") {
            given(securityContext.authentication)
                .willReturn(authentication)
            SecurityContextHolder.setContext(securityContext)
        }

        val repository = mock<UserIdentifierRepository>()

        suppose("user wallet address is not in database") {
            given(repository.getByWalletAddress(walletAddress))
                .willReturn(null)
        }

        val uuidProvider = mock<UuidProvider>()
        val uuid = UUID.randomUUID()

        suppose("some UUID will be returned") {
            given(uuidProvider.getUuid())
                .willReturn(uuid)
        }

        val identifier = UserWalletAddressIdentifier(
            id = uuid,
            walletAddress = walletAddress
        )

        suppose("user identifier will be stored in database") {
            given(repository.store(identifier))
                .willReturn(identifier)
        }

        val resolver = UserIdentifierResolver(uuidProvider, repository)

        verify("user identifier is correctly returned") {
            assertThat(resolver.resolveArgument(mock(), mock(), mock(), mock())).withMessage()
                .isEqualTo(identifier)
        }
    }

    @Test
    fun mustThrowBadAuthenticationExceptionWhenAuthenticationPrincipalIsNotAString() {
        val authentication = mock<Authentication>()

        suppose("authentication principal will return some non-string object") {
            given(authentication.principal)
                .willReturn(emptyList<Nothing>())
        }

        val securityContext = mock<SecurityContext>()

        suppose("security context will return some authentication object") {
            given(securityContext.authentication)
                .willReturn(authentication)
            SecurityContextHolder.setContext(securityContext)
        }

        val resolver = UserIdentifierResolver(mock(), mock())

        verify("BadAuthenticationException is thrown") {
            assertThrows<BadAuthenticationException>(message) {
                resolver.resolveArgument(mock(), mock(), mock(), mock())
            }
        }
    }

    @Test
    fun mustThrowBadAuthenticationExceptionWhenAuthenticationPrincipalIsNull() {
        val authentication = mock<Authentication>()

        suppose("authentication principal is null") {
            given(authentication.principal)
                .willReturn(null)
        }

        val securityContext = mock<SecurityContext>()

        suppose("security context will return some authentication object") {
            given(securityContext.authentication)
                .willReturn(authentication)
            SecurityContextHolder.setContext(securityContext)
        }

        val resolver = UserIdentifierResolver(mock(), mock())

        verify("BadAuthenticationException is thrown") {
            assertThrows<BadAuthenticationException>(message) {
                resolver.resolveArgument(mock(), mock(), mock(), mock())
            }
        }
    }

    @Test
    fun mustThrowBadAuthenticationExceptionWhenAuthenticationIsNull() {
        val securityContext = mock<SecurityContext>()

        suppose("security context authentication is null") {
            given(securityContext.authentication)
                .willReturn(null)
            SecurityContextHolder.setContext(securityContext)
        }

        val resolver = UserIdentifierResolver(mock(), mock())

        verify("BadAuthenticationException is thrown") {
            assertThrows<BadAuthenticationException>(message) {
                resolver.resolveArgument(mock(), mock(), mock(), mock())
            }
        }
    }
}
