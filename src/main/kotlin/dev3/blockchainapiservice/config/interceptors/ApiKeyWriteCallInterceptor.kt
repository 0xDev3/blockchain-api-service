package dev3.blockchainapiservice.config.interceptors

import com.fasterxml.jackson.databind.ObjectMapper
import dev3.blockchainapiservice.blockchain.BlockchainService
import dev3.blockchainapiservice.blockchain.properties.ChainSpec
import dev3.blockchainapiservice.config.ApiUsageProperties
import dev3.blockchainapiservice.config.CustomHeaders
import dev3.blockchainapiservice.config.interceptors.annotation.ApiWriteLimitedMapping
import dev3.blockchainapiservice.config.interceptors.annotation.IdType
import dev3.blockchainapiservice.exception.ErrorCode
import dev3.blockchainapiservice.exception.ErrorResponse
import dev3.blockchainapiservice.model.params.ExecuteReadonlyFunctionCallParams
import dev3.blockchainapiservice.model.params.OutputParameter
import dev3.blockchainapiservice.model.result.UserWalletAddressIdentifier
import dev3.blockchainapiservice.repository.ApiKeyRepository
import dev3.blockchainapiservice.repository.ApiRateLimitRepository
import dev3.blockchainapiservice.repository.UserIdResolverRepository
import dev3.blockchainapiservice.repository.UserIdentifierRepository
import dev3.blockchainapiservice.service.FunctionEncoderService
import dev3.blockchainapiservice.service.UtcDateTimeProvider
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.FunctionArgument
import dev3.blockchainapiservice.util.IntType
import dev3.blockchainapiservice.util.ZeroAddress
import mu.KLogging
import org.springframework.http.HttpStatus
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import java.math.BigInteger
import java.util.UUID
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class ApiKeyWriteCallInterceptor(
    private val blockchainService: BlockchainService,
    private val functionEncoderService: FunctionEncoderService,
    private val apiKeyRepository: ApiKeyRepository,
    private val apiRateLimitRepository: ApiRateLimitRepository,
    private val userIdResolverRepository: UserIdResolverRepository,
    private val userIdentifierRepository: UserIdentifierRepository,
    private val utcDateTimeProvider: UtcDateTimeProvider,
    private val objectMapper: ObjectMapper,
    private val apiUsageProperties: ApiUsageProperties
) : HandlerInterceptor {

    companion object : KLogging() {
        const val TX_COUNT_FUNCTION_NAME = "txCount"
    }

    private val prepaidContractAddress: ContractAddress? = apiUsageProperties.prepaidBalanceContractAddress
        .takeIf { it.isNotBlank() }?.let(::ContractAddress)
    private val prepaidContractChainSpec: ChainSpec = ChainSpec(
        chainId = ChainId(apiUsageProperties.prepaidBalanceContractChainId),
        customRpcUrl = null
    )

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean =
        handleAnnotatedMethod(request, handler) { userId, _ ->
            val userWalletAddress = (userIdentifierRepository.getById(userId) as? UserWalletAddressIdentifier)
                ?.walletAddress ?: ZeroAddress.toWalletAddress()
            val contractApiCalls: BigInteger = prepaidContractAddress?.let {
                blockchainService.callReadonlyFunction(
                    chainSpec = prepaidContractChainSpec,
                    params = ExecuteReadonlyFunctionCallParams(
                        contractAddress = it,
                        callerAddress = userWalletAddress,
                        functionName = TX_COUNT_FUNCTION_NAME,
                        functionData = functionEncoderService.encode(
                            functionName = TX_COUNT_FUNCTION_NAME,
                            arguments = listOf(FunctionArgument(userWalletAddress))
                        ),
                        outputParams = listOf(OutputParameter(IntType))
                    )
                )
            }?.returnValues?.get(0) as? BigInteger ?: BigInteger.ZERO

            val allowedWriteRequests = contractApiCalls + apiUsageProperties.freeWriteRequests.toBigInteger()
            val usedWriteRequests = apiRateLimitRepository.usedWriteRequests(userId)
            val remainingWriteRequests = allowedWriteRequests - usedWriteRequests.toBigInteger()

            logger.debug {
                "Write API requests for wallet $userWalletAddress: $usedWriteRequests / $allowedWriteRequests" +
                    " ($remainingWriteRequests remaining)"
            }

            if (remainingWriteRequests > BigInteger.ZERO) {
                true
            } else {
                logger.warn { "API key rate limit exceeded for userId: $userId" }

                response.status = HttpStatus.PAYMENT_REQUIRED.value()
                response.writer.println(
                    objectMapper.writeValueAsString(
                        ErrorResponse(
                            errorCode = ErrorCode.API_RATE_LIMIT_EXCEEDED,
                            message = "API rate limit exceeded for write requests"
                        )
                    )
                )

                false
            }
        }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?
    ) {
        handleAnnotatedMethod(request, handler) { userId, annotation ->
            if (HttpStatus.resolve(response.status)?.is2xxSuccessful == true) {
                apiRateLimitRepository.addWriteCall(
                    userId = userId,
                    currentTime = utcDateTimeProvider.getUtcDateTime(),
                    method = annotation.method,
                    endpoint = annotation.path
                )
            }

            true
        }
    }

    private fun handleAnnotatedMethod(
        request: HttpServletRequest,
        handler: Any,
        handle: (UUID, ApiWriteLimitedMapping) -> Boolean
    ): Boolean {
        val annotation = (handler as? HandlerMethod)?.method?.getAnnotation(ApiWriteLimitedMapping::class.java)

        return if (annotation != null) {
            annotation.resolveUserId(request)
                ?.let { handle(it, annotation) }
                ?: true
        } else true
    }

    private fun ApiWriteLimitedMapping.resolveUserId(request: HttpServletRequest): UUID? =
        if (idType == IdType.PROJECT_ID) {
            request.getHeader(CustomHeaders.API_KEY_HEADER)
                ?.let { apiKeyRepository.getByValue(it)?.projectId }
                ?.let { userIdResolverRepository.getUserId(idType, it) }
        } else {
            UserIdResolver.resolve(
                userIdResolverRepository = userIdResolverRepository,
                interceptorName = "ApiKeyWriteCallInterceptor",
                request = request,
                idType = idType,
                path = path
            )
        }
}
