package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.util.AbiType.AbiType
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.FunctionArgument
import com.ampnet.blockchainapiservice.util.FunctionData
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.web3j.abi.datatypes.Utf8String
import java.math.BigInteger

class EthereumFunctionEncoderServiceTest : TestBase() {

    @Test
    fun mustCorrectlyEncodeFunctionWithAdditionalData() {
        val service = EthereumFunctionEncoderService()
        val toAddress = WalletAddress("0x495d96FaaaCEe16Dd3ca62cAB20a0F9548CdddB4")
        val amount = Balance(BigInteger("1000"))
        val uuid = Utf8String("6e646f6e-5615-46dc-9583-904ebe37e3c2")

        val encodedData = suppose("some test data will be encoded") {
            service.encode(
                functionName = "transfer",
                arguments = listOf(
                    FunctionArgument(abiType = AbiType.Address, value = toAddress),
                    FunctionArgument(abiType = AbiType.Uint256, value = amount),
                ),
                abiOutputTypes = listOf(AbiType.Bool),
                additionalData = listOf(uuid)
            )
        }

        val expectedData = "0xa9059cbb000000000000000000000000495d96faaacee16dd3ca62cab20a0f9548cdddb4000000000000000" +
            "00000000000000000000000000000000000000000000003e80000000000000000000000000000000000000000000000000000000" +
            "00000002436653634366636652d353631352d343664632d393538332d39303465626533376533633200000000000000000000000" +
            "000000000000000000000000000000000"

        verify("data is correctly encoded") {
            assertThat(encodedData).withMessage()
                .isEqualTo(FunctionData(expectedData))
        }
    }
}
