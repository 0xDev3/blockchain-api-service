package dev3.blockchainapiservice.features.contract.functioncall.controller

import dev3.blockchainapiservice.config.binding.annotation.ApiKeyBinding
import dev3.blockchainapiservice.config.interceptors.annotation.ApiReadLimitedMapping
import dev3.blockchainapiservice.config.interceptors.annotation.ApiWriteLimitedMapping
import dev3.blockchainapiservice.config.interceptors.annotation.IdType
import dev3.blockchainapiservice.config.validation.ValidEthAddress
import dev3.blockchainapiservice.features.api.access.model.result.Project
import dev3.blockchainapiservice.features.contract.functioncall.model.filters.ContractFunctionCallRequestFilters
import dev3.blockchainapiservice.features.contract.functioncall.model.params.CreateContractFunctionCallRequestParams
import dev3.blockchainapiservice.features.contract.functioncall.model.request.CreateContractFunctionCallRequest
import dev3.blockchainapiservice.features.contract.functioncall.model.response.ContractFunctionCallRequestResponse
import dev3.blockchainapiservice.features.contract.functioncall.model.response.ContractFunctionCallRequestsResponse
import dev3.blockchainapiservice.features.contract.functioncall.service.ContractFunctionCallRequestService
import dev3.blockchainapiservice.generated.jooq.id.ContractDeploymentRequestId
import dev3.blockchainapiservice.generated.jooq.id.ContractFunctionCallRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.model.request.AttachTransactionInfoRequest
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@Validated
@RestController
class ContractFunctionCallRequestController(
    private val contractFunctionCallRequestService: ContractFunctionCallRequestService
) {

    @ApiWriteLimitedMapping(IdType.PROJECT_ID, RequestMethod.POST, "/v1/function-call")
    fun createContractFunctionCallRequest(
        @ApiKeyBinding project: Project,
        @Valid @RequestBody requestBody: CreateContractFunctionCallRequest
    ): ResponseEntity<ContractFunctionCallRequestResponse> {
        val params = CreateContractFunctionCallRequestParams(requestBody)
        val createdRequest = contractFunctionCallRequestService.createContractFunctionCallRequest(params, project)
        return ResponseEntity.ok(ContractFunctionCallRequestResponse(createdRequest))
    }

    @ApiReadLimitedMapping(IdType.FUNCTION_CALL_REQUEST_ID, "/v1/function-call/{id}")
    fun getContractFunctionCallRequest(
        @PathVariable("id") id: ContractFunctionCallRequestId
    ): ResponseEntity<ContractFunctionCallRequestResponse> {
        val contractFunctionCallRequest = contractFunctionCallRequestService.getContractFunctionCallRequest(id)
        return ResponseEntity.ok(ContractFunctionCallRequestResponse(contractFunctionCallRequest))
    }

    @ApiReadLimitedMapping(IdType.PROJECT_ID, "/v1/function-call/by-project/{projectId}")
    fun getContractFunctionCallRequestsByProjectIdAndFilters(
        @PathVariable("projectId") projectId: ProjectId,
        @RequestParam("deployedContractId", required = false) deployedContractId: ContractDeploymentRequestId?,
        @ValidEthAddress @RequestParam("contractAddress", required = false) contractAddress: String?
    ): ResponseEntity<ContractFunctionCallRequestsResponse> {
        val contractFunctionCallRequests = contractFunctionCallRequestService
            .getContractFunctionCallRequestsByProjectIdAndFilters(
                projectId = projectId,
                filters = ContractFunctionCallRequestFilters(
                    deployedContractId = deployedContractId,
                    contractAddress = contractAddress?.let { ContractAddress(it) }
                )
            )
        return ResponseEntity.ok(
            ContractFunctionCallRequestsResponse(
                contractFunctionCallRequests.map { ContractFunctionCallRequestResponse(it) }
            )
        )
    }

    @ApiWriteLimitedMapping(IdType.FUNCTION_CALL_REQUEST_ID, RequestMethod.PUT, "/v1/function-call/{id}")
    fun attachTransactionInfo(
        @PathVariable("id") id: ContractFunctionCallRequestId,
        @Valid @RequestBody requestBody: AttachTransactionInfoRequest
    ) {
        contractFunctionCallRequestService.attachTxInfo(
            id = id,
            txHash = TransactionHash(requestBody.txHash),
            caller = WalletAddress(requestBody.callerAddress)
        )
    }
}
