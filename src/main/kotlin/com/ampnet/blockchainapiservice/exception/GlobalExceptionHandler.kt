package com.ampnet.blockchainapiservice.exception

import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    companion object : KLogging()

    @ExceptionHandler(ServiceException::class)
    fun handleServiceException(exception: ServiceException): ResponseEntity<ErrorResponse> {
        logger.debug("ServiceException", exception)
        return ResponseEntity(ErrorResponse(exception.errorCode, exception.message), exception.httpStatus)
    }
}
