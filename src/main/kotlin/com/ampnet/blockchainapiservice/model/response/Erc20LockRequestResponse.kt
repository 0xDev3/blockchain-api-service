package com.ampnet.blockchainapiservice.model.response

import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.result.Erc20LockRequest
import com.ampnet.blockchainapiservice.util.Status
import com.ampnet.blockchainapiservice.util.WithFunctionData
import com.ampnet.blockchainapiservice.util.WithTransactionData
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import java.math.BigInteger
import java.time.OffsetDateTime
import java.util.UUID

data class Erc20LockRequestResponse(
    val id: UUID,
    val projectId: UUID,
    val status: Status,
    val chainId: Long,
    val tokenAddress: String,
    @JsonSerialize(using = ToStringSerializer::class)
    val amount: BigInteger,
    @JsonSerialize(using = ToStringSerializer::class)
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
        lockTx = TransactionResponse(
            txHash = null,
            from = lockRequest.value.tokenSenderAddress?.rawValue,
            to = lockRequest.value.tokenAddress.rawValue,
            data = lockRequest.data.value,
            blockConfirmations = null,
            timestamp = null
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
        lockTx = TransactionResponse(
            txHash = lockRequest.transactionData.txHash?.value,
            from = lockRequest.transactionData.fromAddress?.rawValue,
            to = lockRequest.transactionData.toAddress.rawValue,
            data = lockRequest.transactionData.data.value,
            blockConfirmations = lockRequest.transactionData.blockConfirmations,
            timestamp = lockRequest.transactionData.timestamp?.value
        ),
        createdAt = lockRequest.value.createdAt.value
    )
}
