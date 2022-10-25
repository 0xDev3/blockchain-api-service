package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.model.json.DecompiledContractJson
import dev3.blockchainapiservice.util.ContractBinaryData

interface ContractDecompilerService {
    fun decompile(contractBinary: ContractBinaryData): DecompiledContractJson
}
