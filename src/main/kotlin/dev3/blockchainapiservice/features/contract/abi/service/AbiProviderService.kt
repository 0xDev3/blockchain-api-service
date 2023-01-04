package dev3.blockchainapiservice.features.contract.abi.service

import dev3.blockchainapiservice.blockchain.properties.ChainSpec
import dev3.blockchainapiservice.features.contract.importing.model.json.DecompiledContractJson
import dev3.blockchainapiservice.util.ContractAddress

interface AbiProviderService {
    fun getContractAbi(
        bytecode: String,
        deployedBytecode: String,
        contractAddress: ContractAddress,
        chainSpec: ChainSpec
    ): DecompiledContractJson?
}
