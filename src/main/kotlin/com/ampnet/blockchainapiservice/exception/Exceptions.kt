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

class BlockchainReadException(message: String) : ServiceException(
    errorCode = ErrorCode.BLOCKCHAIN_READ_ERROR,
    httpStatus = HttpStatus.BAD_REQUEST,
    message = message
) {
    companion object {
        private const val serialVersionUID: Long = -5979025245655611755L
    }
}

class CannotAttachTxInfoException(message: String) : ServiceException(
    errorCode = ErrorCode.TX_INFO_ALREADY_SET,
    httpStatus = HttpStatus.BAD_REQUEST,
    message = message
) {
    companion object {
        private const val serialVersionUID: Long = 2973294387463968605L
    }
}

class CannotAttachSignedMessageException(message: String) : ServiceException(
    errorCode = ErrorCode.SIGNED_MESSAGE_ALREADY_SET,
    httpStatus = HttpStatus.BAD_REQUEST,
    message = message
) {
    companion object {
        private const val serialVersionUID: Long = 2487635142233013917L
    }
}

class BadAuthenticationException : ServiceException(
    errorCode = ErrorCode.BAD_AUTHENTICATION,
    httpStatus = HttpStatus.UNAUTHORIZED,
    message = "Provided authentication header has invalid format"
) {
    companion object {
        private const val serialVersionUID: Long = -787538305851627646L
    }
}

class ApiKeyAlreadyExistsException(projectId: UUID) : ServiceException(
    errorCode = ErrorCode.API_KEY_ALREADY_EXISTS,
    httpStatus = HttpStatus.BAD_REQUEST,
    message = "API key already exists for project with ID: $projectId"
) {
    companion object {
        private const val serialVersionUID: Long = 6676987534485377215L
    }
}

class NonExistentApiKeyException : ServiceException(
    errorCode = ErrorCode.NON_EXISTENT_API_KEY,
    httpStatus = HttpStatus.UNAUTHORIZED,
    message = "Non existent API key provided in request"
) {
    companion object {
        private const val serialVersionUID: Long = -176593491332037627L
    }
}

class MissingTokenAddressException : ServiceException(
    errorCode = ErrorCode.MISSING_TOKEN_ADDRESS,
    httpStatus = HttpStatus.BAD_REQUEST,
    message = "Token address is missing from the request"
) {
    companion object {
        private const val serialVersionUID: Long = -8004673014736666252L
    }
}

class TokenAddressNotAllowedException : ServiceException(
    errorCode = ErrorCode.TOKEN_ADDRESS_NOT_ALLOWED,
    httpStatus = HttpStatus.BAD_REQUEST,
    message = "Token address is not allowed for this request"
) {
    companion object {
        private const val serialVersionUID: Long = -2512631824095658324L
    }
}
