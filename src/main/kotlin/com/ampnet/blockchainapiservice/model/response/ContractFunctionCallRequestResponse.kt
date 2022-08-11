package com.ampnet.blockchainapiservice.model.response

import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.result.ContractFunctionCallRequest
import com.ampnet.blockchainapiservice.util.FunctionArgument
import com.ampnet.blockchainapiservice.util.Status
import com.ampnet.blockchainapiservice.util.WithFunctionData
import com.ampnet.blockchainapiservice.util.WithTransactionAndFunctionData
import com.ampnet.blockchainapiservice.util.annotation.SchemaIgnore
import com.ampnet.blockchainapiservice.util.annotation.SchemaName
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import java.math.BigInteger
import java.time.OffsetDateTime
import java.util.UUID

data class ContractFunctionCallRequestResponse(
    val id: UUID,
    val status: Status,
    val deployedContractId: UUID?,
    val contractAddress: String,
    val functionName: String,
    @SchemaIgnore
    val functionParams: JsonNode,
    val functionCallData: String,
    @JsonSerialize(using = ToStringSerializer::class)
    val ethAmount: BigInteger,
    val chainId: Long,
    val redirectUrl: String,
    val projectId: UUID,
    val createdAt: OffsetDateTime,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig?,
    val callerAddress: String?,
    val functionCallTx: TransactionResponse
) {
    constructor(contractFunctionCallRequest: WithFunctionData<ContractFunctionCallRequest>) : this(
        id = contractFunctionCallRequest.value.id,
        status = Status.PENDING,
        deployedContractId = contractFunctionCallRequest.value.deployedContractId,
        contractAddress = contractFunctionCallRequest.value.contractAddress.rawValue,
        functionName = contractFunctionCallRequest.value.functionName,
        functionParams = contractFunctionCallRequest.value.functionParams,
        functionCallData = contractFunctionCallRequest.data.value,
        ethAmount = contractFunctionCallRequest.value.ethAmount.rawValue,
        chainId = contractFunctionCallRequest.value.chainId.value,
        redirectUrl = contractFunctionCallRequest.value.redirectUrl,
        projectId = contractFunctionCallRequest.value.projectId,
        createdAt = contractFunctionCallRequest.value.createdAt.value,
        arbitraryData = contractFunctionCallRequest.value.arbitraryData,
        screenConfig = contractFunctionCallRequest.value.screenConfig.orEmpty(),
        callerAddress = contractFunctionCallRequest.value.callerAddress?.rawValue,
        functionCallTx = TransactionResponse.unmined(
            from = contractFunctionCallRequest.value.callerAddress,
            to = contractFunctionCallRequest.value.contractAddress,
            data = contractFunctionCallRequest.data,
            value = contractFunctionCallRequest.value.ethAmount,
        )
    )

    constructor(contractFunctionCallRequest: WithTransactionAndFunctionData<ContractFunctionCallRequest>) : this(
        id = contractFunctionCallRequest.value.id,
        status = contractFunctionCallRequest.status,
        deployedContractId = contractFunctionCallRequest.value.deployedContractId,
        contractAddress = contractFunctionCallRequest.value.contractAddress.rawValue,
        functionName = contractFunctionCallRequest.value.functionName,
        functionParams = contractFunctionCallRequest.value.functionParams,
        functionCallData = contractFunctionCallRequest.functionData.value,
        ethAmount = contractFunctionCallRequest.value.ethAmount.rawValue,
        chainId = contractFunctionCallRequest.value.chainId.value,
        redirectUrl = contractFunctionCallRequest.value.redirectUrl,
        projectId = contractFunctionCallRequest.value.projectId,
        createdAt = contractFunctionCallRequest.value.createdAt.value,
        arbitraryData = contractFunctionCallRequest.value.arbitraryData,
        screenConfig = contractFunctionCallRequest.value.screenConfig.orEmpty(),
        callerAddress = contractFunctionCallRequest.value.callerAddress?.rawValue,
        functionCallTx = TransactionResponse(contractFunctionCallRequest.transactionData)
    )

    @Suppress("unused") // used for JSON schema generation
    @JsonIgnore
    @SchemaName("function_params")
    private val schemaFunctionParams: List<FunctionArgument> = emptyList()
}
