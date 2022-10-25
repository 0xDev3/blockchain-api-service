package dev3.blockchainapiservice.model.response

import com.fasterxml.jackson.databind.JsonNode
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.model.result.Erc20LockRequest
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.Status
import dev3.blockchainapiservice.util.WithFunctionData
import dev3.blockchainapiservice.util.WithTransactionData
import java.math.BigInteger
import java.time.OffsetDateTime
import java.util.UUID

data class Erc20LockRequestResponse(
    val id: UUID,
    val projectId: UUID,
    val status: Status,
    val chainId: Long,
    val tokenAddress: String,
    val amount: BigInteger,
    val lockDurationInSeconds: BigInteger,
    val unlocksAt: OffsetDateTime?,
    val lockContractAddress: String,
    val senderAddress: String?,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig?,
    val redirectUrl: String,
    val lockTx: TransactionResponse,
    val createdAt: OffsetDateTime
) {
    constructor(lockRequest: WithFunctionData<Erc20LockRequest>) : this(
        id = lockRequest.value.id,
        projectId = lockRequest.value.projectId,
        status = Status.PENDING,
        chainId = lockRequest.value.chainId.value,
        tokenAddress = lockRequest.value.tokenAddress.rawValue,
        amount = lockRequest.value.tokenAmount.rawValue,
        lockDurationInSeconds = lockRequest.value.lockDuration.rawValue,
        unlocksAt = null,
        lockContractAddress = lockRequest.value.lockContractAddress.rawValue,
        senderAddress = lockRequest.value.tokenSenderAddress?.rawValue,
        arbitraryData = lockRequest.value.arbitraryData,
        screenConfig = lockRequest.value.screenConfig.orEmpty(),
        redirectUrl = lockRequest.value.redirectUrl,
        lockTx = TransactionResponse.unmined(
            from = lockRequest.value.tokenSenderAddress,
            to = lockRequest.value.tokenAddress,
            data = lockRequest.data,
            value = Balance.ZERO
        ),
        createdAt = lockRequest.value.createdAt.value
    )

    constructor(lockRequest: WithTransactionData<Erc20LockRequest>) : this(
        id = lockRequest.value.id,
        projectId = lockRequest.value.projectId,
        status = lockRequest.status,
        chainId = lockRequest.value.chainId.value,
        tokenAddress = lockRequest.value.tokenAddress.rawValue,
        amount = lockRequest.value.tokenAmount.rawValue,
        lockDurationInSeconds = lockRequest.value.lockDuration.rawValue,
        unlocksAt = lockRequest.transactionData.timestamp?.plus(lockRequest.value.lockDuration)?.value,
        lockContractAddress = lockRequest.value.lockContractAddress.rawValue,
        senderAddress = lockRequest.value.tokenSenderAddress?.rawValue,
        arbitraryData = lockRequest.value.arbitraryData,
        screenConfig = lockRequest.value.screenConfig.orEmpty(),
        redirectUrl = lockRequest.value.redirectUrl,
        lockTx = TransactionResponse(lockRequest.transactionData),
        createdAt = lockRequest.value.createdAt.value
    )
}
