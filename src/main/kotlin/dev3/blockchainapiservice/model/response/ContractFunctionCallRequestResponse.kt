package dev3.blockchainapiservice.model.response

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import dev3.blockchainapiservice.generated.jooq.id.ContractDeploymentRequestId
import dev3.blockchainapiservice.generated.jooq.id.ContractFunctionCallRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.model.result.ContractFunctionCallRequest
import dev3.blockchainapiservice.util.FunctionArgumentSchema
import dev3.blockchainapiservice.util.Status
import dev3.blockchainapiservice.util.WithFunctionData
import dev3.blockchainapiservice.util.WithTransactionAndFunctionData
import dev3.blockchainapiservice.util.annotation.SchemaIgnore
import dev3.blockchainapiservice.util.annotation.SchemaName
import java.math.BigInteger
import java.time.OffsetDateTime

data class ContractFunctionCallRequestResponse(
    val id: ContractFunctionCallRequestId,
    val status: Status,
    val deployedContractId: ContractDeploymentRequestId?,
    val contractAddress: String,
    val functionName: String,
    @SchemaIgnore
    val functionParams: JsonNode,
    val functionCallData: String,
    val ethAmount: BigInteger,
    val chainId: Long,
    val redirectUrl: String,
    val projectId: ProjectId,
    val createdAt: OffsetDateTime,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig?,
    val callerAddress: String?,
    val functionCallTx: TransactionResponse,
    val events: List<EventInfoResponse>?
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
        ),
        events = null
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
        functionCallTx = TransactionResponse(contractFunctionCallRequest.transactionData),
        events = contractFunctionCallRequest.transactionData.events?.map { EventInfoResponse(it) }
    )

    @Suppress("unused") // used for JSON schema generation
    @JsonIgnore
    @SchemaName("function_params")
    private val schemaFunctionParams: List<FunctionArgumentSchema> = emptyList()
}
