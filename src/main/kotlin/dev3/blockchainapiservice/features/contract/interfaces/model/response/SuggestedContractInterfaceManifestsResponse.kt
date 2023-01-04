package dev3.blockchainapiservice.features.contract.interfaces.model.response

import dev3.blockchainapiservice.features.contract.interfaces.model.result.MatchingContractInterfaces

data class SuggestedContractInterfaceManifestsResponse(
    val manifests: List<ContractInterfaceManifestResponse>,
    val bestMatchingInterfaces: List<String>
) {
    constructor(matchingInterfaces: MatchingContractInterfaces) : this(
        manifests = matchingInterfaces.manifests.map(::ContractInterfaceManifestResponse),
        bestMatchingInterfaces = matchingInterfaces.bestMatchingInterfaces.map { it.value }
    )
}
