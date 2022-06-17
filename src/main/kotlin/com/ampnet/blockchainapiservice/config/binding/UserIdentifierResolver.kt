package com.ampnet.blockchainapiservice.config.binding

import com.ampnet.blockchainapiservice.config.binding.annotation.UserIdentifierBinding
import com.ampnet.blockchainapiservice.exception.BadAuthenticationException
import com.ampnet.blockchainapiservice.model.result.UserIdentifier
import com.ampnet.blockchainapiservice.model.result.UserWalletAddressIdentifier
import com.ampnet.blockchainapiservice.repository.UserIdentifierRepository
import com.ampnet.blockchainapiservice.service.UuidProvider
import com.ampnet.blockchainapiservice.util.WalletAddress
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
        val principal = (SecurityContextHolder.getContext()?.authentication?.principal as? String)
            ?.let { WalletAddress(it) } ?: throw BadAuthenticationException()
        return userIdentifierRepository.getByWalletAddress(principal)
            ?: userIdentifierRepository.store(
                UserWalletAddressIdentifier(
                    id = uuidProvider.getUuid(),
                    walletAddress = principal
                )
            )
    }
}
