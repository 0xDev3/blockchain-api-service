package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.blockchain.BlockchainService
import dev3.blockchainapiservice.blockchain.properties.ChainSpec
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.model.params.CreateReadonlyFunctionCallParams
import dev3.blockchainapiservice.model.params.DeployedContractIdIdentifier
import dev3.blockchainapiservice.model.params.ExecuteReadonlyFunctionCallParams
import dev3.blockchainapiservice.model.params.OutputParameter
import dev3.blockchainapiservice.model.result.ContractDeploymentRequest
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.model.result.ReadonlyFunctionCallResult
import dev3.blockchainapiservice.repository.ContractDeploymentRequestRepository
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.BaseUrl
import dev3.blockchainapiservice.util.BlockName
import dev3.blockchainapiservice.util.BlockNumber
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.ContractBinaryData
import dev3.blockchainapiservice.util.ContractId
import dev3.blockchainapiservice.util.FunctionArgument
import dev3.blockchainapiservice.util.FunctionData
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.UintType
import dev3.blockchainapiservice.util.WalletAddress
import dev3.blockchainapiservice.util.WithDeployedContractIdAndAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.web3j.abi.datatypes.generated.Uint256
import java.math.BigInteger
import java.util.UUID

class ContractReadonlyFunctionCallServiceTest : TestBase() {

    companion object {
        private val PROJECT = Project(
            id = UUID.randomUUID(),
            ownerId = UUID.randomUUID(),
            issuerContractAddress = ContractAddress("a"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = ChainId(1337L),
            customRpcUrl = "custom-rpc-url",
            createdAt = TestData.TIMESTAMP
        )
        private val DEPLOYED_CONTRACT_ID = UUID.randomUUID()
        private val CONTRACT_ADDRESS = ContractAddress("abc123")
        private val CALLER_ADDRESS = WalletAddress("a")
        private val CREATE_PARAMS = CreateReadonlyFunctionCallParams(
            identifier = DeployedContractIdIdentifier(DEPLOYED_CONTRACT_ID),
            blockNumber = null,
            functionName = "example",
            functionParams = listOf(FunctionArgument(Uint256(BigInteger.TEN))),
            outputParams = listOf(OutputParameter(UintType)),
            callerAddress = CALLER_ADDRESS
        )
        private val ENCODED_FUNCTION_DATA = FunctionData("0x1234")
        private val CHAIN_SPEC = ChainSpec(PROJECT.chainId, PROJECT.customRpcUrl)
        private val DEPLOYED_CONTRACT = ContractDeploymentRequest(
            id = DEPLOYED_CONTRACT_ID,
            alias = "contract-alias",
            name = "name",
            description = "description",
            contractId = ContractId("cid"),
            contractData = ContractBinaryData("00"),
            constructorParams = TestData.EMPTY_JSON_ARRAY,
            contractTags = emptyList(),
            contractImplements = emptyList(),
            initialEthAmount = Balance.ZERO,
            chainId = CHAIN_SPEC.chainId,
            redirectUrl = "redirect-url",
            projectId = PROJECT.id,
            createdAt = TestData.TIMESTAMP,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig.EMPTY,
            contractAddress = CONTRACT_ADDRESS,
            deployerAddress = CALLER_ADDRESS,
            txHash = TransactionHash("deployed-contract-hash"),
            imported = false
        )
    }

    @Test
    fun mustCorrectlyCallReadonlyFunction() {
        val functionEncoderService = mock<FunctionEncoderService>()
        val createParams = CREATE_PARAMS

        suppose("function will be encoded") {
            given(
                functionEncoderService.encode(
                    functionName = createParams.functionName,
                    arguments = createParams.functionParams
                )
            )
                .willReturn(ENCODED_FUNCTION_DATA)
        }

        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("deployed contract is returned from database") {
            given(contractDeploymentRequestRepository.getById(DEPLOYED_CONTRACT_ID))
                .willReturn(DEPLOYED_CONTRACT)
        }

        val blockchainService = mock<BlockchainService>()
        val readonlyFunctionCallResult = ReadonlyFunctionCallResult(
            blockNumber = BlockNumber(BigInteger.ONE),
            timestamp = TestData.TIMESTAMP,
            returnValues = listOf(BigInteger.TWO),
            rawReturnValue = "0x0"
        )

        suppose("blockchain service will return some value for readonly function call") {
            given(
                blockchainService.callReadonlyFunction(
                    chainSpec = CHAIN_SPEC,
                    params = ExecuteReadonlyFunctionCallParams(
                        contractAddress = CONTRACT_ADDRESS,
                        callerAddress = CALLER_ADDRESS,
                        functionName = createParams.functionName,
                        functionData = ENCODED_FUNCTION_DATA,
                        outputParams = listOf(OutputParameter(UintType))
                    ),
                    blockParameter = BlockName.LATEST
                )
            ).willReturn(readonlyFunctionCallResult)
        }

        val service = ContractReadonlyFunctionCallServiceImpl(
            functionEncoderService = functionEncoderService,
            deployedContractIdentifierResolverService = service(contractDeploymentRequestRepository),
            blockchainService = blockchainService
        )

        verify("contract readonly function call is correctly executed") {
            assertThat(
                service.callReadonlyContractFunction(createParams, PROJECT)
            ).withMessage()
                .isEqualTo(
                    WithDeployedContractIdAndAddress(
                        value = readonlyFunctionCallResult,
                        deployedContractId = DEPLOYED_CONTRACT_ID,
                        contractAddress = CONTRACT_ADDRESS
                    )
                )
        }
    }

    private fun service(repository: ContractDeploymentRequestRepository) =
        DeployedContractIdentifierResolverServiceImpl(repository, mock())
}
