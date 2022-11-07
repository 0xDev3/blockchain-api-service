package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.model.params.ImportContractParams
import dev3.blockchainapiservice.model.result.Project
import java.util.UUID

interface ContractImportService {
    fun importExistingContract(params: ImportContractParams, project: Project): UUID?
    fun importContract(params: ImportContractParams, project: Project): UUID
}
