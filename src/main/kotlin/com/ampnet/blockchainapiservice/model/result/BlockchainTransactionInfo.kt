package com.ampnet.blockchainapiservice.model.result

import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.EthereumAddress
import com.ampnet.blockchainapiservice.util.FunctionData
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.UtcDateTime
import com.ampnet.blockchainapiservice.util.WalletAddress
import java.math.BigInteger

data class BlockchainTransactionInfo(
    val hash: TransactionHash,
    val from: WalletAddress,
    val to: EthereumAddress,
    val deployedContractAddress: ContractAddress?,
    val data: FunctionData,
    val value: Balance,
    val blockConfirmations: BigInteger,
    val timestamp: UtcDateTime,
    val success: Boolean
) {
    fun hashMatches(expectedHash: TransactionHash?): Boolean =
        hash == expectedHash

    fun fromAddressOptionallyMatches(optionalAddress: WalletAddress?): Boolean =
        optionalAddress == null || from == optionalAddress

    fun toAddressMatches(toAddress: EthereumAddress): Boolean =
        to.toWalletAddress() == toAddress.toWalletAddress()

    fun deployedContractAddressIsNull(): Boolean =
        deployedContractAddress == null

    fun deployedContractAddressMatches(contractAddress: ContractAddress?): Boolean =
        deployedContractAddress != null && contractAddress == deployedContractAddress

    fun dataMatches(expectedData: FunctionData): Boolean =
        data == expectedData

    fun valueMatches(expectedValue: Balance): Boolean =
        value == expectedValue
}
