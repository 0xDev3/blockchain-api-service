package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.blockchain.properties.ChainSpec
import dev3.blockchainapiservice.model.params.ImportContractParams
import dev3.blockchainapiservice.model.result.ContractDecorator
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.util.ContractAddress
import java.util.UUID

interface ContractImportService {
    fun importExistingContract(params: ImportContractParams, project: Project): UUID?
    fun importContract(params: ImportContractParams, project: Project): UUID
    fun previewImport(contractAddress: ContractAddress, chainSpec: ChainSpec): ContractDecorator
}
