package dev3.blockchainapiservice.features.contract.arbitrarycall.model.response

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import dev3.blockchainapiservice.features.contract.arbitrarycall.model.result.ContractArbitraryCallRequest
import dev3.blockchainapiservice.generated.jooq.id.ContractArbitraryCallRequestId
import dev3.blockchainapiservice.generated.jooq.id.ContractDeploymentRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.model.response.EventInfoResponse
import dev3.blockchainapiservice.model.response.TransactionResponse
import dev3.blockchainapiservice.util.FunctionArgumentSchema
import dev3.blockchainapiservice.util.Status
import dev3.blockchainapiservice.util.WithTransactionData
import dev3.blockchainapiservice.util.annotation.SchemaIgnore
import dev3.blockchainapiservice.util.annotation.SchemaName
import java.math.BigInteger
import java.time.OffsetDateTime

data class ContractArbitraryCallRequestResponse(
    val id: ContractArbitraryCallRequestId,
    val status: Status,
    val deployedContractId: ContractDeploymentRequestId?,
    val contractAddress: String,
    val functionName: String?,
    @SchemaIgnore
    val functionParams: JsonNode?,
    val functionCallData: String,
    val ethAmount: BigInteger,
    val chainId: Long,
    val redirectUrl: String,
    val projectId: ProjectId,
    val createdAt: OffsetDateTime,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig?,
    val callerAddress: String?,
    val arbitraryCallTx: TransactionResponse,
    val events: List<EventInfoResponse>?
) {
    constructor(contractArbitraryCallRequest: ContractArbitraryCallRequest) : this(
        id = contractArbitraryCallRequest.id,
        status = Status.PENDING,
        deployedContractId = contractArbitraryCallRequest.deployedContractId,
        contractAddress = contractArbitraryCallRequest.contractAddress.rawValue,
        functionName = contractArbitraryCallRequest.functionName,
        functionParams = contractArbitraryCallRequest.functionParams,
        functionCallData = contractArbitraryCallRequest.functionData.value,
        ethAmount = contractArbitraryCallRequest.ethAmount.rawValue,
        chainId = contractArbitraryCallRequest.chainId.value,
        redirectUrl = contractArbitraryCallRequest.redirectUrl,
        projectId = contractArbitraryCallRequest.projectId,
        createdAt = contractArbitraryCallRequest.createdAt.value,
        arbitraryData = contractArbitraryCallRequest.arbitraryData,
        screenConfig = contractArbitraryCallRequest.screenConfig.orEmpty(),
        callerAddress = contractArbitraryCallRequest.callerAddress?.rawValue,
        arbitraryCallTx = TransactionResponse.unmined(
            from = contractArbitraryCallRequest.callerAddress,
            to = contractArbitraryCallRequest.contractAddress,
            data = contractArbitraryCallRequest.functionData,
            value = contractArbitraryCallRequest.ethAmount,
        ),
        events = null
    )

    constructor(contractArbitraryCallRequest: WithTransactionData<ContractArbitraryCallRequest>) : this(
        id = contractArbitraryCallRequest.value.id,
        status = contractArbitraryCallRequest.status,
        deployedContractId = contractArbitraryCallRequest.value.deployedContractId,
        contractAddress = contractArbitraryCallRequest.value.contractAddress.rawValue,
        functionName = contractArbitraryCallRequest.value.functionName,
        functionParams = contractArbitraryCallRequest.value.functionParams,
        functionCallData = contractArbitraryCallRequest.value.functionData.value,
        ethAmount = contractArbitraryCallRequest.value.ethAmount.rawValue,
        chainId = contractArbitraryCallRequest.value.chainId.value,
        redirectUrl = contractArbitraryCallRequest.value.redirectUrl,
        projectId = contractArbitraryCallRequest.value.projectId,
        createdAt = contractArbitraryCallRequest.value.createdAt.value,
        arbitraryData = contractArbitraryCallRequest.value.arbitraryData,
        screenConfig = contractArbitraryCallRequest.value.screenConfig.orEmpty(),
        callerAddress = contractArbitraryCallRequest.value.callerAddress?.rawValue,
        arbitraryCallTx = TransactionResponse(contractArbitraryCallRequest.transactionData),
        events = contractArbitraryCallRequest.transactionData.events?.map { EventInfoResponse(it) }
    )

    @Suppress("unused") // used for JSON schema generation
    @JsonIgnore
    @SchemaName("function_params")
    private val schemaFunctionParams: List<FunctionArgumentSchema> = emptyList()
}
