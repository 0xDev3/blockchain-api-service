package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.util.AbiType.AbiType
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.FunctionArgument
import com.ampnet.blockchainapiservice.util.FunctionData
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigInteger

class EthereumFunctionEncoderServiceTest : TestBase() {

    @Test
    fun mustCorrectlyEncodeFunctionCall() {
        val service = EthereumFunctionEncoderService()
        val toAddress = WalletAddress("0x495d96FaaaCEe16Dd3ca62cAB20a0F9548CdddB4")
        val amount = Balance(BigInteger("1000"))

        val encodedData = suppose("some test data will be encoded") {
            service.encode(
                functionName = "transfer",
                arguments = listOf(
                    FunctionArgument(abiType = AbiType.Address, value = toAddress),
                    FunctionArgument(abiType = AbiType.Uint256, value = amount),
                ),
                abiOutputTypes = listOf(AbiType.Bool)
            )
        }

        val expectedData = "0xa9059cbb000000000000000000000000495d96faaacee16dd3ca62cab20a0f9548cdddb4000000000000000" +
            "00000000000000000000000000000000000000000000003e8"

        verify("data is correctly encoded") {
            assertThat(encodedData).withMessage()
                .isEqualTo(FunctionData(expectedData))
        }
    }

    @Test
    fun mustCorrectlyEncodeConstructorCall() {
        val service = EthereumFunctionEncoderService()
        val toAddress = WalletAddress("0x495d96FaaaCEe16Dd3ca62cAB20a0F9548CdddB4")
        val amount = Balance(BigInteger("1000"))

        val encodedData = suppose("some test data will be encoded") {
            service.encodeConstructor(
                arguments = listOf(
                    FunctionArgument(abiType = AbiType.Address, value = toAddress),
                    FunctionArgument(abiType = AbiType.Uint256, value = amount),
                )
            )
        }

        val expectedData = "000000000000000000000000495d96faaacee16dd3ca62cab20a0f9548cdddb40000000000000000000000000" +
            "0000000000000000000000000000000000003e8"

        verify("data is correctly encoded") {
            assertThat(encodedData).withMessage()
                .isEqualTo(FunctionData(expectedData))
        }
    }

    @Test
    fun mustCorrectlyEncodeEmptyConstructorCall() {
        val service = EthereumFunctionEncoderService()

        val encodedData = suppose("some empty constructor will be encoded") {
            service.encodeConstructor(arguments = listOf())
        }

        verify("data is correctly encoded") {
            assertThat(encodedData).withMessage()
                .isEqualTo(FunctionData(""))
        }
    }
}
