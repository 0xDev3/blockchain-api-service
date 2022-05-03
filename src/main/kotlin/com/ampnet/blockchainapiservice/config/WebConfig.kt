package com.ampnet.blockchainapiservice.config

import com.ampnet.blockchainapiservice.config.binding.RpcUrlSpecResolver
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig : WebMvcConfigurer {
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(RpcUrlSpecResolver())
    }
}
