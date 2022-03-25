package com.ampnet.blockchainapiservice.config.binding

import com.ampnet.blockchainapiservice.blockchain.properties.ChainSpec
import com.ampnet.blockchainapiservice.config.binding.annotation.ChainBinding
import com.ampnet.blockchainapiservice.util.ChainId
import org.springframework.core.MethodParameter
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.web.servlet.HandlerMapping
import javax.servlet.http.HttpServletRequest

class ChainSpecResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.parameterType == ChainSpec::class.java &&
            parameter.hasParameterAnnotation(ChainBinding::class.java)
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        nativeWebRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): ChainSpec {
        val httpServletRequest = nativeWebRequest.getNativeRequest(HttpServletRequest::class.java)

        @Suppress("UNCHECKED_CAST")
        val pathVariables = httpServletRequest?.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)
            as? Map<String, String>

        val chainIdPathVariable = parameter.getParameterAnnotation(ChainBinding::class.java)?.chainIdPathVariable

        // chainId is a mandatory path variable
        val chainId = pathVariables?.get(chainIdPathVariable)?.let { ChainId(it.toLong()) }
            ?: throw IllegalStateException("Path variable \"chainId\" is missing from the controller specification.")
        // RPC URL is an optional header
        val rpcUrl = httpServletRequest.getHeader("X-RPC-URL")

        return ChainSpec(chainId, rpcUrl)
    }
}
