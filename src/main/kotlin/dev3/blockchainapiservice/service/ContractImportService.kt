package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.model.params.ImportContractParams
import com.ampnet.blockchainapiservice.model.result.Project
import java.util.UUID

interface ContractImportService {
    fun importContract(params: ImportContractParams, project: Project): UUID
}
