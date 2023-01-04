package dev3.blockchainapiservice.features.contract.deployment.model.params

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import dev3.blockchainapiservice.features.api.access.model.result.Project
import dev3.blockchainapiservice.features.contract.deployment.model.result.ContractDeploymentRequest
import dev3.blockchainapiservice.features.contract.importing.model.params.ImportContractParams
import dev3.blockchainapiservice.generated.jooq.id.ContractDeploymentRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.model.params.ParamsFactory
import dev3.blockchainapiservice.model.result.ContractBinaryInfo
import dev3.blockchainapiservice.model.result.ContractDeploymentTransactionInfo
import dev3.blockchainapiservice.model.result.FullContractDeploymentTransactionInfo
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.ContractBinaryData
import dev3.blockchainapiservice.util.ContractId
import dev3.blockchainapiservice.util.UtcDateTime
import dev3.blockchainapiservice.util.WalletAddress
import java.util.UUID

data class StoreContractDeploymentRequestParams(
    val id: ContractDeploymentRequestId,
    val alias: String,
    val contractId: ContractId,
    val contractData: ContractBinaryData,
    val constructorParams: JsonNode,
    val deployerAddress: WalletAddress?,
    val initialEthAmount: Balance,
    val chainId: ChainId,
    val redirectUrl: String,
    val projectId: ProjectId,
    val createdAt: UtcDateTime,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig,
    val imported: Boolean,
    val proxy: Boolean,
    val implementationContractAddress: ContractAddress?
) {
    companion object : ParamsFactory<PreStoreContractDeploymentRequestParams, StoreContractDeploymentRequestParams> {
        private const val PATH = "/request-deploy/\${id}/action"
        private val objectMapper = ObjectMapper()

        override fun fromCreateParams(
            id: UUID,
            params: PreStoreContractDeploymentRequestParams,
            project: Project,
            createdAt: UtcDateTime
        ) = StoreContractDeploymentRequestParams(
            id = ContractDeploymentRequestId(id),
            alias = params.createParams.alias,
            contractId = params.createParams.contractId,
            contractData = ContractBinaryData(
                params.contractDecorator.binary.value + params.encodedConstructor.withoutPrefix
            ),
            constructorParams = objectMapper.createArrayNode().addAll(
                params.createParams.constructorParams.mapNotNull { it.rawJson }
            ),
            deployerAddress = params.createParams.deployerAddress,
            initialEthAmount = params.createParams.initialEthAmount,
            chainId = project.chainId,
            redirectUrl = project.createRedirectUrl(params.createParams.redirectUrl, id, PATH),
            projectId = project.id,
            createdAt = createdAt,
            arbitraryData = params.createParams.arbitraryData,
            screenConfig = params.createParams.screenConfig,
            imported = false,
            proxy = false,
            implementationContractAddress = null
        )

        @Suppress("LongParameterList")
        fun fromImportedContract(
            id: ContractDeploymentRequestId,
            params: ImportContractParams,
            contractId: ContractId,
            contractDeploymentTransactionInfo: ContractDeploymentTransactionInfo,
            constructorParams: JsonNode,
            project: Project,
            createdAt: UtcDateTime,
            proxy: Boolean,
            implementationContractAddress: ContractAddress?
        ) = when (contractDeploymentTransactionInfo) {
            is FullContractDeploymentTransactionInfo ->
                StoreContractDeploymentRequestParams(
                    id = id,
                    alias = params.alias,
                    contractId = contractId,
                    contractData = ContractBinaryData(contractDeploymentTransactionInfo.data.value),
                    constructorParams = constructorParams,
                    deployerAddress = contractDeploymentTransactionInfo.from,
                    initialEthAmount = contractDeploymentTransactionInfo.value,
                    chainId = project.chainId,
                    redirectUrl = project.createRedirectUrl(params.redirectUrl, id.value, PATH),
                    projectId = project.id,
                    createdAt = createdAt,
                    arbitraryData = params.arbitraryData,
                    screenConfig = params.screenConfig,
                    imported = true,
                    proxy = proxy,
                    implementationContractAddress = implementationContractAddress
                )

            is ContractBinaryInfo ->
                StoreContractDeploymentRequestParams(
                    id = id,
                    alias = params.alias,
                    contractId = contractId,
                    contractData = ContractBinaryData(contractDeploymentTransactionInfo.binary.value),
                    constructorParams = constructorParams,
                    deployerAddress = null,
                    initialEthAmount = Balance.ZERO,
                    chainId = project.chainId,
                    redirectUrl = project.createRedirectUrl(params.redirectUrl, id.value, PATH),
                    projectId = project.id,
                    createdAt = createdAt,
                    arbitraryData = params.arbitraryData,
                    screenConfig = params.screenConfig,
                    imported = true,
                    proxy = proxy,
                    implementationContractAddress = implementationContractAddress
                )
        }

        @Suppress("LongParameterList")
        fun fromContractDeploymentRequest(
            id: ContractDeploymentRequestId,
            importContractParams: ImportContractParams,
            contractDeploymentRequest: ContractDeploymentRequest,
            project: Project,
            createdAt: UtcDateTime,
            imported: Boolean
        ) = StoreContractDeploymentRequestParams(
            id = id,
            alias = importContractParams.alias,
            contractId = contractDeploymentRequest.contractId,
            contractData = contractDeploymentRequest.contractData,
            constructorParams = contractDeploymentRequest.constructorParams,
            deployerAddress = contractDeploymentRequest.deployerAddress,
            initialEthAmount = contractDeploymentRequest.initialEthAmount,
            chainId = contractDeploymentRequest.chainId,
            redirectUrl = project.createRedirectUrl(importContractParams.redirectUrl, id.value, PATH),
            projectId = project.id,
            createdAt = createdAt,
            arbitraryData = importContractParams.arbitraryData,
            screenConfig = importContractParams.screenConfig,
            imported = imported,
            proxy = contractDeploymentRequest.proxy,
            implementationContractAddress = contractDeploymentRequest.implementationContractAddress
        )
    }
}
