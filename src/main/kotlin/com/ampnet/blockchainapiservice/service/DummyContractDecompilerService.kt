package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.model.json.DecompiledContractJson
import com.ampnet.blockchainapiservice.util.ContractBinaryData
import org.springframework.stereotype.Service

@Service
class DummyContractDecompilerService : ContractDecompilerService {
    override fun decompile(contractBinary: ContractBinaryData): DecompiledContractJson =
        TODO("implement contract decompile call")
}
