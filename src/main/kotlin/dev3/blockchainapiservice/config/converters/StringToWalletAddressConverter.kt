package dev3.blockchainapiservice.config.converters

import dev3.blockchainapiservice.util.WalletAddress
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
@ConfigurationPropertiesBinding
class StringToWalletAddressConverter : Converter<String, WalletAddress> {
    override fun convert(source: String): WalletAddress = WalletAddress(source)
}
