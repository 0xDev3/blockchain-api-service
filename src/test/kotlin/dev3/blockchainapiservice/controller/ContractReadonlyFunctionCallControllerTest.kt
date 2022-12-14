package dev3.blockchainapiservice.controller

import dev3.blockchainapiservice.JsonSchemaDocumentation
import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.config.JsonConfig
import dev3.blockchainapiservice.features.api.access.model.result.Project
import dev3.blockchainapiservice.features.contract.abi.model.BoolType
import dev3.blockchainapiservice.features.contract.abi.model.StringType
import dev3.blockchainapiservice.features.contract.abi.model.UintType
import dev3.blockchainapiservice.features.contract.deployment.model.params.DeployedContractIdIdentifier
import dev3.blockchainapiservice.features.contract.readcall.controller.ContractReadonlyFunctionCallController
import dev3.blockchainapiservice.features.contract.readcall.model.params.CreateReadonlyFunctionCallParams
import dev3.blockchainapiservice.features.contract.readcall.model.params.OutputParameter
import dev3.blockchainapiservice.features.contract.readcall.model.request.ReadonlyFunctionCallRequest
import dev3.blockchainapiservice.features.contract.readcall.model.response.ReadonlyFunctionCallResponse
import dev3.blockchainapiservice.features.contract.readcall.model.result.ReadonlyFunctionCallResult
import dev3.blockchainapiservice.features.contract.readcall.service.ContractReadonlyFunctionCallService
import dev3.blockchainapiservice.generated.jooq.id.ContractDeploymentRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.generated.jooq.id.UserId
import dev3.blockchainapiservice.util.BaseUrl
import dev3.blockchainapiservice.util.BlockNumber
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.WalletAddress
import dev3.blockchainapiservice.util.WithDeployedContractIdAndAddress
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.http.ResponseEntity
import java.math.BigInteger
import java.util.UUID

class ContractReadonlyFunctionCallControllerTest : TestBase() {

    @Test
    fun mustCorrectlyCallContractReadonlyFunction() {
        val deployedContractId = ContractDeploymentRequestId(UUID.randomUUID())
        val params = CreateReadonlyFunctionCallParams(
            identifier = DeployedContractIdIdentifier(deployedContractId),
            blockNumber = BlockNumber(BigInteger.TEN),
            functionName = "example",
            functionParams = emptyList(),
            outputParams = listOf(
                OutputParameter(StringType),
                OutputParameter(UintType),
                OutputParameter(BoolType)
            ),
            callerAddress = WalletAddress("a")
        )
        val result = WithDeployedContractIdAndAddress(
            value = ReadonlyFunctionCallResult(
                blockNumber = params.blockNumber!!,
                timestamp = TestData.TIMESTAMP,
                returnValues = listOf("value", 1, true),
                rawReturnValue = "0x0"
            ),
            deployedContractId = deployedContractId,
            contractAddress = ContractAddress("cafebafe")
        )
        val project = Project(
            id = ProjectId(UUID.randomUUID()),
            ownerId = UserId(UUID.randomUUID()),
            issuerContractAddress = ContractAddress("b"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = ChainId(1337L),
            customRpcUrl = "custom-rpc-url",
            createdAt = TestData.TIMESTAMP
        )
        val service = mock<ContractReadonlyFunctionCallService>()

        suppose("contract readonly function call request executed") {
            call(service.callReadonlyContractFunction(params, project))
                .willReturn(result)
        }

        val controller = ContractReadonlyFunctionCallController(service, JsonConfig().objectMapper())

        verify("controller returns correct response") {
            val request = ReadonlyFunctionCallRequest(
                deployedContractId = deployedContractId,
                deployedContractAlias = null,
                contractAddress = null,
                blockNumber = params.blockNumber?.value,
                functionName = params.functionName,
                functionParams = params.functionParams,
                outputParams = params.outputParams,
                callerAddress = params.callerAddress.rawValue
            )
            val response = controller.callReadonlyContractFunction(project, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        ReadonlyFunctionCallResponse(
                            deployedContractId = deployedContractId,
                            contractAddress = result.contractAddress.rawValue,
                            blockNumber = result.value.blockNumber.value,
                            timestamp = result.value.timestamp.value,
                            outputParams = response.body!!.outputParams,
                            returnValues = result.value.returnValues,
                            rawReturnValue = result.value.rawReturnValue
                        )
                    )
                )
        }
    }
}
