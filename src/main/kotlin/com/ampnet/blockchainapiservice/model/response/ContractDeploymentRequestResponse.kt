package com.ampnet.blockchainapiservice.model.response

import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.result.ContractDeploymentRequest
import com.ampnet.blockchainapiservice.util.Status
import com.ampnet.blockchainapiservice.util.WithTransactionData
import com.ampnet.blockchainapiservice.util.ZeroAddress
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import java.math.BigInteger
import java.time.OffsetDateTime
import java.util.UUID

data class ContractDeploymentRequestResponse(
    val id: UUID,
    val status: Status,
    val contractId: String,
    val contractDeploymentData: String,
    val contractTags: List<String>,
    val contractImplements: List<String>,
    @JsonSerialize(using = ToStringSerializer::class)
    val initialEthAmount: BigInteger,
    val chainId: Long,
    val redirectUrl: String,
    val projectId: UUID,
    val createdAt: OffsetDateTime,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig?,
    val contractAddress: String?,
    val deployerAddress: String?,
    val deployTx: TransactionResponse
) {
    constructor(contractDeploymentRequest: ContractDeploymentRequest) : this(
        id = contractDeploymentRequest.id,
        status = Status.PENDING,
        contractId = contractDeploymentRequest.contractId.value,
        contractDeploymentData = contractDeploymentRequest.contractData.withPrefix,
        contractTags = contractDeploymentRequest.contractTags.map { it.value },
        contractImplements = contractDeploymentRequest.contractImplements.map { it.value },
        initialEthAmount = contractDeploymentRequest.initialEthAmount.rawValue,
        chainId = contractDeploymentRequest.chainId.value,
        redirectUrl = contractDeploymentRequest.redirectUrl,
        projectId = contractDeploymentRequest.projectId,
        createdAt = contractDeploymentRequest.createdAt.value,
        arbitraryData = contractDeploymentRequest.arbitraryData,
        screenConfig = contractDeploymentRequest.screenConfig.orEmpty(),
        contractAddress = contractDeploymentRequest.contractAddress?.rawValue,
        deployerAddress = contractDeploymentRequest.deployerAddress?.rawValue,
        deployTx = TransactionResponse(
            txHash = null,
            from = contractDeploymentRequest.deployerAddress?.rawValue,
            to = ZeroAddress.rawValue,
            data = contractDeploymentRequest.contractData.withPrefix,
            value = contractDeploymentRequest.initialEthAmount.rawValue,
            blockConfirmations = null,
            timestamp = null
        )
    )

    constructor(contractDeploymentRequest: WithTransactionData<ContractDeploymentRequest>) : this(
        id = contractDeploymentRequest.value.id,
        status = contractDeploymentRequest.status,
        contractId = contractDeploymentRequest.value.contractId.value,
        contractDeploymentData = contractDeploymentRequest.value.contractData.withPrefix,
        contractTags = contractDeploymentRequest.value.contractTags.map { it.value },
        contractImplements = contractDeploymentRequest.value.contractImplements.map { it.value },
        initialEthAmount = contractDeploymentRequest.value.initialEthAmount.rawValue,
        chainId = contractDeploymentRequest.value.chainId.value,
        redirectUrl = contractDeploymentRequest.value.redirectUrl,
        projectId = contractDeploymentRequest.value.projectId,
        createdAt = contractDeploymentRequest.value.createdAt.value,
        arbitraryData = contractDeploymentRequest.value.arbitraryData,
        screenConfig = contractDeploymentRequest.value.screenConfig.orEmpty(),
        contractAddress = contractDeploymentRequest.value.contractAddress?.rawValue,
        deployerAddress = contractDeploymentRequest.value.deployerAddress?.rawValue,
        deployTx = TransactionResponse(
            txHash = contractDeploymentRequest.transactionData.txHash?.value,
            from = contractDeploymentRequest.transactionData.fromAddress?.rawValue,
            to = contractDeploymentRequest.transactionData.toAddress.rawValue,
            data = contractDeploymentRequest.transactionData.data?.value,
            value = contractDeploymentRequest.transactionData.value?.rawValue,
            blockConfirmations = contractDeploymentRequest.transactionData.blockConfirmations,
            timestamp = contractDeploymentRequest.transactionData.timestamp?.value
        )
    )
}
