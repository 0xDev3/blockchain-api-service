package com.ampnet.blockchainapiservice.model.response

import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.result.ContractDeploymentRequest
import com.ampnet.blockchainapiservice.util.FunctionArgument
import com.ampnet.blockchainapiservice.util.FunctionData
import com.ampnet.blockchainapiservice.util.Status
import com.ampnet.blockchainapiservice.util.WithTransactionData
import com.ampnet.blockchainapiservice.util.ZeroAddress
import com.ampnet.blockchainapiservice.util.annotation.SchemaIgnore
import com.ampnet.blockchainapiservice.util.annotation.SchemaName
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import java.math.BigInteger
import java.time.OffsetDateTime
import java.util.UUID

data class ContractDeploymentRequestResponse(
    val id: UUID,
    val alias: String,
    val status: Status,
    val contractId: String,
    val contractDeploymentData: String,
    @SchemaIgnore
    val constructorParams: JsonNode,
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
        alias = contractDeploymentRequest.alias,
        status = Status.PENDING,
        contractId = contractDeploymentRequest.contractId.value,
        contractDeploymentData = contractDeploymentRequest.contractData.withPrefix,
        constructorParams = contractDeploymentRequest.constructorParams,
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
        deployTx = TransactionResponse.unmined(
            from = contractDeploymentRequest.deployerAddress,
            to = ZeroAddress,
            data = FunctionData(contractDeploymentRequest.contractData.value),
            value = contractDeploymentRequest.initialEthAmount
        )
    )

    constructor(contractDeploymentRequest: WithTransactionData<ContractDeploymentRequest>) : this(
        id = contractDeploymentRequest.value.id,
        alias = contractDeploymentRequest.value.alias,
        status = contractDeploymentRequest.status,
        contractId = contractDeploymentRequest.value.contractId.value,
        contractDeploymentData = contractDeploymentRequest.value.contractData.withPrefix,
        constructorParams = contractDeploymentRequest.value.constructorParams,
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
        deployTx = TransactionResponse(contractDeploymentRequest.transactionData)
    )

    @Suppress("unused") // used for JSON schema generation
    @JsonIgnore
    @SchemaName("constructor_params")
    private val schemaConstructorParams: List<FunctionArgument> = emptyList()
}
