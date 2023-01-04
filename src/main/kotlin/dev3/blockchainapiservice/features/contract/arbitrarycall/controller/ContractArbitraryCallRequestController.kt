package dev3.blockchainapiservice.features.contract.arbitrarycall.controller

import dev3.blockchainapiservice.config.binding.annotation.ApiKeyBinding
import dev3.blockchainapiservice.config.interceptors.annotation.ApiReadLimitedMapping
import dev3.blockchainapiservice.config.interceptors.annotation.ApiWriteLimitedMapping
import dev3.blockchainapiservice.config.interceptors.annotation.IdType
import dev3.blockchainapiservice.config.validation.ValidEthAddress
import dev3.blockchainapiservice.features.api.access.model.result.Project
import dev3.blockchainapiservice.features.contract.arbitrarycall.model.filters.ContractArbitraryCallRequestFilters
import dev3.blockchainapiservice.features.contract.arbitrarycall.model.params.CreateContractArbitraryCallRequestParams
import dev3.blockchainapiservice.features.contract.arbitrarycall.model.request.CreateContractArbitraryCallRequest
import dev3.blockchainapiservice.features.contract.arbitrarycall.model.response.ContractArbitraryCallRequestResponse
import dev3.blockchainapiservice.features.contract.arbitrarycall.model.response.ContractArbitraryCallRequestsResponse
import dev3.blockchainapiservice.features.contract.arbitrarycall.service.ContractArbitraryCallRequestService
import dev3.blockchainapiservice.generated.jooq.id.ContractArbitraryCallRequestId
import dev3.blockchainapiservice.generated.jooq.id.ContractDeploymentRequestId
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
class ContractArbitraryCallRequestController(
    private val contractArbitraryCallRequestService: ContractArbitraryCallRequestService
) {

    @ApiWriteLimitedMapping(IdType.PROJECT_ID, RequestMethod.POST, "/v1/arbitrary-call")
    fun createContractArbitraryCallRequest(
        @ApiKeyBinding project: Project,
        @Valid @RequestBody requestBody: CreateContractArbitraryCallRequest
    ): ResponseEntity<ContractArbitraryCallRequestResponse> {
        val params = CreateContractArbitraryCallRequestParams(requestBody)
        val createdRequest = contractArbitraryCallRequestService.createContractArbitraryCallRequest(params, project)
        return ResponseEntity.ok(ContractArbitraryCallRequestResponse(createdRequest))
    }

    @ApiReadLimitedMapping(IdType.ARBITRARY_CALL_REQUEST_ID, "/v1/arbitrary-call/{id}")
    fun getContractArbitraryCallRequest(
        @PathVariable("id") id: ContractArbitraryCallRequestId
    ): ResponseEntity<ContractArbitraryCallRequestResponse> {
        val contractArbitraryCallRequest = contractArbitraryCallRequestService.getContractArbitraryCallRequest(id)
        return ResponseEntity.ok(ContractArbitraryCallRequestResponse(contractArbitraryCallRequest))
    }

    @ApiReadLimitedMapping(IdType.PROJECT_ID, "/v1/arbitrary-call/by-project/{projectId}")
    fun getContractArbitraryCallRequestsByProjectIdAndFilters(
        @PathVariable("projectId") projectId: ProjectId,
        @RequestParam("deployedContractId", required = false) deployedContractId: ContractDeploymentRequestId?,
        @ValidEthAddress @RequestParam("contractAddress", required = false) contractAddress: String?
    ): ResponseEntity<ContractArbitraryCallRequestsResponse> {
        val contractArbitraryCallRequests = contractArbitraryCallRequestService
            .getContractArbitraryCallRequestsByProjectIdAndFilters(
                projectId = projectId,
                filters = ContractArbitraryCallRequestFilters(
                    deployedContractId = deployedContractId,
                    contractAddress = contractAddress?.let { ContractAddress(it) }
                )
            )
        return ResponseEntity.ok(
            ContractArbitraryCallRequestsResponse(
                contractArbitraryCallRequests.map { ContractArbitraryCallRequestResponse(it) }
            )
        )
    }

    @ApiWriteLimitedMapping(IdType.ARBITRARY_CALL_REQUEST_ID, RequestMethod.PUT, "/v1/arbitrary-call/{id}")
    fun attachTransactionInfo(
        @PathVariable("id") id: ContractArbitraryCallRequestId,
        @Valid @RequestBody requestBody: AttachTransactionInfoRequest
    ) {
        contractArbitraryCallRequestService.attachTxInfo(
            id = id,
            txHash = TransactionHash(requestBody.txHash),
            caller = WalletAddress(requestBody.callerAddress)
        )
    }
}
