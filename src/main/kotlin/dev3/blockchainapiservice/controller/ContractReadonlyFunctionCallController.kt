package dev3.blockchainapiservice.controller

import com.fasterxml.jackson.databind.ObjectMapper
import dev3.blockchainapiservice.config.binding.annotation.ApiKeyBinding
import dev3.blockchainapiservice.config.interceptors.annotation.ApiWriteLimitedMapping
import dev3.blockchainapiservice.config.interceptors.annotation.IdType
import dev3.blockchainapiservice.model.params.CreateReadonlyFunctionCallParams
import dev3.blockchainapiservice.model.request.ReadonlyFunctionCallRequest
import dev3.blockchainapiservice.model.response.ReadonlyFunctionCallResponse
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.service.ContractReadonlyFunctionCallService
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@Validated
@RestController
class ContractReadonlyFunctionCallController(
    private val contractReadonlyFunctionCallService: ContractReadonlyFunctionCallService,
    private val objectMapper: ObjectMapper
) {

    @ApiWriteLimitedMapping(IdType.PROJECT_ID, RequestMethod.POST, "/v1/readonly-function-call")
    fun callReadonlyContractFunction(
        @ApiKeyBinding project: Project,
        @Valid @RequestBody requestBody: ReadonlyFunctionCallRequest
    ): ResponseEntity<ReadonlyFunctionCallResponse> {
        val params = CreateReadonlyFunctionCallParams(requestBody)
        val result = contractReadonlyFunctionCallService.callReadonlyContractFunction(params, project)
        return ResponseEntity.ok(
            ReadonlyFunctionCallResponse(
                result = result,
                outputParams = objectMapper.createArrayNode().apply {
                    addAll(requestBody.outputParams.map { it.rawJson })
                }
            )
        )
    }
}
