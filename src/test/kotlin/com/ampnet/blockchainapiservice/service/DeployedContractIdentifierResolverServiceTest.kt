package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.exception.ContractNotYetDeployedException
import com.ampnet.blockchainapiservice.exception.ResourceNotFoundException
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.params.DeployedContractAddressIdentifier
import com.ampnet.blockchainapiservice.model.params.DeployedContractAliasIdentifier
import com.ampnet.blockchainapiservice.model.params.DeployedContractIdIdentifier
import com.ampnet.blockchainapiservice.model.result.ContractDeploymentRequest
import com.ampnet.blockchainapiservice.repository.ContractDeploymentRequestRepository
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.ContractBinaryData
import com.ampnet.blockchainapiservice.util.ContractId
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import java.math.BigInteger
import java.util.UUID

class DeployedContractIdentifierResolverServiceTest : TestBase() {

    companion object {
        private val PROJECT_ID = UUID.randomUUID()
        private val DEPLOYED_CONTRACT_ID = DeployedContractIdIdentifier(UUID.randomUUID())
        private val DEPLOYED_CONTRACT_ALIAS = DeployedContractAliasIdentifier("contract-alias")
        private val DEPLOYED_CONTRACT_ADDRESS = DeployedContractAddressIdentifier(ContractAddress("abc123"))
        private val DEPLOYED_CONTRACT = ContractDeploymentRequest(
            id = DEPLOYED_CONTRACT_ID.id,
            alias = DEPLOYED_CONTRACT_ALIAS.alias,
            contractId = ContractId("cid"),
            contractData = ContractBinaryData("00"),
            constructorParams = TestData.EMPTY_JSON_ARRAY,
            contractTags = emptyList(),
            contractImplements = emptyList(),
            initialEthAmount = Balance(BigInteger.ZERO),
            chainId = ChainId(1337L),
            redirectUrl = "redirect-url",
            projectId = PROJECT_ID,
            createdAt = TestData.TIMESTAMP,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig.EMPTY,
            contractAddress = DEPLOYED_CONTRACT_ADDRESS.contractAddress,
            deployerAddress = WalletAddress("a"),
            txHash = TransactionHash("deployed-contract-hash")
        )
    }

    @Test
    fun mustCorrectlyResolveContractIdAndAddressForDeployedContractId() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("deployed contract is returned from database") {
            given(contractDeploymentRequestRepository.getById(DEPLOYED_CONTRACT_ID.id))
                .willReturn(DEPLOYED_CONTRACT)
        }

        val service = DeployedContractIdentifierResolverServiceImpl(contractDeploymentRequestRepository)

        verify("contract contract ID and address are resolved") {
            assertThat(service.resolveContractIdAndAddress(DEPLOYED_CONTRACT_ID, PROJECT_ID)).withMessage()
                .isEqualTo(Pair(DEPLOYED_CONTRACT.id, DEPLOYED_CONTRACT.contractAddress))
        }
    }

    @Test
    fun mustThrowContractNotYetDeployedExceptionWhenResolvingContractIdAndAddressForNonDeployedContractId() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("deployed contract is returned from database without contract address") {
            given(contractDeploymentRequestRepository.getById(DEPLOYED_CONTRACT_ID.id))
                .willReturn(DEPLOYED_CONTRACT.copy(contractAddress = null))
        }

        val service = DeployedContractIdentifierResolverServiceImpl(contractDeploymentRequestRepository)

        verify("ContractNotYetDeployedException is thrown") {
            assertThrows<ContractNotYetDeployedException>(message) {
                service.resolveContractIdAndAddress(DEPLOYED_CONTRACT_ID, PROJECT_ID)
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenResolvingContractIdAndAddressForNonExistentContractId() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("deployed contract is not found from database") {
            given(contractDeploymentRequestRepository.getById(DEPLOYED_CONTRACT_ID.id))
                .willReturn(null)
        }

        val service = DeployedContractIdentifierResolverServiceImpl(contractDeploymentRequestRepository)

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.resolveContractIdAndAddress(DEPLOYED_CONTRACT_ID, PROJECT_ID)
            }
        }
    }

    @Test
    fun mustCorrectlyResolveContractIdAndAddressForDeployedContractAlias() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("deployed contract is returned from database") {
            given(contractDeploymentRequestRepository.getByAliasAndProjectId(DEPLOYED_CONTRACT_ALIAS.alias, PROJECT_ID))
                .willReturn(DEPLOYED_CONTRACT)
        }

        val service = DeployedContractIdentifierResolverServiceImpl(contractDeploymentRequestRepository)

        verify("contract contract ID and address are resolved") {
            assertThat(service.resolveContractIdAndAddress(DEPLOYED_CONTRACT_ALIAS, PROJECT_ID)).withMessage()
                .isEqualTo(Pair(DEPLOYED_CONTRACT.id, DEPLOYED_CONTRACT.contractAddress))
        }
    }

    @Test
    fun mustThrowContractNotYetDeployedExceptionWhenResolvingContractIdAndAddressForNonDeployedContractAlias() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("deployed contract is returned from database without contract address") {
            given(contractDeploymentRequestRepository.getByAliasAndProjectId(DEPLOYED_CONTRACT_ALIAS.alias, PROJECT_ID))
                .willReturn(DEPLOYED_CONTRACT.copy(contractAddress = null))
        }

        val service = DeployedContractIdentifierResolverServiceImpl(contractDeploymentRequestRepository)

        verify("ContractNotYetDeployedException is thrown") {
            assertThrows<ContractNotYetDeployedException>(message) {
                service.resolveContractIdAndAddress(DEPLOYED_CONTRACT_ALIAS, PROJECT_ID)
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenResolvingContractIdAndAddressForNonExistentContractAlias() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("deployed contract is not found from database") {
            given(contractDeploymentRequestRepository.getByAliasAndProjectId(DEPLOYED_CONTRACT_ALIAS.alias, PROJECT_ID))
                .willReturn(null)
        }

        val service = DeployedContractIdentifierResolverServiceImpl(contractDeploymentRequestRepository)

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.resolveContractIdAndAddress(DEPLOYED_CONTRACT_ALIAS, PROJECT_ID)
            }
        }
    }

    @Test
    fun mustCorrectlyResolveContractIdAndAddressForDeployedContractAddress() {
        val service = DeployedContractIdentifierResolverServiceImpl(mock())

        verify("contract contract ID and address are resolved") {
            assertThat(service.resolveContractIdAndAddress(DEPLOYED_CONTRACT_ADDRESS, PROJECT_ID)).withMessage()
                .isEqualTo(Pair(null, DEPLOYED_CONTRACT.contractAddress))
        }
    }
}
