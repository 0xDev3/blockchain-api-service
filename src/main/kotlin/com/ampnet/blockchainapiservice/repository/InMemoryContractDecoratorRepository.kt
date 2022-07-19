package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.model.filters.AndList
import com.ampnet.blockchainapiservice.model.filters.ContractDecoratorFilters
import com.ampnet.blockchainapiservice.model.filters.OrList
import com.ampnet.blockchainapiservice.model.result.ContractDecorator
import com.ampnet.blockchainapiservice.util.ContractId
import mu.KLogging
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

@Repository
class InMemoryContractDecoratorRepository : ContractDecoratorRepository {

    companion object : KLogging()

    private val storage = ConcurrentHashMap<ContractId, ContractDecorator>()

    override fun store(contractDecorator: ContractDecorator): ContractDecorator {
        logger.info { "Storing contract decorator with ID: ${contractDecorator.id}" }
        storage[contractDecorator.id] = contractDecorator
        return contractDecorator
    }

    override fun delete(id: ContractId): Boolean {
        logger.info { "Deleting contract decorator with ID: $id" }
        return storage.remove(id) != null
    }

    override fun getById(id: ContractId): ContractDecorator? {
        logger.debug { "Get contract decorator by ID: $id" }
        return storage[id]
    }

    override fun getAll(filters: ContractDecoratorFilters): List<ContractDecorator> {
        logger.debug { "Get all contract decorators, filters: $filters" }
        return storage.values
            .filterBy(filters.contractTags) { it.tags }
            .filterBy(filters.contractImplements) { it.implements }
            .toList()
    }

    private fun <T> Collection<ContractDecorator>.filterBy(
        orList: OrList<AndList<T>>,
        values: (ContractDecorator) -> List<T>
    ): Collection<ContractDecorator> {
        val conditions = orList.list.map { it.list }

        return if (conditions.isEmpty()) {
            this
        } else {
            filter { decorator ->
                val decoratorValues = values(decorator)
                conditions.map { condition -> decoratorValues.containsAll(condition) }.contains(true)
            }
        }
    }
}
