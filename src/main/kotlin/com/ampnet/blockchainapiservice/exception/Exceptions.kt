package com.ampnet.blockchainapiservice.exception

import com.ampnet.blockchainapiservice.util.ChainId
import org.springframework.http.HttpStatus
import java.util.UUID

abstract class ServiceException(
    val errorCode: ErrorCode,
    val httpStatus: HttpStatus,
    override val message: String
) : RuntimeException(message) {
    companion object {
        private const val serialVersionUID: Long = 8974557457024980481L
    }
}

class ResourceNotFoundException(message: String) : ServiceException(
    errorCode = ErrorCode.RESOURCE_NOT_FOUND,
    httpStatus = HttpStatus.NOT_FOUND,
    message = message
) {
    companion object {
        private const val serialVersionUID: Long = 8937915498141342807L
    }
}

class UnsupportedChainIdException(chainId: ChainId) : ServiceException(
    errorCode = ErrorCode.UNSUPPORTED_CHAIN_ID,
    httpStatus = HttpStatus.BAD_REQUEST,
    message = "Blockchain id: ${chainId.value} not supported"
) {
    companion object {
        private const val serialVersionUID: Long = -8803854722161717146L
    }
}

class ExpiredValidationMessageException(messageId: UUID) : ServiceException(
    errorCode = ErrorCode.VALIDATION_MESSAGE_EXPIRED,
    httpStatus = HttpStatus.PRECONDITION_FAILED,
    message = "Validation message with ID: $messageId has expired"
) {
    companion object {
        private const val serialVersionUID: Long = -8531346107518192369L
    }
}

class BadSignatureException(message: String) : ServiceException(
    errorCode = ErrorCode.BAD_SIGNATURE,
    httpStatus = HttpStatus.BAD_REQUEST,
    message = message
) {
    companion object {
        private const val serialVersionUID: Long = 1135423205237947970L
    }
}

class BlockchainReadException(message: String) : ServiceException(
    errorCode = ErrorCode.BLOCKCHAIN_READ_ERROR,
    httpStatus = HttpStatus.BAD_REQUEST,
    message = message
) {
    companion object {
        private const val serialVersionUID: Long = -5979025245655611755L
    }
}
