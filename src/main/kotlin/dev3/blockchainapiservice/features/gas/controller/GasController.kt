package dev3.blockchainapiservice.features.gas.controller

import dev3.blockchainapiservice.config.binding.annotation.ApiKeyBinding
import dev3.blockchainapiservice.features.gas.model.request.EstimateGasCostForContractArbitraryCallRequest
import dev3.blockchainapiservice.features.gas.model.response.EstimateGasCostResponse
import dev3.blockchainapiservice.features.gas.model.response.GasPriceResponse
import dev3.blockchainapiservice.features.gas.service.GasService
import dev3.blockchainapiservice.model.result.Project
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@Validated
@RestController
class GasController(private val gasService: GasService) {

    @PostMapping("/v1/gas/estimate/arbitrary-call")
    fun estimateArbitraryCallGasCost(
        @ApiKeyBinding project: Project,
        @Valid @RequestBody requestBody: EstimateGasCostForContractArbitraryCallRequest
    ): ResponseEntity<EstimateGasCostResponse> {
        val result = gasService.estimateGasCost(requestBody, project)
        return ResponseEntity.ok(EstimateGasCostResponse.create(project.chainId, result))
    }

    @GetMapping("/v1/gas/price")
    fun getGasPrice(
        @ApiKeyBinding project: Project
    ): ResponseEntity<GasPriceResponse> {
        val result = gasService.getGasPrice(project)
        return ResponseEntity.ok(GasPriceResponse.create(project.chainId, result))
    }
}
