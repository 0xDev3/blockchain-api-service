package dev3.blockchainapiservice.config.interceptors

import dev3.blockchainapiservice.config.CustomHeaders
import dev3.blockchainapiservice.service.UuidProvider
import org.slf4j.MDC
import org.springframework.web.servlet.HandlerInterceptor
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class CorrelationIdInterceptor(
    private val uuidProvider: UuidProvider
) : HandlerInterceptor {

    companion object {
        private const val CID = "CID"
    }

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val cid = request.getHeader(CustomHeaders.CORRELATION_ID_HEADER) ?: uuidProvider.getUuid().toString()
        response.addHeader(CustomHeaders.CORRELATION_ID_HEADER, cid)
        MDC.put(CID, cid)
        return true
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?
    ) {
        MDC.remove(CID)
    }
}
