package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.config.binding.annotation.ApiKeyBinding
import com.ampnet.blockchainapiservice.model.params.CreateReadonlyFunctionCallParams
import com.ampnet.blockchainapiservice.model.request.ReadonlyFunctionCallRequest
import com.ampnet.blockchainapiservice.model.response.ReadonlyFunctionCallResponse
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.service.ContractReadonlyFunctionCallService
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@Validated
@RestController
class ContractReadonlyFunctionCallController(
    private val contractReadonlyFunctionCallService: ContractReadonlyFunctionCallService
) {

    @PostMapping("/v1/readonly-function-call")
    fun callReadonlyContractFunction(
        @ApiKeyBinding project: Project,
        @Valid @RequestBody requestBody: ReadonlyFunctionCallRequest
    ): ResponseEntity<ReadonlyFunctionCallResponse> {
        val params = CreateReadonlyFunctionCallParams(requestBody)
        val result = contractReadonlyFunctionCallService.callReadonlyContractFunction(params, project)
        return ResponseEntity.ok(ReadonlyFunctionCallResponse(result))
    }
}
