package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.config.binding.annotation.ApiKeyBinding
import com.ampnet.blockchainapiservice.config.validation.ValidEthAddress
import com.ampnet.blockchainapiservice.model.filters.ContractFunctionCallRequestFilters
import com.ampnet.blockchainapiservice.model.params.CreateContractFunctionCallRequestParams
import com.ampnet.blockchainapiservice.model.request.AttachTransactionInfoRequest
import com.ampnet.blockchainapiservice.model.request.CreateContractFunctionCallRequest
import com.ampnet.blockchainapiservice.model.response.ContractFunctionCallRequestResponse
import com.ampnet.blockchainapiservice.model.response.ContractFunctionCallRequestsResponse
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.service.ContractFunctionCallRequestService
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import javax.validation.Valid

@Validated
@RestController
class ContractFunctionCallRequestController(
    private val contractFunctionCallRequestService: ContractFunctionCallRequestService
) {

    @PostMapping("/v1/function-call")
    fun createContractFunctionCallRequest(
        @ApiKeyBinding project: Project,
        @Valid @RequestBody requestBody: CreateContractFunctionCallRequest
    ): ResponseEntity<ContractFunctionCallRequestResponse> {
        val params = CreateContractFunctionCallRequestParams(requestBody)
        val createdRequest = contractFunctionCallRequestService.createContractFunctionCallRequest(params, project)
        return ResponseEntity.ok(ContractFunctionCallRequestResponse(createdRequest))
    }

    @GetMapping("/v1/function-call/{id}")
    fun getContractFunctionCallRequest(
        @PathVariable("id") id: UUID
    ): ResponseEntity<ContractFunctionCallRequestResponse> {
        val contractFunctionCallRequest = contractFunctionCallRequestService.getContractFunctionCallRequest(id)
        return ResponseEntity.ok(ContractFunctionCallRequestResponse(contractFunctionCallRequest))
    }

    @GetMapping("/v1/function-call/by-project/{projectId}")
    fun getContractFunctionCallRequestsByProjectIdAndFilters(
        @PathVariable("projectId") projectId: UUID,
        @RequestParam("deployedContractId", required = false) deployedContractId: UUID?,
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

    @PutMapping("/v1/function-call/{id}")
    fun attachTransactionInfo(
        @PathVariable("id") id: UUID,
        @Valid @RequestBody requestBody: AttachTransactionInfoRequest
    ) {
        contractFunctionCallRequestService.attachTxInfo(
            id = id,
            txHash = TransactionHash(requestBody.txHash),
            caller = WalletAddress(requestBody.callerAddress)
        )
    }
}
