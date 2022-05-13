package com.ampnet.blockchainapiservice.util

import com.ampnet.blockchainapiservice.config.JsonConfig
import com.fasterxml.jackson.databind.JsonNode
import org.jooq.Converter
import org.jooq.JSON
import java.math.BigInteger

class ChainIdConverter : Converter<Long, ChainId> {
    override fun from(value: Long?): ChainId? = value?.let { ChainId(it) }
    override fun to(value: ChainId?): Long? = value?.value
    override fun fromType(): Class<Long> = Long::class.java
    override fun toType(): Class<ChainId> = ChainId::class.java

    companion object {
        private const val serialVersionUID: Long = -7367190641407949448L
    }
}

class ContractAddressConverter : Converter<String, ContractAddress> {
    override fun from(value: String?): ContractAddress? = value?.let { ContractAddress(it) }
    override fun to(value: ContractAddress?): String? = value?.rawValue
    override fun fromType(): Class<String> = String::class.java
    override fun toType(): Class<ContractAddress> = ContractAddress::class.java

    companion object {
        private const val serialVersionUID: Long = 5587113562689277144L
    }
}

class WalletAddressConverter : Converter<String, WalletAddress> {
    override fun from(value: String?): WalletAddress? = value?.let { WalletAddress(it) }
    override fun to(value: WalletAddress?): String? = value?.rawValue
    override fun fromType(): Class<String> = String::class.java
    override fun toType(): Class<WalletAddress> = WalletAddress::class.java

    companion object {
        private const val serialVersionUID: Long = 4048003464768439754L
    }
}

class BalanceConverter : Converter<BigInteger, Balance> {
    override fun from(value: BigInteger?): Balance? = value?.let { Balance(it) }
    override fun to(value: Balance?): BigInteger? = value?.rawValue
    override fun fromType(): Class<BigInteger> = BigInteger::class.java
    override fun toType(): Class<Balance> = Balance::class.java

    companion object {
        private const val serialVersionUID: Long = 6133349110900653628L
    }
}

class BlockNumberConverter : Converter<BigInteger, BlockNumber> {
    override fun from(value: BigInteger?): BlockNumber? = value?.let { BlockNumber(it) }
    override fun to(value: BlockNumber?): BigInteger? = value?.value
    override fun fromType(): Class<BigInteger> = BigInteger::class.java
    override fun toType(): Class<BlockNumber> = BlockNumber::class.java

    companion object {
        private const val serialVersionUID: Long = 154324798075521810L
    }
}

class TransactionHashConverter : Converter<String, TransactionHash> {
    override fun from(value: String?): TransactionHash? = value?.let { TransactionHash(it) }
    override fun to(value: TransactionHash?): String? = value?.value
    override fun fromType(): Class<String> = String::class.java
    override fun toType(): Class<TransactionHash> = TransactionHash::class.java

    companion object {
        private const val serialVersionUID: Long = -4101282162822045477L
    }
}

class SignedMessageConverter : Converter<String, SignedMessage> {
    override fun from(value: String?): SignedMessage? = value?.let { SignedMessage(it) }
    override fun to(value: SignedMessage?): String? = value?.value
    override fun fromType(): Class<String> = String::class.java
    override fun toType(): Class<SignedMessage> = SignedMessage::class.java

    companion object {
        private const val serialVersionUID: Long = 8287866421855372066L
    }
}

class JsonNodeConverter : Converter<JSON, JsonNode> {

    private val objectMapper = JsonConfig().objectMapper()

    override fun from(value: JSON?): JsonNode? = value?.let { objectMapper.readTree(it.data()) }
    override fun to(value: JsonNode?): JSON? = value?.let { JSON.valueOf(objectMapper.writeValueAsString(it)) }
    override fun fromType(): Class<JSON> = JSON::class.java
    override fun toType(): Class<JsonNode> = JsonNode::class.java

    companion object {
        private const val serialVersionUID: Long = 912286490023352427L
    }
}
