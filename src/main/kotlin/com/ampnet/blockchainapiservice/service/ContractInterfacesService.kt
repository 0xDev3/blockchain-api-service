package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.model.json.PartiallyMatchingInterfaceManifest
import java.util.UUID

interface ContractInterfacesService {
    fun getSuggestedInterfacesForImportedSmartContract(id: UUID): List<PartiallyMatchingInterfaceManifest>
}
