package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.generated.jooq.enums.UserIdentifierType
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.AssetMultiSendRequestRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ProjectRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.UserIdentifierRecord
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.params.StoreAssetMultiSendRequestParams
import com.ampnet.blockchainapiservice.model.result.AssetMultiSendRequest
import com.ampnet.blockchainapiservice.testcontainers.SharedTestContainers
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.BaseUrl
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
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
@Import(JooqAssetMultiSendRequestRepository::class)
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JooqAssetMultiSendRequestRepositoryIntegTest : TestBase() {

    companion object {
        private val CHAIN_ID = ChainId(1337L)
        private const val REDIRECT_URL = "redirect-url"
        private val TOKEN_ADDRESS = ContractAddress("a")
        private val DISPERSE_CONTRACT_ADDRESS = ContractAddress("b")
        private val ASSET_AMOUNTS = listOf(Balance(BigInteger.valueOf(123456L)), Balance(BigInteger.valueOf(789L)))
        private val ASSET_RECIPIENT_ADDRESSES = listOf(WalletAddress("c"), WalletAddress("d"))
        private val ITEM_NAMES = listOf("test", null)
        private val ASSET_SENDER_ADDRESS = WalletAddress("e")
        private val ARBITRARY_DATA = TestData.EMPTY_JSON_OBJECT
        private const val APPROVE_SCREEN_BEFORE_ACTION_MESSAGE = "approve-screen-before-action-message"
        private const val APPROVE_SCREEN_AFTER_ACTION_MESSAGE = "approve-screen-after-action-message"
        private const val SEND_SCREEN_BEFORE_ACTION_MESSAGE = "send-screen-before-action-message"
        private const val SEND_SCREEN_AFTER_ACTION_MESSAGE = "send-screen-after-action-message"
        private val APPROVE_TX_HASH = TransactionHash("approve-tx-hash")
        private val SEND_TX_HASH = TransactionHash("send-tx-hash")
        private val PROJECT_ID = UUID.randomUUID()
        private val OWNER_ID = UUID.randomUUID()
    }

    @Suppress("unused")
    private val postgresContainer = SharedTestContainers.postgresContainer

    @Autowired
    private lateinit var repository: JooqAssetMultiSendRequestRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        postgresContainer.cleanAllDatabaseTables(dslContext)

        dslContext.executeInsert(
            UserIdentifierRecord(
                id = OWNER_ID,
                userIdentifier = "user-identifier",
                identifierType = UserIdentifierType.ETH_WALLET_ADDRESS
            )
        )

        dslContext.executeInsert(
            ProjectRecord(
                id = PROJECT_ID,
                ownerId = OWNER_ID,
                issuerContractAddress = ContractAddress("0"),
                baseRedirectUrl = BaseUrl("base-redirect-url"),
                chainId = ChainId(1337L),
                customRpcUrl = "custom-rpc-url",
                createdAt = TestData.TIMESTAMP
            )
        )
    }

    @Test
    fun mustCorrectlyFetchAssetMultiSendRequestById() {
        val id = UUID.randomUUID()

        suppose("some asset multi-send request exists in database") {
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
                    sendTxHash = SEND_TX_HASH,
                    approveScreenBeforeActionMessage = APPROVE_SCREEN_BEFORE_ACTION_MESSAGE,
                    approveScreenAfterActionMessage = APPROVE_SCREEN_AFTER_ACTION_MESSAGE,
                    sendScreenBeforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                    sendScreenAfterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE,
                    projectId = PROJECT_ID,
                    createdAt = TestData.TIMESTAMP
                )
            )
        }

        verify("asset multi-send request is correctly fetched by ID") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(
                    AssetMultiSendRequest(
                        id = id,
                        projectId = PROJECT_ID,
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        tokenAddress = TOKEN_ADDRESS,
                        disperseContractAddress = DISPERSE_CONTRACT_ADDRESS,
                        assetAmounts = ASSET_AMOUNTS,
                        assetRecipientAddresses = ASSET_RECIPIENT_ADDRESSES,
                        assetSenderAddress = ASSET_SENDER_ADDRESS,
                        itemNames = ITEM_NAMES,
                        approveTxHash = APPROVE_TX_HASH,
                        sendTxHash = SEND_TX_HASH,
                        arbitraryData = ARBITRARY_DATA,
                        approveScreenConfig = ScreenConfig(
                            beforeActionMessage = APPROVE_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = APPROVE_SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        sendScreenConfig = ScreenConfig(
                            beforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }
    }

    @Test
    fun mustReturnNullWhenFetchingNonExistentAssetMultiSendRequestById() {
        verify("null is returned when fetching non-existent asset multi-send request") {
            val result = repository.getById(UUID.randomUUID())

            assertThat(result).withMessage()
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyFetchAssetMultiSendRequestsByProject() {
        val otherProjectId = UUID.randomUUID()

        suppose("some other project is in database") {
            dslContext.executeInsert(
                ProjectRecord(
                    id = otherProjectId,
                    ownerId = OWNER_ID,
                    issuerContractAddress = ContractAddress("1"),
                    baseRedirectUrl = BaseUrl("base-redirect-url"),
                    chainId = ChainId(1337L),
                    customRpcUrl = "custom-rpc-url",
                    createdAt = TestData.TIMESTAMP
                )
            )
        }

        val projectRequests = listOf(
            AssetMultiSendRequestRecord(
                id = UUID.randomUUID(),
                chainId = CHAIN_ID,
                redirectUrl = REDIRECT_URL,
                tokenAddress = TOKEN_ADDRESS,
                disperseContractAddress = DISPERSE_CONTRACT_ADDRESS,
                assetAmounts = ASSET_AMOUNTS.map { it.rawValue.toBigDecimal() }.toTypedArray(),
                assetRecipientAddresses = ASSET_RECIPIENT_ADDRESSES.map { it.rawValue }.toTypedArray(),
                itemNames = ITEM_NAMES.toTypedArray() as Array<String>,
                assetSenderAddress = ASSET_SENDER_ADDRESS,
                arbitraryData = ARBITRARY_DATA,
                approveTxHash = APPROVE_TX_HASH,
                sendTxHash = SEND_TX_HASH,
                approveScreenBeforeActionMessage = APPROVE_SCREEN_BEFORE_ACTION_MESSAGE,
                approveScreenAfterActionMessage = APPROVE_SCREEN_AFTER_ACTION_MESSAGE,
                sendScreenBeforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                sendScreenAfterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE,
                projectId = PROJECT_ID,
                createdAt = TestData.TIMESTAMP
            ),
            AssetMultiSendRequestRecord(
                id = UUID.randomUUID(),
                chainId = CHAIN_ID,
                redirectUrl = REDIRECT_URL,
                tokenAddress = TOKEN_ADDRESS,
                disperseContractAddress = DISPERSE_CONTRACT_ADDRESS,
                assetAmounts = ASSET_AMOUNTS.map { it.rawValue.toBigDecimal() }.toTypedArray(),
                assetRecipientAddresses = ASSET_RECIPIENT_ADDRESSES.map { it.rawValue }.toTypedArray(),
                itemNames = ITEM_NAMES.toTypedArray() as Array<String>,
                assetSenderAddress = null,
                arbitraryData = ARBITRARY_DATA,
                approveTxHash = APPROVE_TX_HASH,
                sendTxHash = SEND_TX_HASH,
                approveScreenBeforeActionMessage = APPROVE_SCREEN_BEFORE_ACTION_MESSAGE,
                approveScreenAfterActionMessage = APPROVE_SCREEN_AFTER_ACTION_MESSAGE,
                sendScreenBeforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                sendScreenAfterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE,
                projectId = PROJECT_ID,
                createdAt = TestData.TIMESTAMP
            )
        )
        val otherRequests = listOf(
            AssetMultiSendRequestRecord(
                id = UUID.randomUUID(),
                chainId = CHAIN_ID,
                redirectUrl = REDIRECT_URL,
                tokenAddress = TOKEN_ADDRESS,
                disperseContractAddress = DISPERSE_CONTRACT_ADDRESS,
                assetAmounts = ASSET_AMOUNTS.map { it.rawValue.toBigDecimal() }.toTypedArray(),
                assetRecipientAddresses = ASSET_RECIPIENT_ADDRESSES.map { it.rawValue }.toTypedArray(),
                itemNames = ITEM_NAMES.toTypedArray() as Array<String>,
                assetSenderAddress = ASSET_SENDER_ADDRESS,
                arbitraryData = ARBITRARY_DATA,
                approveTxHash = APPROVE_TX_HASH,
                sendTxHash = SEND_TX_HASH,
                approveScreenBeforeActionMessage = APPROVE_SCREEN_BEFORE_ACTION_MESSAGE,
                approveScreenAfterActionMessage = APPROVE_SCREEN_AFTER_ACTION_MESSAGE,
                sendScreenBeforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                sendScreenAfterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE,
                projectId = otherProjectId,
                createdAt = TestData.TIMESTAMP
            ),
            AssetMultiSendRequestRecord(
                id = UUID.randomUUID(),
                chainId = CHAIN_ID,
                redirectUrl = REDIRECT_URL,
                tokenAddress = TOKEN_ADDRESS,
                disperseContractAddress = DISPERSE_CONTRACT_ADDRESS,
                assetAmounts = ASSET_AMOUNTS.map { it.rawValue.toBigDecimal() }.toTypedArray(),
                assetRecipientAddresses = ASSET_RECIPIENT_ADDRESSES.map { it.rawValue }.toTypedArray(),
                itemNames = ITEM_NAMES.toTypedArray() as Array<String>,
                assetSenderAddress = null,
                arbitraryData = ARBITRARY_DATA,
                approveTxHash = APPROVE_TX_HASH,
                sendTxHash = SEND_TX_HASH,
                approveScreenBeforeActionMessage = APPROVE_SCREEN_BEFORE_ACTION_MESSAGE,
                approveScreenAfterActionMessage = APPROVE_SCREEN_AFTER_ACTION_MESSAGE,
                sendScreenBeforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                sendScreenAfterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE,
                projectId = otherProjectId,
                createdAt = TestData.TIMESTAMP
            )
        )

        suppose("some asset multi-send requests exist in database") {
            dslContext.batchInsert(projectRequests + otherRequests).execute()
        }

        verify("asset multi-send requests are correctly fetched by project") {
            val result = repository.getAllByProjectId(PROJECT_ID)

            assertThat(result).withMessage()
                .containsExactlyInAnyOrderElementsOf(
                    projectRequests.map {
                        AssetMultiSendRequest(
                            id = it.id,
                            projectId = it.projectId,
                            chainId = it.chainId,
                            redirectUrl = it.redirectUrl,
                            tokenAddress = it.tokenAddress,
                            disperseContractAddress = it.disperseContractAddress,
                            assetAmounts = it.assetAmounts.map { Balance(it.toBigInteger()) }.toList(),
                            assetRecipientAddresses = it.assetRecipientAddresses.map { WalletAddress(it) }.toList(),
                            itemNames = it.itemNames.toList(),
                            assetSenderAddress = it.assetSenderAddress,
                            approveTxHash = it.approveTxHash,
                            sendTxHash = it.sendTxHash,
                            arbitraryData = it.arbitraryData,
                            approveScreenConfig = ScreenConfig(
                                beforeActionMessage = it.approveScreenBeforeActionMessage,
                                afterActionMessage = it.approveScreenAfterActionMessage
                            ),
                            sendScreenConfig = ScreenConfig(
                                beforeActionMessage = it.sendScreenBeforeActionMessage,
                                afterActionMessage = it.sendScreenAfterActionMessage
                            ),
                            createdAt = it.createdAt
                        )
                    }
                )
        }
    }

    @Test
    fun mustCorrectlyFetchAssetMultiSendRequestsBySender() {
        val senderRequests = listOf(
            AssetMultiSendRequestRecord(
                id = UUID.randomUUID(),
                chainId = CHAIN_ID,
                redirectUrl = REDIRECT_URL,
                tokenAddress = TOKEN_ADDRESS,
                disperseContractAddress = DISPERSE_CONTRACT_ADDRESS,
                assetAmounts = ASSET_AMOUNTS.map { it.rawValue.toBigDecimal() }.toTypedArray(),
                assetRecipientAddresses = ASSET_RECIPIENT_ADDRESSES.map { it.rawValue }.toTypedArray(),
                itemNames = ITEM_NAMES.toTypedArray() as Array<String>,
                assetSenderAddress = ASSET_SENDER_ADDRESS,
                arbitraryData = ARBITRARY_DATA,
                approveTxHash = APPROVE_TX_HASH,
                sendTxHash = SEND_TX_HASH,
                approveScreenBeforeActionMessage = APPROVE_SCREEN_BEFORE_ACTION_MESSAGE,
                approveScreenAfterActionMessage = APPROVE_SCREEN_AFTER_ACTION_MESSAGE,
                sendScreenBeforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                sendScreenAfterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE,
                projectId = PROJECT_ID,
                createdAt = TestData.TIMESTAMP
            ),
            AssetMultiSendRequestRecord(
                id = UUID.randomUUID(),
                chainId = CHAIN_ID,
                redirectUrl = REDIRECT_URL,
                tokenAddress = TOKEN_ADDRESS,
                disperseContractAddress = DISPERSE_CONTRACT_ADDRESS,
                assetAmounts = ASSET_AMOUNTS.map { it.rawValue.toBigDecimal() }.toTypedArray(),
                assetRecipientAddresses = ASSET_RECIPIENT_ADDRESSES.map { it.rawValue }.toTypedArray(),
                itemNames = ITEM_NAMES.toTypedArray() as Array<String>,
                assetSenderAddress = ASSET_SENDER_ADDRESS,
                arbitraryData = ARBITRARY_DATA,
                approveTxHash = APPROVE_TX_HASH,
                sendTxHash = SEND_TX_HASH,
                approveScreenBeforeActionMessage = APPROVE_SCREEN_BEFORE_ACTION_MESSAGE,
                approveScreenAfterActionMessage = APPROVE_SCREEN_AFTER_ACTION_MESSAGE,
                sendScreenBeforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                sendScreenAfterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE,
                projectId = PROJECT_ID,
                createdAt = TestData.TIMESTAMP
            )
        )
        val otherRequests = listOf(
            AssetMultiSendRequestRecord(
                id = UUID.randomUUID(),
                chainId = CHAIN_ID,
                redirectUrl = REDIRECT_URL,
                tokenAddress = TOKEN_ADDRESS,
                disperseContractAddress = DISPERSE_CONTRACT_ADDRESS,
                assetAmounts = ASSET_AMOUNTS.map { it.rawValue.toBigDecimal() }.toTypedArray(),
                assetRecipientAddresses = ASSET_RECIPIENT_ADDRESSES.map { it.rawValue }.toTypedArray(),
                itemNames = ITEM_NAMES.toTypedArray() as Array<String>,
                assetSenderAddress = WalletAddress("dead"),
                arbitraryData = ARBITRARY_DATA,
                approveTxHash = APPROVE_TX_HASH,
                sendTxHash = SEND_TX_HASH,
                approveScreenBeforeActionMessage = APPROVE_SCREEN_BEFORE_ACTION_MESSAGE,
                approveScreenAfterActionMessage = APPROVE_SCREEN_AFTER_ACTION_MESSAGE,
                sendScreenBeforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                sendScreenAfterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE,
                projectId = PROJECT_ID,
                createdAt = TestData.TIMESTAMP
            ),
            AssetMultiSendRequestRecord(
                id = UUID.randomUUID(),
                chainId = CHAIN_ID,
                redirectUrl = REDIRECT_URL,
                tokenAddress = TOKEN_ADDRESS,
                disperseContractAddress = DISPERSE_CONTRACT_ADDRESS,
                assetAmounts = ASSET_AMOUNTS.map { it.rawValue.toBigDecimal() }.toTypedArray(),
                assetRecipientAddresses = ASSET_RECIPIENT_ADDRESSES.map { it.rawValue }.toTypedArray(),
                itemNames = ITEM_NAMES.toTypedArray() as Array<String>,
                assetSenderAddress = null,
                arbitraryData = ARBITRARY_DATA,
                approveTxHash = APPROVE_TX_HASH,
                sendTxHash = SEND_TX_HASH,
                approveScreenBeforeActionMessage = APPROVE_SCREEN_BEFORE_ACTION_MESSAGE,
                approveScreenAfterActionMessage = APPROVE_SCREEN_AFTER_ACTION_MESSAGE,
                sendScreenBeforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                sendScreenAfterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE,
                projectId = PROJECT_ID,
                createdAt = TestData.TIMESTAMP
            )
        )

        suppose("some asset multi-send requests exist in database") {
            dslContext.batchInsert(senderRequests + otherRequests).execute()
        }

        verify("asset multi-send requests are correctly fetched by sender") {
            val result = repository.getBySender(ASSET_SENDER_ADDRESS)

            assertThat(result).withMessage()
                .containsExactlyInAnyOrderElementsOf(
                    senderRequests.map {
                        AssetMultiSendRequest(
                            id = it.id,
                            projectId = it.projectId,
                            chainId = it.chainId,
                            redirectUrl = it.redirectUrl,
                            tokenAddress = it.tokenAddress,
                            disperseContractAddress = it.disperseContractAddress,
                            assetAmounts = it.assetAmounts.map { Balance(it.toBigInteger()) }.toList(),
                            assetRecipientAddresses = it.assetRecipientAddresses.map { WalletAddress(it) }.toList(),
                            itemNames = it.itemNames.toList(),
                            assetSenderAddress = it.assetSenderAddress,
                            approveTxHash = it.approveTxHash,
                            sendTxHash = it.sendTxHash,
                            arbitraryData = it.arbitraryData,
                            approveScreenConfig = ScreenConfig(
                                beforeActionMessage = it.approveScreenBeforeActionMessage,
                                afterActionMessage = it.approveScreenAfterActionMessage
                            ),
                            sendScreenConfig = ScreenConfig(
                                beforeActionMessage = it.sendScreenBeforeActionMessage,
                                afterActionMessage = it.sendScreenAfterActionMessage
                            ),
                            createdAt = it.createdAt
                        )
                    }
                )
        }
    }

    @Test
    fun mustCorrectlyStoreAssetMultiSendRequest() {
        val id = UUID.randomUUID()
        val params = StoreAssetMultiSendRequestParams(
            id = id,
            projectId = PROJECT_ID,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            tokenAddress = TOKEN_ADDRESS,
            disperseContractAddress = DISPERSE_CONTRACT_ADDRESS,
            assetAmounts = ASSET_AMOUNTS,
            assetRecipientAddresses = ASSET_RECIPIENT_ADDRESSES,
            itemNames = ITEM_NAMES,
            assetSenderAddress = ASSET_SENDER_ADDRESS,
            arbitraryData = ARBITRARY_DATA,
            approveScreenConfig = ScreenConfig(
                beforeActionMessage = APPROVE_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = APPROVE_SCREEN_AFTER_ACTION_MESSAGE
            ),
            sendScreenConfig = ScreenConfig(
                beforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE
            ),
            createdAt = TestData.TIMESTAMP
        )

        val storedAssetMultiSendRequest = suppose("asset multi-send request is stored in database") {
            repository.store(params)
        }

        val expectedAssetMultiSendRequest = AssetMultiSendRequest(
            id = id,
            projectId = PROJECT_ID,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            tokenAddress = TOKEN_ADDRESS,
            disperseContractAddress = DISPERSE_CONTRACT_ADDRESS,
            assetAmounts = ASSET_AMOUNTS,
            assetRecipientAddresses = ASSET_RECIPIENT_ADDRESSES,
            itemNames = ITEM_NAMES,
            assetSenderAddress = ASSET_SENDER_ADDRESS,
            approveTxHash = null,
            sendTxHash = null,
            arbitraryData = ARBITRARY_DATA,
            approveScreenConfig = ScreenConfig(
                beforeActionMessage = APPROVE_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = APPROVE_SCREEN_AFTER_ACTION_MESSAGE
            ),
            sendScreenConfig = ScreenConfig(
                beforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE
            ),
            createdAt = TestData.TIMESTAMP
        )

        verify("storing asset multi-send request returns correct result") {
            assertThat(storedAssetMultiSendRequest).withMessage()
                .isEqualTo(expectedAssetMultiSendRequest)
        }

        verify("asset multi-send request was stored in database") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(expectedAssetMultiSendRequest)
        }
    }

    @Test
    fun mustCorrectlySetApproveTxInfoForAssetMultiSendRequestWithNullApproveTxHash() {
        val id = UUID.randomUUID()
        val params = StoreAssetMultiSendRequestParams(
            id = id,
            projectId = PROJECT_ID,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            tokenAddress = TOKEN_ADDRESS,
            disperseContractAddress = DISPERSE_CONTRACT_ADDRESS,
            assetAmounts = ASSET_AMOUNTS,
            assetRecipientAddresses = ASSET_RECIPIENT_ADDRESSES,
            itemNames = ITEM_NAMES,
            assetSenderAddress = null,
            arbitraryData = ARBITRARY_DATA,
            approveScreenConfig = ScreenConfig(
                beforeActionMessage = APPROVE_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = APPROVE_SCREEN_AFTER_ACTION_MESSAGE
            ),
            sendScreenConfig = ScreenConfig(
                beforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE
            ),
            createdAt = TestData.TIMESTAMP
        )

        suppose("asset multi-send request is stored in database") {
            repository.store(params)
        }

        verify("setting approve txInfo will succeed") {
            assertThat(repository.setApproveTxInfo(id, APPROVE_TX_HASH, ASSET_SENDER_ADDRESS)).withMessage()
                .isTrue()
        }

        verify("approve txInfo was correctly set in database") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(
                    AssetMultiSendRequest(
                        id = id,
                        projectId = PROJECT_ID,
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        tokenAddress = TOKEN_ADDRESS,
                        disperseContractAddress = DISPERSE_CONTRACT_ADDRESS,
                        assetAmounts = ASSET_AMOUNTS,
                        assetRecipientAddresses = ASSET_RECIPIENT_ADDRESSES,
                        itemNames = ITEM_NAMES,
                        assetSenderAddress = ASSET_SENDER_ADDRESS,
                        approveTxHash = APPROVE_TX_HASH,
                        sendTxHash = null,
                        arbitraryData = ARBITRARY_DATA,
                        approveScreenConfig = ScreenConfig(
                            beforeActionMessage = APPROVE_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = APPROVE_SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        sendScreenConfig = ScreenConfig(
                            beforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }
    }

    @Test
    fun mustNotUpdateTokenSenderAddressForAssetMultiSendRequestWhenTokenSenderIsAlreadySetForApproveTxHash() {
        val id = UUID.randomUUID()
        val params = StoreAssetMultiSendRequestParams(
            id = id,
            projectId = PROJECT_ID,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            tokenAddress = TOKEN_ADDRESS,
            disperseContractAddress = DISPERSE_CONTRACT_ADDRESS,
            assetAmounts = ASSET_AMOUNTS,
            assetRecipientAddresses = ASSET_RECIPIENT_ADDRESSES,
            itemNames = ITEM_NAMES,
            assetSenderAddress = ASSET_SENDER_ADDRESS,
            arbitraryData = ARBITRARY_DATA,
            approveScreenConfig = ScreenConfig(
                beforeActionMessage = APPROVE_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = APPROVE_SCREEN_AFTER_ACTION_MESSAGE
            ),
            sendScreenConfig = ScreenConfig(
                beforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE
            ),
            createdAt = TestData.TIMESTAMP
        )

        suppose("asset multi-send request is stored in database") {
            repository.store(params)
        }

        verify("setting approve txInfo will succeed") {
            val ignoredTokenSender = WalletAddress("f")
            assertThat(repository.setApproveTxInfo(id, APPROVE_TX_HASH, ignoredTokenSender)).withMessage()
                .isTrue()
        }

        verify("approve txHash was correctly set while token sender was not updated") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(
                    AssetMultiSendRequest(
                        id = id,
                        projectId = PROJECT_ID,
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        tokenAddress = TOKEN_ADDRESS,
                        disperseContractAddress = DISPERSE_CONTRACT_ADDRESS,
                        assetAmounts = ASSET_AMOUNTS,
                        assetRecipientAddresses = ASSET_RECIPIENT_ADDRESSES,
                        itemNames = ITEM_NAMES,
                        assetSenderAddress = ASSET_SENDER_ADDRESS,
                        approveTxHash = APPROVE_TX_HASH,
                        sendTxHash = null,
                        arbitraryData = ARBITRARY_DATA,
                        approveScreenConfig = ScreenConfig(
                            beforeActionMessage = APPROVE_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = APPROVE_SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        sendScreenConfig = ScreenConfig(
                            beforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }
    }

    @Test
    fun mustNotSetApproveTxHashForAssetMultiSendRequestWhenApproveTxHashIsAlreadySet() {
        val id = UUID.randomUUID()
        val params = StoreAssetMultiSendRequestParams(
            id = id,
            projectId = PROJECT_ID,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            tokenAddress = TOKEN_ADDRESS,
            disperseContractAddress = DISPERSE_CONTRACT_ADDRESS,
            assetAmounts = ASSET_AMOUNTS,
            assetRecipientAddresses = ASSET_RECIPIENT_ADDRESSES,
            itemNames = ITEM_NAMES,
            assetSenderAddress = ASSET_SENDER_ADDRESS,
            arbitraryData = ARBITRARY_DATA,
            approveScreenConfig = ScreenConfig(
                beforeActionMessage = APPROVE_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = APPROVE_SCREEN_AFTER_ACTION_MESSAGE
            ),
            sendScreenConfig = ScreenConfig(
                beforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE
            ),
            createdAt = TestData.TIMESTAMP
        )

        suppose("asset multi-send request is stored in database") {
            repository.store(params)
        }

        verify("setting approve txInfo will succeed") {
            assertThat(repository.setApproveTxInfo(id, APPROVE_TX_HASH, ASSET_SENDER_ADDRESS)).withMessage()
                .isTrue()
        }

        verify("setting another approve txInfo will not succeed") {
            assertThat(
                repository.setApproveTxInfo(
                    id = id,
                    txHash = TransactionHash("different-tx-hash"),
                    caller = ASSET_SENDER_ADDRESS
                )
            ).withMessage().isFalse()
        }

        verify("first approve txHash remains in database") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(
                    AssetMultiSendRequest(
                        id = id,
                        projectId = PROJECT_ID,
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        tokenAddress = TOKEN_ADDRESS,
                        disperseContractAddress = DISPERSE_CONTRACT_ADDRESS,
                        assetAmounts = ASSET_AMOUNTS,
                        assetRecipientAddresses = ASSET_RECIPIENT_ADDRESSES,
                        itemNames = ITEM_NAMES,
                        assetSenderAddress = ASSET_SENDER_ADDRESS,
                        approveTxHash = APPROVE_TX_HASH,
                        sendTxHash = null,
                        arbitraryData = ARBITRARY_DATA,
                        approveScreenConfig = ScreenConfig(
                            beforeActionMessage = APPROVE_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = APPROVE_SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        sendScreenConfig = ScreenConfig(
                            beforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }
    }

    @Test
    fun mustNotSetApproveTxHashForAssetMultiSendRequestWhenTokenAddressIsNull() {
        val id = UUID.randomUUID()
        val params = StoreAssetMultiSendRequestParams(
            id = id,
            projectId = PROJECT_ID,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            tokenAddress = null,
            disperseContractAddress = DISPERSE_CONTRACT_ADDRESS,
            assetAmounts = ASSET_AMOUNTS,
            assetRecipientAddresses = ASSET_RECIPIENT_ADDRESSES,
            itemNames = ITEM_NAMES,
            assetSenderAddress = ASSET_SENDER_ADDRESS,
            arbitraryData = ARBITRARY_DATA,
            approveScreenConfig = ScreenConfig(
                beforeActionMessage = APPROVE_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = APPROVE_SCREEN_AFTER_ACTION_MESSAGE
            ),
            sendScreenConfig = ScreenConfig(
                beforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE
            ),
            createdAt = TestData.TIMESTAMP
        )

        suppose("asset multi-send request is stored in database") {
            repository.store(params)
        }

        verify("setting approve txInfo will not succeed") {
            assertThat(
                repository.setApproveTxInfo(
                    id = id,
                    txHash = APPROVE_TX_HASH,
                    caller = ASSET_SENDER_ADDRESS
                )
            ).withMessage().isFalse()
        }

        verify("approve tx hash is not set in database") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(
                    AssetMultiSendRequest(
                        id = id,
                        projectId = PROJECT_ID,
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        tokenAddress = null,
                        disperseContractAddress = DISPERSE_CONTRACT_ADDRESS,
                        assetAmounts = ASSET_AMOUNTS,
                        assetRecipientAddresses = ASSET_RECIPIENT_ADDRESSES,
                        itemNames = ITEM_NAMES,
                        assetSenderAddress = ASSET_SENDER_ADDRESS,
                        approveTxHash = null,
                        sendTxHash = null,
                        arbitraryData = ARBITRARY_DATA,
                        approveScreenConfig = ScreenConfig(
                            beforeActionMessage = APPROVE_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = APPROVE_SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        sendScreenConfig = ScreenConfig(
                            beforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }
    }

    @Test
    fun mustCorrectlySetSendTxInfoForAssetMultiSendRequestWithNullSendTxHash() {
        val id = UUID.randomUUID()
        val params = StoreAssetMultiSendRequestParams(
            id = id,
            projectId = PROJECT_ID,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            tokenAddress = TOKEN_ADDRESS,
            disperseContractAddress = DISPERSE_CONTRACT_ADDRESS,
            assetAmounts = ASSET_AMOUNTS,
            assetRecipientAddresses = ASSET_RECIPIENT_ADDRESSES,
            itemNames = ITEM_NAMES,
            assetSenderAddress = null,
            arbitraryData = ARBITRARY_DATA,
            approveScreenConfig = ScreenConfig(
                beforeActionMessage = APPROVE_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = APPROVE_SCREEN_AFTER_ACTION_MESSAGE
            ),
            sendScreenConfig = ScreenConfig(
                beforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE
            ),
            createdAt = TestData.TIMESTAMP
        )

        suppose("asset multi-send request is stored in database") {
            repository.store(params)
        }

        verify("setting approve txInfo will succeed") {
            assertThat(repository.setApproveTxInfo(id, APPROVE_TX_HASH, ASSET_SENDER_ADDRESS)).withMessage()
                .isTrue()
        }

        verify("setting send txInfo will succeed") {
            assertThat(repository.setSendTxInfo(id, SEND_TX_HASH, ASSET_SENDER_ADDRESS)).withMessage()
                .isTrue()
        }

        verify("send txInfo was correctly set in database") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(
                    AssetMultiSendRequest(
                        id = id,
                        projectId = PROJECT_ID,
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        tokenAddress = TOKEN_ADDRESS,
                        disperseContractAddress = DISPERSE_CONTRACT_ADDRESS,
                        assetAmounts = ASSET_AMOUNTS,
                        assetRecipientAddresses = ASSET_RECIPIENT_ADDRESSES,
                        itemNames = ITEM_NAMES,
                        assetSenderAddress = ASSET_SENDER_ADDRESS,
                        approveTxHash = APPROVE_TX_HASH,
                        sendTxHash = SEND_TX_HASH,
                        arbitraryData = ARBITRARY_DATA,
                        approveScreenConfig = ScreenConfig(
                            beforeActionMessage = APPROVE_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = APPROVE_SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        sendScreenConfig = ScreenConfig(
                            beforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }
    }

    @Test
    fun mustNotUpdateTokenSenderAddressForAssetMultiSendRequestWhenTokenSenderIsAlreadySetForSendTxHash() {
        val id = UUID.randomUUID()
        val params = StoreAssetMultiSendRequestParams(
            id = id,
            projectId = PROJECT_ID,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            tokenAddress = null,
            disperseContractAddress = DISPERSE_CONTRACT_ADDRESS,
            assetAmounts = ASSET_AMOUNTS,
            assetRecipientAddresses = ASSET_RECIPIENT_ADDRESSES,
            itemNames = ITEM_NAMES,
            assetSenderAddress = ASSET_SENDER_ADDRESS,
            arbitraryData = ARBITRARY_DATA,
            approveScreenConfig = ScreenConfig(
                beforeActionMessage = APPROVE_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = APPROVE_SCREEN_AFTER_ACTION_MESSAGE
            ),
            sendScreenConfig = ScreenConfig(
                beforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE
            ),
            createdAt = TestData.TIMESTAMP
        )

        suppose("asset multi-send request is stored in database") {
            repository.store(params)
        }

        verify("setting send txInfo will succeed") {
            val ignoredTokenSender = WalletAddress("f")
            assertThat(repository.setSendTxInfo(id, SEND_TX_HASH, ignoredTokenSender)).withMessage()
                .isTrue()
        }

        verify("send txHash was correctly set while token sender was not updated") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(
                    AssetMultiSendRequest(
                        id = id,
                        projectId = PROJECT_ID,
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        tokenAddress = null,
                        disperseContractAddress = DISPERSE_CONTRACT_ADDRESS,
                        assetAmounts = ASSET_AMOUNTS,
                        assetRecipientAddresses = ASSET_RECIPIENT_ADDRESSES,
                        itemNames = ITEM_NAMES,
                        assetSenderAddress = ASSET_SENDER_ADDRESS,
                        approveTxHash = null,
                        sendTxHash = SEND_TX_HASH,
                        arbitraryData = ARBITRARY_DATA,
                        approveScreenConfig = ScreenConfig(
                            beforeActionMessage = APPROVE_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = APPROVE_SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        sendScreenConfig = ScreenConfig(
                            beforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }
    }

    @Test
    fun mustNotSetSendTxHashForAssetMultiSendRequestWhenSendTxHashIsAlreadySet() {
        val id = UUID.randomUUID()
        val params = StoreAssetMultiSendRequestParams(
            id = id,
            projectId = PROJECT_ID,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            tokenAddress = TOKEN_ADDRESS,
            disperseContractAddress = DISPERSE_CONTRACT_ADDRESS,
            assetAmounts = ASSET_AMOUNTS,
            assetRecipientAddresses = ASSET_RECIPIENT_ADDRESSES,
            itemNames = ITEM_NAMES,
            assetSenderAddress = ASSET_SENDER_ADDRESS,
            arbitraryData = ARBITRARY_DATA,
            approveScreenConfig = ScreenConfig(
                beforeActionMessage = APPROVE_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = APPROVE_SCREEN_AFTER_ACTION_MESSAGE
            ),
            sendScreenConfig = ScreenConfig(
                beforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE
            ),
            createdAt = TestData.TIMESTAMP
        )

        suppose("asset multi-send request is stored in database") {
            repository.store(params)
        }

        verify("setting approve txInfo will succeed") {
            assertThat(repository.setApproveTxInfo(id, APPROVE_TX_HASH, ASSET_SENDER_ADDRESS)).withMessage()
                .isTrue()
        }

        verify("setting send txInfo will succeed") {
            assertThat(repository.setSendTxInfo(id, SEND_TX_HASH, ASSET_SENDER_ADDRESS)).withMessage()
                .isTrue()
        }

        verify("setting another send txInfo will not succeed") {
            assertThat(
                repository.setSendTxInfo(
                    id = id,
                    txHash = TransactionHash("different-tx-hash"),
                    caller = ASSET_SENDER_ADDRESS
                )
            ).withMessage().isFalse()
        }

        verify("first send txHash remains in database") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(
                    AssetMultiSendRequest(
                        id = id,
                        projectId = PROJECT_ID,
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        tokenAddress = TOKEN_ADDRESS,
                        disperseContractAddress = DISPERSE_CONTRACT_ADDRESS,
                        assetAmounts = ASSET_AMOUNTS,
                        assetRecipientAddresses = ASSET_RECIPIENT_ADDRESSES,
                        itemNames = ITEM_NAMES,
                        assetSenderAddress = ASSET_SENDER_ADDRESS,
                        approveTxHash = APPROVE_TX_HASH,
                        sendTxHash = SEND_TX_HASH,
                        arbitraryData = ARBITRARY_DATA,
                        approveScreenConfig = ScreenConfig(
                            beforeActionMessage = APPROVE_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = APPROVE_SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        sendScreenConfig = ScreenConfig(
                            beforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }
    }

    @Test
    fun mustNotSetSendTxHashForAssetMultiSendRequestWhenTokenAddressIsNotNullWithNullApproveHash() {
        val id = UUID.randomUUID()
        val params = StoreAssetMultiSendRequestParams(
            id = id,
            projectId = PROJECT_ID,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            tokenAddress = TOKEN_ADDRESS,
            disperseContractAddress = DISPERSE_CONTRACT_ADDRESS,
            assetAmounts = ASSET_AMOUNTS,
            assetRecipientAddresses = ASSET_RECIPIENT_ADDRESSES,
            itemNames = ITEM_NAMES,
            assetSenderAddress = ASSET_SENDER_ADDRESS,
            arbitraryData = ARBITRARY_DATA,
            approveScreenConfig = ScreenConfig(
                beforeActionMessage = APPROVE_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = APPROVE_SCREEN_AFTER_ACTION_MESSAGE
            ),
            sendScreenConfig = ScreenConfig(
                beforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE
            ),
            createdAt = TestData.TIMESTAMP
        )

        suppose("asset multi-send request is stored in database") {
            repository.store(params)
        }

        verify("setting send txInfo will not succeed") {
            assertThat(
                repository.setSendTxInfo(
                    id = id,
                    txHash = SEND_TX_HASH,
                    caller = ASSET_SENDER_ADDRESS
                )
            ).withMessage().isFalse()
        }

        verify("approve tx hash is not set in database") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(
                    AssetMultiSendRequest(
                        id = id,
                        projectId = PROJECT_ID,
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        tokenAddress = TOKEN_ADDRESS,
                        disperseContractAddress = DISPERSE_CONTRACT_ADDRESS,
                        assetAmounts = ASSET_AMOUNTS,
                        assetRecipientAddresses = ASSET_RECIPIENT_ADDRESSES,
                        itemNames = ITEM_NAMES,
                        assetSenderAddress = ASSET_SENDER_ADDRESS,
                        approveTxHash = null,
                        sendTxHash = null,
                        arbitraryData = ARBITRARY_DATA,
                        approveScreenConfig = ScreenConfig(
                            beforeActionMessage = APPROVE_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = APPROVE_SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        sendScreenConfig = ScreenConfig(
                            beforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }
    }
}
