package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.model.json.DecompiledContractJson
import com.ampnet.blockchainapiservice.util.ContractBinaryData

interface ContractDecompilerService {
    fun decompile(contractBinary: ContractBinaryData): DecompiledContractJson
}
