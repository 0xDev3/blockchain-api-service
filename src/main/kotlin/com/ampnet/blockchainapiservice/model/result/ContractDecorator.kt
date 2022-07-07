package com.ampnet.blockchainapiservice.model.result

import com.ampnet.blockchainapiservice.util.ContractBinaryData
import com.ampnet.blockchainapiservice.util.ContractId
import com.ampnet.blockchainapiservice.util.ContractTag
import com.ampnet.blockchainapiservice.util.ContractTrait

data class ContractDecorator(
    val id: ContractId,
    val binary: ContractBinaryData,
    val tags: List<ContractTag>,
    val implements: List<ContractTrait>
)
