package com.ampnet.blockchainapiservice.config.binding

import com.ampnet.blockchainapiservice.util.BlockNumber
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import java.math.BigInteger

@Component
class StringToBlockNumberConverter : Converter<String, BlockNumber> {
    override fun convert(source: String) = BlockNumber(BigInteger(source))
}

@Component
class StringToContractAddressConverter : Converter<String, ContractAddress> {
    override fun convert(source: String) = ContractAddress(source)
}

@Component
class StringToWalletAddressConverter : Converter<String, WalletAddress> {
    override fun convert(source: String) = WalletAddress(source)
}
