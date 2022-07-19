package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.exception.ResourceNotFoundException
import com.ampnet.blockchainapiservice.model.filters.AndList
import com.ampnet.blockchainapiservice.model.filters.ContractDecoratorFilters
import com.ampnet.blockchainapiservice.model.filters.OrList
import com.ampnet.blockchainapiservice.model.filters.parseOrListWithNestedAndLists
import com.ampnet.blockchainapiservice.model.response.ContractDecoratorResponse
import com.ampnet.blockchainapiservice.model.response.ContractDecoratorsResponse
import com.ampnet.blockchainapiservice.repository.ContractDecoratorRepository
import com.ampnet.blockchainapiservice.util.ContractId
import com.ampnet.blockchainapiservice.util.ContractTag
import com.ampnet.blockchainapiservice.util.ContractTrait
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class ContractDecoratorController(private val contractDecoratorRepository: ContractDecoratorRepository) {

    @GetMapping("/v1/deployable-contracts/{id}")
    fun getContractDecorator(
        @PathVariable("id") id: String
    ): ResponseEntity<ContractDecoratorResponse> {
        val contractDecorator = contractDecoratorRepository.getById(ContractId(id)) ?: throw ResourceNotFoundException(
            "Contract decorator not found for contract ID: $id"
        )
        return ResponseEntity.ok(ContractDecoratorResponse(contractDecorator))
    }

    @GetMapping("/v1/deployable-contracts")
    fun getContractDecorators(
        @RequestParam("tags", required = false) contractTags: List<String>?,
        @RequestParam("implements", required = false) contractImplements: List<String>?
    ): ResponseEntity<ContractDecoratorsResponse> {
        val contractDecorators = contractDecoratorRepository.getAll(
            ContractDecoratorFilters(
                contractTags = contractTags.parseOrListWithNestedAndLists { ContractTag(it) },
                contractImplements = contractImplements.parseOrListWithNestedAndLists { ContractTrait(it) }
            )
        )
        return ResponseEntity.ok(ContractDecoratorsResponse(contractDecorators.map { ContractDecoratorResponse(it) }))
    }

    private fun <T> List<String>?.toOrListWithNestedAndLists(wrap: (String) -> T): OrList<AndList<T>> =
        OrList(this.orEmpty().map { AndList(it.split(" AND ").map { wrap(it) }) })
}
