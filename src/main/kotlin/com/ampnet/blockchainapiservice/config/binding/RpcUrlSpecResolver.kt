package com.ampnet.blockchainapiservice.config.binding

import com.ampnet.blockchainapiservice.blockchain.properties.RpcUrlSpec
import com.ampnet.blockchainapiservice.config.binding.annotation.RpcUrlBinding
import org.springframework.core.MethodParameter
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import javax.servlet.http.HttpServletRequest

@Deprecated("for removal") // TODO remove in SD-880
class RpcUrlSpecResolver : HandlerMethodArgumentResolver {

    companion object {
        const val RPC_URL_HEADER = "X-RPC-URL"
        const val RPC_URL_OVERRIDE_HEADER = "X-RPC-URL-OVERRIDE"
    }

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.parameterType == RpcUrlSpec::class.java &&
            parameter.hasParameterAnnotation(RpcUrlBinding::class.java)
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        nativeWebRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): RpcUrlSpec {
        val httpServletRequest = nativeWebRequest.getNativeRequest(HttpServletRequest::class.java)
        // RPC URL is an optional header
        val rpcUrl = httpServletRequest?.getHeader(RPC_URL_HEADER)
        val rpcUrlOverride = httpServletRequest?.getHeader(RPC_URL_OVERRIDE_HEADER)

        return RpcUrlSpec(rpcUrl, rpcUrlOverride)
    }
}
