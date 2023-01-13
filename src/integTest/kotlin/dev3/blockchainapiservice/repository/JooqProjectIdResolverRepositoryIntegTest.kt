package dev3.blockchainapiservice.repository

import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.config.interceptors.annotation.IdType
import dev3.blockchainapiservice.features.api.usage.repository.JooqUserIdResolverRepository
import dev3.blockchainapiservice.features.payout.util.AssetSnapshotStatus
import dev3.blockchainapiservice.generated.jooq.enums.UserIdentifierType
import dev3.blockchainapiservice.generated.jooq.id.AssetBalanceRequestId
import dev3.blockchainapiservice.generated.jooq.id.AssetMultiSendRequestId
import dev3.blockchainapiservice.generated.jooq.id.AssetSendRequestId
import dev3.blockchainapiservice.generated.jooq.id.AssetSnapshotId
import dev3.blockchainapiservice.generated.jooq.id.AuthorizationRequestId
import dev3.blockchainapiservice.generated.jooq.id.ContractArbitraryCallRequestId
import dev3.blockchainapiservice.generated.jooq.id.ContractDeploymentRequestId
import dev3.blockchainapiservice.generated.jooq.id.ContractFunctionCallRequestId
import dev3.blockchainapiservice.generated.jooq.id.ContractMetadataId
import dev3.blockchainapiservice.generated.jooq.id.Erc20LockRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.generated.jooq.id.UserId
import dev3.blockchainapiservice.generated.jooq.tables.records.AssetBalanceRequestRecord
import dev3.blockchainapiservice.generated.jooq.tables.records.AssetMultiSendRequestRecord
import dev3.blockchainapiservice.generated.jooq.tables.records.AssetSendRequestRecord
import dev3.blockchainapiservice.generated.jooq.tables.records.AssetSnapshotRecord
import dev3.blockchainapiservice.generated.jooq.tables.records.AuthorizationRequestRecord
import dev3.blockchainapiservice.generated.jooq.tables.records.ContractArbitraryCallRequestRecord
import dev3.blockchainapiservice.generated.jooq.tables.records.ContractDeploymentRequestRecord
import dev3.blockchainapiservice.generated.jooq.tables.records.ContractFunctionCallRequestRecord
import dev3.blockchainapiservice.generated.jooq.tables.records.ContractMetadataRecord
import dev3.blockchainapiservice.generated.jooq.tables.records.Erc20LockRequestRecord
import dev3.blockchainapiservice.generated.jooq.tables.records.ProjectRecord
import dev3.blockchainapiservice.generated.jooq.tables.records.UserIdentifierRecord
import dev3.blockchainapiservice.testcontainers.SharedTestContainers
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.BaseUrl
import dev3.blockchainapiservice.util.BlockNumber
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.Constants
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.ContractBinaryData
import dev3.blockchainapiservice.util.ContractId
import dev3.blockchainapiservice.util.DurationSeconds
import dev3.blockchainapiservice.util.FunctionData
import dev3.blockchainapiservice.util.SignedMessage
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import java.math.BigInteger
import java.util.UUID

@JooqTest
@Import(JooqUserIdResolverRepository::class)
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JooqProjectIdResolverRepositoryIntegTest : TestBase() {

    companion object {
        private val CHAIN_ID = ChainId(1337L)
        private const val REDIRECT_URL = "redirect-url"
        private val TOKEN_ADDRESS = ContractAddress("a")
        private val BLOCK_NUMBER = BlockNumber(BigInteger.valueOf(123L))
        private val REQUESTED_WALLET_ADDRESS = WalletAddress("b")
        private val ARBITRARY_DATA = TestData.EMPTY_JSON_OBJECT
        private const val BALANCE_SCREEN_BEFORE_ACTION_MESSAGE = "balance-screen-before-action-message"
        private const val BALANCE_SCREEN_AFTER_ACTION_MESSAGE = "balance-screen-after-action-message"
        private val ACTUAL_WALLET_ADDRESS = WalletAddress("c")
        private val SIGNED_MESSAGE = SignedMessage("signed-message")
        private val PROJECT_ID = ProjectId(UUID.randomUUID())
        private val USER_ID = UserId(UUID.randomUUID())
        private val DISPERSE_CONTRACT_ADDRESS = ContractAddress("b")
        private val ASSET_AMOUNTS = listOf(Balance(BigInteger.valueOf(123456L)), Balance(BigInteger.valueOf(789L)))
        private val ASSET_RECIPIENT_ADDRESSES = listOf(WalletAddress("c"), WalletAddress("d"))
        private val ITEM_NAMES = listOf("test", null)
        private val ASSET_SENDER_ADDRESS = WalletAddress("e")
        private const val APPROVE_SCREEN_BEFORE_ACTION_MESSAGE = "approve-screen-before-action-message"
        private const val APPROVE_SCREEN_AFTER_ACTION_MESSAGE = "approve-screen-after-action-message"
        private const val DISPERSE_SCREEN_BEFORE_ACTION_MESSAGE = "disperse-screen-before-action-message"
        private const val DISPERSE_SCREEN_AFTER_ACTION_MESSAGE = "disperse-screen-after-action-message"
        private val APPROVE_TX_HASH = TransactionHash("approve-tx-hash")
        private val DISPERSE_TX_HASH = TransactionHash("disperse-tx-hash")
        private val ASSET_AMOUNT = Balance(BigInteger.valueOf(123456L))
        private val ASSET_RECIPIENT_ADDRESS = WalletAddress("c")
        private const val SEND_SCREEN_BEFORE_ACTION_MESSAGE = "send-screen-before-action-message"
        private const val SEND_SCREEN_AFTER_ACTION_MESSAGE = "send-screen-after-action-message"
        private val TX_HASH = TransactionHash("tx-hash")
        private const val MESSAGE_TO_SIGN_OVERRIDE = "message-to-sign-override"
        private const val STORE_INDEFINITELY = true
        private const val SCREEN_BEFORE_ACTION_MESSAGE = "before-action-message"
        private const val SCREEN_AFTER_ACTION_MESSAGE = "after-action-message"
        private const val ALIAS = "alias"
        private const val NAME = "name"
        private const val DESCRIPTION = "description"
        private val CONTRACT_ID = ContractId("contract-id")
        private val CONTRACT_DATA = ContractBinaryData("00")
        private val INITIAL_ETH_AMOUNT = Balance(BigInteger("10000"))
        private const val DEPLOY_SCREEN_BEFORE_ACTION_MESSAGE = "deploy-screen-before-action-message"
        private const val DEPLOY_SCREEN_AFTER_ACTION_MESSAGE = "deploy-screen-after-action-message"
        private val CONTRACT_ADDRESS = ContractAddress("1337")
        private val DEPLOYER_ADDRESS = WalletAddress("123")
        private val FUNCTION_DATA = FunctionData("00")
        private const val FUNCTION_NAME = "balanceOf"
        private val ETH_AMOUNT = Balance(BigInteger("10000"))
        private val CALLER_ADDRESS = WalletAddress("123")
        private val TOKEN_AMOUNT = Balance(BigInteger.valueOf(123456L))
        private val LOCK_DURATION = DurationSeconds(BigInteger.valueOf(123L))
        private val LOCK_CONTRACT_ADDRESS = ContractAddress("b")
        private val TOKEN_SENDER_ADDRESS = WalletAddress("c")
        private const val LOCK_SCREEN_BEFORE_ACTION_MESSAGE = "lock-screen-before-action-message"
        private const val LOCK_SCREEN_AFTER_ACTION_MESSAGE = "lock-screen-after-action-message"
    }

    @Suppress("unused")
    private val postgresContainer = SharedTestContainers.postgresContainer

    @Autowired
    private lateinit var repository: JooqUserIdResolverRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        postgresContainer.cleanAllDatabaseTables(dslContext)

        dslContext.executeInsert(
            UserIdentifierRecord(
                id = USER_ID,
                userIdentifier = "user-identifier",
                identifierType = UserIdentifierType.ETH_WALLET_ADDRESS,
                stripeClientId = null
            )
        )

        dslContext.executeInsert(
            ProjectRecord(
                id = PROJECT_ID,
                ownerId = USER_ID,
                issuerContractAddress = ContractAddress("0"),
                baseRedirectUrl = BaseUrl("base-redirect-url"),
                chainId = ChainId(1337L),
                customRpcUrl = "custom-rpc-url",
                createdAt = TestData.TIMESTAMP
            )
        )
    }

    @Test
    fun mustCorrectlyReturnUserIdForProject() {
        verify("correct userId is returned") {
            expectThat(repository.getByProjectId(PROJECT_ID))
                .isEqualTo(USER_ID)
            expectThat(repository.getUserId(IdType.PROJECT_ID, PROJECT_ID.value))
                .isEqualTo(USER_ID)
        }
    }

    @Test
    fun mustCorrectlyReturnUserIdForAssetBalanceRequest() {
        val id = AssetBalanceRequestId(UUID.randomUUID())

        suppose("some asset balance request exists in database") {
            dslContext.executeInsert(
                AssetBalanceRequestRecord(
                    id = id,
                    projectId = PROJECT_ID,
                    chainId = CHAIN_ID,
                    redirectUrl = REDIRECT_URL,
                    tokenAddress = TOKEN_ADDRESS,
                    blockNumber = BLOCK_NUMBER,
                    requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
                    arbitraryData = ARBITRARY_DATA,
                    screenBeforeActionMessage = BALANCE_SCREEN_BEFORE_ACTION_MESSAGE,
                    screenAfterActionMessage = BALANCE_SCREEN_AFTER_ACTION_MESSAGE,
                    actualWalletAddress = ACTUAL_WALLET_ADDRESS,
                    signedMessage = SIGNED_MESSAGE,
                    createdAt = TestData.TIMESTAMP
                )
            )
        }

        verify("correct userId is returned") {
            expectThat(repository.getUserId(IdType.ASSET_BALANCE_REQUEST_ID, id.value))
                .isEqualTo(USER_ID)
        }
    }

    @Test
    fun mustCorrectlyReturnUserIdForAssetMultiSendRequest() {
        val id = AssetMultiSendRequestId(UUID.randomUUID())

        suppose("some asset multi-send request exists in database") {
            @Suppress("UNCHECKED_CAST")
            dslContext.executeInsert(
                AssetMultiSendRequestRecord(
                    id = id,
                    chainId = CHAIN_ID,
                    redirectUrl = REDIRECT_URL,
                    tokenAddress = TOKEN_ADDRESS,
                    disperseContractAddress = DISPERSE_CONTRACT_ADDRESS,
                    assetAmounts = ASSET_AMOUNTS.map { it.rawValue.toBigDecimal() }.toTypedArray(),
                    assetRecipientAddresses = ASSET_RECIPIENT_ADDRESSES.map { it.rawValue }.toTypedArray(),
                    assetSenderAddress = ASSET_SENDER_ADDRESS,
                    itemNames = ITEM_NAMES.toTypedArray() as Array<String>,
                    arbitraryData = ARBITRARY_DATA,
                    approveTxHash = APPROVE_TX_HASH,
                    disperseTxHash = DISPERSE_TX_HASH,
                    approveScreenBeforeActionMessage = APPROVE_SCREEN_BEFORE_ACTION_MESSAGE,
                    approveScreenAfterActionMessage = APPROVE_SCREEN_AFTER_ACTION_MESSAGE,
                    disperseScreenBeforeActionMessage = DISPERSE_SCREEN_BEFORE_ACTION_MESSAGE,
                    disperseScreenAfterActionMessage = DISPERSE_SCREEN_AFTER_ACTION_MESSAGE,
                    projectId = PROJECT_ID,
                    createdAt = TestData.TIMESTAMP
                )
            )
        }

        verify("correct userId is returned") {
            expectThat(repository.getUserId(IdType.ASSET_MULTI_SEND_REQUEST_ID, id.value))
                .isEqualTo(USER_ID)
        }
    }

    @Test
    fun mustCorrectlyReturnUserIdForAssetSendRequest() {
        val id = AssetSendRequestId(UUID.randomUUID())

        suppose("some asset send request exists in database") {
            dslContext.executeInsert(
                AssetSendRequestRecord(
                    id = id,
                    projectId = PROJECT_ID,
                    chainId = CHAIN_ID,
                    redirectUrl = REDIRECT_URL,
                    tokenAddress = TOKEN_ADDRESS,
                    assetAmount = ASSET_AMOUNT,
                    assetSenderAddress = ASSET_SENDER_ADDRESS,
                    assetRecipientAddress = ASSET_RECIPIENT_ADDRESS,
                    arbitraryData = ARBITRARY_DATA,
                    screenBeforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                    screenAfterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE,
                    txHash = TX_HASH,
                    createdAt = TestData.TIMESTAMP
                )
            )
        }

        verify("correct userId is returned") {
            expectThat(repository.getUserId(IdType.ASSET_SEND_REQUEST_ID, id.value))
                .isEqualTo(USER_ID)
        }
    }

    @Test
    fun mustCorrectlyReturnUserIdForAuthorizationRequest() {
        val id = AuthorizationRequestId(UUID.randomUUID())

        suppose("some authorization request exists in database") {
            dslContext.executeInsert(
                AuthorizationRequestRecord(
                    id = id,
                    projectId = PROJECT_ID,
                    redirectUrl = REDIRECT_URL,
                    messageToSignOverride = MESSAGE_TO_SIGN_OVERRIDE,
                    storeIndefinitely = STORE_INDEFINITELY,
                    requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
                    arbitraryData = ARBITRARY_DATA,
                    screenBeforeActionMessage = SCREEN_BEFORE_ACTION_MESSAGE,
                    screenAfterActionMessage = SCREEN_AFTER_ACTION_MESSAGE,
                    actualWalletAddress = ACTUAL_WALLET_ADDRESS,
                    signedMessage = SIGNED_MESSAGE,
                    createdAt = TestData.TIMESTAMP
                )
            )
        }

        verify("correct userId is returned") {
            expectThat(repository.getUserId(IdType.AUTHORIZATION_REQUEST_ID, id.value))
                .isEqualTo(USER_ID)
        }
    }

    @Test
    fun mustCorrectlyReturnUserIdForContractDeploymentSendRequest() {
        val id = ContractDeploymentRequestId(UUID.randomUUID())

        suppose("some contract deployment request exists in database") {
            val metadataId = ContractMetadataId(UUID.randomUUID())

            dslContext.executeInsert(
                ContractMetadataRecord(
                    id = metadataId,
                    name = NAME,
                    description = DESCRIPTION,
                    contractId = CONTRACT_ID,
                    contractTags = emptyArray(),
                    contractImplements = emptyArray(),
                    projectId = Constants.NIL_PROJECT_ID
                )
            )

            dslContext.executeInsert(
                ContractDeploymentRequestRecord(
                    id = id,
                    alias = ALIAS,
                    contractMetadataId = metadataId,
                    contractData = CONTRACT_DATA,
                    constructorParams = TestData.EMPTY_JSON_ARRAY,
                    initialEthAmount = INITIAL_ETH_AMOUNT,
                    chainId = CHAIN_ID,
                    redirectUrl = REDIRECT_URL,
                    projectId = PROJECT_ID,
                    createdAt = TestData.TIMESTAMP,
                    arbitraryData = ARBITRARY_DATA,
                    screenBeforeActionMessage = DEPLOY_SCREEN_BEFORE_ACTION_MESSAGE,
                    screenAfterActionMessage = DEPLOY_SCREEN_AFTER_ACTION_MESSAGE,
                    contractAddress = CONTRACT_ADDRESS,
                    deployerAddress = DEPLOYER_ADDRESS,
                    txHash = TX_HASH,
                    imported = false,
                    deleted = false,
                    proxy = false,
                    implementationContractAddress = null
                )
            )
        }

        verify("correct userId is returned") {
            expectThat(repository.getUserId(IdType.CONTRACT_DEPLOYMENT_REQUEST_ID, id.value))
                .isEqualTo(USER_ID)
        }
    }

    @Test
    fun mustCorrectlyReturnUserIdForContractFunctionCallRequest() {
        val id = ContractFunctionCallRequestId(UUID.randomUUID())

        suppose("some contract function call request exists in database") {
            dslContext.executeInsert(
                ContractFunctionCallRequestRecord(
                    id = id,
                    deployedContractId = null,
                    contractAddress = CONTRACT_ADDRESS,
                    functionName = FUNCTION_NAME,
                    functionParams = TestData.EMPTY_JSON_ARRAY,
                    ethAmount = ETH_AMOUNT,
                    chainId = CHAIN_ID,
                    redirectUrl = REDIRECT_URL,
                    projectId = PROJECT_ID,
                    createdAt = TestData.TIMESTAMP,
                    arbitraryData = ARBITRARY_DATA,
                    screenBeforeActionMessage = SCREEN_BEFORE_ACTION_MESSAGE,
                    screenAfterActionMessage = SCREEN_AFTER_ACTION_MESSAGE,
                    callerAddress = CALLER_ADDRESS,
                    txHash = TX_HASH
                )
            )
        }

        verify("correct userId is returned") {
            expectThat(repository.getUserId(IdType.FUNCTION_CALL_REQUEST_ID, id.value))
                .isEqualTo(USER_ID)
        }
    }

    @Test
    fun mustCorrectlyReturnUserIdForContractArbitraryCallRequest() {
        val id = ContractArbitraryCallRequestId(UUID.randomUUID())

        suppose("some contract arbitrary call request exists in database") {
            dslContext.executeInsert(
                ContractArbitraryCallRequestRecord(
                    id = id,
                    deployedContractId = null,
                    contractAddress = CONTRACT_ADDRESS,
                    functionData = FUNCTION_DATA,
                    functionName = FUNCTION_NAME,
                    functionParams = TestData.EMPTY_JSON_ARRAY,
                    ethAmount = ETH_AMOUNT,
                    chainId = CHAIN_ID,
                    redirectUrl = REDIRECT_URL,
                    projectId = PROJECT_ID,
                    createdAt = TestData.TIMESTAMP,
                    arbitraryData = ARBITRARY_DATA,
                    screenBeforeActionMessage = SCREEN_BEFORE_ACTION_MESSAGE,
                    screenAfterActionMessage = SCREEN_AFTER_ACTION_MESSAGE,
                    callerAddress = CALLER_ADDRESS,
                    txHash = TX_HASH
                )
            )
        }

        verify("correct userId is returned") {
            expectThat(repository.getUserId(IdType.ARBITRARY_CALL_REQUEST_ID, id.value))
                .isEqualTo(USER_ID)
        }
    }

    @Test
    fun mustCorrectlyReturnUserIdForErc20LockRequest() {
        val id = Erc20LockRequestId(UUID.randomUUID())

        suppose("some ERC20 lock request exists in database") {
            dslContext.executeInsert(
                Erc20LockRequestRecord(
                    id = id,
                    projectId = PROJECT_ID,
                    chainId = CHAIN_ID,
                    redirectUrl = REDIRECT_URL,
                    tokenAddress = TOKEN_ADDRESS,
                    tokenAmount = TOKEN_AMOUNT,
                    lockDurationSeconds = LOCK_DURATION,
                    lockContractAddress = LOCK_CONTRACT_ADDRESS,
                    tokenSenderAddress = TOKEN_SENDER_ADDRESS,
                    arbitraryData = ARBITRARY_DATA,
                    screenBeforeActionMessage = LOCK_SCREEN_BEFORE_ACTION_MESSAGE,
                    screenAfterActionMessage = LOCK_SCREEN_AFTER_ACTION_MESSAGE,
                    txHash = TX_HASH,
                    createdAt = TestData.TIMESTAMP
                )
            )
        }

        verify("correct userId is returned") {
            expectThat(repository.getUserId(IdType.ERC20_LOCK_REQUEST_ID, id.value))
                .isEqualTo(USER_ID)
        }
    }

    @Test
    fun mustCorrectlyReturnUserIdForAssetSnapshot() {
        val id = AssetSnapshotId(UUID.randomUUID())

        suppose("asset snapshot exists in database") {
            dslContext.executeInsert(
                AssetSnapshotRecord(
                    id = id,
                    projectId = PROJECT_ID,
                    name = NAME,
                    chainId = CHAIN_ID,
                    assetContractAddress = CONTRACT_ADDRESS,
                    blockNumber = BLOCK_NUMBER,
                    ignoredHolderAddresses = emptyArray(),
                    status = AssetSnapshotStatus.SUCCESS,
                    resultTree = null,
                    treeIpfsHash = null,
                    totalAssetAmount = null,
                    failureCause = null
                )
            )
        }

        verify("correct userId is returned") {
            expectThat(repository.getUserId(IdType.ASSET_SNAPSHOT_ID, id.value))
                .isEqualTo(USER_ID)
        }
    }
}
