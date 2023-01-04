package dev3.blockchainapiservice.features.contract.importing.service

import dev3.blockchainapiservice.features.contract.importing.model.json.DecompiledContractJson
import dev3.blockchainapiservice.util.ContractBinaryData

interface ContractDecompilerService {
    fun decompile(contractBinary: ContractBinaryData): DecompiledContractJson
}
