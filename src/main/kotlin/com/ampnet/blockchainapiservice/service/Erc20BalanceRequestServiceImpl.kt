package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.blockchain.BlockchainService
import com.ampnet.blockchainapiservice.blockchain.properties.ChainSpec
import com.ampnet.blockchainapiservice.blockchain.properties.RpcUrlSpec
import com.ampnet.blockchainapiservice.exception.CannotAttachSignedMessageException
import com.ampnet.blockchainapiservice.exception.NonExistentClientIdException
import com.ampnet.blockchainapiservice.exception.ResourceNotFoundException
import com.ampnet.blockchainapiservice.model.params.CreateErc20BalanceRequestParams
import com.ampnet.blockchainapiservice.model.params.StoreErc20BalanceRequestParams
import com.ampnet.blockchainapiservice.model.result.ClientInfo
import com.ampnet.blockchainapiservice.model.result.Erc20BalanceRequest
import com.ampnet.blockchainapiservice.model.result.FullErc20BalanceRequest
import com.ampnet.blockchainapiservice.repository.ClientInfoRepository
import com.ampnet.blockchainapiservice.repository.Erc20BalanceRequestRepository
import com.ampnet.blockchainapiservice.util.BlockName
import com.ampnet.blockchainapiservice.util.Erc20Balance
import com.ampnet.blockchainapiservice.util.SignedMessage
import com.ampnet.blockchainapiservice.util.Status
import com.ampnet.blockchainapiservice.util.WalletAddress
import mu.KLogging
import org.kethereum.crypto.signedMessageToKey
import org.kethereum.crypto.toAddress
import org.kethereum.model.SignatureData
import org.springframework.stereotype.Service
import java.math.BigInteger
import java.util.UUID

@Service
class Erc20BalanceRequestServiceImpl( // TODO test
    private val uuidProvider: UuidProvider,
    private val blockchainService: BlockchainService,
    private val clientInfoRepository: ClientInfoRepository,
    private val erc20BalanceRequestRepository: Erc20BalanceRequestRepository
) : Erc20BalanceRequestService {

    companion object : KLogging() {
        private const val EIP_191_MAGIC_BYTE = 0x19.toByte()
        private const val SIGNATURE_LENGTH = 132
        private const val R_START = 2
        private const val R_END = 66
        private const val S_START = 66
        private const val S_END = 130
        private const val V_START = 130
        private const val V_END = 132
        private const val HEX_RADIX = 16
        private val V_OFFSET = BigInteger.valueOf(27L)
    }

    override fun createErc20BalanceRequest(params: CreateErc20BalanceRequestParams): Erc20BalanceRequest {
        logger.info { "Creating ERC20 balance request, params: $params" }

        val clientInfo = params.getClientInfo()
        val id = uuidProvider.getUuid()
        val databaseParams = StoreErc20BalanceRequestParams.fromCreateParams(id, params, clientInfo)

        return erc20BalanceRequestRepository.store(databaseParams)
    }

    override fun getErc20BalanceRequest(id: UUID, rpcSpec: RpcUrlSpec): FullErc20BalanceRequest {
        logger.debug { "Fetching ERC20 balance request, id: $id, rpcSpec: $rpcSpec" }

        val erc20BalanceRequest = erc20BalanceRequestRepository.getById(id)
            ?: throw ResourceNotFoundException("Balance check request not found for ID: $id")
        val balance = erc20BalanceRequest.actualWalletAddress?.let {
            blockchainService.fetchErc20AccountBalance(
                chainSpec = ChainSpec(
                    chainId = erc20BalanceRequest.chainId,
                    rpcSpec = rpcSpec
                ),
                contractAddress = erc20BalanceRequest.tokenAddress,
                walletAddress = it,
                blockParameter = erc20BalanceRequest.blockNumber ?: BlockName.LATEST
            )
        }
        val status = erc20BalanceRequest.determineStatus(balance)

        return FullErc20BalanceRequest.fromErc20BalanceRequest(
            request = erc20BalanceRequest,
            status = status,
            balance = balance
        )
    }

    override fun attachWalletAddressAndSignedMessage(
        id: UUID,
        walletAddress: WalletAddress,
        signedMessage: SignedMessage
    ) {
        logger.info {
            "Attach walletAddress and signedMessage to ERC20 balance request, id: $id, walletAddress: $walletAddress," +
                " signedMessage: $signedMessage"
        }

        val attached = erc20BalanceRequestRepository.setSignedMessage(id, walletAddress, signedMessage)

        if (attached.not()) {
            throw CannotAttachSignedMessageException(
                "Unable to attach signed message to ERC20 balance request with ID: $id"
            )
        }
    }

    private fun CreateErc20BalanceRequestParams.getClientInfo(): ClientInfo =
        if (this.clientId != null) {
            logger.debug { "Fetching info for clientId: $clientId" }
            clientInfoRepository.getById(this.clientId) ?: throw NonExistentClientIdException(this.clientId)
        } else {
            ClientInfo.EMPTY
        }

    private fun Erc20BalanceRequest.determineStatus(balance: Erc20Balance?): Status =
        if (balance == null || this.signedMessage == null) {
            Status.PENDING
        } else if (isSuccess()) {
            Status.SUCCESS
        } else {
            Status.FAILED
        }

    private fun Erc20BalanceRequest.isSuccess(): Boolean {
        val signatureData = this.signedMessage?.let { getSignatureData(it.value) }
        val eip919 = generateEip191Message(this.messageToSign.toByteArray())
        val publicKey = signatureData?.let { signedMessageToKey(eip919, it) }
        val signatureAddress = publicKey?.let { WalletAddress(it.toAddress().toString()) }

        return walletAddressMatches(this.requestedWalletAddress, this.actualWalletAddress) &&
            signatureAddress != null && // signature is valid
            signatureAddress == this.actualWalletAddress // signature wallet matches
    }

    private fun walletAddressMatches(requestedAddress: WalletAddress?, actualAddress: WalletAddress?): Boolean =
        requestedAddress == null || requestedAddress == actualAddress

    private fun generateEip191Message(message: ByteArray): ByteArray =
        byteArrayOf(EIP_191_MAGIC_BYTE) + ("Ethereum Signed Message:\n" + message.size).toByteArray() + message

    private fun BigInteger.withVOffset(): BigInteger =
        if (this == BigInteger.ZERO || this == BigInteger.ONE) {
            this + V_OFFSET
        } else {
            this
        }

    private fun getSignatureData(signature: String): SignatureData? =
        if (signature.length != SIGNATURE_LENGTH) null else try {
            val r = signature.substring(R_START, R_END)
            val s = signature.substring(S_START, S_END)
            val v = signature.substring(V_START, V_END)

            SignatureData(
                r = BigInteger(r, HEX_RADIX),
                s = BigInteger(s, HEX_RADIX),
                v = BigInteger(v, HEX_RADIX).withVOffset()
            )
        } catch (ex: NumberFormatException) {
            null
        }
}
