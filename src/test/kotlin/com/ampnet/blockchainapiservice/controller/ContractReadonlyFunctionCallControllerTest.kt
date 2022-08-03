package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.JsonSchemaDocumentation
import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.model.params.CreateReadonlyFunctionCallParams
import com.ampnet.blockchainapiservice.model.params.DeployedContractIdIdentifier
import com.ampnet.blockchainapiservice.model.request.ReadonlyFunctionCallRequest
import com.ampnet.blockchainapiservice.model.response.ReadonlyFunctionCallResponse
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.model.result.ReadonlyFunctionCallResult
import com.ampnet.blockchainapiservice.service.ContractReadonlyFunctionCallService
import com.ampnet.blockchainapiservice.util.BaseUrl
import com.ampnet.blockchainapiservice.util.BlockNumber
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.ampnet.blockchainapiservice.util.WithDeployedContractIdAndAddress
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.springframework.http.ResponseEntity
import java.math.BigInteger
import java.util.UUID

class ContractReadonlyFunctionCallControllerTest : TestBase() {

    @Test
    fun mustCorrectlyCallContractReadonlyFunction() {
        val deployedContractId = UUID.randomUUID()
        val params = CreateReadonlyFunctionCallParams(
            identifier = DeployedContractIdIdentifier(deployedContractId),
            blockNumber = BlockNumber(BigInteger.TEN),
            functionName = "example",
            functionParams = emptyList(),
            outputParameters = listOf("string", "uint256", "bool"),
            callerAddress = WalletAddress("a")
        )
        val result = WithDeployedContractIdAndAddress(
            value = ReadonlyFunctionCallResult(
                blockNumber = params.blockNumber!!,
                timestamp = TestData.TIMESTAMP,
                returnValues = listOf("value", 1, true)
            ),
            deployedContractId = deployedContractId,
            contractAddress = ContractAddress("caebafe")
        )
        val project = Project(
            id = UUID.randomUUID(),
            ownerId = UUID.randomUUID(),
            issuerContractAddress = ContractAddress("b"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = ChainId(1337L),
            customRpcUrl = "custom-rpc-url",
            createdAt = TestData.TIMESTAMP
        )
        val service = mock<ContractReadonlyFunctionCallService>()

        suppose("contract readonly function call request executed") {
            given(service.callReadonlyContractFunction(params, project))
                .willReturn(result)
        }

        val controller = ContractReadonlyFunctionCallController(service)

        verify("controller returns correct response") {
            val request = ReadonlyFunctionCallRequest(
                deployedContractId = deployedContractId,
                deployedContractAlias = null,
                contractAddress = null,
                blockNumber = params.blockNumber?.value,
                functionName = params.functionName,
                functionParams = params.functionParams,
                outputParameters = params.outputParameters,
                callerAddress = params.callerAddress.rawValue
            )
            val response = controller.callReadonlyContractFunction(project, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            Assertions.assertThat(response).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(
                        ReadonlyFunctionCallResponse(
                            deployedContractId = deployedContractId,
                            contractAddress = result.contractAddress.rawValue,
                            blockNumber = result.value.blockNumber.value,
                            timestamp = result.value.timestamp.value,
                            returnValues = result.value.returnValues.map { it.toString() }
                        )
                    )
                )
        }
    }
}
