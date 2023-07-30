package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.exception.ContractNotYetDeployedException
import dev3.blockchainapiservice.exception.ResourceNotFoundException
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.model.params.DeployedContractAddressIdentifier
import dev3.blockchainapiservice.model.params.DeployedContractAliasIdentifier
import dev3.blockchainapiservice.model.params.DeployedContractIdIdentifier
import dev3.blockchainapiservice.model.result.BlockchainTransactionInfo
import dev3.blockchainapiservice.model.result.ContractDeploymentRequest
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.repository.ContractDeploymentRequestRepository
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.BaseUrl
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.ContractBinaryData
import dev3.blockchainapiservice.util.ContractId
import dev3.blockchainapiservice.util.FunctionData
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress
import dev3.blockchainapiservice.util.ZeroAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import java.math.BigInteger
import java.util.UUID

class DeployedContractIdentifierResolverServiceTest : TestBase() {

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
        private val DEPLOYED_CONTRACT_ID = DeployedContractIdIdentifier(UUID.randomUUID())
        private val DEPLOYED_CONTRACT_ALIAS = DeployedContractAliasIdentifier("contract-alias")
        private val DEPLOYED_CONTRACT_ADDRESS = DeployedContractAddressIdentifier(ContractAddress("abc123"))
        private val DEPLOYER_ADDRESS = WalletAddress("a")
        private val TX_HASH = TransactionHash("deployed-contract-hash")
        private val DEPLOYED_CONTRACT = ContractDeploymentRequest(
            id = DEPLOYED_CONTRACT_ID.id,
            alias = DEPLOYED_CONTRACT_ALIAS.alias,
            name = "name",
            description = "description",
            contractId = ContractId("cid"),
            contractData = ContractBinaryData("00"),
            constructorParams = TestData.EMPTY_JSON_ARRAY,
            contractTags = emptyList(),
            contractImplements = emptyList(),
            initialEthAmount = Balance.ZERO,
            chainId = ChainId(1337L),
            redirectUrl = "redirect-url",
            projectId = PROJECT.id,
            createdAt = TestData.TIMESTAMP,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig.EMPTY,
            contractAddress = DEPLOYED_CONTRACT_ADDRESS.contractAddress,
            deployerAddress = DEPLOYER_ADDRESS,
            txHash = TX_HASH,
            imported = false,
            proxy = false,
            implementationContractAddress = null
        )
        private val BLOCKCHAIN_TRANSACTION_INFO = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = DEPLOYER_ADDRESS,
            to = ZeroAddress,
            deployedContractAddress = DEPLOYED_CONTRACT_ADDRESS.contractAddress,
            data = FunctionData(DEPLOYED_CONTRACT.contractData.value),
            value = DEPLOYED_CONTRACT.initialEthAmount,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true
        )
    }

    @Test
    fun mustCorrectlyResolveContractIdAndAddressForDeployedContractId() {
        val repository = mock<ContractDeploymentRequestRepository>()

        suppose("deployed contract is returned from database") {
            given(repository.getById(DEPLOYED_CONTRACT_ID.id))
                .willReturn(DEPLOYED_CONTRACT)
        }

        val service = DeployedContractIdentifierResolverServiceImpl(repository, mock())

        verify("contract contract ID and address are resolved") {
            assertThat(service.resolveContractIdAndAddress(DEPLOYED_CONTRACT_ID, PROJECT)).withMessage()
                .isEqualTo(Pair(DEPLOYED_CONTRACT.id, DEPLOYED_CONTRACT.contractAddress))
        }
    }

    @Test
    fun mustCorrectlyResolveContractIdAndAddressAndSetContractAddressForDeployedContractId() {
        val repository = mock<ContractDeploymentRequestRepository>()

        suppose("deployed contract is returned from database") {
            given(repository.getById(DEPLOYED_CONTRACT_ID.id))
                .willReturn(DEPLOYED_CONTRACT.copy(contractAddress = null))
        }

        val ethCommonService = mock<EthCommonService>()

        suppose("deployed contract address will be fetched from blockchain") {
            given(
                ethCommonService.fetchTransactionInfo(
                    txHash = TX_HASH,
                    chainId = DEPLOYED_CONTRACT.chainId,
                    customRpcUrl = PROJECT.customRpcUrl
                )
            ).willReturn(BLOCKCHAIN_TRANSACTION_INFO)
        }

        val service = DeployedContractIdentifierResolverServiceImpl(repository, ethCommonService)

        verify("contract contract ID and address are resolved") {
            assertThat(service.resolveContractIdAndAddress(DEPLOYED_CONTRACT_ID, PROJECT)).withMessage()
                .isEqualTo(Pair(DEPLOYED_CONTRACT.id, DEPLOYED_CONTRACT.contractAddress))
        }
    }

    @Test
    fun mustThrowContractNotYetDeployedExceptionWhenResolvingContractIdAndAddressForNonDeployedContractId() {
        val repository = mock<ContractDeploymentRequestRepository>()

        suppose("deployed contract is returned from database without contract address") {
            given(repository.getById(DEPLOYED_CONTRACT_ID.id))
                .willReturn(DEPLOYED_CONTRACT.copy(contractAddress = null))
        }

        val service = DeployedContractIdentifierResolverServiceImpl(repository, mock())

        verify("ContractNotYetDeployedException is thrown") {
            assertThrows<ContractNotYetDeployedException>(message) {
                service.resolveContractIdAndAddress(DEPLOYED_CONTRACT_ID, PROJECT)
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenResolvingContractIdAndAddressForNonExistentContractId() {
        val repository = mock<ContractDeploymentRequestRepository>()

        suppose("deployed contract is not found from database") {
            given(repository.getById(DEPLOYED_CONTRACT_ID.id))
                .willReturn(null)
        }

        val service = DeployedContractIdentifierResolverServiceImpl(repository, mock())

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.resolveContractIdAndAddress(DEPLOYED_CONTRACT_ID, PROJECT)
            }
        }
    }

    @Test
    fun mustCorrectlyResolveContractIdAndAddressForDeployedContractAlias() {
        val repository = mock<ContractDeploymentRequestRepository>()

        suppose("deployed contract is returned from database") {
            given(repository.getByAliasAndProjectId(DEPLOYED_CONTRACT_ALIAS.alias, PROJECT.id))
                .willReturn(DEPLOYED_CONTRACT)
        }

        val service = DeployedContractIdentifierResolverServiceImpl(repository, mock())

        verify("contract contract ID and address are resolved") {
            assertThat(service.resolveContractIdAndAddress(DEPLOYED_CONTRACT_ALIAS, PROJECT)).withMessage()
                .isEqualTo(Pair(DEPLOYED_CONTRACT.id, DEPLOYED_CONTRACT.contractAddress))
        }
    }

    @Test
    fun mustCorrectlyResolveContractIdAndAddressAndSetContractAddressForDeployedContractAlias() {
        val repository = mock<ContractDeploymentRequestRepository>()

        suppose("deployed contract is returned from database") {
            given(repository.getByAliasAndProjectId(DEPLOYED_CONTRACT_ALIAS.alias, PROJECT.id))
                .willReturn(DEPLOYED_CONTRACT.copy(contractAddress = null))
        }

        val ethCommonService = mock<EthCommonService>()

        suppose("deployed contract address will be fetched from blockchain") {
            given(
                ethCommonService.fetchTransactionInfo(
                    txHash = TX_HASH,
                    chainId = DEPLOYED_CONTRACT.chainId,
                    customRpcUrl = PROJECT.customRpcUrl
                )
            ).willReturn(BLOCKCHAIN_TRANSACTION_INFO)
        }

        val service = DeployedContractIdentifierResolverServiceImpl(repository, ethCommonService)

        verify("contract contract ID and address are resolved") {
            assertThat(service.resolveContractIdAndAddress(DEPLOYED_CONTRACT_ALIAS, PROJECT)).withMessage()
                .isEqualTo(Pair(DEPLOYED_CONTRACT.id, DEPLOYED_CONTRACT.contractAddress))
        }
    }

    @Test
    fun mustThrowContractNotYetDeployedExceptionWhenResolvingContractIdAndAddressForNonDeployedContractAlias() {
        val repository = mock<ContractDeploymentRequestRepository>()

        suppose("deployed contract is returned from database without contract address") {
            given(repository.getByAliasAndProjectId(DEPLOYED_CONTRACT_ALIAS.alias, PROJECT.id))
                .willReturn(DEPLOYED_CONTRACT.copy(contractAddress = null))
        }

        val service = DeployedContractIdentifierResolverServiceImpl(repository, mock())

        verify("ContractNotYetDeployedException is thrown") {
            assertThrows<ContractNotYetDeployedException>(message) {
                service.resolveContractIdAndAddress(DEPLOYED_CONTRACT_ALIAS, PROJECT)
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenResolvingContractIdAndAddressForNonExistentContractAlias() {
        val repository = mock<ContractDeploymentRequestRepository>()

        suppose("deployed contract is not found from database") {
            given(repository.getByAliasAndProjectId(DEPLOYED_CONTRACT_ALIAS.alias, PROJECT.id))
                .willReturn(null)
        }

        val service = DeployedContractIdentifierResolverServiceImpl(repository, mock())

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.resolveContractIdAndAddress(DEPLOYED_CONTRACT_ALIAS, PROJECT)
            }
        }
    }

    @Test
    fun mustCorrectlyResolveContractIdAndAddressForDeployedContractAddress() {
        val service = DeployedContractIdentifierResolverServiceImpl(mock(), mock())

        verify("contract contract ID and address are resolved") {
            assertThat(service.resolveContractIdAndAddress(DEPLOYED_CONTRACT_ADDRESS, PROJECT)).withMessage()
                .isEqualTo(Pair(null, DEPLOYED_CONTRACT.contractAddress))
        }
    }
}
