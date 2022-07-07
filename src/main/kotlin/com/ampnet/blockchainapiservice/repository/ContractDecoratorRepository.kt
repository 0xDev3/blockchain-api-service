package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.model.result.ContractDecorator
import com.ampnet.blockchainapiservice.util.ContractId

interface ContractDecoratorRepository {
    fun store(contractDecorator: ContractDecorator): ContractDecorator
    fun delete(id: ContractId): Boolean
    fun getById(id: ContractId): ContractDecorator?
}
