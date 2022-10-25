package dev3.blockchainapiservice.model.params

import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.exception.InvalidRequestBodyException
import dev3.blockchainapiservice.util.ContractAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class DeployedContractIdentifierTest : TestBase() {

    companion object {
        data class RequestBody(
            override val deployedContractId: UUID?,
            override val deployedContractAlias: String?,
            override val contractAddress: String?
        ) : DeployedContractIdentifierRequestBody
    }

    @Test
    fun mustCorrectlyCreateDeployedContractIdentifierFromDeployedContractId() {
        val id = UUID.randomUUID()

        val result = suppose("deployed contract identifier will be created") {
            DeployedContractIdentifier(
                RequestBody(
                    deployedContractId = id,
                    deployedContractAlias = null,
                    contractAddress = null
                )
            )
        }

        verify("correct identifier is created") {
            assertThat(result).withMessage()
                .isEqualTo(DeployedContractIdIdentifier(id))
        }
    }

    @Test
    fun mustCorrectlyCreateDeployedContractIdentifierFromDeployedContractAlias() {
        val alias = "alias"

        val result = suppose("deployed contract identifier will be created") {
            DeployedContractIdentifier(
                RequestBody(
                    deployedContractId = null,
                    deployedContractAlias = alias,
                    contractAddress = null
                )
            )
        }

        verify("correct identifier is created") {
            assertThat(result).withMessage()
                .isEqualTo(DeployedContractAliasIdentifier(alias))
        }
    }

    @Test
    fun mustCorrectlyCreateDeployedContractIdentifierFromDeployedContractAddress() {
        val contractAddress = ContractAddress("a")

        val result = suppose("deployed contract identifier will be created") {
            DeployedContractIdentifier(
                RequestBody(
                    deployedContractId = null,
                    deployedContractAlias = null,
                    contractAddress = contractAddress.rawValue
                )
            )
        }

        verify("correct identifier is created") {
            assertThat(result).withMessage()
                .isEqualTo(DeployedContractAddressIdentifier(contractAddress))
        }
    }

    @Test
    fun mustThrowInvalidRequestBodyExceptionWhenAllContractIdentifiersArePresent() {
        verify("InvalidRequestBodyException is thrown") {
            assertThrows<InvalidRequestBodyException>(message) {
                DeployedContractIdentifier(
                    RequestBody(
                        deployedContractId = UUID.randomUUID(),
                        deployedContractAlias = "alias",
                        contractAddress = "a"
                    )
                )
            }
        }
    }

    @Test
    fun mustThrowInvalidRequestBodyExceptionWhenNoContractIdentifiersArePresent() {
        verify("InvalidRequestBodyException is thrown") {
            assertThrows<InvalidRequestBodyException>(message) {
                DeployedContractIdentifier(
                    RequestBody(
                        deployedContractId = null,
                        deployedContractAlias = null,
                        contractAddress = null
                    )
                )
            }
        }
    }
}
