package dev3.blockchainapiservice.controller

import dev3.blockchainapiservice.JsonSchemaDocumentation
import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.features.gas.controller.GasController
import dev3.blockchainapiservice.features.gas.model.request.EstimateGasCostForContractArbitraryCallRequest
import dev3.blockchainapiservice.features.gas.model.response.EstimateGasCostResponse
import dev3.blockchainapiservice.features.gas.model.response.GasPriceResponse
import dev3.blockchainapiservice.features.gas.service.GasService
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.util.BaseUrl
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.FunctionData
import dev3.blockchainapiservice.util.GasEstimate
import dev3.blockchainapiservice.util.GasPrice
import dev3.blockchainapiservice.util.WalletAddress
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.http.ResponseEntity
import java.math.BigInteger
import java.util.UUID

class GasControllerTest : TestBase() {

    @Test
    fun mustCorrectlyEstimateArbitraryCallGasCost() {
        val project = Project(
            id = UUID.randomUUID(),
            ownerId = UUID.randomUUID(),
            issuerContractAddress = ContractAddress("a"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = ChainId(1337L),
            customRpcUrl = "custom-rpc-url",
            createdAt = TestData.TIMESTAMP
        )
        val request = EstimateGasCostForContractArbitraryCallRequest(
            deployedContractId = null,
            deployedContractAlias = null,
            contractAddress = ContractAddress("abc").rawValue,
            functionData = FunctionData("1234").value,
            ethAmount = BigInteger.ZERO,
            callerAddress = WalletAddress("1").rawValue
        )

        val service = mock<GasService>()

        suppose("gas cost will be estimated") {
            call(service.estimateGasCost(request, project))
                .willReturn(GasEstimate(BigInteger.ONE))
        }

        val controller = GasController(service)

        verify("controller returns correct response") {
            val response = controller.estimateArbitraryCallGasCost(project, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(EstimateGasCostResponse(project.chainId.value, BigInteger.ONE)))
        }
    }

    @Test
    fun mustCorrectlyGetGasPrice() {
        val project = Project(
            id = UUID.randomUUID(),
            ownerId = UUID.randomUUID(),
            issuerContractAddress = ContractAddress("a"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = ChainId(1337L),
            customRpcUrl = "custom-rpc-url",
            createdAt = TestData.TIMESTAMP
        )

        val service = mock<GasService>()

        suppose("gas cost will be estimated") {
            call(service.getGasPrice(project))
                .willReturn(GasPrice(BigInteger.ONE))
        }

        val controller = GasController(service)

        verify("controller returns correct response") {
            val response = controller.getGasPrice(project)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(GasPriceResponse(project.chainId.value, BigInteger.ONE)))
        }
    }
}
