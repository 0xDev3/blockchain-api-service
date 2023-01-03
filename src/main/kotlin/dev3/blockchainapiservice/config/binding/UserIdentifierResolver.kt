package dev3.blockchainapiservice.config.binding

import dev3.blockchainapiservice.config.binding.annotation.UserIdentifierBinding
import dev3.blockchainapiservice.exception.BadAuthenticationException
import dev3.blockchainapiservice.generated.jooq.id.UserId
import dev3.blockchainapiservice.model.result.UserIdentifier
import dev3.blockchainapiservice.model.result.UserWalletAddressIdentifier
import dev3.blockchainapiservice.repository.UserIdentifierRepository
import dev3.blockchainapiservice.service.UuidProvider
import dev3.blockchainapiservice.util.WalletAddress
import org.springframework.core.MethodParameter
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

class UserIdentifierResolver(
    private val uuidProvider: UuidProvider,
    private val userIdentifierRepository: UserIdentifierRepository
) : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.parameterType == UserIdentifier::class.java &&
            parameter.hasParameterAnnotation(UserIdentifierBinding::class.java)
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        nativeWebRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): UserIdentifier {
        val principal = (SecurityContextHolder.getContext().authentication?.principal as? String)
            ?.let {
                try {
                    WalletAddress(it)
                } catch (e: NumberFormatException) {
                    null
                }
            }
            ?: throw BadAuthenticationException()

        return userIdentifierRepository.getByWalletAddress(principal)
            ?: userIdentifierRepository.store(
                UserWalletAddressIdentifier(
                    id = uuidProvider.getUuid(UserId),
                    walletAddress = principal,
                    stripeClientId = null
                )
            )
    }
}
