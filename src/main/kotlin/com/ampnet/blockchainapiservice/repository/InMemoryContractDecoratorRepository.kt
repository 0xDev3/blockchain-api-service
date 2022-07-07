package com.ampnet.blockchainapiservice.repository

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
        return storage[id]
    }
}
